package com.aiassistant.ml;

import android.content.Context;
import android.util.Log;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

/**
 * DQNAgent — full Deep Q-Network agent integrating:
 *   - MemoryReplayBuffer (Prioritized Experience Replay)
 *   - NeuralNetworkOptimizer (Adam)
 *   - AdaptiveExplorationStrategy (meta-bandit)
 *   - CuriosityModule (intrinsic reward)
 *   - TemporalDifferenceTracker (training diagnostics)
 *   - TargetNetwork with periodic hard copy (Double DQN)
 *
 * Network architecture:
 *   Online:  state → [hiddenDim] → [hiddenDim/2] → actionDim   (used for action selection)
 *   Target:  frozen copy of online network, synced every targetUpdateFreq steps
 *
 * Training loop (called via trainStep()):
 *   1. Sample batch from PER buffer
 *   2. Compute Double DQN targets: y = r + γ · Q_target(s', argmax_a Q_online(s',a))
 *   3. TD errors → optimizer step + PER priority update
 *   4. Add intrinsic reward from CuriosityModule; update Curiosity model
 *   5. Feed TD errors to TemporalDifferenceTracker
 *
 * All weight storage uses float[][] matrices compatible with NeuralNetworkOptimizer.
 * Thread-safe training via a dedicated background executor.
 */
public class DQNAgent {

    private static final String TAG = "DQNAgent";

    // -------------------------------------------------------------------------
    // Network topology
    // -------------------------------------------------------------------------
    private final int stateDim;
    private final int hiddenDim;
    private final int actionDim;

    // ---- Online network weights (3 layers) ----
    private final float[][] W1, W2, W3;
    private final float[]   B1, B2, B3;

    // ---- Target network weights (hard copy of online) ----
    private float[][] tW1, tW2, tW3;
    private float[]   tB1, tB2, tB3;

    // -------------------------------------------------------------------------
    // Sub-systems
    // -------------------------------------------------------------------------
    private final MemoryReplayBuffer       replayBuffer;
    private final NeuralNetworkOptimizer   optimizer;
    private final AdaptiveExplorationStrategy exploration;
    private final CuriosityModule          curiosity;
    private final TemporalDifferenceTracker tdTracker;

    // -------------------------------------------------------------------------
    // Hyper-parameters
    // -------------------------------------------------------------------------
    private final float  gamma;
    private final int    batchSize;
    private final int    minReplaySize;
    private final int    targetUpdateFreq;   // hard copy every N optimizer steps
    private final float  intrinsicScale;    // weight of curiosity reward

    // -------------------------------------------------------------------------
    // State
    // -------------------------------------------------------------------------
    private final AtomicInteger trainStepCount  = new AtomicInteger(0);
    private final AtomicInteger episodeCount    = new AtomicInteger(0);
    private final AtomicLong    totalReward     = new AtomicLong(0);
    private final AtomicBoolean training        = new AtomicBoolean(false);

    private float avgExtrinsicReward = 0f;
    private float avgLoss            = 0f;

    private final ExecutorService trainExecutor = Executors.newSingleThreadExecutor();

    private final java.util.Random rng = new java.util.Random(42L);

    // -------------------------------------------------------------------------
    // Construction
    // -------------------------------------------------------------------------

    public DQNAgent(int stateDim, int hiddenDim, int actionDim,
                    float gamma, int batchSize, int replayCapacity,
                    int targetUpdateFreq, float intrinsicScale, float lr) {
        this.stateDim        = stateDim;
        this.hiddenDim       = hiddenDim;
        this.actionDim       = actionDim;
        this.gamma           = gamma;
        this.batchSize       = batchSize;
        this.minReplaySize   = batchSize * 4;
        this.targetUpdateFreq = targetUpdateFreq;
        this.intrinsicScale  = intrinsicScale;

        // Xavier init
        float s1 = scale(stateDim, hiddenDim);
        float s2 = scale(hiddenDim, hiddenDim / 2);
        float s3 = scale(hiddenDim / 2, actionDim);

        W1 = xavierMat(hiddenDim,     stateDim,      s1);
        B1 = new float[hiddenDim];
        W2 = xavierMat(hiddenDim / 2, hiddenDim,     s2);
        B2 = new float[hiddenDim / 2];
        W3 = xavierMat(actionDim,     hiddenDim / 2, s3);
        B3 = new float[actionDim];

        // Target network starts as a copy
        syncTargetNetwork();

        this.replayBuffer = new MemoryReplayBuffer(replayCapacity);
        this.optimizer    = new NeuralNetworkOptimizer(lr);
        this.exploration  = new AdaptiveExplorationStrategy(actionDim);
        this.curiosity    = new CuriosityModule(stateDim, actionDim);
        this.tdTracker    = new TemporalDifferenceTracker();

        Log.i(TAG, "DQNAgent: stateDim=" + stateDim + " hiddenDim=" + hiddenDim
                + " actionDim=" + actionDim + " replayCapacity=" + replayCapacity);
    }

