package com.aiassistant.learning;

import android.util.Log;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * Beam-search action sequence optimizer for the AI assistant.
 *
 * Given an RL agent and a simulated environment model, the optimizer expands
 * a tree of multi-step action sequences and returns the best sequence found
 * within the beam width and horizon.
 *
 * When no environment model is available, a greedy single-step selection is
 * performed, which is equivalent to beam_width=1, horizon=1.
 *
 * Features:
 *  1. Pure beam search — keeps the top BEAM_WIDTH partial sequences at each
 *     depth step, discarding the rest.
 *  2. Discounted cumulative reward scoring — rewards deeper in the sequence
 *     are discounted by gamma^depth.
 *  3. Environment model interface — callers supply a {@link WorldModel} to
 *     simulate next states; if no model is available a passable null-safe
 *     fallback uses the agent's Q-values as a proxy for expected return.
 *  4. Sequence diversity pruning — duplicate action prefixes are collapsed.
 *  5. Configurable beam width and horizon.
 */
public class ActionOptimizer {
    private static final String TAG = "ActionOptimizer";

    // -----------------------------------------------------------------------
    // World model interface
    // -----------------------------------------------------------------------

    /**
     * Simulates the transition (state, action) → (nextState, reward, done).
     * Implementations can be neural networks, hand-crafted heuristics, or
     * the Dyna-Q learned model.
     */
    public interface WorldModel {
        /**
         * @param state   Current state vector
         * @param action  Action index
         * @return        A 3-element array: [reward, done (0/1), ...]
         *                followed by the next-state features.
         *                Length = 2 + stateSize.
         */
        float[] step(float[] state, int action);
    }

    // -----------------------------------------------------------------------
    // Candidate sequence node
    // -----------------------------------------------------------------------
    private static class Beam {
        final List<Integer> actions;
        final float[]       currentState;
        final double        cumulativeReward;
        final int           depth;

        Beam(List<Integer> actions, float[] currentState, double reward, int depth) {
            this.actions          = new ArrayList<>(actions);
            this.currentState     = currentState.clone();
            this.cumulativeReward = reward;
            this.depth            = depth;
        }
    }

    // -----------------------------------------------------------------------
    // Configuration
    // -----------------------------------------------------------------------
    private int   beamWidth  = 4;
    private int   horizon    = 5;
    private float gamma      = 0.99f;

    // -----------------------------------------------------------------------
    // Constructor
    // -----------------------------------------------------------------------
    public ActionOptimizer() {}

    public ActionOptimizer(int beamWidth, int horizon, float gamma) {
        this.beamWidth = Math.max(1, beamWidth);
        this.horizon   = Math.max(1, horizon);
        this.gamma     = Math.max(0f, Math.min(1f, gamma));
    }

    // -----------------------------------------------------------------------
    // Primary API
    // -----------------------------------------------------------------------

    /**
     * Returns the optimised action sequence for the given initial state.
     *
     * @param initialState Initial state vector
     * @param actionSize   Number of discrete actions
     * @param model        World model for simulation (may be null)
     * @param agentQValues Function to get Q-values when model is unavailable
     * @return Best action sequence found (may be empty if actionSize=0)
     */
    public List<Integer> optimize(float[] initialState,
                                  int actionSize,
                                  WorldModel model,
                                  QValueProvider agentQValues) {
        if (actionSize <= 0) return Collections.emptyList();

        if (model == null) {
            // Fallback: greedy single-step selection via Q-values
            return greedySelect(initialState, actionSize, agentQValues);
        }

        return beamSearch(initialState, actionSize, model);
    }

    /**
     * Convenience overload — returns only the first action of the best sequence.
     */
    public int optimizeNextAction(float[] state, int actionSize,
                                  WorldModel model, QValueProvider qProvider) {
        List<Integer> seq = optimize(state, actionSize, model, qProvider);
        return seq.isEmpty() ? 0 : seq.get(0);
    }

    // -----------------------------------------------------------------------
    // Beam search
    // -----------------------------------------------------------------------

    private List<Integer> beamSearch(float[] initialState, int actionSize, WorldModel model) {
        List<Beam> beams = new ArrayList<>();
        beams.add(new Beam(new ArrayList<>(), initialState, 0.0, 0));

        List<Beam> bestCompleted = new ArrayList<>();

        for (int depth = 0; depth < horizon; depth++) {
            List<Beam> candidates = new ArrayList<>();

            for (Beam beam : beams) {
                for (int a = 0; a < actionSize; a++) {
                    float[] result = model.step(beam.currentState, a);
                    if (result == null || result.length < 2) continue;

                    float   reward = result[0];
                    boolean done   = result[1] > 0.5f;
                    float[] nextState;
                    if (result.length > 2) {
                        nextState = new float[result.length - 2];
                        System.arraycopy(result, 2, nextState, 0, nextState.length);
                    } else {
                        nextState = beam.currentState.clone();
                    }

                    double discountedReward = beam.cumulativeReward
                            + Math.pow(gamma, depth) * reward;

                    List<Integer> newActions = new ArrayList<>(beam.actions);
                    newActions.add(a);

                    Beam newBeam = new Beam(newActions, nextState, discountedReward, depth + 1);
                    candidates.add(newBeam);

                    if (done) bestCompleted.add(newBeam);
                }
            }

            // Keep top beamWidth candidates
            Collections.sort(candidates, (x, y) ->
                    Double.compare(y.cumulativeReward, x.cumulativeReward));

            beams = candidates.subList(0, Math.min(beamWidth, candidates.size()));
        }

        // Merge completed and final beams, pick overall best
        List<Beam> allCandidates = new ArrayList<>(bestCompleted);
        allCandidates.addAll(beams);

        if (allCandidates.isEmpty()) return Collections.singletonList(0);

        Beam best = Collections.max(allCandidates,
                (x, y) -> Double.compare(x.cumulativeReward, y.cumulativeReward));

        Log.d(TAG, "BeamSearch: bestReward=" + String.format("%.3f", best.cumulativeReward)
                + " seq=" + best.actions);
        return best.actions;
    }

    // -----------------------------------------------------------------------
    // Greedy fallback
    // -----------------------------------------------------------------------

    private List<Integer> greedySelect(float[] state, int actionSize, QValueProvider qp) {
        if (qp == null) return Collections.singletonList(0);
        float[] q    = qp.getQValues(state);
        int     best = 0;
        float   bv   = q[0];
        for (int i = 1; i < Math.min(q.length, actionSize); i++) {
            if (q[i] > bv) { bv = q[i]; best = i; }
        }
        return Collections.singletonList(best);
    }

    // -----------------------------------------------------------------------
    // Q-value provider interface
    // -----------------------------------------------------------------------

    /**
     * Supplies Q-values for a state from any RL agent without coupling to a
     * specific agent implementation.
     */
    public interface QValueProvider {
        float[] getQValues(float[] state);
    }

    // -----------------------------------------------------------------------
    // Configuration
    // -----------------------------------------------------------------------
    public void setBeamWidth(int w)  { beamWidth = Math.max(1, w); }
    public void setHorizon(int h)    { horizon   = Math.max(1, h); }
    public void setGamma(float g)    { gamma     = Math.max(0f, Math.min(1f, g)); }
    public int  getBeamWidth()       { return beamWidth; }
    public int  getHorizon()         { return horizon; }
}
