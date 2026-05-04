package com.aiassistant.ml;

import android.util.Log;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * HierarchicalRL — Options Framework (Sutton et al. 1999) for temporally-extended
 * actions in game-playing RL.
 *
 * Hierarchy:
 *   Meta-controller  : selects an Option (sub-goal) given the current state.
 *   Sub-controller   : executes primitive actions to achieve the active Option's sub-goal.
 *
 * Each Option has:
 *   - Initiation set  : I(s) — states where the option can be started (always true here)
 *   - Policy          : π_o(a|s) — linear softmax policy for primitives
 *   - Termination     : β_o(s) — probability of terminating the option in state s
 *
 * Meta-controller learns via Q-learning over options (Option-Q).
 * Sub-controllers learn via policy gradient on primitive actions.
 *
 * Benefits:
 *   - Handles long-horizon tasks by decomposing into sub-goals
 *   - Reuses sub-policies across similar contexts
 *   - Naturally produces curriculum (easy options first)
 *
 * Thread-safe.
 */
public class HierarchicalRL {

    private static final String TAG = "HierarchicalRL";

    // ─────────────────────────────────────────────────────────────────────────
    // Option definition
    // ─────────────────────────────────────────────────────────────────────────
    private static class Option {
        final int id;
        final int stateDim, actionDim;

        // Sub-policy: linear softmax  π_o(a|s) = softmax(W·s + b)
        final float[][] W;   // [actionDim][stateDim]
        final float[]   b;   // [actionDim]

        // Termination head: β_o(s) = sigmoid(w_β·s + b_β)
        final float[] wBeta;
        float bBeta;

        // Stats
        int   activations = 0;
        int   steps       = 0;
        float avgReturn   = 0f;

        Option(int id, int stateDim, int actionDim, Random rng) {
            this.id        = id;
            this.stateDim  = stateDim;
            this.actionDim = actionDim;
            float s = (float) Math.sqrt(2.0 / (stateDim + actionDim));
            W     = new float[actionDim][stateDim];
            b     = new float[actionDim];
            wBeta = new float[stateDim];
            for (int a = 0; a < actionDim; a++)
                for (int i = 0; i < stateDim; i++) W[a][i] = (rng.nextFloat()*2f-1f)*s;
            for (int i = 0; i < stateDim; i++) wBeta[i] = (rng.nextFloat()*2f-1f)*0.01f;
            bBeta = -1f; // initially hard to terminate
        }

        float[] actionLogits(float[] state) {
            float[] out = new float[actionDim];
            for (int a = 0; a < actionDim; a++) {
                out[a] = b[a];
                for (int i = 0; i < Math.min(state.length, stateDim); i++)
                    out[a] += W[a][i] * state[i];
            }
            return out;
        }

