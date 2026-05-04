package com.aiassistant.ml;

import android.util.Log;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * ReturnEstimator — multi-method trajectory return computation for RL agents.
 *
 * Provides five return estimators that trade off bias vs. variance:
 *
 *   MONTE_CARLO      — G_t = Σ_{k≥t} γ^{k-t} r_k            (zero bias, high var)
 *   TD_1             — G_t = r_t + γ·V(s_{t+1})              (high bias, zero var)
 *   TD_LAMBDA        — G_t = (1-λ) Σ_{n≥1} λ^{n-1} G_t^n    (interpolated)
 *   GAE              — A_t = Σ_{l≥0} (γλ)^l δ_{t+l}         (Schulman 2015 GAE)
 *   VTRACE           — V-trace target (off-policy, Espeholt 2018)
 *
 * Also provides:
 *   - Reward normalization (running Welford mean/std)
 *   - Return clipping
 *   - Discounted cumulative sum utility
 *
 * Thread-safe.
 */
public class ReturnEstimator {

    private static final String TAG = "ReturnEstimator";

    public enum Method { MONTE_CARLO, TD_1, TD_LAMBDA, GAE, VTRACE }

    // ─────────────────────────────────────────────────────────────────────────
    // Config
    // ─────────────────────────────────────────────────────────────────────────
    private final Method method;
    private final float  gamma;
    private final float  lambda;      // for TD_LAMBDA and GAE
    private final float  rhoClip;     // for V-TRACE
    private final float  cClip;
    private final boolean normalizeRewards;
    private final float  returnClip;  // clip returns to [-returnClip, returnClip], 0 = off

    // Running reward stats (Welford)
    private double rwMean = 0, rwM2 = 0;
    private long   rwN    = 0;

    private final AtomicInteger computeCount = new AtomicInteger(0);
    private float avgReturn = 0f;

    // ─────────────────────────────────────────────────────────────────────────
    // Construction
    // ─────────────────────────────────────────────────────────────────────────

    public ReturnEstimator(Method method, float gamma, float lambda,
                            float rhoClip, float cClip,
                            boolean normalizeRewards, float returnClip) {
        this.method           = method;
        this.gamma            = gamma;
        this.lambda           = lambda;
        this.rhoClip          = rhoClip;
        this.cClip            = cClip;
        this.normalizeRewards = normalizeRewards;
        this.returnClip       = returnClip;
        Log.i(TAG, "ReturnEstimator: " + method + " γ=" + gamma + " λ=" + lambda);
    }

    public ReturnEstimator(Method method, float gamma, float lambda) {
        this(method, gamma, lambda, 1.0f, 1.0f, true, 10f);
    }

