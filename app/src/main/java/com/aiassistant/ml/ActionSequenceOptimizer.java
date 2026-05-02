package com.aiassistant.ml;

import android.util.Log;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

/**
 * ActionSequenceOptimizer — plans optimal multi-step action sequences using
 * a lightweight on-device beam search over the agent's learned Q-model.
 *
 * Algorithm:
 *   Beam Search with configurable beam width and horizon:
 *   1. Start with a single root node containing the current state.
 *   2. At each step, expand each beam node by all possible actions,
 *      evaluating the expected cumulative Q-value via the provided Q-model.
 *   3. Keep only the top-K sequences (the "beam").
 *   4. After `horizon` steps, return the first action of the highest-scoring sequence.
 *
 * Features:
 *   - Pluggable Q-value oracle (any class implementing QOracle interface).
 *   - Action masking: pass a boolean[] to exclude illegal/cooldown-blocked actions.
 *   - Discount factor γ for multi-step returns.
 *   - Optional lookahead diversity bonus: penalise sequences that share the same first action
 *     (encourages diverse beam hypothesis exploration).
 *   - Caches partial sequences to avoid redundant Q-function calls.
 *   - Statistics: tracks calls, avg planning time, best sequence score history.
 *
 * Usage:
 *   ActionSequenceOptimizer aso = new ActionSequenceOptimizer(actionDim, beamWidth=5, horizon=4, γ=0.99);
 *   aso.setOracle((state, action) -> myQNetwork.qValue(state, action));
 *   int bestFirstAction = aso.plan(currentState, actionMask);
 */
public class ActionSequenceOptimizer {

    private static final String TAG = "ActionSeqOptimizer";

    // -------------------------------------------------------------------------
    // Q-value oracle interface
    // -------------------------------------------------------------------------
    public interface QOracle {
        /**
         * Return Q(state, action) — the estimated value of taking action in state.
         *
         * @param state  Encoded state feature vector.
         * @param action Action index.
         * @return Scalar Q-value.
         */
        float q(float[] state, int action);

        /**
         * (Optional) Transition model: predict the next state given (state, action).
         * Return null to use the current state as a proxy (greedy approximation).
         */
        default float[] predictNextState(float[] state, int action) { return state; }
    }

    // -------------------------------------------------------------------------
    // Beam node
    // -------------------------------------------------------------------------
    private static class BeamNode {
        final float[] state;
        final List<Integer> actions;   // sequence of actions taken so far
        final float   cumulativeScore; // discounted cumulative Q-value

        BeamNode(float[] state, List<Integer> actions, float score) {
            this.state           = state;
            this.actions         = actions;
            this.cumulativeScore = score;
        }
    }

    // -------------------------------------------------------------------------
    // Fields
    // -------------------------------------------------------------------------
    private final int   actionDim;
    private final int   beamWidth;
    private final int   horizon;
    private final float gamma;
    private final float diversityPenalty; // penalty for beam nodes with duplicate first action

    private QOracle oracle;

    // Stats
    private final AtomicInteger planCalls     = new AtomicInteger(0);
    private final AtomicLong    totalPlanMs   = new AtomicLong(0);
    private float               avgBestScore  = 0f;

    // Cache: (stateHash + action) → q-value
    private final Map<String, Float> qCache   = new HashMap<>();
    private static final int         CACHE_MAX = 2000;

    // -------------------------------------------------------------------------
    // Construction
    // -------------------------------------------------------------------------

    /**
     * @param actionDim      Number of discrete actions.
     * @param beamWidth      Beam width K (number of hypotheses retained per step).
     * @param horizon        Planning horizon H (steps to look ahead).
     * @param gamma          Discount factor.
     * @param diversityPenalty Penalty for duplicate first action in beam (0 = disabled).
     */
    public ActionSequenceOptimizer(int actionDim, int beamWidth, int horizon,
                                    float gamma, float diversityPenalty) {
        this.actionDim       = actionDim;
        this.beamWidth       = Math.max(1, beamWidth);
        this.horizon         = Math.max(1, horizon);
        this.gamma           = Math.min(1.0f, Math.max(0f, gamma));
        this.diversityPenalty = diversityPenalty;
    }

    /** Convenience constructor with sensible defaults. */
    public ActionSequenceOptimizer(int actionDim) {
        this(actionDim, 5, 4, 0.99f, 0.1f);
    }

    public void setOracle(QOracle oracle) { this.oracle = oracle; }

    // -------------------------------------------------------------------------
    // Planning
    // -------------------------------------------------------------------------

