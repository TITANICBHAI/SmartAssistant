package com.aiassistant.ml;

import android.util.Log;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * SafeExploration — constraint-aware action selection for safe RL in game environments.
 *
 * Ensures the agent avoids catastrophic or irreversible actions during exploration,
 * implementing three safety paradigms:
 *
 *   1. SAFETY_FILTER (Hard constraint):
 *      A trained safety classifier labels actions as safe/unsafe given state.
 *      Unsafe actions are blocked before execution (project to safe set).
 *
 *   2. CONSTRAINED_POLICY_GRADIENT (CPG / Lagrangian):
 *      Maintains a Lagrange multiplier λ that penalises constraint violations:
 *      L(π, λ) = E[r] - λ·(E[cost] - limit)
 *      λ is updated online via dual gradient descent.
 *
 *   3. CONSERVATIVE_Q (CQL-style):
 *      Subtract a penalty from Q-values of actions that have historically
 *      caused high cost/danger, discouraging their selection.
 *
 * Cost signal: provided externally (0 = safe, 1 = constraint violated).
 * Constraint limit: maximum acceptable average cost (e.g., 0.1 = 10% violation rate).
 *
 * Thread-safe.
 */
public class SafeExploration {

    private static final String TAG = "SafeExploration";

    public enum SafetyMode { SAFETY_FILTER, CONSTRAINED_POLICY_GRADIENT, CONSERVATIVE_Q }

    // ─────────────────────────────────────────────────────────────────────────
    // Safety classifier: (state, action) → P(unsafe)
    // ─────────────────────────────────────────────────────────────────────────
    private final float[][] sfW1, sfW2;   // [hidDim][stateDim+actionDim], [1][hidDim]
    private final float[]   sfB1, sfB2;
    private final NeuralNetworkOptimizer sfOpt;

    // ─────────────────────────────────────────────────────────────────────────
    // Lagrangian state
    // ─────────────────────────────────────────────────────────────────────────
    private float lambda       = 0f;     // Lagrange multiplier
    private float avgCost      = 0f;     // running average cost
    private final float costLimit;       // constraint threshold d
    private final float lagrangeLr;

    // ─────────────────────────────────────────────────────────────────────────
    // Conservative Q: per-action cost history
    // ─────────────────────────────────────────────────────────────────────────
    private final float[] actionCostHistory;  // EWMA cost per action

    // ─────────────────────────────────────────────────────────────────────────
    // Fields
    // ─────────────────────────────────────────────────────────────────────────
    private final int         stateDim;
    private final int         actionDim;
    private final int         hidDim;
    private final SafetyMode  mode;
    private final float       safetyThresh; // P(unsafe) threshold for SAFETY_FILTER
    private final float       cqlAlpha;     // conservative Q penalty weight

    private final AtomicInteger stepCount       = new AtomicInteger(0);
    private final AtomicInteger blockedCount    = new AtomicInteger(0);
    private final AtomicInteger violationCount  = new AtomicInteger(0);
    private float avgUnsafeProb = 0f;

    private final Random rng = new Random(167L);

    // ─────────────────────────────────────────────────────────────────────────
    // Construction
    // ─────────────────────────────────────────────────────────────────────────

    public SafeExploration(int stateDim, int actionDim, int hidDim,
                            SafetyMode mode, float costLimit, float lagrangeLr,
                            float safetyThresh, float cqlAlpha, float lr) {
        this.stateDim    = stateDim;
        this.actionDim   = actionDim;
        this.hidDim      = hidDim;
        this.mode        = mode;
        this.costLimit   = costLimit;
        this.lagrangeLr  = lagrangeLr;
        this.safetyThresh= safetyThresh;
        this.cqlAlpha    = cqlAlpha;
        this.sfOpt       = new NeuralNetworkOptimizer(lr);
        this.actionCostHistory = new float[actionDim];

        int inDim = stateDim + actionDim;
        float s   = (float) Math.sqrt(2.0 / (inDim + hidDim));
        sfW1 = xav(hidDim, inDim, s); sfB1 = new float[hidDim];
        sfW2 = xav(1, hidDim, s);     sfB2 = new float[1];

        Log.i(TAG, "SafeExploration: mode=" + mode + " limit=" + costLimit);
    }

