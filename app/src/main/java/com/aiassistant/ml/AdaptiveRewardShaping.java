package com.aiassistant.ml;

import android.util.Log;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * AdaptiveRewardShaping — dynamic potential-based reward shaping for faster RL.
 *
 * Potential-based shaping (Ng et al. 1999) guarantees policy invariance while
 * accelerating learning:
 *
 *   F(s, s') = γ·Φ(s') - Φ(s)      (shaping function)
 *   r_shaped  = r + F(s, s')
 *
 * This class maintains and adapts the potential function Φ via:
 *
 *   1. DISTANCE_TO_GOAL   — Φ(s) = -||s - goal||₂ (needs a goal state)
 *   2. LEARNED_VALUE      — Φ(s) ≈ V(s) from a critic (bootstrapped)
 *   3. PROGRESS_SIGNAL    — Φ(s) = w·s (linear progress toward good features)
 *   4. DIVERSITY_BONUS    — Φ(s) = novelty(s) scaled by visit frequency
 *
 * Adaptive elements:
 *   - Shaping strength κ anneals: κ = κ_start * exp(-decay * step)
 *   - Goal state updated online if extrinsic reward exceeds threshold
 *   - Learned potential updated via semi-gradient TD
 *
 * Thread-safe.
 */
public class AdaptiveRewardShaping {

    private static final String TAG = "AdaptiveRewardShaping";

    public enum Mode { DISTANCE_TO_GOAL, LEARNED_VALUE, PROGRESS_SIGNAL, DIVERSITY_BONUS }

    // ─────────────────────────────────────────────────────────────────────────
    // Learned potential approximator (linear + one hidden layer)
    // ─────────────────────────────────────────────────────────────────────────
    private final float[][] W1, W2;   // [hidden][state], [1][hidden]
    private final float[]   b1, b2;
    private final NeuralNetworkOptimizer potOpt;

    // ─────────────────────────────────────────────────────────────────────────
    // Fields
    // ─────────────────────────────────────────────────────────────────────────
    private final int    stateDim;
    private final int    hidDim;
    private Mode         mode;
    private float        gamma;

    // Shaping strength
    private float        kappa;         // current strength
    private final float  kappaStart;
    private final float  kappaDecay;    // per-step exponential decay

    // Goal state (for DISTANCE_TO_GOAL)
    private float[]      goal;
    private final float  goalUpdateThresh; // extrinsic reward threshold to update goal

    // Progress weights (for PROGRESS_SIGNAL)
    private final float[] progressW;

    // Diversity module reference (for DIVERSITY_BONUS)
    private NoveltyDetector noveltyDetector = null;

    // Stats
    private final AtomicInteger stepCount  = new AtomicInteger(0);
    private float avgShaping  = 0f;
    private float avgExtrinsic= 0f;
    private float maxBonus    = 0f;
    private float minBonus    = 0f;

    private final java.util.Random rng = new java.util.Random(131L);

    // ─────────────────────────────────────────────────────────────────────────
    // Construction
    // ─────────────────────────────────────────────────────────────────────────

    public AdaptiveRewardShaping(int stateDim, int hidDim, Mode mode,
                                  float gamma, float kappaStart, float kappaDecay,
                                  float goalUpdateThresh, float lr) {
        this.stateDim        = stateDim;
        this.hidDim          = hidDim;
        this.mode            = mode;
        this.gamma           = gamma;
        this.kappa           = kappaStart;
        this.kappaStart      = kappaStart;
        this.kappaDecay      = kappaDecay;
        this.goalUpdateThresh= goalUpdateThresh;
        this.potOpt          = new NeuralNetworkOptimizer(lr);
        this.progressW       = new float[stateDim];
        this.goal            = new float[stateDim];

        float s = (float) Math.sqrt(2.0 / (stateDim + hidDim));
        W1 = xav(hidDim, stateDim, s);  b1 = new float[hidDim];
        W2 = xav(1, hidDim, s);         b2 = new float[1];

        // Default progress: all features equally important
        java.util.Arrays.fill(progressW, 1f / stateDim);

        Log.i(TAG, "AdaptiveRewardShaping: mode=" + mode + " κ=" + kappaStart);
    }

    public AdaptiveRewardShaping(int stateDim, Mode mode) {
        this(stateDim, 64, mode, 0.99f, 1.0f, 5e-5f, 1.0f, 1e-3f);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Core API
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Shape the reward: r_shaped = r + F(s, s')
     *
     * @param state     s_t
     * @param nextState s_{t+1}
     * @param reward    Extrinsic reward r_t
     * @param done      Episode terminated
     * @return Shaped reward
     */
    public synchronized float shape(float[] state, float[] nextState, float reward, boolean done) {
        float phiS  = potential(state);
        float phiSp = done ? 0f : potential(nextState);
        float F     = kappa * (gamma * phiSp - phiS);

        // Update potential model
        if (mode == Mode.LEARNED_VALUE) updatePotential(state, nextState, reward, done);

        // Optionally update goal
        if (mode == Mode.DISTANCE_TO_GOAL && reward > goalUpdateThresh)
            goal = nextState.clone();

        // Anneal kappa
        kappa = kappaStart * (float) Math.exp(-kappaDecay * stepCount.get());
        kappa = Math.max(kappa, 0.01f);

        float shaped = reward + F;
        avgShaping   = 0.99f * avgShaping   + 0.01f * F;
        avgExtrinsic = 0.99f * avgExtrinsic + 0.01f * reward;
        if (F > maxBonus) maxBonus = F;
        if (F < minBonus) minBonus = F;
        stepCount.incrementAndGet();
        return shaped;
    }

    /** Set or update a reference to the novelty detector (for DIVERSITY_BONUS). */
    public synchronized void setNoveltyDetector(NoveltyDetector nd) {
        this.noveltyDetector = nd;
    }

    /** Override progress feature weights. */
    public synchronized void setProgressWeights(float[] w) {
        System.arraycopy(w, 0, progressW, 0, Math.min(w.length, stateDim));
    }

    public synchronized void setMode(Mode m) { this.mode = m; }

    // ─────────────────────────────────────────────────────────────────────────
    // Potential function
    // ─────────────────────────────────────────────────────────────────────────

    private float potential(float[] state) {
        float[] s = pad(state, stateDim);
        switch (mode) {
            case DISTANCE_TO_GOAL:
                float d = 0; for (int i = 0; i < stateDim; i++) { float dx = s[i] - goal[i]; d += dx*dx; }
                return -(float) Math.sqrt(d);

            case PROGRESS_SIGNAL:
                float prog = 0; for (int i = 0; i < stateDim; i++) prog += progressW[i] * s[i];
                return prog;

            case DIVERSITY_BONUS:
                return noveltyDetector != null ? noveltyDetector.novelty(s) : 0f;

            case LEARNED_VALUE:
            default:
                return learnedPotential(s);
        }
    }

    private float learnedPotential(float[] s) {
        float[] h = lin(W1, b1, s, true);
        return lin(W2, b2, h, false)[0];
    }

    private void updatePotential(float[] state, float[] nextState, float reward, boolean done) {
        float[] s  = pad(state, stateDim);
        float[] sp = pad(nextState, stateDim);

        float vS  = learnedPotential(s);
        float vSp = done ? 0f : learnedPotential(sp);
        float target = reward + gamma * vSp;
        float delta  = target - vS;

        float[] h  = lin(W1, b1, s, true);
        float[]  dO = {2f * delta};

        float[][] dW2 = new float[1][hidDim];
        for (int j = 0; j < hidDim; j++) dW2[0][j] = dO[0] * h[j];
        potOpt.step("pot_W2", W2, dW2);

        float[] dH = new float[hidDim];
        for (int j = 0; j < hidDim; j++) {
            if (h[j] <= 0) continue;
            dH[j] = dO[0] * W2[0][j];
        }
        float[][] dW1 = new float[hidDim][stateDim];
        for (int i = 0; i < hidDim; i++)
            for (int j = 0; j < stateDim; j++) dW1[i][j] = dH[i] * s[j];
        potOpt.step("pot_W1", W1, dW1);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Helpers
    // ─────────────────────────────────────────────────────────────────────────

    private static float[] lin(float[][] W, float[] b, float[] x, boolean relu) {
        float[] o = new float[W.length];
        for (int i = 0; i < W.length; i++) {
            float s = b[i];
            for (int j = 0; j < Math.min(x.length, W[i].length); j++) s += W[i][j] * x[j];
            o[i] = relu ? Math.max(0f, s) : s;
        }
        return o;
    }

    private static float[] pad(float[] x, int dim) {
        if (x.length == dim) return x;
        float[] p = new float[dim]; System.arraycopy(x, 0, p, 0, Math.min(x.length, dim));
        return p;
    }

    private float[][] xav(int r, int c, float s) {
        float[][] m = new float[r][c];
        for (int i = 0; i < r; i++)
            for (int j = 0; j < c; j++) m[i][j] = (rng.nextFloat()*2f-1f)*s;
        return m;
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Stats
    // ─────────────────────────────────────────────────────────────────────────

    public synchronized Map<String, Object> getStats() {
        Map<String, Object> s = new HashMap<>();
        s.put("mode",         mode.name());
        s.put("kappa",        kappa);
        s.put("stepCount",    stepCount.get());
        s.put("avgShaping",   avgShaping);
        s.put("avgExtrinsic", avgExtrinsic);
        s.put("maxBonus",     maxBonus);
        s.put("minBonus",     minBonus);
        return s;
    }
}
