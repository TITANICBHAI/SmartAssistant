package com.aiassistant.ml;

import android.util.Log;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

/**
 * QTableManager — LRU-bounded, visit-adaptive tabular Q-table.
 *
 * Responsibilities:
 *   1. Discretize continuous state vectors into hash keys using configurable
 *      bins-per-dimension (default 10).
 *   2. Store Q[state_key][action] with optimistic initialisation (+0.1).
 *   3. LRU eviction when the table exceeds MAX_STATES entries.
 *   4. Adaptive learning rate: α(s,a) = α₀ / (1 + visits(s,a)·β)
 *      so rarely-visited pairs learn faster.
 *   5. Q(λ) eligibility trace propagation: trace update on every step.
 *   6. Statistics: size, hit/miss rate, most/least visited states.
 *   7. Serializable stats map for logging and remote monitoring.
 *
 * Designed as a standalone utility used by QLearningAgent and SARSAAgent.
 */
public class QTableManager {

    private static final String TAG           = "QTableManager";
    private static final float  OPTIMISTIC_Q  = 0.1f;
    private static final int    MAX_STATES    = 100_000;
    private static final float  LAMBDA        = 0.9f;
    private static final float  TRACE_MIN     = 0.005f;

    // ─────────────────────────────────────────────────────────────────────────
    // LRU Q-table storage
    // ─────────────────────────────────────────────────────────────────────────
    private final LinkedHashMap<String, float[]> table;
    private final Map<String, int[]>             visitCounts = new HashMap<>();
    private final Map<String, float[]>           traces      = new HashMap<>();

    private final int     stateDim;
    private final int     actionDim;
    private final int     binsPerDim;
    private final float   alphaBase;
    private final float   alphaAdaptBeta;  // α(s,a) = α / (1 + visits·β)
    private final float   discountFactor;

    // Stats
    private final AtomicInteger hits   = new AtomicInteger(0);
    private final AtomicInteger misses = new AtomicInteger(0);
    private final AtomicLong    totalUpdates = new AtomicLong(0);
    private final Random        rng    = new Random(42L);

    // ─────────────────────────────────────────────────────────────────────────
    // Construction
    // ─────────────────────────────────────────────────────────────────────────

    public QTableManager(int stateDim, int actionDim, int binsPerDim,
                          float alphaBase, float alphaAdaptBeta, float discountFactor) {
        this.stateDim       = stateDim;
        this.actionDim      = actionDim;
        this.binsPerDim     = binsPerDim;
        this.alphaBase      = alphaBase;
        this.alphaAdaptBeta = alphaAdaptBeta;
        this.discountFactor = discountFactor;

        table = new LinkedHashMap<String, float[]>(MAX_STATES + 1, 0.75f, true) {
            @Override
            protected boolean removeEldestEntry(Map.Entry<String, float[]> e) {
                if (size() > MAX_STATES) {
                    visitCounts.remove(e.getKey());
                    traces.remove(e.getKey());
                    return true;
                }
                return false;
            }
        };
    }