        float terminationProb(float[] state) {
            float z = bBeta;
            for (int i = 0; i < Math.min(state.length, stateDim); i++) z += wBeta[i] * state[i];
            return 1f / (1f + (float) Math.exp(-z));
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Fields
    // ─────────────────────────────────────────────────────────────────────────
    private final int        stateDim;
    private final int        actionDim;
    private final int        numOptions;
    private final Option[]   options;

    // Meta-controller Q-table over options: Q(s_hash, o)
    private final HashMap<Integer, float[]> metaQ = new HashMap<>();
    private final float metaGamma  = 0.99f;
    private final float metaAlpha  = 0.1f;
    private final float metaEps;

    // Sub-controller LR
    private final float subLr;

    // Active option tracking
    private int   activeOption   = 0;
    private float[] optionStart  = null;
    private float optionReturn   = 0f;
    private float optionDiscount = 1f;
    private int   optionSteps    = 0;

    private final AtomicInteger totalSteps    = new AtomicInteger(0);
    private final AtomicInteger optionChanges = new AtomicInteger(0);

    private final Random rng;

    // ─────────────────────────────────────────────────────────────────────────
    // Construction
    // ─────────────────────────────────────────────────────────────────────────

    public HierarchicalRL(int stateDim, int actionDim, int numOptions,
                           float metaEps, float subLr, long seed) {
        this.stateDim   = stateDim;
        this.actionDim  = actionDim;
        this.numOptions = numOptions;
        this.metaEps    = metaEps;
        this.subLr      = subLr;
        this.rng        = new Random(seed);

        options = new Option[numOptions];
        for (int o = 0; o < numOptions; o++)
            options[o] = new Option(o, stateDim, actionDim, rng);

        Log.i(TAG, "HierarchicalRL: options=" + numOptions + " state=" + stateDim
                + " actions=" + actionDim);
    }

    public HierarchicalRL(int stateDim, int actionDim, int numOptions) {
        this(stateDim, actionDim, numOptions, 0.1f, 5e-4f, 37L);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Core API
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Select the next primitive action.
     * Internally manages option selection and termination.
     *
     * @param state Current state vector.
     * @return Primitive action index.
     */
    public synchronized int selectAction(float[] state) {
        // Check if active option should terminate
        if (optionStart == null || shouldTerminate(state)) {
            activeOption = selectOption(state);
            optionStart  = state.clone();
            optionReturn = 0f;
            optionDiscount = 1f;
            optionSteps  = 0;
            options[activeOption].activations++;
            optionChanges.incrementAndGet();
        }

        // Execute sub-policy of active option
        Option opt    = options[activeOption];
        float[] logits = opt.actionLogits(state);
        float[] probs  = softmax(logits);
        int action     = sampleAction(probs);
        opt.steps++;
        optionSteps++;
        totalSteps.incrementAndGet();
        return action;
    }

    /**
     * Provide feedback after a primitive action.
     * Updates sub-policy and accumulates return for meta-update.
     *
     * @param state     State before action.
     * @param action    Primitive action taken.
     * @param reward    Extrinsic reward received.
     * @param nextState State after action.
     * @param done      Episode ended.
     */
    public synchronized void update(float[] state, int action, float reward,
                                    float[] nextState, boolean done) {
        Option opt = options[activeOption];

        // Accumulate option return
        optionReturn   += optionDiscount * reward;
        optionDiscount *= metaGamma;

        // Sub-policy gradient update
        float[] logits = opt.actionLogits(state);
        float[] probs  = softmax(logits);
        // Simple REINFORCE: push action probability up by advantage
        float baseline = optionReturn; // use running return as crude baseline
        for (int a = 0; a < actionDim; a++) {
            float g = (a == action ? 1f - probs[a] : -probs[a]) * reward;
            opt.b[a] += subLr * g;
            for (int i = 0; i < Math.min(state.length, stateDim); i++)
                opt.W[a][i] += subLr * g * state[i];
        }

        // Termination gradient: encourage early termination if reward is high
        float beta = opt.terminationProb(nextState);
        float dBeta = beta * (1f - beta) * (metaQValue(nextState, activeOption) - maxMetaQ(nextState));
        opt.bBeta -= subLr * dBeta;
        for (int i = 0; i < Math.min(nextState.length, stateDim); i++)
            opt.wBeta[i] -= subLr * dBeta * nextState[i];

        // Meta-controller Q update on option termination or episode end
        if (done || shouldTerminate(nextState)) {
            float nextMaxQ = done ? 0f : maxMetaQ(nextState);
            float target   = optionReturn + (float) Math.pow(metaGamma, optionSteps) * nextMaxQ;
            float[] qVals  = getOrInitMetaQ(stateHash(optionStart));
            float td       = target - qVals[activeOption];
            qVals[activeOption] += metaAlpha * td;
            opt.avgReturn = 0.95f * opt.avgReturn + 0.05f * optionReturn;
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Internal
    // ─────────────────────────────────────────────────────────────────────────

    private boolean shouldTerminate(float[] state) {
        return rng.nextFloat() < options[activeOption].terminationProb(state);
    }

    private int selectOption(float[] state) {
        if (rng.nextFloat() < metaEps) return rng.nextInt(numOptions);
        float[] qVals = getOrInitMetaQ(stateHash(state));
        int best = 0; float bestQ = qVals[0];
        for (int o = 1; o < numOptions; o++) if (qVals[o] > bestQ) { bestQ = qVals[o]; best = o; }
        return best;
    }

    private float metaQValue(float[] state, int option) {
        return getOrInitMetaQ(stateHash(state))[option];
    }

    private float maxMetaQ(float[] state) {
        float[] q = getOrInitMetaQ(stateHash(state));
        float max = q[0]; for (float v : q) if (v > max) max = v;
        return max;
    }

    private float[] getOrInitMetaQ(int hash) {
        return metaQ.computeIfAbsent(hash, k -> new float[numOptions]);
    }

    private int stateHash(float[] s) {
        // Discretize and hash
        int h = 0;
        for (int i = 0; i < Math.min(s.length, stateDim); i++) {
            int b = (int)(s[i] * 10f) & 0xFF;
            h = h * 31 + b;
        }
        return h;
    }

    private static float[] softmax(float[] v) {
        float mx = v[0]; for (float x : v) if (x > mx) mx = x;
        float sum = 0f; float[] o = new float[v.length];
        for (int i = 0; i < v.length; i++) { o[i] = (float) Math.exp(v[i] - mx); sum += o[i]; }
        for (int i = 0; i < v.length; i++) o[i] /= sum;
        return o;
    }

    private int sampleAction(float[] probs) {
        float r = rng.nextFloat(), cum = 0f;
        for (int a = 0; a < probs.length - 1; a++) { cum += probs[a]; if (r < cum) return a; }
        return probs.length - 1;
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Stats
    // ─────────────────────────────────────────────────────────────────────────

    public synchronized Map<String, Object> getStats() {
        Map<String, Object> s = new HashMap<>();
        s.put("numOptions",     numOptions);
        s.put("totalSteps",     totalSteps.get());
        s.put("optionChanges",  optionChanges.get());
        s.put("activeOption",   activeOption);
        s.put("metaQStates",    metaQ.size());
        double[] avgReturns = new double[numOptions];
        int[] activations = new int[numOptions];
        for (int o = 0; o < numOptions; o++) {
            avgReturns[o] = options[o].avgReturn;
            activations[o] = options[o].activations;
        }
        s.put("optionAvgReturns", avgReturns);
        s.put("optionActivations", activations);
        return s;
    }
}
