package com.aiassistant.learning;

import android.util.Log;

import java.util.HashMap;
import java.util.Map;

/**
 * Potential-based reward shaping for the AI assistant's RL agents.
 *
 * Reward shaping replaces the raw environment reward r(s,a,s') with
 * a shaped reward:
 *
 *     r̃(s,a,s') = r(s,a,s') + γ·Φ(s') − Φ(s)
 *
 * where Φ is a potential function.  Potential-based shaping is provably
 * policy-invariant — it does not change the optimal policy — but dramatically
 * speeds up learning by providing dense learning signal.
 *
 * Features:
 *  1. Multiple built-in potential functions selectable via {@link PotentialType}.
 *  2. Custom potential support — callers can supply a {@link PotentialFn}.
 *  3. Action-smoothness bonus — penalises rapid action switching to encourage
 *     consistent behaviour.
 *  4. Progress bonus — rewards movement toward a goal state (if defined).
 *  5. Curiosity bonus — encourages visiting novel states using a visit-count
 *     heuristic.  Novel states get extra reward; familiar states get 0.
 *  6. Composite mode — combines multiple shaping signals with configurable weights.
 *  7. Clamping — shaped rewards are clamped to [-MAX_REWARD, +MAX_REWARD] to
 *     prevent runaway gradients.
 */
public class RewardShaper {
    private static final String TAG          = "RewardShaper";
    private static final float  MAX_REWARD   = 10f;
    private static final float  CURIOSITY_K  = 0.5f;  // bonus scale for curiosity
    private static final int    MAX_VISIT_MAP = 10_000;

    // -----------------------------------------------------------------------
    // Potential function types
    // -----------------------------------------------------------------------
    public enum PotentialType {
        NONE,          // no shaping (raw reward passthrough)
        LINEAR,        // Φ(s) = w · s
        GOAL_DISTANCE, // Φ(s) = −distance(s, goal)
        CURIOSITY,     // Φ(s) = 1 / √(visits(s) + 1)
        COMPOSITE      // weighted sum of the above
    }

    // -----------------------------------------------------------------------
    // Custom potential interface
    // -----------------------------------------------------------------------
    public interface PotentialFn {
        float potential(float[] state);
    }

    // -----------------------------------------------------------------------
    // Fields
    // -----------------------------------------------------------------------
    private PotentialType type         = PotentialType.NONE;
    private PotentialFn   customFn     = null;
    private float         gamma        = 0.99f;
    private float[]       goalState    = null;
    private float[]       linearWeights = null;

    // Composite weights
    private float weightLinear   = 0.5f;
    private float weightGoal     = 0.3f;
    private float weightCuriosity = 0.2f;

    // Action smoothness
    private boolean actionSmoothnessEnabled = true;
    private float   smoothnessPenalty       = 0.05f;
    private int     lastAction              = -1;

    // Visit-count map for curiosity
    private final Map<String, Integer> visitCounts = new HashMap<>();

    // -----------------------------------------------------------------------
    // Constructors
    // -----------------------------------------------------------------------
    public RewardShaper() {}

    public RewardShaper(PotentialType type, float gamma) {
        this.type  = type;
        this.gamma = Math.max(0f, Math.min(1f, gamma));
    }

    // -----------------------------------------------------------------------
    // Primary API
    // -----------------------------------------------------------------------

    /**
     * Computes the shaped reward for a transition (s, a, r, s').
     *
     * @param state     Current state vector
     * @param action    Action taken
     * @param reward    Raw environment reward
     * @param nextState Next state vector
     * @param done      Whether the episode ended
     * @return Shaped reward (clamped to [-MAX_REWARD, +MAX_REWARD])
     */
    public float shape(float[] state, int action, float reward,
                       float[] nextState, boolean done) {
        float phiS  = potential(state);
        float phiNS = done ? 0f : potential(nextState);
        float shaped = reward + gamma * phiNS - phiS;

        // Action smoothness penalty
        if (actionSmoothnessEnabled && lastAction >= 0 && action != lastAction) {
            shaped -= smoothnessPenalty;
        }
        lastAction = action;

        // Update visit count for curiosity
        updateVisits(state);

        shaped = Math.max(-MAX_REWARD, Math.min(MAX_REWARD, shaped));
        return shaped;
    }

    // -----------------------------------------------------------------------
    // Potential function dispatch
    // -----------------------------------------------------------------------

    private float potential(float[] state) {
        if (state == null) return 0f;
        switch (type) {
            case NONE:          return 0f;
            case LINEAR:        return linearPotential(state);
            case GOAL_DISTANCE: return goalPotential(state);
            case CURIOSITY:     return curiosityPotential(state);
            case COMPOSITE:     return compositePotential(state);
            default:
                if (customFn != null) return customFn.potential(state);
                return 0f;
        }
    }

    private float linearPotential(float[] state) {
        if (linearWeights == null) return 0f;
        float dot = 0f;
        int   len = Math.min(state.length, linearWeights.length);
        for (int i = 0; i < len; i++) dot += state[i] * linearWeights[i];
        return dot;
    }

    private float goalPotential(float[] state) {
        if (goalState == null) return 0f;
        float dist = 0f;
        int   len  = Math.min(state.length, goalState.length);
        for (int i = 0; i < len; i++) {
            float d = state[i] - goalState[i];
            dist += d * d;
        }
        return -(float) Math.sqrt(dist);
    }

    private float curiosityPotential(float[] state) {
        String k  = stateKey(state);
        int    n  = visitCounts.getOrDefault(k, 0);
        return CURIOSITY_K / (float) Math.sqrt(n + 1);
    }

    private float compositePotential(float[] state) {
        return weightLinear    * linearPotential(state)
             + weightGoal      * goalPotential(state)
             + weightCuriosity * curiosityPotential(state);
    }

    // -----------------------------------------------------------------------
    // Visit-count map
    // -----------------------------------------------------------------------

    private void updateVisits(float[] state) {
        if (visitCounts.size() >= MAX_VISIT_MAP) {
            // Simple clear — could be replaced with LRU in a future iteration
            visitCounts.clear();
        }
        visitCounts.merge(stateKey(state), 1, Integer::sum);
    }

    private String stateKey(float[] state) {
        StringBuilder sb = new StringBuilder(state.length * 3);
        for (float v : state) sb.append((int) (v * 10)).append('_');
        return sb.toString();
    }

    // -----------------------------------------------------------------------
    // Configuration
    // -----------------------------------------------------------------------

    public RewardShaper setPotentialType(PotentialType t)  { type = t; return this; }
    public RewardShaper setCustomPotential(PotentialFn fn) { customFn = fn; return this; }
    public RewardShaper setGamma(float g)  { gamma = Math.max(0f, Math.min(1f, g)); return this; }
    public RewardShaper setGoalState(float[] goal) { goalState = goal.clone(); return this; }
    public RewardShaper setLinearWeights(float[] w) { linearWeights = w.clone(); return this; }

    public RewardShaper setCompositeWeights(float linear, float goal, float curiosity) {
        float sum = linear + goal + curiosity;
        if (sum > 0) { weightLinear = linear / sum; weightGoal = goal / sum; weightCuriosity = curiosity / sum; }
        return this;
    }

    public RewardShaper setActionSmoothness(boolean enabled, float penalty) {
        actionSmoothnessEnabled = enabled;
        smoothnessPenalty       = Math.max(0f, penalty);
        return this;
    }

    public void reset() { visitCounts.clear(); lastAction = -1; }

    public int getVisitCount(float[] state) { return visitCounts.getOrDefault(stateKey(state), 0); }
}
