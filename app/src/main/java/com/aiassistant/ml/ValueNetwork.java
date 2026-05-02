package com.aiassistant.ml;

import android.util.Log;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * ValueNetwork — state-value function V(s) approximator for actor-critic.
 *
 * Architecture:  state → [W1,b1,ReLU] → [W2,b2,ReLU] → [W3,b3] → scalar V(s)
 *
 * Features:
 *   - MSE loss: L = (V(s) − target)²
 *   - Value clipping (PPO2-style): clip(V(s), V_old±ε) to avoid huge value updates
 *   - TD(λ) target: target = r + γ·V(s')  or  Monte-Carlo return G
 *   - Batch update with gradient averaging
 *   - Running normalisation of targets to reduce training instability
 *   - Returns explained variance: 1 − Var(target−V) / Var(target)
 */
public class ValueNetwork {

    private static final String TAG = "ValueNetwork";

    // ── dimensions ────────────────────────────────────────────────────────────
    private final int stateDim;
    private final int hiddenDim;

    // ── weights ───────────────────────────────────────────────────────────────
    private final float[][] W1, W2, W3;
    private final float[]   B1, B2, B3;

    // ── optimiser ─────────────────────────────────────────────────────────────
    private final NeuralNetworkOptimizer optimiser;

    // ── target normalisation (Welford) ────────────────────────────────────────
    private double normMean = 0.0, normM2 = 0.0;
    private long   normCount = 0L;
    private boolean normaliseTargets;

    // ── value clipping ────────────────────────────────────────────────────────
    private float clipEpsilon;

    // ── stats ──────────────────────────────────────────────────────────────────
    private final AtomicInteger updateCount = new AtomicInteger(0);
    private float avgLoss = 0f;
    private double targetSumSq = 0.0, residualSumSq = 0.0;
    private int    evN = 0;

    private final Random rng = new Random(77L);

    // ─────────────────────────────────────────────────────────────────────────
    // Construction
    // ─────────────────────────────────────────────────────────────────────────

    public ValueNetwork(int stateDim, int hiddenDim, float lr,
                        float clipEpsilon, boolean normaliseTargets) {
        this.stateDim          = stateDim;
        this.hiddenDim         = hiddenDim;
        this.clipEpsilon       = clipEpsilon;
        this.normaliseTargets  = normaliseTargets;
        this.optimiser         = new NeuralNetworkOptimizer(lr);

        float s1 = scale(stateDim, hiddenDim);
        float s2 = scale(hiddenDim, hiddenDim);
        float s3 = scale(hiddenDim, 1);

        W1 = xavier(hiddenDim, stateDim,  s1); B1 = new float[hiddenDim];
        W2 = xavier(hiddenDim, hiddenDim, s2); B2 = new float[hiddenDim];
        W3 = xavier(1,         hiddenDim, s3); B3 = new float[1];
    }

    public ValueNetwork(int stateDim) {
        this(stateDim, 128, 1e-3f, 0.2f, true);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Inference
    // ─────────────────────────────────────────────────────────────────────────

    /** Return V(s). */
    public synchronized float getValue(float[] state) {
        float[] h1 = lin(W1, B1, state, true);
        float[] h2 = lin(W2, B2, h1,    true);
        return     lin(W3, B3, h2,   false)[0];
    }

    /** Batch V(s) for an array of states. */
    public synchronized float[] getValues(float[][] states) {
        float[] out = new float[states.length];
        for (int i = 0; i < states.length; i++) out[i] = getValue(states[i]);
        return out;
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Training
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Single-sample MSE update.
     *
     * @param state      Current state.
     * @param target     TD or MC return target.
     * @param valuePrev  V(s) from the previous pass (for clipping). Pass NaN to skip clipping.
     * @return Scalar MSE loss.
     */
    public synchronized float update(float[] state, float target, float valuePrev) {
        float normTarget = normaliseTargets ? normalise(target) : target;

        // Forward
        float[] h1  = lin(W1, B1, state, true);
        float[] h2  = lin(W2, B2, h1,    true);
        float   vPred = lin(W3, B3, h2, false)[0];

        // Clipped value
        float vClipped = vPred;
        if (!Float.isNaN(valuePrev)) {
            vClipped = valuePrev + Math.max(-clipEpsilon, Math.min(clipEpsilon, vPred - valuePrev));
        }

        // Loss = max(mse_unclipped, mse_clipped)
        float errU = vPred    - normTarget;
        float errC = vClipped - normTarget;
        float err  = Math.abs(errU) >= Math.abs(errC) ? errU : errC;
        float loss = err * err;

        // dL/dV = 2·err
        float dV = 2f * err;

        // Backprop W3
        float[][] dW3 = new float[1][hiddenDim];
        float[]   dB3 = new float[]{dV};
        for (int j = 0; j < hiddenDim; j++) dW3[0][j] = dV * h2[j];

        // Backprop h2 (ReLU)
        float[] dh2 = new float[hiddenDim];
        for (int j = 0; j < hiddenDim; j++) {
            if (h2[j] > 0f) dh2[j] = dV * W3[0][j];
        }

        // Backprop W2
        float[][] dW2 = new float[hiddenDim][hiddenDim];
        float[]   dB2 = new float[hiddenDim];
        for (int j = 0; j < hiddenDim; j++) {
            dB2[j] = dh2[j];
            for (int k = 0; k < hiddenDim; k++) dW2[j][k] = dh2[j] * h1[k];
        }

        // Backprop h1 (ReLU)
        float[] dh1 = new float[hiddenDim];
        for (int k = 0; k < hiddenDim; k++) {
            if (h1[k] > 0f) for (int j = 0; j < hiddenDim; j++) dh1[k] += dh2[j] * W2[j][k];
        }

        // Backprop W1
        float[][] dW1 = new float[hiddenDim][stateDim];
        float[]   dB1 = new float[hiddenDim];
        int sdim = Math.min(state.length, stateDim);
        for (int k = 0; k < hiddenDim; k++) {
            dB1[k] = dh1[k];
            for (int s = 0; s < sdim; s++) dW1[k][s] = dh1[k] * state[s];
        }

        optimiser.step("vn_W3", W3, dW3);
        optimiser.step("vn_W2", W2, dW2);
        optimiser.step("vn_W1", W1, dW1);

        // Explained variance tracking
        double tBar = target - (normCount > 0 ? normMean : 0);
        targetSumSq  += tBar * tBar;
        residualSumSq += (vPred - target) * (vPred - target);
        evN++;

        avgLoss = 0.95f * avgLoss + 0.05f * loss;
        updateCount.incrementAndGet();
        return loss;
    }

    /** Batch MSE update — averages gradients across the batch. */
    public synchronized float batchUpdate(float[][] states, float[] targets, float[] prevValues) {
        int n = states.length;
        float total = 0f;
        for (int i = 0; i < n; i++) {
            float pv = (prevValues != null && i < prevValues.length) ? prevValues[i] : Float.NaN;
            total += update(states[i], targets[i], pv);
        }
        return n > 0 ? total / n : 0f;
    }

    /** Explained variance ∈ (-∞, 1]. 1 = perfect, 0 = no better than mean, <0 = worse. */
    public synchronized float explainedVariance() {
        if (evN < 2 || targetSumSq < 1e-10) return 0f;
        return (float)(1.0 - residualSumSq / targetSumSq);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Stats
    // ─────────────────────────────────────────────────────────────────────────

    public Map<String, Object> getStats() {
        Map<String, Object> s = new HashMap<>();
        s.put("updateCount",      updateCount.get());
        s.put("avgLoss",          avgLoss);
        s.put("explainedVariance", explainedVariance());
        s.put("stateDim",         stateDim);
        s.put("hiddenDim",        hiddenDim);
        s.put("clipEpsilon",      clipEpsilon);
        s.put("normaliseTargets", normaliseTargets);
        return s;
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Helpers
    // ─────────────────────────────────────────────────────────────────────────

    private float[] lin(float[][] W, float[] b, float[] inp, boolean relu) {
        float[] out = new float[W.length];
        for (int i = 0; i < W.length; i++) {
            float sum = b[i];
            int dim = Math.min(inp.length, W[i].length);
            for (int j = 0; j < dim; j++) sum += W[i][j] * inp[j];
            out[i] = relu ? Math.max(0f, sum) : sum;
        }
        return out;
    }

    private float normalise(float x) {
        normCount++;
        double d = x - normMean;
        normMean += d / normCount;
        normM2   += d * (x - normMean);
        double std = normCount < 2 ? 1.0 : Math.sqrt(normM2 / (normCount - 1));
        return std < 1e-8 ? 0f : (float)((x - normMean) / std);
    }

    private static float scale(int in, int out) { return (float) Math.sqrt(2.0 / (in + out)); }

    private float[][] xavier(int rows, int cols, float s) {
        float[][] m = new float[rows][cols];
        for (int i = 0; i < rows; i++)
            for (int j = 0; j < cols; j++) m[i][j] = (rng.nextFloat() * 2f - 1f) * s;
        return m;
    }
}
