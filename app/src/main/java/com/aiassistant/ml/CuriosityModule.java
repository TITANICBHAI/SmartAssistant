package com.aiassistant.ml;

import android.util.Log;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.atomic.AtomicLong;

/**
 * CuriosityModule — Intrinsic Curiosity Module (ICM) for exploration.
 *
 * Based on "Curiosity-driven Exploration by Self-Supervised Prediction"
 * (Pathak et al., 2017), simplified for on-device RL without autograd.
 *
 * Components:
 *
 *   1. Forward model:  f(φ(s_t), a_t) → φ̂(s_{t+1})
 *      Predicts the next encoded state from the current encoded state + action.
 *      Intrinsic reward r_i = ||φ(s_{t+1}) − φ̂(s_{t+1})||²  (prediction error).
 *
 *   2. Inverse model:  g(φ(s_t), φ(s_{t+1})) → â_t
 *      Predicts the action that caused the transition.
 *      Training signal for the shared feature encoder φ.
 *
 * Implementation details:
 *   - Both models are 2-layer linear networks (ReLU hidden, linear output).
 *   - φ is a separate linear projection (learned encoder).
 *   - Updates done via mini-batch gradient descent (Adam via NeuralNetworkOptimizer).
 *   - r_i is clipped to [0, MAX_INTRINSIC] and can be scaled by eta.
 *   - Running normalization keeps intrinsic rewards in [0, 1].
 *
 * Usage:
 *   CuriosityModule cm = new CuriosityModule(stateDim, encodedDim, actionDim);
 *   float intrinsicReward = cm.computeIntrinsicReward(state, action, nextState);
 *   cm.update(state, action, nextState);  // train the models
 */
public class CuriosityModule {

    private static final String TAG      = "CuriosityModule";
    private static final float  MAX_INR  = 1.0f;
    private static final float  RELU_NEG = 0.0f;

    // -------------------------------------------------------------------------
    // Dimensions
    // -------------------------------------------------------------------------
    private final int stateDim;
    private final int encodedDim;  // φ output dimension
    private final int actionDim;
    private final int hiddenDim;
    private final float eta;       // intrinsic reward scale

    // -------------------------------------------------------------------------
    // Encoder φ: stateDim → encodedDim   (linear)
    // -------------------------------------------------------------------------
    private final float[][] encW;  // [encodedDim][stateDim]
    private final float[]   encB;  // [encodedDim]

    // -------------------------------------------------------------------------
    // Forward model: [encodedDim + actionDim] → [hidden] → [encodedDim]
    // -------------------------------------------------------------------------
    private final float[][] fwdW1;  // [hiddenDim][encodedDim + actionDim]
    private final float[]   fwdB1;  // [hiddenDim]
    private final float[][] fwdW2;  // [encodedDim][hiddenDim]
    private final float[]   fwdB2;  // [encodedDim]

    // -------------------------------------------------------------------------
    // Inverse model: [encodedDim * 2] → [hidden] → [actionDim]
    // -------------------------------------------------------------------------
    private final float[][] invW1;  // [hiddenDim][encodedDim * 2]
    private final float[]   invB1;  // [hiddenDim]
    private final float[][] invW2;  // [actionDim][hiddenDim]
    private final float[]   invB2;  // [actionDim]

    // -------------------------------------------------------------------------
    // Optimizer
    // -------------------------------------------------------------------------
    private final NeuralNetworkOptimizer optimizer;

    // -------------------------------------------------------------------------
    // Running stats for normalizing intrinsic reward (Welford)
    // -------------------------------------------------------------------------
    private double runMean = 0.0, runM2 = 0.0;
    private long   runCount = 0L;

    // -------------------------------------------------------------------------
    // Monitoring
    // -------------------------------------------------------------------------
    private final AtomicLong updateCount = new AtomicLong(0L);
    private float avgFwdLoss = 0f;
    private float avgInvLoss = 0f;
    private float avgIntrinsic = 0f;

    private final Random rng = new Random(17L);

    // -------------------------------------------------------------------------
    // Construction
    // -------------------------------------------------------------------------

    public CuriosityModule(int stateDim, int encodedDim, int actionDim,
                            int hiddenDim, float eta, float lr) {
        this.stateDim   = stateDim;
        this.encodedDim = encodedDim;
        this.actionDim  = actionDim;
        this.hiddenDim  = hiddenDim;
        this.eta        = eta;
        this.optimizer  = new NeuralNetworkOptimizer(lr);

        float scEnc = scale(stateDim, encodedDim);
        float scFwd = scale(encodedDim + actionDim, encodedDim);
        float scInv = scale(encodedDim * 2, actionDim);

        encW = xavierMatrix(encodedDim, stateDim,              scEnc);
        encB = new float[encodedDim];

        fwdW1 = xavierMatrix(hiddenDim, encodedDim + actionDim, scFwd);
        fwdB1 = new float[hiddenDim];
        fwdW2 = xavierMatrix(encodedDim, hiddenDim,             scFwd);
        fwdB2 = new float[encodedDim];

        invW1 = xavierMatrix(hiddenDim, encodedDim * 2,         scInv);
        invB1 = new float[hiddenDim];
        invW2 = xavierMatrix(actionDim, hiddenDim,              scInv);
        invB2 = new float[actionDim];
    }