    /** Convenience constructor with sensible defaults. */
    public DQNAgent(int stateDim, int actionDim) {
        this(stateDim, 128, actionDim, 0.99f, 32, 10000, 500, 0.1f, 1e-3f);
    }

    // -------------------------------------------------------------------------
    // Action selection (online)
    // -------------------------------------------------------------------------

    /**
     * Select an action using the online network + exploration strategy.
     */
    public synchronized int selectAction(float[] state) {
        float[] qValues = onlineForward(state);
        return exploration.selectAction(qValues);
    }

    /**
     * Store a transition and optionally trigger a background train step.
     */
    public void observe(float[] state, int action, float reward,
                         float[] nextState, boolean done) {
        // Compute intrinsic reward and add to extrinsic
        float inr = curiosity.computeIntrinsicReward(state, action, nextState);
        float totalR = reward + intrinsicScale * inr;

        replayBuffer.add(state, action, totalR, nextState, done);

        avgExtrinsicReward = 0.99f * avgExtrinsicReward + 0.01f * reward;

        if (done) {
            exploration.onEpisodeEnd(avgExtrinsicReward);
            episodeCount.incrementAndGet();
        }

        // Background train step (non-blocking)
        if (replayBuffer.isReady(minReplaySize) && !training.get()) {
            trainAsync();
        }
    }

    // -------------------------------------------------------------------------
    // Training
    // -------------------------------------------------------------------------

    /**
     * Perform one synchronous training step.  Returns TD error mean, or 0 if buffer not ready.
     */
    public synchronized float trainStep() {
        if (!replayBuffer.isReady(minReplaySize)) return 0f;

        List<MemoryReplayBuffer.Experience> batch = replayBuffer.sample(batchSize);
        if (batch.isEmpty()) return 0f;

        int   n        = batch.size();
        float totalLoss = 0f;
        int[] indices   = new int[n];
        float[] tdErrors = new float[n];

        for (int i = 0; i < n; i++) {
            MemoryReplayBuffer.Experience exp = batch.get(i);

            // Double DQN target:
            //   a* = argmax_a Q_online(s', a)
            //   y  = r + γ · Q_target(s', a*)
            float[] onlineQsp = onlineForward(exp.nextState);
            int     aBest     = argmax(onlineQsp);
            float[] targetQsp = targetForward(exp.nextState);
            float   target    = exp.done
                    ? exp.reward
                    : exp.reward + gamma * targetQsp[aBest];

            float[] onlineQs  = onlineForward(exp.state);
            float   predicted = onlineQs[exp.action];
            float   tdError   = target - predicted;

            indices[i]  = exp.bufferIdx;
            tdErrors[i] = tdError;

            // Compute loss-weighted gradients and apply
            float isW = exp.isWeight;
            backpropStep(exp.state, exp.action, tdError * isW);

            totalLoss += tdError * tdError * isW;
        }

        // Update PER priorities
        replayBuffer.updatePriorities(indices, tdErrors);

        // Update curiosity model (sample random experience from batch for efficiency)
        if (!batch.isEmpty()) {
            MemoryReplayBuffer.Experience sample = batch.get(rng.nextInt(batch.size()));
            curiosity.update(sample.state, sample.action, sample.nextState);
        }

        // TD tracker
        tdTracker.recordBatch(tdErrors, null);

        // Update exploration feedback
        for (MemoryReplayBuffer.Experience exp : batch) {
            exploration.recordActionOutcome(exp.action, exp.reward, exp.reward > 0);
        }

        // Sync target network periodically
        int step = trainStepCount.incrementAndGet();
        if (step % targetUpdateFreq == 0) {
            syncTargetNetwork();
            Log.d(TAG, "Target network synced at step " + step);
        }

        float meanLoss = totalLoss / n;
        avgLoss = 0.95f * avgLoss + 0.05f * meanLoss;
        return meanLoss;
    }

    private void trainAsync() {
        if (!training.compareAndSet(false, true)) return;
        trainExecutor.execute(() -> {
            try { trainStep(); }
            finally { training.set(false); }
        });
    }

    // -------------------------------------------------------------------------
    // Backpropagation (manual chain-rule through 3-layer ReLU network)
    // -------------------------------------------------------------------------

