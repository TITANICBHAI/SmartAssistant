package com.aiassistant.ml;

import android.util.Log;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * TransitionModel — learned forward model predicting s_{t+1} from (s_t, a_t).
 *
 * Architecture:
 *   f_θ(s, a) → ŝ'   (2-layer MLP with ReLU hidden layer)
 *
 * Input:  concat(state_features, one_hot_action)   length = stateDim + actionDim
 * Output: predicted next state features              length = stateDim
 *
 * Applications:
 *   - Model-based RL: generate synthetic rollouts for Dyna-Q style updates
 *   - ActionSequenceOptimizer.predictNextState oracle
 *   - Early termination detection (predict if done=true)
 *   - Data augmentation: generate novel (s, a, ŝ') tuples for experience replay
 *
 * Training:
 *   - Mini-batch MSE between ŝ' and actual s' from replay buffer
 *   - Adam optimizer via NeuralNetworkOptimizer
 *   - Optional uncertainty quantification: ensemble of K=5 models,
 *     epistemic uncertainty = variance of ensemble predictions
 *
 * Thread-safe.
 */
public class TransitionModel implements ActionSequenceOptimizer.QOracle {

    private static final String TAG = "TransitionModel";

    // -------------------------------------------------------------------------
    // Architecture
    // -------------------------------------------------------------------------
    private final int stateDim;
    private final int actionDim;
    private final int hiddenDim;
    private final int inputDim;   // stateDim + actionDim

    // Layer weights
    private final float[][] W1; // [hiddenDim][inputDim]
    private final float[]   B1; // [hiddenDim]
    private final float[][] W2; // [stateDim][hiddenDim]
    private final float[]   B2; // [stateDim]

    // Optional ensemble for uncertainty
    private final List<float[][]> ensembleW1;
    private final List<float[]>   ensembleB1;
    private final List<float[][]> ensembleW2;
    private final List<float[]>   ensembleB2;
    private final int ensembleSize;

    // Optimizer
    private final NeuralNetworkOptimizer optimizer;

    // Stats
    private final AtomicInteger updateCount = new AtomicInteger(0);
    private float avgMse = 0f;

    private final Random rng = new Random(23L);

    // -------------------------------------------------------------------------
    // Construction
    // -------------------------------------------------------------------------

    public TransitionModel(int stateDim, int actionDim, int hiddenDim,
                            int ensembleSize, float lr) {
        this.stateDim     = stateDim;
        this.actionDim    = actionDim;
        this.hiddenDim    = hiddenDim;
        this.inputDim     = stateDim + actionDim;
        this.ensembleSize = ensembleSize;
        this.optimizer    = new NeuralNetworkOptimizer(lr);

        float s1 = scale(inputDim, hiddenDim);
        float s2 = scale(hiddenDim, stateDim);

        W1 = xavierMat(hiddenDim, inputDim,  s1);
        B1 = new float[hiddenDim];
        W2 = xavierMat(stateDim,  hiddenDim, s2);
        B2 = new float[stateDim];

        ensembleW1 = new ArrayList<>();
        ensembleB1 = new ArrayList<>();
        ensembleW2 = new ArrayList<>();
        ensembleB2 = new ArrayList<>();

        if (ensembleSize > 1) {
            for (int k = 0; k < ensembleSize; k++) {
                ensembleW1.add(xavierMat(hiddenDim, inputDim,  s1 * (1f + 0.1f * k)));
                ensembleB1.add(new float[hiddenDim]);
                ensembleW2.add(xavierMat(stateDim,  hiddenDim, s2 * (1f + 0.1f * k)));
                ensembleB2.add(new float[stateDim]);
            }
        }
    }

    public TransitionModel(int stateDim, int actionDim) {
        this(stateDim, actionDim, 128, 5, 1e-3f);
    }

    // -------------------------------------------------------------------------
    // Prediction
    // -------------------------------------------------------------------------

    /**
     * Predict the next state given current state and action.
     */
    public synchronized float[] predictNextState(float[] state, int action) {
        float[] inp = buildInput(state, action);
        float[] h   = linear(W1, B1, inp, true);
        float[] out = linear(W2, B2, h,   false);
        // Clip to [0, 1] since states are assumed normalized
        for (int i = 0; i < out.length; i++) out[i] = Math.max(0f, Math.min(1f, out[i]));
        return out;
    }

    /**
     * Predict next states from all ensemble members and compute mean + variance.
     * Returns float[2][stateDim]: [0] = mean prediction, [1] = per-feature variance.
     */
    public synchronized float[][] predictWithUncertainty(float[] state, int action) {
        if (ensembleSize <= 1) {
            float[] pred = predictNextState(state, action);
            return new float[][]{pred, new float[stateDim]};
        }

        float[] inp = buildInput(state, action);
        float[][] preds = new float[ensembleSize][];

        for (int k = 0; k < ensembleSize; k++) {
            float[] h = linear(ensembleW1.get(k), ensembleB1.get(k), inp, true);
            preds[k]  = linear(ensembleW2.get(k), ensembleB2.get(k), h,   false);
        }

        float[] mean = new float[stateDim];
        float[] var  = new float[stateDim];

        for (float[] p : preds) for (int i = 0; i < stateDim; i++) mean[i] += p[i];
        for (int i = 0; i < stateDim; i++) mean[i] /= ensembleSize;

        for (float[] p : preds) for (int i = 0; i < stateDim; i++) {
            float d = p[i] - mean[i];
            var[i] += d * d;
        }
        for (int i = 0; i < stateDim; i++) var[i] /= ensembleSize;

        return new float[][]{mean, var};
    }

    /**
     * Generate a synthetic rollout of length {@code steps} from the given start state.
     */
    public synchronized List<float[]> rollout(float[] startState,
                                               int[] actions, int steps) {
        List<float[]> trajectory = new ArrayList<>();
        float[] state = startState.clone();
        for (int t = 0; t < Math.min(steps, actions.length); t++) {
            state = predictNextState(state, actions[t]);
            trajectory.add(state.clone());
        }
        return trajectory;
    }

    // -------------------------------------------------------------------------
    // Training
    // -------------------------------------------------------------------------

    /**
     * Update the model on a single (state, action, nextState) triple.
     * @return MSE loss.
     */
    public synchronized float update(float[] state, int action, float[] nextState) {
        if (state == null || nextState == null) return 0f;

        float[] inp   = buildInput(state, action);
        float[] h     = linear(W1, B1, inp, true);
        float[] pred  = linear(W2, B2, h,   false);

        // MSE gradients
        int dim = Math.min(pred.length, stateDim);
        float mse = 0f;
        float[] dOut = new float[stateDim];
        for (int i = 0; i < dim; i++) {
            float err  = pred[i] - (i < nextState.length ? nextState[i] : 0f);
            dOut[i]    = 2f * err;
            mse       += err * err;
        }
        mse /= dim;

        // Backprop W2
        float[][] dW2 = new float[stateDim][hiddenDim];
        float[]   dB2 = new float[stateDim];
        for (int i = 0; i < stateDim; i++) {
            dB2[i] = dOut[i];
            for (int j = 0; j < hiddenDim; j++) dW2[i][j] = dOut[i] * h[j];
        }

        // Backprop h (through ReLU)
        float[] dh = new float[hiddenDim];
        for (int j = 0; j < hiddenDim; j++) {
            if (h[j] <= 0) continue;
            for (int i = 0; i < stateDim; i++) dh[j] += dOut[i] * W2[i][j];
        }

        // Backprop W1
        float[][] dW1 = new float[hiddenDim][inputDim];
        float[]   dB1 = new float[hiddenDim];
        for (int j = 0; j < hiddenDim; j++) {
            dB1[j] = dh[j];
            for (int k = 0; k < inputDim; k++) dW1[j][k] = dh[j] * inp[k];
        }

        optimizer.step("tm_W2", W2, dW2);
        optimizer.step("tm_W1", W1, dW1);

        // Train ensemble members on the same example (slightly different update)
        for (int e = 0; e < ensembleSize; e++) {
            float[] eh  = linear(ensembleW1.get(e), ensembleB1.get(e), inp, true);
            float[] ep  = linear(ensembleW2.get(e), ensembleB2.get(e), eh,  false);
            float[] edO = new float[stateDim];
            for (int i = 0; i < dim; i++) edO[i] = 2f * (ep[i] - (i < nextState.length ? nextState[i] : 0f));
            float[][] edW2 = new float[stateDim][hiddenDim];
            float[]   edB2 = new float[stateDim];
            for (int i = 0; i < stateDim; i++) { edB2[i] = edO[i]; for (int j = 0; j < hiddenDim; j++) edW2[i][j] = edO[i] * eh[j]; }
            float[][] edW1 = new float[hiddenDim][inputDim];
            float[]   edB1 = new float[hiddenDim];
            for (int j = 0; j < hiddenDim; j++) {
                if (eh[j] <= 0) continue;
                float deh = 0; for (int i = 0; i < stateDim; i++) deh += edO[i] * ensembleW2.get(e)[i][j];
                edB1[j] = deh; for (int k = 0; k < inputDim; k++) edW1[j][k] = deh * inp[k];
            }
            optimizer.step("tm_e" + e + "_W2", ensembleW2.get(e), edW2);
            optimizer.step("tm_e" + e + "_W1", ensembleW1.get(e), edW1);
        }

        avgMse = 0.95f * avgMse + 0.05f * mse;
        updateCount.incrementAndGet();
        return mse;
    }

    // -------------------------------------------------------------------------
    // QOracle implementation (for ActionSequenceOptimizer)
    // -------------------------------------------------------------------------

    @Override
    public float q(float[] state, int action) {
        // Approximate Q = -prediction_uncertainty (lower uncertainty → more predictable → safer)
        if (ensembleSize <= 1) return 0f;
        float[][] pu = predictWithUncertainty(state, action);
        float varSum = 0f;
        for (float v : pu[1]) varSum += v;
        return -varSum; // higher Q = lower uncertainty
    }

    // -------------------------------------------------------------------------
    // Monitoring
    // -------------------------------------------------------------------------

    public Map<String, Object> getStats() {
        Map<String, Object> s = new HashMap<>();
        s.put("updateCount",   updateCount.get());
        s.put("avgMse",        avgMse);
        s.put("stateDim",      stateDim);
        s.put("actionDim",     actionDim);
        s.put("hiddenDim",     hiddenDim);
        s.put("ensembleSize",  ensembleSize);
        return s;
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    private float[] buildInput(float[] state, int action) {
        float[] inp = new float[inputDim];
        int sdim = Math.min(state.length, stateDim);
        System.arraycopy(state, 0, inp, 0, sdim);
        if (action >= 0 && action < actionDim) inp[stateDim + action] = 1f;
        return inp;
    }

    private static float[] linear(float[][] W, float[] b, float[] inp, boolean relu) {
        float[] out = new float[W.length];
        for (int i = 0; i < W.length; i++) {
            float sum = b[i];
            int dim = Math.min(inp.length, W[i].length);
            for (int j = 0; j < dim; j++) sum += W[i][j] * inp[j];
            out[i] = relu ? Math.max(0f, sum) : sum;
        }
        return out;
    }

    private static float scale(int in, int out) { return (float) Math.sqrt(2.0 / (in + out)); }

    private float[][] xavierMat(int rows, int cols, float s) {
        float[][] m = new float[rows][cols];
        for (int i = 0; i < rows; i++) for (int j = 0; j < cols; j++) m[i][j] = (rng.nextFloat() * 2f - 1f) * s;
        return m;
    }
}
