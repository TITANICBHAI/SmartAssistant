package com.aiassistant.ml;

import android.util.Log;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

/**
 * IntrinsicMotivation — unified intrinsic reward module combining multiple
 * exploration bonuses into a single configurable bonus r_i.
 *
 * Supported components (any subset can be enabled):
 *
 *   COUNT_BASED     — β_c / √N(s)   (visit-count novelty)
 *                     Uses StateHasher for approximate count.
 *
 *   PREDICTION_ERROR— β_p · ||φ(s') − φ̂(s')||²
 *                     Delegates to CuriosityModule (ICM).
 *
 *   RANDOM_NETWORK  — β_r · ||f_target(s) − f_predictor(s)||²
 *                     RND: a fixed random target network + a trained predictor.
 *                     Novelty = prediction error of the fast learner.
 *
 *   ENTROPY_BONUS   — β_e · H(π(·|s))
 *                     Directly adds policy entropy as an exploration bonus.
 *                     Requires the agent to supply the entropy value.
 *
 * Combined bonus:
 *   r_i = β_c·count_bonus + β_p·icm_bonus + β_r·rnd_bonus + β_e·entropy_bonus
 *
 * Thread-safe.
 */
public class IntrinsicMotivation {

    private static final String TAG = "IntrinsicMotivation";

    // ─────────────────────────────────────────────────────────────────────────
    // Component flags & scales
    // ─────────────────────────────────────────────────────────────────────────
    private float betaCount;
    private float betaPrediction;
    private float betaRnd;
    private float betaEntropy;

    // ─────────────────────────────────────────────────────────────────────────
    // Sub-modules
    // ─────────────────────────────────────────────────────────────────────────
    private final StateHasher     stateHasher;
    private final CuriosityModule curiosity;

    // RND: fixed random target + trained predictor (both single linear layers)
    private final int     rndStateDim;
    private final int     rndOutDim;
    private final float[][] rndTargetW;   // fixed random [rndOutDim][rndStateDim]
    private final float[][] rndPredW;    // trained       [rndOutDim][rndStateDim]
    private final float[]   rndTargetB, rndPredB;
    private final NeuralNetworkOptimizer rndOptimiser;
    private double rndRunMean = 0.0, rndRunM2 = 0.0;
    private long   rndRunCount = 0;

    // ─────────────────────────────────────────────────────────────────────────
    // Stats
    // ─────────────────────────────────────────────────────────────────────────
    private final AtomicLong callCount = new AtomicLong(0);
    private float avgBonus       = 0f;
    private float avgCountBonus  = 0f;
    private float avgIcmBonus    = 0f;
    private float avgRndBonus    = 0f;
    private float avgEntBonus    = 0f;

    private final java.util.Random rng = new java.util.Random(19L);

    // ─────────────────────────────────────────────────────────────────────────
    // Construction
    // ─────────────────────────────────────────────────────────────────────────

    public IntrinsicMotivation(int stateDim, int actionDim,
                                float betaCount, float betaPrediction,
                                float betaRnd, float betaEntropy) {
        this.betaCount      = betaCount;
        this.betaPrediction = betaPrediction;
        this.betaRnd        = betaRnd;
        this.betaEntropy    = betaEntropy;

        this.stateHasher  = new StateHasher(stateDim, 64, 31L);
        this.curiosity    = new CuriosityModule(stateDim, actionDim);

        // RND layers
        this.rndStateDim = stateDim;
        this.rndOutDim   = Math.min(64, stateDim);
        float s = (float) Math.sqrt(2.0 / (stateDim + rndOutDim));
        rndTargetW  = xavierMat(rndOutDim, stateDim, s);
        rndTargetB  = new float[rndOutDim];
        rndPredW    = xavierMat(rndOutDim, stateDim, s * 0.5f); // predictor starts close
        rndPredB    = new float[rndOutDim];
        rndOptimiser = new NeuralNetworkOptimizer(1e-3f);

        Log.i(TAG, "IntrinsicMotivation: stateDim=" + stateDim + " actionDim=" + actionDim
                + " βc=" + betaCount + " βp=" + betaPrediction
                + " βr=" + betaRnd  + " βe=" + betaEntropy);
    }

