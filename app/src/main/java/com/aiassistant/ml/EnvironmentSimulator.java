package com.aiassistant.ml;

import android.util.Log;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * EnvironmentSimulator — Dyna-Q style simulated environment for model-based RL.
 *
 * Uses a learned {@link TransitionModel} to generate synthetic (s, a, r, s')
 * transitions that can augment real experience replay without needing actual
 * environment steps.
 *
 * Features:
 *   1. Synthetic rollout generation — produce N synthetic transitions from any
 *      seed state by sampling actions and predicting next states.
 *   2. Reward model — separate linear reward predictor r(s, a) trained from
 *      observed (s, a, r) triples.
 *   3. Terminal predictor — binary classifier estimating P(done | s, a).
 *   4. Dyna-Q loop — after each real step, generate K synthetic steps and
 *      inject them into a provided MemoryReplayBuffer.
 *   5. Model uncertainty gate — only inject synthetic steps when ensemble
 *      uncertainty is below a threshold (prevents garbage data).
 *   6. Statistics: synthetic step count, avg reward error, uncertainty histogram.
 *
 * Thread-safe.
 */
public class EnvironmentSimulator {

    private static final String TAG = "EnvSimulator";

    // ─────────────────────────────────────────────────────────────────────────
    // Sub-models
    // ─────────────────────────────────────────────────────────────────────────
    private final TransitionModel  transitionModel;

    // Reward model: linear r = W_r · [s, one_hot(a)] + b_r
    private final int              stateDim;
    private final int              actionDim;
    private final float[][]        Wr;   // [1][stateDim + actionDim]
    private final float[]          br;   // [1]
    private final NeuralNetworkOptimizer rewardOptimiser;

    // Terminal predictor: logistic sigmoid on linear output
    private final float[][]        Wt;
    private final float[]          bt;
    private final NeuralNetworkOptimizer termOptimiser;

    // Uncertainty gate
    private final float maxUncertainty;

    // ─────────────────────────────────────────────────────────────────────────
    // Stats
    // ─────────────────────────────────────────────────────────────────────────
    private final AtomicInteger syntheticSteps  = new AtomicInteger(0);
    private final AtomicInteger rejectedByGate  = new AtomicInteger(0);
    private final AtomicInteger rewardUpdates   = new AtomicInteger(0);
    private float avgRewardError = 0f;

    private final Random rng = new Random(61L);

    // ─────────────────────────────────────────────────────────────────────────
    // Construction
    // ─────────────────────────────────────────────────────────────────────────

    public EnvironmentSimulator(int stateDim, int actionDim,
                                 float maxUncertainty) {
        this.stateDim       = stateDim;
        this.actionDim      = actionDim;
        this.maxUncertainty = maxUncertainty;
        this.transitionModel = new TransitionModel(stateDim, actionDim);
        this.rewardOptimiser = new NeuralNetworkOptimizer(1e-3f);
        this.termOptimiser   = new NeuralNetworkOptimizer(1e-3f);

        int inputDim = stateDim + actionDim;
        float s = (float) Math.sqrt(2.0 / inputDim);
        Wr = new float[1][inputDim];
        br = new float[1];
        Wt = new float[1][inputDim];
        bt = new float[1];
        for (int j = 0; j < inputDim; j++) {
            Wr[0][j] = (rng.nextFloat() * 2f - 1f) * s;
            Wt[0][j] = (rng.nextFloat() * 2f - 1f) * s;
        }
    }

    public EnvironmentSimulator(int stateDim, int actionDim) {
        this(stateDim, actionDim, 2.0f);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Training the models from real data
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Update all internal models from one real (s, a, r, s', done) transition.
     */
    public synchronized void learn(float[] state, int action, float reward,
                                    float[] nextState, boolean done) {
        // Transition model
        transitionModel.update(state, action, nextState);

        // Reward model (MSE)
        float[] inp  = buildInput(state, action);
        float   rPred = Wr[0][0] * inp[0];
        for (int j = 0; j < inp.length; j++) rPred += Wr[0][j] * inp[j];
        rPred += br[0];
        float rErr = rPred - reward;
        float[][] dWr = new float[1][inp.length];
        for (int j = 0; j < inp.length; j++) dWr[0][j] = 2f * rErr * inp[j];
        rewardOptimiser.step("rwd_W", Wr, dWr);
        avgRewardError = 0.95f * avgRewardError + 0.05f * Math.abs(rErr);
        rewardUpdates.incrementAndGet();

        // Terminal predictor (cross-entropy)
        float logit = 0;
        for (int j = 0; j < inp.length; j++) logit += Wt[0][j] * inp[j];
        logit += bt[0];
        float prob   = sigmoid(logit);
        float target = done ? 1f : 0f;
        float tErr   = prob - target;
        float[][] dWt = new float[1][inp.length];
        for (int j = 0; j < inp.length; j++) dWt[0][j] = tErr * inp[j];
        termOptimiser.step("term_W", Wt, dWt);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Synthetic rollout generation
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Generate up to {@code steps} synthetic transitions starting from {@code seedState}.
     * Actions are sampled uniformly (can be replaced with a policy).
     * Stops early on predicted terminal.
     *
     * @return List of synthetic (s, a, r, s', done) arrays: float[state+1+1+state+1].
     */
    public synchronized List<float[]> generateRollout(float[] seedState, int steps) {
        List<float[]> rollout = new ArrayList<>();
        float[] state = seedState.clone();

        for (int t = 0; t < steps; t++) {
            int action = rng.nextInt(actionDim);

            // Uncertainty gate
            float[][] pu     = transitionModel.predictWithUncertainty(state, action);
            float     varSum = 0f;
            for (float v : pu[1]) varSum += v;
            float uncertainty = varSum / Math.max(1, pu[1].length);

            if (uncertainty > maxUncertainty) {
                rejectedByGate.incrementAndGet();
                break;
            }

            float[]  nextState = pu[0]; // ensemble mean
            float    reward    = predictReward(state, action);
            boolean  done      = predictDone(state, action);

            float[] record = new float[stateDim + 3 + stateDim];
            System.arraycopy(state, 0, record, 0, Math.min(stateDim, state.length));
            record[stateDim]     = action;
            record[stateDim + 1] = reward;
            record[stateDim + 2] = done ? 1f : 0f;
            System.arraycopy(nextState, 0, record, stateDim + 3, Math.min(stateDim, nextState.length));

            rollout.add(record);
            syntheticSteps.incrementAndGet();

            if (done) break;
            state = nextState;
        }
        return rollout;
    }

    /**
     * Dyna-Q: generate K synthetic steps from real buffer seeds and inject
     * them back into the replay buffer.
     *
     * @param buffer  The replay buffer to inject into.
     * @param seeds   Real states to use as rollout seeds.
     * @param kSteps  Number of synthetic steps per seed.
     */
    public synchronized void dynaQUpdate(MemoryReplayBuffer buffer,
                                          List<float[]> seeds, int kSteps) {
        for (float[] seed : seeds) {
            List<float[]> rollout = generateRollout(seed, kSteps);
            for (float[] rec : rollout) {
                float[] s  = new float[stateDim];
                float[] ns = new float[stateDim];
                System.arraycopy(rec, 0,                  s,  0, stateDim);
                System.arraycopy(rec, stateDim + 3, ns, 0, stateDim);
                int   a    = (int) rec[stateDim];
                float r    = rec[stateDim + 1];
                boolean d  = rec[stateDim + 2] > 0.5f;
                buffer.add(s, a, r, ns, d);
            }
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Monitoring
    // ─────────────────────────────────────────────────────────────────────────

    public Map<String, Object> getStats() {
        Map<String, Object> s = new HashMap<>();
        s.put("syntheticSteps",   syntheticSteps.get());
        s.put("rejectedByGate",   rejectedByGate.get());
        s.put("rewardUpdates",    rewardUpdates.get());
        s.put("avgRewardError",   avgRewardError);
        s.put("maxUncertainty",   maxUncertainty);
        s.put("transitionModel",  transitionModel.getStats());
        return s;
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Helpers
    // ─────────────────────────────────────────────────────────────────────────

    private float predictReward(float[] state, int action) {
        float[] inp = buildInput(state, action);
        float r = br[0];
        for (int j = 0; j < inp.length; j++) r += Wr[0][j] * inp[j];
        return r;
    }

    private boolean predictDone(float[] state, int action) {
        float[] inp   = buildInput(state, action);
        float   logit = bt[0];
        for (int j = 0; j < inp.length; j++) logit += Wt[0][j] * inp[j];
        return sigmoid(logit) > 0.5f;
    }

    private float[] buildInput(float[] state, int action) {
        float[] inp = new float[stateDim + actionDim];
        int sdim = Math.min(state.length, stateDim);
        System.arraycopy(state, 0, inp, 0, sdim);
        if (action >= 0 && action < actionDim) inp[stateDim + action] = 1f;
        return inp;
    }

    private static float sigmoid(float x) {
        return 1f / (1f + (float) Math.exp(-x));
    }
}
