package com.aiassistant.ml;

import android.util.Log;

import java.util.HashMap;
import java.util.Map;

/**
 * NeuralNetworkOptimizer — on-device Adam optimizer for updating neural network
 * weight matrices without any external ML framework dependency.
 *
 * Supported update rules:
 *   - ADAM   (default) — Adaptive Moment Estimation (Kingma & Ba, 2015)
 *   - SGD    — vanilla stochastic gradient descent
 *   - RMSPROP — RMSProp with configurable decay
 *   - ADAMW  — Adam with decoupled L2 weight decay
 *
 * The optimizer manages first-moment (m), second-moment (v), and step-count
 * state internally, keyed by a parameter-group name.  This lets a single
 * optimizer instance handle multiple separate weight matrices (e.g. policy
 * + value networks) without interfering with each other's momentum history.
 *
 * All operations are thread-safe via per-group synchronization.
 */
public class NeuralNetworkOptimizer {

    private static final String TAG = "NeuralNetworkOptimizer";

    // -------------------------------------------------------------------------
    // Update rule enum
    // -------------------------------------------------------------------------
    public enum UpdateRule { ADAM, SGD, RMSPROP, ADAMW }

    // -------------------------------------------------------------------------
    // Hyper-parameters
    // -------------------------------------------------------------------------
    private final UpdateRule rule;
    private final float      lr;        // Learning rate
    private final float      beta1;     // Adam first-moment decay (default 0.9)
    private final float      beta2;     // Adam second-moment decay (default 0.999)
    private final float      epsilon;   // Numerical stability (default 1e-8)
    private final float      weightDecay; // L2 coefficient for ADAMW

    // -------------------------------------------------------------------------
    // Per-group momentum state
    // -------------------------------------------------------------------------
    private static class GroupState {
        float[][] m;    // First moment (same shape as weights)
        float[][] v;    // Second moment (same shape as weights)
        int       step;

        GroupState(int rows, int cols) {
            m    = new float[rows][cols];
            v    = new float[rows][cols];
            step = 0;
        }
    }

    private final Map<String, GroupState> groups = new HashMap<>();

    // -------------------------------------------------------------------------
    // Construction
    // -------------------------------------------------------------------------

    public NeuralNetworkOptimizer(UpdateRule rule, float lr,
                                   float beta1, float beta2,
                                   float epsilon, float weightDecay) {
        this.rule        = rule;
        this.lr          = lr;
        this.beta1       = beta1;
        this.beta2       = beta2;
        this.epsilon     = epsilon;
        this.weightDecay = weightDecay;
    }

    /** Adam with default hyper-parameters (lr=1e-3, β₁=0.9, β₂=0.999, ε=1e-8). */
    public NeuralNetworkOptimizer(float lr) {
        this(UpdateRule.ADAM, lr, 0.9f, 0.999f, 1e-8f, 0.0f);
    }

    /** Default Adam lr=1e-3. */
    public NeuralNetworkOptimizer() {
        this(1e-3f);
    }

    // -------------------------------------------------------------------------
    // Public API
    // -------------------------------------------------------------------------

    /**
     * Apply one gradient step to {@code weights} using gradient {@code grads}.
     *
     * @param groupName Unique name for this weight matrix (e.g. "policy", "value").
     * @param weights   Weight matrix (modified in-place).
     * @param grads     Gradient matrix with the same shape as {@code weights}.
     */
    public void step(String groupName, float[][] weights, float[][] grads) {
        if (weights == null || grads == null) return;
        int rows = weights.length;
        if (rows == 0) return;
        int cols = weights[0].length;

        GroupState state;
        synchronized (groups) {
            state = groups.get(groupName);
            if (state == null) {
                state = new GroupState(rows, cols);
                groups.put(groupName, state);
            }
        }

        synchronized (state) {
            state.step++;
            float t = state.step;

            switch (rule) {
                case ADAM:
                case ADAMW:
                    applyAdam(weights, grads, state, t);
                    break;
                case RMSPROP:
                    applyRMSProp(weights, grads, state);
                    break;
                case SGD:
                default:
                    applySGD(weights, grads, state);
                    break;
            }
        }
    }

