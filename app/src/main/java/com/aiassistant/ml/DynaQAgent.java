package com.aiassistant.ml;

import android.util.Log;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * DynaQAgent — model-based RL combining Q-learning with simulated experience.
 *
 * Algorithm (Sutton, 1990):
 *   Each real step:
 *     1. Update Q via real transition (s, a, r, s')  [Q-learning rule]
 *     2. Update EnvironmentSimulator with (s, a, r, s')
 *     3. Generate K synthetic transitions via EnvironmentSimulator
 *     4. Update Q using each synthetic transition
 *
 * Components:
 *   QTableManager        — LRU Q-table with adaptive learning rate + Q(λ)
 *   EnvironmentSimulator — learned transition + reward + terminal models
 *   IntrinsicMotivation  — count-based exploration bonus on real steps
 *   AdaptiveExplorationStrategy — meta-bandit ε management
 *
 * Advantages of Dyna-Q:
 *   - Dramatically improves sample efficiency by reusing real data
 *   - Planning depth (K) is a free parameter: higher K = more planning per step
 *   - Works even when the environment model is imperfect
 *
 * Thread-safe.
 */
public class DynaQAgent {

    private static final String TAG = "DynaQAgent";

    // ── sub-systems ───────────────────────────────────────────────────────────
    private final QTableManager              qTable;
    private final EnvironmentSimulator       envSim;
    private final IntrinsicMotivation        intrinsic;
    private final AdaptiveExplorationStrategy exploration;

    // ── config ────────────────────────────────────────────────────────────────
    private final int   stateDim;
    private final int   actionDim;
    private final int   kPlanningSteps;    // synthetic steps per real step
    private final float discountFactor;
    private float       epsilon;

    // ── seed state buffer for Dyna planning ──────────────────────────────────
    private final List<float[]> seenStates   = new ArrayList<>();
    private static final int    MAX_SEEDS    = 500;

    // ── stats ──────────────────────────────────────────────────────────────────
    private final AtomicInteger realSteps      = new AtomicInteger(0);
    private final AtomicInteger syntheticSteps = new AtomicInteger(0);
    private final AtomicInteger episodeCount   = new AtomicInteger(0);
    private float avgTdError   = 0f;
    private float avgReturn    = 0f;
    private float episodeReturn= 0f;

    private final Random rng = new Random(37L);

    // ─────────────────────────────────────────────────────────────────────────
    public DynaQAgent(int stateDim, int actionDim, int kPlanningSteps,
                      float discountFactor, float alphaBase, float epsilon) {
        this.stateDim       = stateDim;
        this.actionDim      = actionDim;
        this.kPlanningSteps = kPlanningSteps;
        this.discountFactor = discountFactor;
        this.epsilon        = epsilon;

        qTable     = new QTableManager(stateDim, actionDim, 10,
                                        alphaBase, 0.01f, discountFactor);
        envSim     = new EnvironmentSimulator(stateDim, actionDim);
        intrinsic  = new IntrinsicMotivation(stateDim, actionDim);
        exploration= new AdaptiveExplorationStrategy(actionDim);
    }

    /** Defaults: K=10, γ=0.99, α=0.1, ε=0.15. */
    public DynaQAgent(int stateDim, int actionDim) {
        this(stateDim, actionDim, 10, 0.99f, 0.1f, 0.15f);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Action selection
    // ─────────────────────────────────────────────────────────────────────────

    public synchronized int selectAction(float[] state) {
        float[] qValues = qTable.getQRow(state);
        return exploration.selectAction(qValues);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Observation: real environment step
    // ─────────────────────────────────────────────────────────────────────────

    public synchronized float observe(float[] state, int action, float reward,
                                       float[] nextState, boolean done) {
        // Intrinsic bonus
        float intBonus = intrinsic.computeBonus(state, action, nextState);
        float shapedR  = reward + intBonus;

        // ── Real Q update ──────────────────────────────────────────────────
        float tdError = qTable.updateQLambda(state, action, shapedR, nextState, done);
        avgTdError    = 0.99f * avgTdError + 0.01f * Math.abs(tdError);
        episodeReturn += reward;
        realSteps.incrementAndGet();

        if (done) {
            qTable.clearTraces();
            episodeCount.incrementAndGet();
            avgReturn     = 0.99f * avgReturn + 0.01f * episodeReturn;
            episodeReturn = 0f;
            exploration.onEpisodeEnd(reward);
        }

        // ── Update environment model ───────────────────────────────────────
        envSim.learn(state, action, shapedR, nextState, done);

        // ── Store seed for planning ────────────────────────────────────────
        if (seenStates.size() < MAX_SEEDS) {
            seenStates.add(state.clone());
        } else {
            seenStates.set(rng.nextInt(MAX_SEEDS), state.clone());
        }

        // ── Dyna-Q planning: K synthetic updates ─────────────────────────
        dynaPlanning();

        if (realSteps.get() % 500 == 0) {
            Log.d(TAG, "RealSteps=" + realSteps.get()
                    + " synthSteps=" + syntheticSteps.get()
                    + " avgTD=" + String.format("%.4f", avgTdError)
                    + " avgReturn=" + String.format("%.2f", avgReturn)
                    + " tableSize=" + qTable.tableSize());
        }
        return tdError;
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Dyna planning
    // ─────────────────────────────────────────────────────────────────────────

    private void dynaPlanning() {
        if (seenStates.isEmpty()) return;
        for (int k = 0; k < kPlanningSteps; k++) {
            // Random seed state from buffer
            float[] seedState = seenStates.get(rng.nextInt(seenStates.size()));
            int     seedAction = rng.nextInt(actionDim);

            // Generate one synthetic transition
            List<float[]> rollout = envSim.generateRollout(seedState, 1);
            if (rollout.isEmpty()) continue;

            float[] rec     = rollout.get(0);
            float[] s       = new float[stateDim];
            float[] ns      = new float[stateDim];
            System.arraycopy(rec, 0,           s,  0, stateDim);
            System.arraycopy(rec, stateDim + 3, ns, 0, stateDim);
            int   a  = (int) rec[stateDim];
            float r  = rec[stateDim + 1];
            boolean d = rec[stateDim + 2] > 0.5f;

            qTable.updateQLearning(s, a, r, ns, d);
            syntheticSteps.incrementAndGet();
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Stats
    // ─────────────────────────────────────────────────────────────────────────

    public Map<String, Object> getStats() {
        Map<String, Object> s = new HashMap<>();
        s.put("realSteps",       realSteps.get());
        s.put("syntheticSteps",  syntheticSteps.get());
        s.put("episodeCount",    episodeCount.get());
        s.put("avgTdError",      avgTdError);
        s.put("avgReturn",       avgReturn);
        s.put("epsilon",         exploration.getCurrentEpsilon());
        s.put("qTable",          qTable.getStats());
        s.put("envSim",          envSim.getStats());
        s.put("intrinsic",       intrinsic.getStats());
        return s;
    }
}