    /**
     * Run beam search from the current state and return the best first action.
     *
     * @param state      Current state feature vector.
     * @param actionMask Boolean mask; mask[a]=false means action a is forbidden.
     *                   Pass null to allow all actions.
     * @return Best first action index, or 0 if planning fails.
     */
    public synchronized int plan(float[] state, boolean[] actionMask) {
        if (oracle == null || state == null) return 0;

        long t0 = System.currentTimeMillis();
        planCalls.incrementAndGet();

        // Initialize beam with empty sequence
        List<BeamNode> beam = new ArrayList<>();
        beam.add(new BeamNode(state, new ArrayList<>(), 0f));

        for (int step = 0; step < horizon; step++) {
            beam = expandBeam(beam, actionMask, step);
            if (beam.isEmpty()) break;
        }

        // Select the best sequence
        BeamNode best = selectBest(beam);
        int bestFirstAction = 0;
        if (best != null && !best.actions.isEmpty()) {
            bestFirstAction = best.actions.get(0);
            float score = best.cumulativeScore;
            avgBestScore = 0.95f * avgBestScore + 0.05f * score;
        }

        long elapsed = System.currentTimeMillis() - t0;
        totalPlanMs.addAndGet(elapsed);

        if (planCalls.get() % 50 == 0) {
            Log.d(TAG, "Plan #" + planCalls.get()
                    + " bestAction=" + bestFirstAction
                    + " avgScore=" + String.format("%.3f", avgBestScore)
                    + " avgMs=" + (totalPlanMs.get() / planCalls.get()));
        }

        return bestFirstAction;
    }

    /** Convenience: plan without action mask. */
    public int plan(float[] state) {
        return plan(state, null);
    }

    // -------------------------------------------------------------------------
    // Beam expansion
    // -------------------------------------------------------------------------

    private List<BeamNode> expandBeam(List<BeamNode> beam, boolean[] mask, int step) {
        float discount = (float) Math.pow(gamma, step);
        PriorityQueue<BeamNode> candidates = new PriorityQueue<>(
                beamWidth * actionDim + 1,
                Comparator.comparingDouble(n -> -n.cumulativeScore)); // max-heap by negating

        Map<Integer, Integer> firstActionCount = new HashMap<>();

        for (BeamNode node : beam) {
            for (int a = 0; a < actionDim; a++) {
                if (mask != null && a < mask.length && !mask[a]) continue;

                float q = cachedQ(node.state, a);
                float diversity = 0f;

                // Diversity penalty: count beam nodes with same first action
                int firstAction = node.actions.isEmpty() ? a : node.actions.get(0);
                if (step == 0 && diversityPenalty > 0) {
                    int cnt = firstActionCount.getOrDefault(firstAction, 0);
                    diversity = -diversityPenalty * cnt;
                    firstActionCount.put(firstAction, cnt + 1);
                }

                float newScore = node.cumulativeScore + discount * q + diversity;
                float[] nextState = oracle.predictNextState(node.state, a);
                if (nextState == null) nextState = node.state;

                List<Integer> newActions = new ArrayList<>(node.actions);
                newActions.add(a);

                candidates.offer(new BeamNode(nextState, newActions, newScore));
            }
        }

        // Trim to beam width
        List<BeamNode> newBeam = new ArrayList<>(beamWidth);
        while (!candidates.isEmpty() && newBeam.size() < beamWidth) {
            newBeam.add(candidates.poll());
        }
        return newBeam;
    }

    private BeamNode selectBest(List<BeamNode> beam) {
        if (beam.isEmpty()) return null;
        BeamNode best = beam.get(0);
        for (BeamNode n : beam) if (n.cumulativeScore > best.cumulativeScore) best = n;
        return best;
    }

    // -------------------------------------------------------------------------
    // Q-value cache
    // -------------------------------------------------------------------------

    private float cachedQ(float[] state, int action) {
        if (oracle == null) return 0f;
        // Light cache key: use reference identity + action (fast, approximate)
        String key = System.identityHashCode(state) + ":" + action;
        Float cached = qCache.get(key);
        if (cached != null) return cached;
        float q = oracle.q(state, action);
        if (qCache.size() >= CACHE_MAX) qCache.clear(); // simple eviction
        qCache.put(key, q);
        return q;
    }

    public void clearCache() { qCache.clear(); }

    // -------------------------------------------------------------------------
    // Monitoring
    // -------------------------------------------------------------------------

    public Map<String, Object> getStats() {
        Map<String, Object> s = new HashMap<>();
        int calls = planCalls.get();
        s.put("planCalls",    calls);
        s.put("avgPlanMs",    calls > 0 ? totalPlanMs.get() / calls : 0L);
        s.put("avgBestScore", avgBestScore);
        s.put("beamWidth",    beamWidth);
        s.put("horizon",      horizon);
        s.put("gamma",        gamma);
        s.put("cacheSize",    qCache.size());
        return s;
    }
}