    /**
     * Apply one gradient step to a 1-D weight vector.
     *
     * @param groupName Unique name (keyed separately from 2-D groups).
     * @param weights   Weight vector (modified in-place).
     * @param grads     Gradient vector with the same length.
     */
    public void step(String groupName, float[] weights, float[] grads) {
        if (weights == null || grads == null || weights.length == 0) return;
        // Wrap as single-row 2-D and unwrap
        float[][] W = new float[][]{weights};
        float[][] G = new float[][]{grads};
        step(groupName + "_1d", W, G);
        System.arraycopy(W[0], 0, weights, 0, weights.length);
    }

    /** Zero all momentum state for a group (useful after environment reset). */
    public synchronized void resetGroup(String groupName) {
        GroupState s = groups.get(groupName);
        if (s == null) return;
        synchronized (s) {
            for (int i = 0; i < s.m.length; i++) {
                java.util.Arrays.fill(s.m[i], 0f);
                java.util.Arrays.fill(s.v[i], 0f);
            }
            s.step = 0;
        }
    }

    /** Zero all momentum state across all groups. */
    public synchronized void resetAll() {
        for (String key : groups.keySet()) resetGroup(key);
    }

    /** Export statistics for monitoring. */
    public Map<String, Object> getStats() {
        Map<String, Object> s = new HashMap<>();
        s.put("rule",        rule.name());
        s.put("lr",          lr);
        s.put("beta1",       beta1);
        s.put("beta2",       beta2);
        s.put("weightDecay", weightDecay);
        Map<String, Integer> steps = new HashMap<>();
        synchronized (groups) {
            for (Map.Entry<String, GroupState> e : groups.entrySet()) {
                steps.put(e.getKey(), e.getValue().step);
            }
        }
        s.put("groupSteps", steps);
        return s;
    }

    // -------------------------------------------------------------------------
    // Update rule implementations
    // -------------------------------------------------------------------------

    private void applyAdam(float[][] W, float[][] G, GroupState s, float t) {
        // Bias-correction factors
        float bc1 = 1f - (float) Math.pow(beta1, t);
        float bc2 = 1f - (float) Math.pow(beta2, t);
        float lrCorrected = lr * (float) Math.sqrt(bc2) / bc1;

        for (int i = 0; i < W.length; i++) {
            for (int j = 0; j < W[i].length; j++) {
                float g = G[i][j];
                s.m[i][j] = beta1 * s.m[i][j] + (1f - beta1) * g;
                s.v[i][j] = beta2 * s.v[i][j] + (1f - beta2) * g * g;

                float update = lrCorrected * s.m[i][j]
                        / ((float) Math.sqrt(s.v[i][j]) + epsilon);

                // ADAMW: decouple weight decay from gradient
                if (rule == UpdateRule.ADAMW && weightDecay > 0f) {
                    W[i][j] *= (1f - lr * weightDecay);
                }
                W[i][j] -= update;
            }
        }
    }

    private void applyRMSProp(float[][] W, float[][] G, GroupState s) {
        float decay = beta2; // reuse beta2 as decay
        for (int i = 0; i < W.length; i++) {
            for (int j = 0; j < W[i].length; j++) {
                float g = G[i][j];
                s.v[i][j] = decay * s.v[i][j] + (1f - decay) * g * g;
                W[i][j] -= lr * g / ((float) Math.sqrt(s.v[i][j]) + epsilon);
            }
        }
    }

    private void applySGD(float[][] W, float[][] G, GroupState s) {
        // Momentum SGD (beta1 as momentum coefficient)
        for (int i = 0; i < W.length; i++) {
            for (int j = 0; j < W[i].length; j++) {
                s.m[i][j] = beta1 * s.m[i][j] + lr * G[i][j];
                W[i][j] -= s.m[i][j];
                // L2 regularisation
                if (weightDecay > 0f) W[i][j] -= lr * weightDecay * W[i][j];
            }
        }
    }
}