    /** Enable only count-based and ICM bonuses (typical lightweight setup). */
    public IntrinsicMotivation(int stateDim, int actionDim) {
        this(stateDim, actionDim, 0.05f, 0.1f, 0.0f, 0.01f);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Core API
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Compute the combined intrinsic reward for transition (s, a, s').
     * Also updates internal models (curiosity, RND predictor, visit counts).
     *
     * @param state      Current state features.
     * @param action     Action taken.
     * @param nextState  Next state features.
     * @param policyEntropy H(π(·|s)) — pass 0 if not using ENTROPY_BONUS.
     * @return Total intrinsic bonus r_i ≥ 0.
     */
    public synchronized float computeBonus(float[] state, int action,
                                            float[] nextState, float policyEntropy) {
        float countBonus = 0f, icmBonus = 0f, rndBonus = 0f, entBonus = 0f;

        // ── COUNT-BASED ────────────────────────────────────────────────────
        if (betaCount > 0f) {
            int visits = stateHasher.recordVisit(state);
            countBonus  = betaCount / (float) Math.sqrt(visits);
        }

        // ── ICM PREDICTION ERROR ───────────────────────────────────────────
        if (betaPrediction > 0f) {
            icmBonus = curiosity.computeIntrinsicReward(state, action, nextState);
            curiosity.update(state, action, nextState);
        }

        // ── RND ────────────────────────────────────────────────────────────
        if (betaRnd > 0f) {
            rndBonus = betaRnd * rndBonus(state);
        }

        // ── ENTROPY BONUS ──────────────────────────────────────────────────
        if (betaEntropy > 0f) {
            entBonus = betaEntropy * policyEntropy;
        }

        float total = countBonus + icmBonus + rndBonus + entBonus;

        // EMA stats
        float a = 0.99f;
        avgCountBonus  = a * avgCountBonus  + (1 - a) * countBonus;
        avgIcmBonus    = a * avgIcmBonus    + (1 - a) * icmBonus;
        avgRndBonus    = a * avgRndBonus    + (1 - a) * rndBonus;
        avgEntBonus    = a * avgEntBonus    + (1 - a) * entBonus;
        avgBonus       = a * avgBonus       + (1 - a) * total;

        callCount.incrementAndGet();
        return total;
    }

    /** Convenience — no entropy bonus. */
    public float computeBonus(float[] state, int action, float[] nextState) {
        return computeBonus(state, action, nextState, 0f);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Scale setters (for dynamic tuning)
    // ─────────────────────────────────────────────────────────────────────────
    public void setBetaCount(float b)      { this.betaCount      = b; }
    public void setBetaPrediction(float b) { this.betaPrediction = b; }
    public void setBetaRnd(float b)        { this.betaRnd        = b; }
    public void setBetaEntropy(float b)    { this.betaEntropy    = b; }

    // ─────────────────────────────────────────────────────────────────────────
    // Stats
    // ─────────────────────────────────────────────────────────────────────────

    public synchronized Map<String, Object> getStats() {
        Map<String, Object> s = new HashMap<>();
        s.put("callCount",    callCount.get());
        s.put("avgBonus",     avgBonus);
        s.put("avgCount",     avgCountBonus);
        s.put("avgIcm",       avgIcmBonus);
        s.put("avgRnd",       avgRndBonus);
        s.put("avgEntropy",   avgEntBonus);
        s.put("betaCount",    betaCount);
        s.put("betaIcm",      betaPrediction);
        s.put("betaRnd",      betaRnd);
        s.put("betaEntropy",  betaEntropy);
        s.put("hasher",       stateHasher.getStats());
        s.put("curiosity",    curiosity.getStats());
        return s;
    }

    // ─────────────────────────────────────────────────────────────────────────
    // RND implementation
    // ─────────────────────────────────────────────────────────────────────────

    private float rndBonus(float[] state) {
        float[] tOut = linRelu(rndTargetW, rndTargetB, state);
        float[] pOut = linRelu(rndPredW,   rndPredB,   state);

        // Prediction error (unnormalised)
        float mse = 0f;
        for (int i = 0; i < rndOutDim; i++) { float d = pOut[i] - tOut[i]; mse += d * d; }
        mse /= rndOutDim;

        // Welford normalisation of MSE
        rndRunCount++;
        double d  = mse - rndRunMean;
        rndRunMean += d / rndRunCount;
        rndRunM2   += d * (mse - rndRunMean);
        double std  = rndRunCount < 2 ? 1.0 : Math.sqrt(rndRunM2 / (rndRunCount - 1));
        float normMse = std < 1e-8 ? 0f : (float)((mse - rndRunMean) / std);

        // Update predictor (gradient descent on MSE)
        float[][] dW = new float[rndOutDim][rndStateDim];
        int sdim = Math.min(state.length, rndStateDim);
        for (int i = 0; i < rndOutDim; i++) {
            float grad = 2f * (pOut[i] - tOut[i]) / rndOutDim;
            for (int j = 0; j < sdim; j++) dW[i][j] = grad * state[j];
        }
        rndOptimiser.step("rnd_pred", rndPredW, dW);

        return Math.max(0f, 0.5f + normMse * 0.5f); // mapped to [0,1]
    }

    private float[] linRelu(float[][] W, float[] b, float[] inp) {
        float[] out = new float[W.length];
        int sdim = Math.min(inp.length, rndStateDim);
        for (int i = 0; i < W.length; i++) {
            float sum = b[i];
            for (int j = 0; j < sdim; j++) sum += W[i][j] * inp[j];
            out[i] = Math.max(0f, sum);
        }
        return out;
    }

    private float[][] xavierMat(int rows, int cols, float s) {
        float[][] m = new float[rows][cols];
        for (int i = 0; i < rows; i++)
            for (int j = 0; j < cols; j++) m[i][j] = (rng.nextFloat() * 2f - 1f) * s;
        return m;
    }
}
