package com.aiassistant.ml;

import android.util.Log;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * OffPolicyCorrector — importance-sampling corrections for off-policy RL.
 *
 * When data is collected under a behaviour policy μ but used to update a target
 * policy π, the gradient estimate is biased.  This class provides:
 *
 *   IMPORTANCE_WEIGHTING  — ρ = π(a|s) / μ(a|s), clip to [0, ρ_max]
 *                            Used in standard IS / V-trace.
 *
 *   V_TRACE               — Espeholt et al. (2018) V-trace targets:
 *                           v_s = V(s) + Σ_{t≥s} γ^{t-s} · c_s…c_{t-1} · δ_t
 *                           where ρ_t = clip(π/μ, ρ̄), c_t = clip(π/μ, c̄)
 *
 *   RETRACE               — Munos et al. (2016): off-policy safe return estimator
 *                           Q^{ret}(s,a) = r + γ · Σ λ·min(c, π/μ) · δ
 *
 *   CLIPPED_IS            — Simple clip: ρ_clip = min(1, π(a|s)/μ(a|s))
 *                            Standard PPO-like correction.
 *
 * All methods:
 *   - Accept pre-computed log-probabilities (avoids recomputing the policy)
 *   - Clip ratios to prevent variance explosion
 *   - Track mean ratio, clip rate, effective sample size
 *   - Thread-safe
 */
public class OffPolicyCorrector {

    private static final String TAG = "OffPolicyCorrtor";

    public enum Method { IMPORTANCE_WEIGHTING, V_TRACE, RETRACE, CLIPPED_IS }

    // ─────────────────────────────────────────────────────────────────────────
    // Config
    // ─────────────────────────────────────────────────────────────────────────
    private final Method  method;
    private final float   rhoMax;    // ρ_max for IS / V-trace
    private final float   cMax;      // c̄ for V-trace / Retrace
    private final float   gamma;
    private final float   lambda;    // for Retrace

    // ─────────────────────────────────────────────────────────────────────────
    // Stats
    // ─────────────────────────────────────────────────────────────────────────
    private final AtomicInteger callCount   = new AtomicInteger(0);
    private final AtomicInteger clippedCount= new AtomicInteger(0);
    private float avgRatio   = 1f;
    private float avgWeight  = 1f;
    private float minRatio   = Float.MAX_VALUE;
    private float maxRatio   = 0f;

    // ─────────────────────────────────────────────────────────────────────────
    // Construction
    // ─────────────────────────────────────────────────────────────────────────

    public OffPolicyCorrector(Method method, float rhoMax, float cMax,
                               float gamma, float lambda) {
        this.method = method;
        this.rhoMax = rhoMax;
        this.cMax   = cMax;
        this.gamma  = gamma;
        this.lambda = lambda;
    }

    public OffPolicyCorrector(Method method) {
        this(method, 1.0f, 1.0f, 0.99f, 0.95f);
    }