    /** Default constructor: encodedDim=64, hiddenDim=128, eta=0.1, lr=1e-3. */
    public CuriosityModule(int stateDim, int actionDim) {
        this(stateDim, 64, actionDim, 128, 0.1f, 1e-3f);
    }

    // -------------------------------------------------------------------------
    // Public API
    // -------------------------------------------------------------------------

    /**
     * Compute the intrinsic reward for (s, a, s') without updating weights.
     *
     * @return Normalized intrinsic reward in [0, eta].
     */
    public synchronized float computeIntrinsicReward(float[] state, int action,
                                                      float[] nextState) {
        float[] phi_s  = encode(state);
        float[] phi_sp = encode(nextState);
        float[] phi_hat = forwardPredict(phi_s, action);
        float   mse    = mse(phi_sp, phi_hat);
        float   normed = normalizeIntrinsic(mse);
        return eta * normed;
    }

    /**
     * Perform one gradient update using the (s, a, s') tuple.
     * Updates the encoder, forward model, and inverse model jointly.
     */
    public synchronized void update(float[] state, int action, float[] nextState) {
        if (state == null || nextState == null) return;

        // ---- Encode states ----
        float[] phi_s  = encode(state);
        float[] phi_sp = encode(nextState);

        // ---- Forward model update ----
        float[] phi_hat = forwardPredict(phi_s, action);
        float fwdLoss = updateForwardModel(phi_s, action, phi_sp, phi_hat);

        // ---- Inverse model update ----
        float invLoss = updateInverseModel(phi_s, phi_sp, action);

        // ---- Stats ----
        avgFwdLoss   = 0.95f * avgFwdLoss   + 0.05f * fwdLoss;
        avgInvLoss   = 0.95f * avgInvLoss   + 0.05f * invLoss;
        float inr = eta * normalizeIntrinsic(fwdLoss);
        avgIntrinsic = 0.95f * avgIntrinsic + 0.05f * inr;

        long n = updateCount.incrementAndGet();
        if (n % 100 == 0) {
            Log.d(TAG, "Update " + n + " fwdLoss=" + String.format("%.4f", avgFwdLoss)
                    + " invLoss=" + String.format("%.4f", avgInvLoss)
                    + " avgInr=" + String.format("%.4f", avgIntrinsic));
        }
    }

    // -------------------------------------------------------------------------
    // Forward pass helpers
    // -------------------------------------------------------------------------

    private float[] encode(float[] state) {
        float[] out = new float[encodedDim];
        int dim = Math.min(state.length, stateDim);
        for (int i = 0; i < encodedDim; i++) {
            float sum = encB[i];
            for (int j = 0; j < dim; j++) sum += encW[i][j] * state[j];
            out[i] = relu(sum);
        }
        return out;
    }

    private float[] forwardPredict(float[] phi_s, int action) {
        // Input: concat(phi_s, one-hot(action))
        float[] inp = new float[encodedDim + actionDim];
        System.arraycopy(phi_s, 0, inp, 0, encodedDim);
        if (action >= 0 && action < actionDim) inp[encodedDim + action] = 1f;

        float[] h   = linear(fwdW1, fwdB1, inp, true);  // hidden (ReLU)
        return linear(fwdW2, fwdB2, h, false);           // output (linear)
    }

    private float[] inversePredict(float[] phi_s, float[] phi_sp) {
        float[] inp = new float[encodedDim * 2];
        System.arraycopy(phi_s,  0, inp, 0,         encodedDim);
        System.arraycopy(phi_sp, 0, inp, encodedDim, encodedDim);
        float[] h = linear(invW1, invB1, inp, true);
        return softmax(linear(invW2, invB2, h, false));
    }

    // -------------------------------------------------------------------------
    // Gradient updates (analytical, MSE / cross-entropy)
    // -------------------------------------------------------------------------

    private float updateForwardModel(float[] phi_s, int action, float[] phi_sp, float[] phi_hat) {
        // MSE loss: L = ||phi_sp - phi_hat||²
        float mse = mse(phi_sp, phi_hat);

        // dL/dOutput_i = 2(phi_hat_i - phi_sp_i)
        float[] dOut = new float[encodedDim];
        for (int i = 0; i < encodedDim; i++) dOut[i] = 2f * (phi_hat[i] - phi_sp[i]);

        // Backprop through W2
        float[][] dW2 = new float[encodedDim][hiddenDim];
        float[]   db2 = new float[encodedDim];
        // Need hidden layer values; recompute
        float[] inp = new float[encodedDim + actionDim];
        System.arraycopy(phi_s, 0, inp, 0, encodedDim);
        if (action >= 0 && action < actionDim) inp[encodedDim + action] = 1f;
        float[] h = linear(fwdW1, fwdB1, inp, true);

        for (int i = 0; i < encodedDim; i++) {
            db2[i] = dOut[i];
            for (int j = 0; j < hiddenDim; j++) dW2[i][j] = dOut[i] * h[j];
        }

        // Backprop through W1 (ReLU)
        float[][] dW1 = new float[hiddenDim][encodedDim + actionDim];
        float[]   db1 = new float[hiddenDim];
        for (int j = 0; j < hiddenDim; j++) {
            if (h[j] <= 0) continue; // ReLU mask
            float dh = 0;
            for (int i = 0; i < encodedDim; i++) dh += dOut[i] * fwdW2[i][j];
            db1[j] = dh;
            for (int k = 0; k < encodedDim + actionDim; k++) dW1[j][k] = dh * inp[k];
        }

        optimizer.step("fwd_W2", fwdW2, dW2);
        optimizer.step("fwd_W1", fwdW1, dW1);
        return mse;
    }