    public ReturnEstimator() {
        this(Method.GAE, 0.99f, 0.95f);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Core computation
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Compute per-step returns for a trajectory.
     *
     * @param rewards    r_0 … r_{T-1}
     * @param values     V(s_0) … V(s_T)   (length T+1; values[T]=0 for terminal)
     * @param dones      done_0 … done_{T-1}
     * @param logPis     log π(a|s)   (only used for V-TRACE)
     * @param logMus     log μ(a|s)   (only used for V-TRACE)
     * @return Returns G_0 … G_{T-1}
     */
    public synchronized float[] compute(float[] rewards, float[] values,
                                         boolean[] dones,
                                         float[] logPis, float[] logMus) {
        int T = rewards.length;
        if (normalizeRewards) rewards = normalizeRewards(rewards);

        switch (method) {
            case MONTE_CARLO: return monteCarlo(rewards, dones);
            case TD_1:        return td1(rewards, values, dones);
            case TD_LAMBDA:   return tdLambda(rewards, values, dones);
            case VTRACE:      return vTrace(rewards, values, dones, logPis, logMus);
            case GAE:
            default:          return gae(rewards, values, dones);
        }
    }

    /** Convenience: compute without IS ratios (for on-policy methods). */
    public synchronized float[] compute(float[] rewards, float[] values, boolean[] dones) {
        return compute(rewards, values, dones, new float[rewards.length], new float[rewards.length]);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Estimator implementations
    // ─────────────────────────────────────────────────────────────────────────

    private float[] monteCarlo(float[] rewards, boolean[] dones) {
        int T = rewards.length;
        float[] G = new float[T];
        float g = 0;
        for (int t = T - 1; t >= 0; t--) {
            g = rewards[t] + (dones[t] ? 0f : gamma * g);
            G[t] = clip(g);
        }
        updateStats(G);
        return G;
    }

    private float[] td1(float[] rewards, float[] values, boolean[] dones) {
        int T = rewards.length;
        float[] G = new float[T];
        for (int t = 0; t < T; t++) {
            float nextV = (t + 1 < values.length && !dones[t]) ? values[t + 1] : 0f;
            G[t] = clip(rewards[t] + gamma * nextV);
        }
        updateStats(G);
        return G;
    }

    private float[] tdLambda(float[] rewards, float[] values, boolean[] dones) {
        int T = rewards.length;
        float[] G = new float[T];
        float g = (T < values.length) ? values[T] : 0f;
        for (int t = T - 1; t >= 0; t--) {
            float nextV = (t + 1 < values.length && !dones[t]) ? values[t + 1] : 0f;
            g = rewards[t] + (dones[t] ? 0f : gamma * (lambda * g + (1f - lambda) * nextV));
            G[t] = clip(g);
        }
        updateStats(G);
        return G;
    }

    private float[] gae(float[] rewards, float[] values, boolean[] dones) {
        int T = rewards.length;
        float[] adv = new float[T];
        float gae   = 0f;
        for (int t = T - 1; t >= 0; t--) {
            float nextV = (t + 1 < values.length && !dones[t]) ? values[t + 1] : 0f;
            float delta = rewards[t] + gamma * nextV - values[t];
            gae = delta + (dones[t] ? 0f : gamma * lambda * gae);
            adv[t] = clip(gae);
        }
        updateStats(adv);
        return adv;
    }

    private float[] vTrace(float[] rewards, float[] values, boolean[] dones,
                            float[] logPis, float[] logMus) {
        int T = rewards.length;
        float[] rhos   = new float[T];
        float[] cs     = new float[T];
        float[] deltas = new float[T];

        for (int t = 0; t < T; t++) {
            float ratio = (float) Math.exp(logPis[t] - logMus[t]);
            rhos[t]   = Math.min(rhoClip, ratio);
            cs[t]     = Math.min(cClip,   ratio);
            float nextV = (t + 1 < values.length && !dones[t]) ? values[t + 1] : 0f;
            deltas[t] = rhos[t] * (rewards[t] + gamma * nextV - values[t]);
        }

        float[] targets = new float[T];
        float acc = 0;
        for (int t = T - 1; t >= 0; t--) {
            targets[t] = values[t] + deltas[t] + (t < T - 1 ? gamma * cs[t] * acc : 0f);
            acc        = deltas[t] + (t < T - 1 ? gamma * cs[t] * acc : 0f);
            targets[t] = clip(targets[t]);
        }
        updateStats(targets);
        return targets;
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Utilities
    // ─────────────────────────────────────────────────────────────────────────

    /** Normalize an array of values using running statistics. */
    public synchronized float[] normalizeArray(float[] v) {
        double mean = 0, m2 = 0;
        for (float x : v) { double d = x - mean; mean += d / v.length; m2 += d * (x - mean); }
        double std = Math.sqrt(m2 / v.length + 1e-8);
        float[] out = new float[v.length];
        for (int i = 0; i < v.length; i++) out[i] = (float)((v[i] - mean) / std);
        return out;
    }

    /** Discounted cumulative sum: G_t = r_t + γ·G_{t+1} */
    public static float[] discountedCumSum(float[] rewards, float gamma) {
        float[] G = new float[rewards.length];
        float g = 0;
        for (int t = rewards.length - 1; t >= 0; t--) { g = rewards[t] + gamma * g; G[t] = g; }
        return G;
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Stats
    // ─────────────────────────────────────────────────────────────────────────

    public synchronized Map<String, Object> getStats() {
        Map<String, Object> s = new HashMap<>();
        s.put("method",       method.name());
        s.put("gamma",        gamma);
        s.put("lambda",       lambda);
        s.put("computeCount", computeCount.get());
        s.put("avgReturn",    avgReturn);
        return s;
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Helpers
    // ─────────────────────────────────────────────────────────────────────────

    private float clip(float v) {
        if (returnClip <= 0) return v;
        return Math.max(-returnClip, Math.min(returnClip, v));
    }

    private float[] normalizeRewards(float[] rewards) {
        float[] out = new float[rewards.length];
        for (int i = 0; i < rewards.length; i++) {
            rwN++;
            double d = rewards[i] - rwMean; rwMean += d / rwN; rwM2 += d * (rewards[i] - rwMean);
            double std = rwN < 2 ? 1.0 : Math.sqrt(rwM2 / (rwN - 1));
            out[i] = std < 1e-8 ? 0f : (float)((rewards[i] - rwMean) / std);
        }
        return out;
    }

    private void updateStats(float[] G) {
        float sum = 0; for (float g : G) sum += g;
        avgReturn = 0.99f * avgReturn + 0.01f * (G.length > 0 ? sum / G.length : 0f);
        computeCount.incrementAndGet();
    }
}
