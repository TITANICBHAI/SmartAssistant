package com.aiassistant.ml;

import android.util.Log;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * SuccessorRepresentation (SR) — Dayan (1993) predictive state representation.
 *
 * The SR encodes "where will I be in the future given current state s?"
 *
 *   M(s, s') = E[ Σ_{t≥0} γ^t · 1[s_t = s'] | s_0 = s ]
 *
 * The SR enables fast reward function changes without relearning:
 *   V(s) = M(s, ·) · w   where w = reward weights
 *
 * This implementation uses linear function approximation for the SR:
 *   M(s, ·) ≈ W_SR · φ(s)   where φ(s) is a feature vector.
 *
 * Training: TD update on M:
 *   M(s) ← M(s) + α·(φ(s) + γ·M(s') - M(s))
 *
 * Applications:
 *   - Fast reward adaptation (just update w, not the full policy)
 *   - Goal-conditioned RL: plan to states with high M(s, goal)
 *   - State similarity: ||M(s) - M(s')||₂ measures reachability distance
 *
 * Thread-safe.
 */
public class SuccessorRepresentation {

    private static final String TAG = "SuccessorRepr";

    // ─────────────────────────────────────────────────────────────────────────
    // Fields
    // ─────────────────────────────────────────────────────────────────────────
    private final int    featDim;     // φ(s) dimension
    private final float  gamma;
    private final float  alpha;       // SR learning rate
    private final float  rewardLr;    // reward weight learning rate

    // SR matrix approximation: M(s) ≈ W · φ(s)   [featDim][featDim]
    private final float[][] W;        // initialized to identity (M ≈ φ in early training)
    // Reward weights: V(s) = M(s) · w
    private final float[] w;          // [featDim]

    // Feature encoder (linear): state → φ(s)
    private final float[][] encW;     // [featDim][stateDim]
    private final float[]   encB;
    private final int        stateDim;

    private final NeuralNetworkOptimizer srOpt, rewOpt;

    // Stats
    private final AtomicInteger updateCount = new AtomicInteger(0);
    private float avgSrLoss  = 0f;
    private float avgRewLoss = 0f;

    private final java.util.Random rng = new java.util.Random(173L);

    // ─────────────────────────────────────────────────────────────────────────
    // Construction
    // ─────────────────────────────────────────────────────────────────────────

    public SuccessorRepresentation(int stateDim, int featDim, float gamma,
                                    float alpha, float rewardLr, float lr) {
        this.stateDim = stateDim;
        this.featDim  = featDim;
        this.gamma    = gamma;
        this.alpha    = alpha;
        this.rewardLr = rewardLr;
        this.srOpt    = new NeuralNetworkOptimizer(lr);
        this.rewOpt   = new NeuralNetworkOptimizer(rewardLr);

        // Init W = I (perfect prediction at start: M(s) ≈ φ(s))
        W   = new float[featDim][featDim];
        for (int i = 0; i < featDim; i++) W[i][i] = 1f;
        w   = new float[featDim];

        float s = (float) Math.sqrt(2.0 / (stateDim + featDim));
        encW = new float[featDim][stateDim];
        encB = new float[featDim];
        for (int i = 0; i < featDim; i++)
            for (int j = 0; j < stateDim; j++) encW[i][j] = (rng.nextFloat()*2f-1f)*s;

        Log.i(TAG, "SuccessorRepresentation: state=" + stateDim + " feat=" + featDim);
    }

    public SuccessorRepresentation(int stateDim, int featDim) {
        this(stateDim, featDim, 0.99f, 0.05f, 0.01f, 1e-3f);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Inference
    // ─────────────────────────────────────────────────────────────────────────

    /** Encode state to feature vector φ(s). */
    public synchronized float[] encode(float[] state) {
        float[] phi = new float[featDim];
        float[] s   = pad(state, stateDim);
        for (int i = 0; i < featDim; i++) {
            phi[i] = encB[i];
            for (int j = 0; j < stateDim; j++) phi[i] += encW[i][j] * s[j];
            phi[i] = (float) Math.tanh(phi[i]);
        }
        return phi;
    }

    /** Compute M(s) = W · φ(s) — the successor representation vector. */
    public synchronized float[] successorFeature(float[] state) {
        float[] phi = encode(state);
        float[] M   = new float[featDim];
        for (int i = 0; i < featDim; i++) {
            for (int j = 0; j < featDim; j++) M[i] += W[i][j] * phi[j];
        }
        return M;
    }

    /** Estimate V(s) = M(s) · w. */
    public synchronized float value(float[] state) {
        float[] M = successorFeature(state);
        float v = 0;
        for (int i = 0; i < featDim; i++) v += M[i] * w[i];
        return v;
    }

    /**
     * Reachability distance between two states (using SR):
     *   d(s, s') = ||M(s) - M(s')||₂
     * Low distance → s' is easily reachable from s.
     */
    public synchronized float reachabilityDistance(float[] stateA, float[] stateB) {
        float[] MA = successorFeature(stateA);
        float[] MB = successorFeature(stateB);
        float d = 0;
        for (int i = 0; i < featDim; i++) { float diff = MA[i] - MB[i]; d += diff * diff; }
        return (float) Math.sqrt(d);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Training
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Update the SR matrix and reward weights given a transition.
     *
     * @param state      s_t
     * @param nextState  s_{t+1}
     * @param reward     r_t (used to update reward weight vector w)
     * @param done       Episode terminated
     */
    public synchronized void update(float[] state, float[] nextState,
                                    float reward, boolean done) {
        float[] phi  = encode(state);
        float[] phiN = done ? new float[featDim] : encode(nextState);

        // ── SR TD update: target = φ(s) + γ·M(s') ────────────────────────
        float[] M    = matVec(W, phi);
        float[] MN   = matVec(W, phiN);
        float[] target = new float[featDim];
        for (int i = 0; i < featDim; i++) target[i] = phi[i] + (done ? 0f : gamma * MN[i]);

        float srLoss = 0;
        float[][] dW = new float[featDim][featDim];
        for (int i = 0; i < featDim; i++) {
            float err = M[i] - target[i];
            srLoss += err * err;
            for (int j = 0; j < featDim; j++) dW[i][j] = err * phi[j];
        }
        srOpt.step("sr_W", W, dW);

        // ── Reward weight update: V̂(s) = M(s)·w; δ_r = r - V̂(s) ─────────
        float V      = dot(M, w);
        float rDelta = reward - V;
        float rewLoss= rDelta * rDelta;
        for (int i = 0; i < featDim; i++) w[i] += rewardLr * rDelta * M[i];

        avgSrLoss  = 0.99f * avgSrLoss  + 0.01f * srLoss;
        avgRewLoss = 0.99f * avgRewLoss + 0.01f * rewLoss;
        updateCount.incrementAndGet();
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Helpers
    // ─────────────────────────────────────────────────────────────────────────

    private static float[] matVec(float[][] M, float[] v) {
        float[] o = new float[M.length];
        for (int i = 0; i < M.length; i++)
            for (int j = 0; j < Math.min(v.length, M[i].length); j++) o[i] += M[i][j] * v[j];
        return o;
    }

    private static float dot(float[] a, float[] b) {
        float s = 0; for (int i = 0; i < Math.min(a.length, b.length); i++) s += a[i]*b[i];
        return s;
    }

    private static float[] pad(float[] x, int dim) {
        if (x.length == dim) return x;
        float[] p = new float[dim]; System.arraycopy(x, 0, p, 0, Math.min(x.length, dim));
        return p;
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Stats
    // ─────────────────────────────────────────────────────────────────────────

    public synchronized Map<String, Object> getStats() {
        Map<String, Object> s = new HashMap<>();
        s.put("updateCount", updateCount.get());
        s.put("avgSrLoss",   avgSrLoss);
        s.put("avgRewLoss",  avgRewLoss);
        s.put("featDim",     featDim);
        s.put("stateDim",    stateDim);
        s.put("gamma",       gamma);
        return s;
    }
}