    private void backpropStep(float[] state, int action, float weightedTdError) {
        // Forward pass — save activations for backprop
        float[] h1   = linear(W1, B1, state, true);
        float[] h2   = linear(W2, B2, h1,    true);
        float[] qOut = linear(W3, B3, h2,    false);

        // dL/d(Q_action) = −2 * weightedTdError  (gradient of MSE through chosen action only)
        float[] dQOut = new float[actionDim];
        dQOut[action] = -2f * weightedTdError;

        // Layer 3 gradients
        float[][] dW3 = new float[actionDim][hiddenDim / 2];
        float[]   dB3 = new float[actionDim];
        for (int o = 0; o < actionDim; o++) {
            dB3[o] = dQOut[o];
            for (int j = 0; j < hiddenDim / 2; j++) dW3[o][j] = dQOut[o] * h2[j];
        }

        // Backprop to h2 (through W3, ReLU)
        float[] dh2 = new float[hiddenDim / 2];
        for (int j = 0; j < hiddenDim / 2; j++) {
            if (h2[j] <= 0) continue; // ReLU
            for (int o = 0; o < actionDim; o++) dh2[j] += dQOut[o] * W3[o][j];
        }

        // Layer 2 gradients
        float[][] dW2 = new float[hiddenDim / 2][hiddenDim];
        float[]   dB2 = new float[hiddenDim / 2];
        for (int j = 0; j < hiddenDim / 2; j++) {
            dB2[j] = dh2[j];
            for (int k = 0; k < hiddenDim; k++) dW2[j][k] = dh2[j] * h1[k];
        }

        // Backprop to h1
        float[] dh1 = new float[hiddenDim];
        int dim2 = Math.min(hiddenDim, W2[0].length);
        for (int k = 0; k < hiddenDim; k++) {
            if (h1[k] <= 0) continue; // ReLU
            for (int j = 0; j < hiddenDim / 2; j++) dh1[k] += dh2[j] * W2[j][k];
        }

        // Layer 1 gradients
        float[][] dW1 = new float[hiddenDim][stateDim];
        float[]   dB1 = new float[hiddenDim];
        int dim1 = Math.min(state.length, stateDim);
        for (int k = 0; k < hiddenDim; k++) {
            dB1[k] = dh1[k];
            for (int s = 0; s < dim1; s++) dW1[k][s] = dh1[k] * state[s];
        }

        // Adam updates
        optimizer.step("dqn_W3", W3, dW3);
        optimizer.step("dqn_W2", W2, dW2);
        optimizer.step("dqn_W1", W1, dW1);
    }

    // -------------------------------------------------------------------------
    // Forward passes
    // -------------------------------------------------------------------------

    private float[] onlineForward(float[] state) {
        float[] h1 = linear(W1, B1, state, true);
        float[] h2 = linear(W2, B2, h1,   true);
        return         linear(W3, B3, h2,   false);
    }

    private float[] targetForward(float[] state) {
        float[] h1 = linear(tW1, tB1, state, true);
        float[] h2 = linear(tW2, tB2, h1,    true);
        return         linear(tW3, tB3, h2,   false);
    }

    /** Hard copy online → target. */
    private synchronized void syncTargetNetwork() {
        tW1 = deepCopy(W1); tB1 = B1.clone();
        tW2 = deepCopy(W2); tB2 = B2.clone();
        tW3 = deepCopy(W3); tB3 = B3.clone();
    }

    // -------------------------------------------------------------------------
    // Monitoring
    // -------------------------------------------------------------------------

    public Map<String, Object> getStats() {
        Map<String, Object> s = new HashMap<>();
        s.put("trainSteps",       trainStepCount.get());
        s.put("episodeCount",     episodeCount.get());
        s.put("avgLoss",          avgLoss);
        s.put("avgExtrinsicReward", avgExtrinsicReward);
        s.put("replaySize",       replayBuffer.size());
        s.put("explorationStrategy", exploration.getCurrentStrategy().name());
        s.put("epsilon",          exploration.getCurrentEpsilon());
        s.put("tdTracker",        tdTracker.getStats());
        s.put("curiosity",        curiosity.getStats());
        return s;
    }

    public void shutdown() { trainExecutor.shutdownNow(); }

    // -------------------------------------------------------------------------
    // Math helpers
    // -------------------------------------------------------------------------

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

    private static int argmax(float[] v) {
        int best = 0;
        for (int i = 1; i < v.length; i++) if (v[i] > v[best]) best = i;
        return best;
    }

    private static float scale(int in, int out) {
        return (float) Math.sqrt(2.0 / (in + out));
    }

    private float[][] xavierMat(int rows, int cols, float s) {
        float[][] m = new float[rows][cols];
        for (int i = 0; i < rows; i++)
            for (int j = 0; j < cols; j++)
                m[i][j] = (rng.nextFloat() * 2f - 1f) * s;
        return m;
    }

    private static float[][] deepCopy(float[][] src) {
        float[][] dst = new float[src.length][];
        for (int i = 0; i < src.length; i++) dst[i] = src[i].clone();
        return dst;
    }
}
