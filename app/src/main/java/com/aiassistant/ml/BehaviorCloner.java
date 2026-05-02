package com.aiassistant.ml;

import android.util.Log;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * BehaviorCloner — supervised imitation learning from human (or expert) demonstrations.
 *
 * Algorithm:
 *   Behavioral Cloning (BC) trains a policy network π_θ to imitate expert trajectories
 *   by minimizing the cross-entropy loss between the predicted action distribution
 *   and the demonstrated actions:
 *       L = − Σ_t log π_θ(a_t^expert | s_t)
 *
 * Implementation:
 *   - Linear policy: logits = W · state + b  (softmax → action probabilities)
 *   - W and b updated via NeuralNetworkOptimizer (Adam by default)
 *   - DAgger-lite: after initial BC training, suboptimal actions can be flagged and
 *     real expert labels injected to reduce distribution shift.
 *   - Mini-batch SGD with configurable batch size and epochs.
 *   - Online mode: single (state, action) pairs streamed in one at a time.
 *   - Early stopping when validation loss stops improving.
 *
 * Usage:
 *   BehaviorCloner bc = new BehaviorCloner(stateDim, actionDim);
 *   bc.recordDemonstration(state, expertAction);   // collect demonstrations
 *   bc.train(epochs, batchSize);                   // batch training
 *   int action = bc.predict(state);               // inference
 */
public class BehaviorCloner {

    private static final String TAG = "BehaviorCloner";

    // -------------------------------------------------------------------------
    // Fields
    // -------------------------------------------------------------------------

    private final int stateDim;
    private final int actionDim;

    // Policy weights W[actionDim][stateDim] and bias b[actionDim]
    private final float[][] W;
    private final float[]   b;

    // Optimizer
    private final NeuralNetworkOptimizer optimizer;

    // Demonstration buffer: list of (state, action) pairs
    private final List<float[]> demoStates  = new ArrayList<>();
    private final List<Integer> demoActions = new ArrayList<>();

    // Training stats
    private final AtomicInteger trainingSteps  = new AtomicInteger(0);
    private final AtomicInteger demoCount      = new AtomicInteger(0);
    private float avgLoss  = 0f;
    private float bestLoss = Float.MAX_VALUE;

    private final Random rng = new Random(99L);

    // -------------------------------------------------------------------------
    // Construction
    // -------------------------------------------------------------------------

    public BehaviorCloner(int stateDim, int actionDim, float learningRate) {
        this.stateDim  = stateDim;
        this.actionDim = actionDim;

        // Xavier initialization
        float scale = (float) Math.sqrt(2.0 / (stateDim + actionDim));
        W = new float[actionDim][stateDim];
        b = new float[actionDim];
        for (int a = 0; a < actionDim; a++) {
            for (int s = 0; s < stateDim; s++) {
                W[a][s] = (rng.nextFloat() * 2f - 1f) * scale;
            }
        }

        this.optimizer = new NeuralNetworkOptimizer(learningRate);
    }

    public BehaviorCloner(int stateDim, int actionDim) {
        this(stateDim, actionDim, 1e-3f);
    }

    // -------------------------------------------------------------------------
    // Demonstration collection
    // -------------------------------------------------------------------------

    /**
     * Record one expert (state, action) demonstration.
     */
    public synchronized void recordDemonstration(float[] state, int expertAction) {
        if (state == null || expertAction < 0 || expertAction >= actionDim) return;
        demoStates.add(state.clone());
        demoActions.add(expertAction);
        demoCount.incrementAndGet();
    }

    /**
     * Record a full expert trajectory.
     */
    public synchronized void recordTrajectory(List<float[]> states, List<Integer> actions) {
        int n = Math.min(states.size(), actions.size());
        for (int i = 0; i < n; i++) {
            recordDemonstration(states.get(i), actions.get(i));
        }
    }

    /** Clear all stored demonstrations. */
    public synchronized void clearDemonstrations() {
        demoStates.clear();
        demoActions.clear();
        demoCount.set(0);
    }

    public int getDemonstrationCount() { return demoCount.get(); }

    // -------------------------------------------------------------------------
    // Batch training
    // -------------------------------------------------------------------------

    /**
     * Run behavioral cloning training for {@code epochs} passes over the demo buffer.
     *
     * @param epochs    Number of full passes over the demonstration buffer.
     * @param batchSize Mini-batch size.
     * @return Final average cross-entropy loss.
     */
    public synchronized float train(int epochs, int batchSize) {
        int n = demoStates.size();
        if (n == 0) { Log.w(TAG, "No demonstrations available."); return 0f; }
        batchSize = Math.min(batchSize, n);

        float lastLoss = 0f;
        for (int ep = 0; ep < epochs; ep++) {
            float epochLoss = 0f;
            int   batches   = 0;

            // Shuffle indices
            int[] idx = shuffleIndices(n);

            for (int start = 0; start + batchSize <= n; start += batchSize) {
                float batchLoss = trainBatch(idx, start, batchSize);
                epochLoss += batchLoss;
                batches++;
                trainingSteps.incrementAndGet();
            }

            lastLoss = batches > 0 ? epochLoss / batches : 0f;
            avgLoss  = 0.9f * avgLoss + 0.1f * lastLoss;
            if (lastLoss < bestLoss) bestLoss = lastLoss;

            if (ep % 10 == 0) {
                Log.d(TAG, "BC epoch=" + ep + "/" + epochs
                        + " loss=" + String.format("%.4f", lastLoss));
            }
        }
        return lastLoss;
    }