    public SafeExploration(int stateDim, int actionDim, SafetyMode mode) {
        this(stateDim, actionDim, 64, mode, 0.1f, 0.01f, 0.5f, 1.0f, 1e-3f);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Core API
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Filter or adjust Q-values to enforce safety.
     *
     * @param state    Current state.
     * @param qValues  Raw Q-values from the agent Q[actionDim].
     * @return Safe Q-values: unsafe actions have reduced/blocked values.
     */
    public synchronized float[] safeQValues(float[] state, float[] qValues) {
        float[] safeQ = qValues.clone();
        stepCount.incrementAndGet();

        switch (mode) {
            case SAFETY_FILTER:
                for (int a = 0; a < actionDim; a++) {
                    float pUnsafe = unsafeProb(state, a);
                    avgUnsafeProb = 0.999f * avgUnsafeProb + 0.001f * pUnsafe;
                    if (pUnsafe > safetyThresh) {
                        safeQ[a] = Float.NEGATIVE_INFINITY;
                        blockedCount.incrementAndGet();
                    }
                }
                break;

            case CONSTRAINED_POLICY_GRADIENT:
                // Subtract λ·cost_penalty from each action's Q
                for (int a = 0; a < actionDim; a++)
                    safeQ[a] -= lambda * actionCostHistory[a];
                break;

            case CONSERVATIVE_Q:
                // Penalise actions with high historical cost
                for (int a = 0; a < actionDim; a++)
                    safeQ[a] -= cqlAlpha * actionCostHistory[a];
                break;
        }
        return safeQ;
    }

    /**
     * Provide safety feedback after executing an action.
     *
     * @param state  State in which action was executed.
     * @param action Action taken.
     * @param cost   Safety cost (0 = safe, 1 = violation).
     */
    public synchronized void observeCost(float[] state, int action, float cost) {
        if (cost > 0) violationCount.incrementAndGet();

        // Update action cost history
        if (action >= 0 && action < actionDim)
            actionCostHistory[action] = 0.95f * actionCostHistory[action] + 0.05f * cost;

        // Update average cost
        avgCost = 0.99f * avgCost + 0.01f * cost;

        // Lagrangian update: λ ← max(0, λ + lr·(avgCost - limit))
        if (mode == SafetyMode.CONSTRAINED_POLICY_GRADIENT)
            lambda = Math.max(0f, lambda + lagrangeLr * (avgCost - costLimit));

        // Train safety classifier
        trainClassifier(state, action, cost > 0.5f);
    }

    /**
     * P(action is unsafe | state).
     */
    public synchronized float unsafeProb(float[] state, int action) {
        float[] inp = buildInput(state, action);
        float[] h   = lin(sfW1, sfB1, inp, true);
        return sigmoid(lin(sfW2, sfB2, h, false)[0]);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Classifier training
    // ─────────────────────────────────────────────────────────────────────────

    private void trainClassifier(float[] state, int action, boolean unsafe) {
        float[] inp    = buildInput(state, action);
        float[] h      = lin(sfW1, sfB1, inp, true);
        float   pUnsafe= sigmoid(lin(sfW2, sfB2, h, false)[0]);
        float   label  = unsafe ? 1f : 0f;
        float   dOut   = pUnsafe - label;

        float[][] dW2 = new float[1][hidDim];
        for (int j = 0; j < hidDim; j++) dW2[0][j] = dOut * h[j];
        sfOpt.step("sf_W2", sfW2, dW2);

        float[] dH = new float[hidDim];
        for (int j = 0; j < hidDim; j++) {
            if (h[j] <= 0) continue;
            dH[j] = dOut * sfW2[0][j];
        }
        float[][] dW1 = new float[hidDim][inp.length];
        for (int i = 0; i < hidDim; i++)
            for (int j = 0; j < inp.length; j++) dW1[i][j] = dH[i] * inp[j];
        sfOpt.step("sf_W1", sfW1, dW1);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Helpers
    // ─────────────────────────────────────────────────────────────────────────

    private float[] buildInput(float[] state, int action) {
        float[] inp = new float[stateDim + actionDim];
        System.arraycopy(pad(state, stateDim), 0, inp, 0, stateDim);
        if (action >= 0 && action < actionDim) inp[stateDim + action] = 1f;
        return inp;
    }

    private static float[] lin(float[][] W, float[] b, float[] x, boolean relu) {
        float[] o = new float[W.length];
        for (int i = 0; i < W.length; i++) {
            float s = b[i];
            for (int j = 0; j < Math.min(x.length, W[i].length); j++) s += W[i][j] * x[j];
            o[i] = relu ? Math.max(0f, s) : s;
        }
        return o;
    }

    private static float sigmoid(float x) { return 1f / (1f + (float) Math.exp(-x)); }

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
        s.put("mode",           mode.name());
        s.put("stepCount",      stepCount.get());
        s.put("blockedCount",   blockedCount.get());
        s.put("violationCount", violationCount.get());
        s.put("blockRate",      stepCount.get() > 0 ? (float)blockedCount.get()/stepCount.get() : 0f);
        s.put("violationRate",  stepCount.get() > 0 ? (float)violationCount.get()/stepCount.get() : 0f);
        s.put("avgCost",        avgCost);
        s.put("lambda",         lambda);
        s.put("avgUnsafeProb",  avgUnsafeProb);
        return s;
    }
}
