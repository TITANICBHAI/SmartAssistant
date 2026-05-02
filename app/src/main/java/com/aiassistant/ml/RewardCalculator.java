package com.aiassistant.ml;

import android.util.Log;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * RewardCalculator — multi-signal reward computation for on-device RL.
 *
 * Combines several reward components into a single scalar reward signal:
 *
 *   r_total = w_ext  · r_extrinsic          (sparse task reward from game)
 *           + w_int  · r_intrinsic           (curiosity / novelty bonus)
 *           + w_prog · r_progress            (dense progress shaping)
 *           + w_smooth · r_smoothness        (action-smoothness penalty)
 *           + w_time · r_time                (time-efficiency bonus)
 *           + w_surv · r_survival            (per-step survival bonus)
 *
 * Components:
 *   r_extrinsic  — raw reward from the environment (score delta, kill, etc.)
 *   r_intrinsic  — 1 / √(visit_count(state)) curiosity bonus (ICM-lite)
 *   r_progress   — Φ(s') − γ·Φ(s) potential-based shaping (policy-invariant)
 *   r_smoothness — penalty when consecutive actions are very different
 *   r_time       — bonus when task completed quickly; penalty for time wasted
 *   r_survival   — small constant reward per alive step (encourages longevity)
 *
 * All component weights are tunable and can be updated online via
 * {@link #setWeight(String, float)}.  Running statistics are tracked for
 * normalization so rewards stay in a stable scale.
 */
public class RewardCalculator {

    private static final String TAG = "RewardCalculator";

    // -------------------------------------------------------------------------
    // Weight keys
    // -------------------------------------------------------------------------
    public static final String W_EXTRINSIC  = "extrinsic";
    public static final String W_INTRINSIC  = "intrinsic";
    public static final String W_PROGRESS   = "progress";
    public static final String W_SMOOTHNESS = "smoothness";
    public static final String W_TIME       = "time";
    public static final String W_SURVIVAL   = "survival";

    // -------------------------------------------------------------------------
    // Fields
    // -------------------------------------------------------------------------

    private final Map<String, Float> weights = new ConcurrentHashMap<>();

    // State-visit counts for curiosity bonus (hashed)
    private final Map<String, Integer> visitCounts = new ConcurrentHashMap<>();

    // Potential function values per state hash (for progress shaping)
    private final Map<String, Float> potentials = new ConcurrentHashMap<>();

    // Smoothness: track last action
    private int   lastAction     = -1;
    private float lastActionTime =  0f;

    // Running reward stats for per-component normalization (Welford)
    private final Map<String, double[]> componentStats = new ConcurrentHashMap<>();
    // double[] = {mean, m2, count}

    private final float gamma;            // Discount for progress shaping
    private final boolean normalizeComponents; // Whether to z-score each component

    // Reward history for debugging
    private final List<Map<String, Float>> history = new ArrayList<>();
    private static final int MAX_HISTORY = 200;

    // -------------------------------------------------------------------------
    // Construction
    // -------------------------------------------------------------------------

    public RewardCalculator(float gamma, boolean normalizeComponents) {
        this.gamma               = gamma;
        this.normalizeComponents = normalizeComponents;

        // Default weights (all components enabled)
        weights.put(W_EXTRINSIC,  1.00f);
        weights.put(W_INTRINSIC,  0.10f);
        weights.put(W_PROGRESS,   0.30f);
        weights.put(W_SMOOTHNESS, -0.05f);
        weights.put(W_TIME,       0.05f);
        weights.put(W_SURVIVAL,   0.01f);

        // Init stats maps
        for (String k : new String[]{W_EXTRINSIC, W_INTRINSIC, W_PROGRESS,
                                      W_SMOOTHNESS, W_TIME, W_SURVIVAL}) {
            componentStats.put(k, new double[]{0, 0, 0}); // mean, m2, count
        }
    }

    public RewardCalculator() {
        this(0.99f, true);
    }

    // -------------------------------------------------------------------------
    // Public API
    // -------------------------------------------------------------------------

    /**
     * Compute the total shaped reward for one step.
     *
     * @param state     Current game state (used to derive state hash + potential).
     * @param nextState Next game state (after action).
     * @param action    Chosen action index.
     * @param extrinsic Raw environment reward signal.
     * @param done      True if episode terminated.
     * @return Shaped total reward scalar.
     */
    public float compute(Map<String, Object> state,
                         Map<String, Object> nextState,
                         int action,
                         float extrinsic,
                         boolean done) {

        String stateHash     = stateHash(state);
        String nextStateHash = stateHash(nextState);

        // ---------- Component 1: extrinsic ----------
        float rExt = extrinsic;

        // ---------- Component 2: intrinsic curiosity ----------
        int visits = visitCounts.getOrDefault(nextStateHash, 0) + 1;
        visitCounts.put(nextStateHash, visits);
        float rInt = 1f / (float) Math.sqrt(visits);

        // ---------- Component 3: progress (potential shaping) ----------
        float phi_s  = getPotential(stateHash, state);
        float phi_sp = getPotential(nextStateHash, nextState);
        float rProg  = phi_sp - gamma * phi_s;

        // ---------- Component 4: action smoothness ----------
        float rSmooth = 0f;
        if (lastAction >= 0) {
            // Penalise wild action changes (thrashing)
            rSmooth = (action == lastAction) ? 0.5f : -0.5f;
        }
        lastAction = action;

        // ---------- Component 5: time efficiency ----------
        float rTime = done ? 1.0f : -0.001f; // bonus on completion, tiny penalty per step

        // ---------- Component 6: survival ----------
        float rSurv = done ? 0f : 1f; // +1 per alive step

        // ---------- Normalize each component ----------
        float nExt    = normalizeComponents ? normalize(W_EXTRINSIC,  rExt)    : rExt;
        float nInt    = normalizeComponents ? normalize(W_INTRINSIC,  rInt)    : rInt;
        float nProg   = normalizeComponents ? normalize(W_PROGRESS,   rProg)   : rProg;
        float nSmooth = normalizeComponents ? normalize(W_SMOOTHNESS, rSmooth) : rSmooth;
        float nTime   = normalizeComponents ? normalize(W_TIME,       rTime)   : rTime;
        float nSurv   = normalizeComponents ? normalize(W_SURVIVAL,   rSurv)   : rSurv;

        // ---------- Weighted sum ----------
        float total = w(W_EXTRINSIC)  * nExt
                    + w(W_INTRINSIC)  * nInt
                    + w(W_PROGRESS)   * nProg
                    + w(W_SMOOTHNESS) * nSmooth
                    + w(W_TIME)       * nTime
                    + w(W_SURVIVAL)   * nSurv;

        // ---------- Record history ----------
        if (history.size() >= MAX_HISTORY) history.remove(0);
        Map<String, Float> entry = new HashMap<>();
        entry.put("total",      total);
        entry.put(W_EXTRINSIC,  rExt);
        entry.put(W_INTRINSIC,  rInt);
        entry.put(W_PROGRESS,   rProg);
        entry.put(W_SMOOTHNESS, rSmooth);
        entry.put(W_TIME,       rTime);
        entry.put(W_SURVIVAL,   rSurv);
        history.add(entry);

        return total;
    }

    // -------------------------------------------------------------------------
    // Configuration API
    // -------------------------------------------------------------------------

    public void setWeight(String component, float value) {
        weights.put(component, value);
    }

    public float getWeight(String component) {
        return weights.getOrDefault(component, 0f);
    }

    /**
     * Register a manual potential value for a state hash.
     * Useful when the caller has domain knowledge (e.g. distance to goal).
     */
    public void setPotential(Map<String, Object> state, float value) {
        potentials.put(stateHash(state), value);
    }

    /** Reset visit counts, potentials, and stats (e.g. between game levels). */
    public void reset() {
        visitCounts.clear();
        potentials.clear();
        lastAction = -1;
        history.clear();
        for (double[] s : componentStats.values()) { s[0] = 0; s[1] = 0; s[2] = 0; }
    }

    // -------------------------------------------------------------------------
    // Monitoring
    // -------------------------------------------------------------------------

    public Map<String, Object> getStats() {
        Map<String, Object> s = new HashMap<>();
        s.put("weights",       new HashMap<>(weights));
        s.put("visitedStates", visitCounts.size());
        s.put("historySize",   history.size());

        if (!history.isEmpty()) {
            float sumTotal = 0f;
            for (Map<String, Float> h : history) sumTotal += h.get("total");
            s.put("avgTotalReward", sumTotal / history.size());
        }

        Map<String, Double> compMeans = new HashMap<>();
        for (Map.Entry<String, double[]> e : componentStats.entrySet()) {
            compMeans.put(e.getKey(), e.getValue()[0]);
        }
        s.put("componentMeans", compMeans);
        return s;
    }

    public List<Map<String, Float>> getHistory() {
        return new ArrayList<>(history);
    }

    // -------------------------------------------------------------------------
    // Internal helpers
    // -------------------------------------------------------------------------

    private float w(String key) {
        return weights.getOrDefault(key, 0f);
    }

    /** Derive a lightweight potential from the state (health + score proxy). */
    private float getPotential(String hash, Map<String, Object> state) {
        Float cached = potentials.get(hash);
        if (cached != null) return cached;

        float health = extractFloat(state, "health", 1.0f);
        float score  = extractFloat(state, "score",  0.0f);
        float pot    = 0.5f * health + 0.01f * Math.min(score, 100f);
        potentials.put(hash, pot);
        return pot;
    }

    private static float extractFloat(Map<String, Object> m, String key, float def) {
        Object v = m.get(key);
        if (v instanceof Number) return ((Number) v).floatValue();
        return def;
    }

    /** Welford online normalization — updates running stats and returns z-score clamped to [-3,3]. */
    private float normalize(String key, float value) {
        double[] s = componentStats.get(key);
        if (s == null) return value;
        s[2]++;
        double delta  = value - s[0];
        s[0] += delta / s[2];
        s[1] += delta * (value - s[0]);
        if (s[2] < 2) return value;
        double std = Math.sqrt(s[1] / (s[2] - 1));
        if (std < 1e-6) return 0f;
        float z = (float) ((value - s[0]) / std);
        return Math.max(-3f, Math.min(3f, z));
    }

    /** Deterministic hash of a game-state map — uses key subset for speed. */
    private static String stateHash(Map<String, Object> state) {
        if (state == null || state.isEmpty()) return "empty";
        // Use a fixed set of discriminative keys to avoid computing a full hash
        StringBuilder sb = new StringBuilder();
        for (String key : new String[]{"x","y","health","score","level","enemyCount","action"}) {
            Object v = state.get(key);
            if (v != null) sb.append(key.charAt(0)).append(':').append(v).append('|');
        }
        return sb.length() > 0 ? sb.toString() : state.toString().substring(0, Math.min(64, state.toString().length()));
    }
}