    public QTableManager(int stateDim, int actionDim) {
        this(stateDim, actionDim, 10, 0.1f, 0.01f, 0.99f);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Core Q operations
    // ─────────────────────────────────────────────────────────────────────────

    /** Return Q(s, a). */
    public synchronized float getQ(float[] state, int action) {
        return getRow(discretize(state))[action];
    }

    /** Return Q(s, *) for all actions. */
    public synchronized float[] getQRow(float[] state) {
        return getRow(discretize(state)).clone();
    }

    /** Return argmax_a Q(s, a). */
    public synchronized int greedyAction(float[] state) {
        float[] q = getRow(discretize(state));
        int best = 0;
        for (int a = 1; a < actionDim; a++) if (q[a] > q[best]) best = a;
        return best;
    }

    /** Return greedy action with ε-soft exploration. */
    public synchronized int selectAction(float[] state, float epsilon) {
        if (rng.nextFloat() < epsilon) return rng.nextInt(actionDim);
        return greedyAction(state);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Q-learning update (Bellman)
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Standard Q-learning: Q(s,a) ← Q(s,a) + α·(r + γ·max_a'Q(s',a') − Q(s,a))
     *
     * @return TD error δ.
     */
    public synchronized float updateQLearning(float[] state, int action, float reward,
                                               float[] nextState, boolean done) {
        String sk  = discretize(state);
        String nsk = discretize(nextState);
        float[] qs  = getRow(sk);
        float[] qns = getRow(nsk);

        float maxNext = done ? 0f : max(qns);
        float delta   = reward + discountFactor * maxNext - qs[action];
        float alpha   = adaptiveAlpha(sk, action);

        qs[action] += alpha * delta;
        incrementVisit(sk, action);
        totalUpdates.incrementAndGet();
        return delta;
    }

    // ─────────────────────────────────────────────────────────────────────────
    // SARSA update (on-policy)
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * SARSA: Q(s,a) ← Q(s,a) + α·(r + γ·Q(s',a') − Q(s,a))
     */
    public synchronized float updateSARSA(float[] state, int action, float reward,
                                           float[] nextState, int nextAction, boolean done) {
        String sk  = discretize(state);
        String nsk = discretize(nextState);
        float[] qs  = getRow(sk);
        float[] qns = getRow(nsk);

        float nextQ = done ? 0f : qns[nextAction];
        float delta = reward + discountFactor * nextQ - qs[action];
        float alpha = adaptiveAlpha(sk, action);

        qs[action] += alpha * delta;
        incrementVisit(sk, action);
        totalUpdates.incrementAndGet();
        return delta;
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Q(λ) eligibility traces
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Replacing-trace Q(λ) update.  Updates all active traces then propagates
     * the TD error backward through the eligibility trace.
     *
     * @return TD error δ.
     */
    public synchronized float updateQLambda(float[] state, int action, float reward,
                                             float[] nextState, boolean done) {
        String sk  = discretize(state);
        String nsk = discretize(nextState);

        float[] qs  = getRow(sk);
        float[] qns = getRow(nsk);
        float   maxN  = done ? 0f : max(qns);
        float   delta = reward + discountFactor * maxN - qs[action];
        float   alpha = adaptiveAlpha(sk, action);

        // Replacing trace for current (s, a)
        float[] e = traceGet(sk);
        Arrays.fill(e, 0f);
        e[action] = 1f;

        // Propagate δ through all traces
        List<String> expired = new ArrayList<>();
        for (Map.Entry<String, float[]> entry : traces.entrySet()) {
            float[] qt = getRow(entry.getKey());
            float[] et = entry.getValue();
            boolean tiny = true;
            for (int a = 0; a < actionDim; a++) {
                qt[a]  += alpha * delta * et[a];
                et[a]  *= discountFactor * LAMBDA;
                if (Math.abs(et[a]) >= TRACE_MIN) tiny = false;
            }
            if (tiny) expired.add(entry.getKey());
        }
        for (String k : expired) traces.remove(k);

        incrementVisit(sk, action);
        if (done) traces.clear();
        totalUpdates.incrementAndGet();
        return delta;
    }

    /** Reset eligibility traces (on episode start). */
    public synchronized void clearTraces() { traces.clear(); }

    // ─────────────────────────────────────────────────────────────────────────
    // Stats
    // ─────────────────────────────────────────────────────────────────────────

    public synchronized int tableSize()  { return table.size(); }
    public synchronized int traceSize()  { return traces.size(); }
    public long getTotalUpdates()         { return totalUpdates.get(); }
    public float getHitRate() {
        int h = hits.get(), m = misses.get();
        return (h + m) > 0 ? (float) h / (h + m) : 0f;
    }

    public synchronized Map<String, Object> getStats() {
        Map<String, Object> s = new HashMap<>();
        s.put("tableSize",     table.size());
        s.put("traceSize",     traces.size());
        s.put("totalUpdates",  totalUpdates.get());
        s.put("hitRate",       getHitRate());
        s.put("binsPerDim",    binsPerDim);
        s.put("stateDim",      stateDim);
        s.put("actionDim",     actionDim);
        s.put("maxStates",     MAX_STATES);
        return s;
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Internal helpers
    // ─────────────────────────────────────────────────────────────────────────

    private String discretize(float[] state) {
        StringBuilder sb = new StringBuilder(stateDim * 3);
        int dim = Math.min(state.length, stateDim);
        for (int i = 0; i < dim; i++) {
            int bin = Math.max(0, Math.min(binsPerDim - 1, (int)(state[i] * binsPerDim)));
            sb.append(bin).append(',');
        }
        return sb.toString();
    }

    private float[] getRow(String key) {
        float[] row = table.get(key);
        if (row == null) {
            misses.incrementAndGet();
            row = new float[actionDim];
            Arrays.fill(row, OPTIMISTIC_Q);
            table.put(key, row);
        } else {
            hits.incrementAndGet();
        }
        return row;
    }

    private float[] traceGet(String key) {
        return traces.computeIfAbsent(key, k -> new float[actionDim]);
    }

    private float adaptiveAlpha(String key, int action) {
        int[] v = visitCounts.get(key);
        int cnt = (v != null && action < v.length) ? v[action] : 0;
        return alphaBase / (1f + cnt * alphaAdaptBeta);
    }

    private void incrementVisit(String key, int action) {
        int[] v = visitCounts.computeIfAbsent(key, k -> new int[actionDim]);
        if (action < v.length) v[action]++;
    }

    private static float max(float[] q) {
        float m = q[0];
        for (float v : q) if (v > m) m = v;
        return m;
    }
}