    public OffPolicyCorrector() {
        this(Method.V_TRACE, 1.0f, 1.0f, 0.99f, 0.95f);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Core API
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Compute importance weight for a single (s, a) pair.
     *
     * @param logPiA  log π(a|s)  — target policy log-prob
     * @param logMuA  log μ(a|s)  — behaviour policy log-prob
     * @return Clipped importance weight ρ̄.
     */
    public synchronized float computeWeight(float logPiA, float logMuA) {
        float ratio = (float) Math.exp(logPiA - logMuA);
        return updateStats(ratio);
    }

    /**
     * Compute IS-corrected advantages for a trajectory.
     *
     * @param logPiAs Log π(a_t|s_t) for each step t.
     * @param logMuAs Log μ(a_t|s_t) for each step t.
     * @param advantages Raw advantage estimates A_t.
     * @return IS-corrected advantages: ρ_t · A_t (or V-trace / Retrace targets).
     */
    public synchronized float[] correctAdvantages(float[] logPiAs, float[] logMuAs,
                                                   float[] advantages) {
        int T = Math.min(logPiAs.length, Math.min(logMuAs.length, advantages.length));
        float[] out = new float[T];
        for (int t = 0; t < T; t++) {
            float rho = computeWeight(logPiAs[t], logMuAs[t]);
            out[t] = rho * advantages[t];
        }
        return out;
    }

    /**
     * Compute V-trace value targets for a trajectory.
     *
     * @param logPiAs   log π(a_t|s_t) — target policy
     * @param logMuAs   log μ(a_t|s_t) — behaviour policy
     * @param values    V(s_t) from critic
     * @param rewards   r_t
     * @param dones     done_t (1 if terminal)
     * @return V-trace target v_t for each step.
     */
    public synchronized float[] vTraceTargets(float[] logPiAs, float[] logMuAs,
                                               float[] values, float[] rewards,
                                               boolean[] dones) {
        int T = logPiAs.length;
        float[] rhos = new float[T];
        float[] cs   = new float[T];
        float[] deltas = new float[T];

        for (int t = 0; t < T; t++) {
            float ratio = (float) Math.exp(logPiAs[t] - logMuAs[t]);
            rhos[t]   = Math.min(rhoMax, ratio);
            cs[t]     = Math.min(cMax,   ratio);
            float nextV = (t < T - 1 && !dones[t]) ? values[t + 1] : 0f;
            deltas[t] = rhos[t] * (rewards[t] + gamma * nextV - values[t]);
            updateStats(ratio);
        }

        float[] targets = new float[T];
        float accumulated = 0f;
        for (int t = T - 1; t >= 0; t--) {
            targets[t]  = values[t] + deltas[t] + (t < T - 1 ? gamma * cs[t] * accumulated : 0f);
            accumulated = deltas[t] + (t < T - 1 ? gamma * cs[t] * accumulated : 0f);
        }
        callCount.addAndGet(T);
        return targets;
    }

    /**
     * Compute Retrace Q-targets.
     *
     * @param logPiAs   log π(a_t|s_t)
     * @param logMuAs   log μ(a_t|s_t)
     * @param qValues   Q(s_t, a_t) from critic
     * @param values    V(s_t) = Σ_a π(a|s)·Q(s,a)
     * @param rewards   r_t
     * @param dones     done_t
     * @return Retrace Q-targets.
     */
    public synchronized float[] retraceTargets(float[] logPiAs, float[] logMuAs,
                                                float[] qValues, float[] values,
                                                float[] rewards, boolean[] dones) {
        int T = logPiAs.length;
        float[] targets = new float[T];
        float accumulated = 0f;

        for (int t = T - 1; t >= 0; t--) {
            float ratio  = (float) Math.exp(logPiAs[t] - logMuAs[t]);
            float c      = lambda * Math.min(1f, ratio);
            float nextV  = (t < T - 1 && !dones[t]) ? values[t + 1] : 0f;
            float delta  = rewards[t] + gamma * nextV - qValues[t];
            targets[t]   = qValues[t] + delta + (t < T - 1 ? gamma * c * accumulated : 0f);
            accumulated  = delta + (t < T - 1 ? gamma * c * accumulated : 0f);
            updateStats(ratio);
        }
        callCount.addAndGet(T);
        return targets;
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Effective sample size
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Effective sample size (ESS) from a batch of IS weights.
     * ESS = (Σ w_i)² / Σ w_i²    High ESS = little IS variance.
     */
    public float effectiveSampleSize(float[] weights) {
        double sumW = 0, sumW2 = 0;
        for (float w : weights) { sumW += w; sumW2 += w * w; }
        return sumW2 > 0 ? (float)(sumW * sumW / sumW2) : 0f;
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Stats
    // ─────────────────────────────────────────────────────────────────────────

    public Map<String, Object> getStats() {
        Map<String, Object> s = new HashMap<>();
        s.put("method",       method.name());
        s.put("callCount",    callCount.get());
        s.put("clippedCount", clippedCount.get());
        s.put("clipRate",     callCount.get() > 0 ? (float) clippedCount.get() / callCount.get() : 0f);
        s.put("avgRatio",     avgRatio);
        s.put("avgWeight",    avgWeight);
        s.put("minRatio",     minRatio == Float.MAX_VALUE ? 0f : minRatio);
        s.put("maxRatio",     maxRatio);
        s.put("rhoMax",       rhoMax);
        s.put("cMax",         cMax);
        return s;
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Internal
    // ─────────────────────────────────────────────────────────────────────────

    private float updateStats(float ratio) {
        if (ratio < minRatio) minRatio = ratio;
        if (ratio > maxRatio) maxRatio = ratio;
        avgRatio = 0.99f * avgRatio + 0.01f * ratio;

        float clipped;
        switch (method) {
            case CLIPPED_IS:           clipped = Math.min(1f, ratio);  break;
            case V_TRACE:
            case IMPORTANCE_WEIGHTING: clipped = Math.min(rhoMax, ratio); break;
            case RETRACE:              clipped = lambda * Math.min(1f, ratio); break;
            default:                   clipped = ratio;
        }
        if (clipped < ratio) clippedCount.incrementAndGet();
        avgWeight = 0.99f * avgWeight + 0.01f * clipped;
        callCount.incrementAndGet();
        return clipped;
    }
}