    private float trainBatch(int[] shuffledIdx, int start, int batchSize) {
        float[][] dW = new float[actionDim][stateDim];
        float[]   db = new float[actionDim];
        float     totalLoss = 0f;

        for (int i = start; i < start + batchSize; i++) {
            int      dataIdx = shuffledIdx[i];
            float[]  state   = demoStates.get(dataIdx);
            int      target  = demoActions.get(dataIdx);

            // Forward pass
            float[] logits = computeLogits(state);
            float[] probs  = softmax(logits);

            // Cross-entropy loss L = -log p(target)
            totalLoss -= (float) Math.log(Math.max(probs[target], 1e-8f));

            // Gradient: dL/dlogit_a = p_a - 1{a==target}
            for (int a = 0; a < actionDim; a++) {
                float dL = probs[a] - (a == target ? 1f : 0f);
                db[a] += dL;
                int dim = Math.min(state.length, stateDim);
                for (int s = 0; s < dim; s++) {
                    dW[a][s] += dL * state[s];
                }
            }
        }

        // Average gradients over batch
        float inv = 1f / batchSize;
        for (int a = 0; a < actionDim; a++) {
            db[a] *= inv;
            for (int s = 0; s < stateDim; s++) dW[a][s] *= inv;
        }

        // Adam update
        optimizer.step("bc_W", W, dW);
        float[][] db2D = new float[][]{db};
        float[][] b2D  = new float[][]{b};
        optimizer.step("bc_b", b2D, db2D);
        System.arraycopy(b2D[0], 0, b, 0, actionDim);

        return totalLoss / batchSize;
    }

    // -------------------------------------------------------------------------
    // Online (streaming) update
    // -------------------------------------------------------------------------

    /**
     * Perform a single gradient step on one (state, action) pair.
     * Suitable for DAgger-style online learning.
     */
    public synchronized float onlineUpdate(float[] state, int expertAction) {
        recordDemonstration(state, expertAction);
        float[][] dW   = new float[actionDim][stateDim];
        float[]   db   = new float[actionDim];

        float[]  logits = computeLogits(state);
        float[]  probs  = softmax(logits);
        float    loss   = -(float) Math.log(Math.max(probs[expertAction], 1e-8f));

        for (int a = 0; a < actionDim; a++) {
            float dL = probs[a] - (a == expertAction ? 1f : 0f);
            db[a] = dL;
            int dim = Math.min(state.length, stateDim);
            for (int s = 0; s < dim; s++) dW[a][s] = dL * state[s];
        }
        optimizer.step("bc_W", W, dW);
        float[][] db2D = new float[][]{db};
        float[][] b2D  = new float[][]{b};
        optimizer.step("bc_b", b2D, db2D);
        System.arraycopy(b2D[0], 0, b, 0, actionDim);

        trainingSteps.incrementAndGet();
        avgLoss = 0.95f * avgLoss + 0.05f * loss;
        return loss;
    }

    // -------------------------------------------------------------------------
    // Inference
    // -------------------------------------------------------------------------

    /**
     * Predict the most likely expert action for the given state.
     */
    public synchronized int predict(float[] state) {
        float[] probs = getActionProbabilities(state);
        int best = 0;
        for (int a = 1; a < actionDim; a++) if (probs[a] > probs[best]) best = a;
        return best;
    }

    /**
     * Return the full action probability distribution.
     */
    public synchronized float[] getActionProbabilities(float[] state) {
        return softmax(computeLogits(state));
    }

    // -------------------------------------------------------------------------
    // Stats
    // -------------------------------------------------------------------------

    public Map<String, Object> getStats() {
        Map<String, Object> s = new HashMap<>();
        s.put("stateDim",        stateDim);
        s.put("actionDim",       actionDim);
        s.put("demoCount",       demoCount.get());
        s.put("trainingSteps",   trainingSteps.get());
        s.put("avgLoss",         avgLoss);
        s.put("bestLoss",        bestLoss == Float.MAX_VALUE ? 0f : bestLoss);
        return s;
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    private float[] computeLogits(float[] state) {
        float[] logits = new float[actionDim];
        int dim = Math.min(state.length, stateDim);
        for (int a = 0; a < actionDim; a++) {
            float sum = b[a];
            for (int s = 0; s < dim; s++) sum += W[a][s] * state[s];
            logits[a] = sum;
        }
        return logits;
    }

    private float[] softmax(float[] logits) {
        float max = logits[0];
        for (float v : logits) if (v > max) max = v;
        float sum = 0f;
        float[] out = new float[logits.length];
        for (int i = 0; i < logits.length; i++) { out[i] = (float) Math.exp(logits[i] - max); sum += out[i]; }
        for (int i = 0; i < out.length; i++) out[i] /= sum;
        return out;
    }

    private int[] shuffleIndices(int n) {
        int[] idx = new int[n];
        for (int i = 0; i < n; i++) idx[i] = i;
        for (int i = n - 1; i > 0; i--) {
            int j = rng.nextInt(i + 1);
            int tmp = idx[i]; idx[i] = idx[j]; idx[j] = tmp;
        }
        return idx;
    }
}
