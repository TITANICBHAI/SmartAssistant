package com.aiassistant.ml;

import android.util.Log;

import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * PrioritizedSweeping — model-based planning with a priority queue of updates.
 *
 * Implements Peng & Williams (1993) Prioritized Sweeping for tabular/tile-coding RL:
 *
 *   1. When a real transition (s,a,r,s') arrives:
 *      a. Update model: P(s'|s,a) and R(s,a)
 *      b. Compute |δ| = |r + γ·max_a' Q(s',a') - Q(s,a)|
 *      c. If |δ| > θ, push (s,a) onto the priority queue.
 *
 *   2. Perform n_plan Q-updates from the highest-priority (s,a) pairs:
 *      a. Pop (s,a) with highest priority.
 *      b. Q(s,a) ← R(s,a) + γ·Σ_s' P(s'|s,a)·max_a' Q(s',a')
 *      c. For each predecessor (s̃, ã) with P(s|s̃,ã) > 0:
 *         compute |δ| and push if large.
 *
 * Uses discrete state hashes (configurable granularity).
 *
 * Thread-safe.
 */
public class PrioritizedSweeping {

    private static final String TAG = "PrioritizedSweeping";

    // ─────────────────────────────────────────────────────────────────────────
    // Internal types
    // ─────────────────────────────────────────────────────────────────────────

    private static class StateAction {
        final long stateKey;
        final int  action;
        StateAction(long s, int a) { stateKey = s; action = a; }
        @Override public boolean equals(Object o) {
            if (!(o instanceof StateAction)) return false;
            StateAction other = (StateAction) o;
            return stateKey == other.stateKey && action == other.action;
        }
        @Override public int hashCode() { return Long.hashCode(stateKey) * 31 + action; }
    }

    // Model: (s,a) → distribution over next states and expected reward
    private final HashMap<StateAction, HashMap<Long, Float>> transitionCounts = new HashMap<>();
    private final HashMap<StateAction, Float>                rewardModel      = new HashMap<>();
    // Q-table: (s,a) → value
    private final HashMap<StateAction, Float>                qTable           = new HashMap<>();

    private final PriorityQueue<Map.Entry<StateAction, Float>> pQueue;
    private final HashMap<StateAction, Float>                  pMap = new HashMap<>();

    // Config
    private final int   actionDim;
    private final float gamma;
    private final float alpha;       // Q update LR
    private final float theta;       // minimum priority threshold
    private final int   nPlan;       // sweeping steps per real step
    private final int   hashGranularity;

    // Stats
    private final AtomicInteger realUpdates    = new AtomicInteger(0);
    private final AtomicInteger planningSteps  = new AtomicInteger(0);
    private float avgTdError = 0f;

    // ─────────────────────────────────────────────────────────────────────────
    // Construction
    // ─────────────────────────────────────────────────────────────────────────

    public PrioritizedSweeping(int actionDim, float gamma, float alpha,
                                float theta, int nPlan, int hashGranularity) {
        this.actionDim       = actionDim;
        this.gamma           = gamma;
        this.alpha           = alpha;
        this.theta           = theta;
        this.nPlan           = nPlan;
        this.hashGranularity = hashGranularity;

        pQueue = new PriorityQueue<>(Comparator.<Map.Entry<StateAction,Float>>
                comparingByValue().reversed());

        Log.i(TAG, "PrioritizedSweeping: actions=" + actionDim + " θ=" + theta
                + " nPlan=" + nPlan);
    }

    public PrioritizedSweeping(int actionDim) {
        this(actionDim, 0.99f, 0.1f, 0.01f, 10, 10);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Real transition
    // ─────────────────────────────────────────────────────────────────────────

    public synchronized void observe(float[] state, int action, float reward,
                                     float[] nextState, boolean done) {
        long sKey  = hash(state);
        long spKey = done ? -1L : hash(nextState);
        StateAction sa = new StateAction(sKey, action);

        // Update model
        transitionCounts.computeIfAbsent(sa, k -> new HashMap<>())
                .merge(spKey, 1f, Float::sum);
        rewardModel.merge(sa, reward, (old, r) -> 0.9f * old + 0.1f * r);

        // Compute TD error and possibly push to queue
        float qSA    = qTable.getOrDefault(sa, 0f);
        float maxQ   = done ? 0f : maxQ(spKey);
        float target = reward + gamma * maxQ;
        float tdErr  = Math.abs(target - qSA);
        avgTdError   = 0.99f * avgTdError + 0.01f * tdErr;
        realUpdates.incrementAndGet();

        if (tdErr > theta) push(sa, tdErr);

        // Perform planning sweeps
        sweep();
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Q-value query
    // ─────────────────────────────────────────────────────────────────────────

    public synchronized float qValue(float[] state, int action) {
        return qTable.getOrDefault(new StateAction(hash(state), action), 0f);
    }

    public synchronized int greedyAction(float[] state) {
        long key = hash(state);
        int best = 0; float bestQ = Float.NEGATIVE_INFINITY;
        for (int a = 0; a < actionDim; a++) {
            float q = qTable.getOrDefault(new StateAction(key, a), 0f);
            if (q > bestQ) { bestQ = q; best = a; }
        }
        return best;
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Sweeping
    // ─────────────────────────────────────────────────────────────────────────

    private void sweep() {
        for (int i = 0; i < nPlan && !pQueue.isEmpty(); i++) {
            Map.Entry<StateAction, Float> entry = pQueue.poll();
            StateAction sa = entry.getKey();
            pMap.remove(sa);

            // Model-based Q-update
            float r       = rewardModel.getOrDefault(sa, 0f);
            float nextVal = expectedNextValue(sa);
            float target  = r + gamma * nextVal;
            float qOld    = qTable.getOrDefault(sa, 0f);
            float qNew    = qOld + alpha * (target - qOld);
            qTable.put(sa, qNew);
            planningSteps.incrementAndGet();

            // Back-propagate: find predecessors
            backPropagate(sa.stateKey);
        }
    }

    private float expectedNextValue(StateAction sa) {
        HashMap<Long, Float> counts = transitionCounts.get(sa);
        if (counts == null || counts.isEmpty()) return 0f;
        float total = 0f; for (float c : counts.values()) total += c;
        float ev = 0f;
        for (Map.Entry<Long, Float> e : counts.entrySet()) {
            float prob = e.getValue() / total;
            ev += prob * maxQ(e.getKey());
        }
        return ev;
    }

    private float maxQ(long stateKey) {
        float max = 0f;
        for (int a = 0; a < actionDim; a++) {
            float q = qTable.getOrDefault(new StateAction(stateKey, a), 0f);
            if (q > max) max = q;
        }
        return max;
    }

    private void backPropagate(long targetStateKey) {
        // For each (s,a) whose model leads to targetStateKey, recompute priority
        for (Map.Entry<StateAction, HashMap<Long, Float>> entry : transitionCounts.entrySet()) {
            if (!entry.getValue().containsKey(targetStateKey)) continue;
            StateAction predSA = entry.getKey();
            float r     = rewardModel.getOrDefault(predSA, 0f);
            float maxNQ = maxQ(targetStateKey);
            float qOld  = qTable.getOrDefault(predSA, 0f);
            float td    = Math.abs(r + gamma * maxNQ - qOld);
            if (td > theta) push(predSA, td);
        }
    }

    private void push(StateAction sa, float priority) {
        Float existing = pMap.get(sa);
        if (existing != null && existing >= priority) return;
        pMap.put(sa, priority);
        pQueue.removeIf(e -> e.getKey().equals(sa));
        pQueue.add(Map.entry(sa, priority));
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Hash
    // ─────────────────────────────────────────────────────────────────────────

    private long hash(float[] state) {
        long h = 0;
        for (float v : state) {
            int b = (int)(v * hashGranularity);
            h = h * 31L + b;
        }
        return h;
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Stats
    // ─────────────────────────────────────────────────────────────────────────

    public synchronized Map<String, Object> getStats() {
        Map<String, Object> s = new HashMap<>();
        s.put("realUpdates",   realUpdates.get());
        s.put("planningSteps", planningSteps.get());
        s.put("qTableSize",    qTable.size());
        s.put("modelSize",     transitionCounts.size());
        s.put("queueSize",     pQueue.size());
        s.put("avgTdError",    avgTdError);
        s.put("theta",         theta);
        s.put("nPlan",         nPlan);
        return s;
    }
}