    private float updateInverseModel(float[] phi_s, float[] phi_sp, int action) {
        float[] probs = inversePredict(phi_s, phi_sp);
        float loss = -(float) Math.log(Math.max(probs[action], 1e-8f));

        // Cross-entropy gradient
        float[][] dW2 = new float[actionDim][hiddenDim];
        float[]   db2 = new float[actionDim];
        float[] inp = new float[encodedDim * 2];
        System.arraycopy(phi_s,  0, inp, 0,          encodedDim);
        System.arraycopy(phi_sp, 0, inp, encodedDim, encodedDim);
        float[] h = linear(invW1, invB1, inp, true);

        for (int a = 0; a < actionDim; a++) {
            float dL = probs[a] - (a == action ? 1f : 0f);
            db2[a] = dL;
            for (int j = 0; j < hiddenDim; j++) dW2[a][j] = dL * h[j];
        }
        float[][] dW1 = new float[hiddenDim][encodedDim * 2];
        float[]   db1 = new float[hiddenDim];
        for (int j = 0; j < hiddenDim; j++) {
            if (h[j] <= 0) continue;
            float dh = 0;
            for (int a = 0; a < actionDim; a++) dh += (probs[a] - (a == action ? 1f : 0f)) * invW2[a][j];
            db1[j] = dh;
            for (int k = 0; k < encodedDim * 2; k++) dW1[j][k] = dh * inp[k];
        }

        optimizer.step("inv_W2", invW2, dW2);
        optimizer.step("inv_W1", invW1, dW1);
        return loss;
    }

    // -------------------------------------------------------------------------
    // Monitoring
    // -------------------------------------------------------------------------

    public Map<String, Object> getStats() {
        Map<String, Object> s = new HashMap<>();
        s.put("updateCount",   updateCount.get());
        s.put("avgFwdLoss",    avgFwdLoss);
        s.put("avgInvLoss",    avgInvLoss);
        s.put("avgIntrinsic",  avgIntrinsic);
        s.put("eta",           eta);
        s.put("stateDim",      stateDim);
        s.put("encodedDim",    encodedDim);
        s.put("actionDim",     actionDim);
        return s;
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    private float[] linear(float[][] W, float[] b, float[] inp, boolean applyRelu) {
        float[] out = new float[W.length];
        for (int i = 0; i < W.length; i++) {
            float sum = b[i];
            int dim = Math.min(inp.length, W[i].length);
            for (int j = 0; j < dim; j++) sum += W[i][j] * inp[j];
            out[i] = applyRelu ? relu(sum) : sum;
        }
        return out;
    }

    private static float relu(float v)  { return Math.max(RELU_NEG, v); }

    private static float mse(float[] a, float[] b) {
        float sum = 0;
        int dim = Math.min(a.length, b.length);
        for (int i = 0; i < dim; i++) { float d = a[i] - b[i]; sum += d * d; }
        return dim > 0 ? sum / dim : 0f;
    }

    private static float[] softmax(float[] v) {
        float max = v[0];
        for (float x : v) if (x > max) max = x;
        float sum = 0;
        float[] out = new float[v.length];
        for (int i = 0; i < v.length; i++) { out[i] = (float) Math.exp(v[i] - max); sum += out[i]; }
        for (int i = 0; i < v.length; i++) out[i] /= sum;
        return out;
    }

    private float normalizeIntrinsic(float rawMse) {
        // Welford update
        runCount++;
        double delta = rawMse - runMean;
        runMean += delta / runCount;
        runM2   += delta * (rawMse - runMean);
        if (runCount < 2) return Math.min(1f, rawMse);
        double std = Math.sqrt(runM2 / (runCount - 1));
        if (std < 1e-8) return 0f;
        return Math.min(MAX_INR, Math.max(0f, (float)((rawMse - runMean) / std + 0.5)));
    }

    private static float scale(int fanIn, int fanOut) {
        return (float) Math.sqrt(2.0 / (fanIn + fanOut));
    }

    private float[][] xavierMatrix(int rows, int cols, float scale) {
        float[][] m = new float[rows][cols];
        for (int i = 0; i < rows; i++)
            for (int j = 0; j < cols; j++)
                m[i][j] = (rng.nextFloat() * 2f - 1f) * scale;
        return m;
    }
}
