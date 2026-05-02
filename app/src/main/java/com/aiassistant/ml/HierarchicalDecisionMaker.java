package com.aiassistant.ml;

import android.util.Log;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * HierarchicalDecisionMaker — two-level hierarchical RL using the Options Framework.
 *
 * Architecture:
 *   ┌─────────────────────────────────┐
 *   │         Meta-Controller         │  selects OPTIONS (macro-actions)
 *   │   Q_meta(s, ω) — tabular/linear │
 *   └──────────────┬──────────────────┘
 *                  │ ω (option index)
 *   ┌──────────────▼──────────────────┐
 *   │   Intra-Option Policy π_ω(a|s)  │  executes primitive actions until
 *   │   (separate for each option)    │  termination condition β_ω(s) fires
 *   └─────────────────────────────────┘
 *
 * Options:
 *   EXPLORE_AREA   — random walk / curiosity-driven exploration sub-policy
 *   ATTACK_ENEMY   — aggressive towards nearest detected threat
 *   COLLECT_ITEMS  — navigate and collect resources
 *   DEFEND         — dodge, block, maintain distance from threats
 *   USE_SKILL      — charge and release a special ability
 *   RETREAT        — move away from combat when low health
 *
 * Training:
 *   Meta-controller: Q-learning update over options.
 *   Intra-option policies: ε-greedy Q-learning over primitive actions.
 *   Termination: fixed step budget per option + state-dependent β.
 *
 * Both levels share a feature encoder (the passed state vector).
 * Thread-safe.
 */
public class HierarchicalDecisionMaker {

    private static final String TAG = "HierarchicalDM";

    // -------------------------------------------------------------------------
    // Options (macro-actions)
    // -------------------------------------------------------------------------
    public enum Option {
        EXPLORE_AREA,
        ATTACK_ENEMY,
        COLLECT_ITEMS,
        DEFEND,
        USE_SKILL,
        RETREAT
    }
    private static final Option[] ALL_OPTIONS = Option.values();
    private static final int NUM_OPTIONS = ALL_OPTIONS.length;

    // -------------------------------------------------------------------------
    // Constants
    // -------------------------------------------------------------------------
    private static final int   MAX_OPTION_STEPS = 20;   // max primitive steps per option
    private static final float GAMMA_META        = 0.95f;
    private static final float GAMMA_INTRA       = 0.99f;
    private static final float LR_META           = 0.01f;
    private static final float LR_INTRA          = 0.05f;
    private static final float EPSILON_META      = 0.15f;
    private static final float EPSILON_INTRA     = 0.20f;

    // -------------------------------------------------------------------------
    // Fields
    // -------------------------------------------------------------------------
    private final int stateDim;
    private final int actionDim;

    // Meta-controller: linear Q-table over options — W_meta[NUM_OPTIONS][stateDim]
    private final float[][] Q_meta;
    // Intra-option Q-tables: Q_intra[option][action][stateDim] → linear
    private final float[][][] Q_intra;

    // Current option state
    private Option  currentOption       = null;
    private int     optionStepsRemaining = 0;
    private float[] optionStartState    = null;
    private float   optionCumReward     = 0f;

    private final AtomicInteger metaUpdates  = new AtomicInteger(0);
    private final AtomicInteger intraUpdates = new AtomicInteger(0);
    private final AtomicInteger optionSwitches = new AtomicInteger(0);

    // Per-option stats
    private final Map<Option, float[]> optionStats = new HashMap<>();
    // float[]: {totalReward, episodeCount, avgSteps}

    private final NeuralNetworkOptimizer optimizer;
    private final Random rng = new Random(55L);

    // -------------------------------------------------------------------------
    // Construction
    // -------------------------------------------------------------------------

    public HierarchicalDecisionMaker(int stateDim, int actionDim) {
        this.stateDim  = stateDim;
        this.actionDim = actionDim;
        this.optimizer = new NeuralNetworkOptimizer(LR_META);

        float scale = (float) Math.sqrt(2.0 / (stateDim + actionDim));
        Q_meta = new float[NUM_OPTIONS][stateDim];
        for (int o = 0; o < NUM_OPTIONS; o++)
            for (int s = 0; s < stateDim; s++)
                Q_meta[o][s] = (rng.nextFloat() * 2f - 1f) * scale;

        Q_intra = new float[NUM_OPTIONS][actionDim][stateDim];
        for (int o = 0; o < NUM_OPTIONS; o++)
            for (int a = 0; a < actionDim; a++)
                for (int s = 0; s < stateDim; s++)
                    Q_intra[o][a][s] = (rng.nextFloat() * 2f - 1f) * scale;

        for (Option opt : ALL_OPTIONS) optionStats.put(opt, new float[3]);
    }

    // -------------------------------------------------------------------------
    // Core decision loop
    // -------------------------------------------------------------------------

    /**
     * Select the next primitive action given the current state.
     * Automatically manages option selection and termination.
     *
     * @param state       Current state feature vector.
     * @param gameContext Contextual hints (e.g. health, enemyPresent, resourceNearby).
     * @return Primitive action index in [0, actionDim).
     */
    public synchronized int selectAction(float[] state, Map<String, Object> gameContext) {
        // Check if current option should terminate
        if (currentOption == null || shouldTerminate(state, gameContext)) {
            startNewOption(state, gameContext);
        }

        // Execute intra-option policy
        optionStepsRemaining--;
        return intraOptionAction(state, currentOption);
    }

    /**
     * Update both levels after observing a reward.
     *
     * @param state      State before action.
     * @param action     Primitive action taken.
     * @param reward     Extrinsic reward.
     * @param nextState  State after action.
     * @param done       Episode terminal flag.
     */
    public synchronized void update(float[] state, int action, float reward,
                                     float[] nextState, boolean done) {
        if (currentOption == null) return;
        optionCumReward += reward;

        // ---- Intra-option Q update ----
        updateIntraOption(state, action, reward, nextState, currentOption, done);

        // ---- Meta-controller update on option termination or episode end ----
        boolean terminated = optionStepsRemaining <= 0 || done
                || shouldTerminate(nextState, null);
        if (terminated || done) {
            updateMetaController(optionStartState, currentOption, optionCumReward, nextState, done);
            updateOptionStats(currentOption, optionCumReward);
            if (done) currentOption = null;
        }
    }

    // -------------------------------------------------------------------------
    // Option management
    // -------------------------------------------------------------------------

    private void startNewOption(float[] state, Map<String, Object> gameContext) {
        // Meta-controller ε-greedy option selection
        Option newOption;
        if (rng.nextFloat() < EPSILON_META) {
            newOption = ALL_OPTIONS[rng.nextInt(NUM_OPTIONS)];
        } else {
            // Contextual override first
            Option contextOption = contextualOptionHint(gameContext);
            newOption = contextOption != null ? contextOption : greedyMetaOption(state);
        }

        currentOption        = newOption;
        optionStepsRemaining = MAX_OPTION_STEPS;
        optionStartState     = state != null ? state.clone() : null;
        optionCumReward      = 0f;
        optionSwitches.incrementAndGet();

        Log.d(TAG, "New option: " + newOption + " (switch #" + optionSwitches.get() + ")");
    }

    /** State-dependent termination condition β_ω(s). */
    private boolean shouldTerminate(float[] state, Map<String, Object> ctx) {
        if (optionStepsRemaining <= 0) return true;
        if (currentOption == null) return true;

        // Contextual termination for health-sensitive options
        if (ctx != null) {
            float health = getFloat(ctx, "health");
            if (currentOption == Option.ATTACK_ENEMY && health < 0.2f) return true;
            if (currentOption == Option.COLLECT_ITEMS && getBoolean(ctx, "enemyPresent")) return true;
            if (currentOption == Option.RETREAT      && health > 0.6f) return true;
        }

        return false;
    }

    // -------------------------------------------------------------------------
    // Intra-option policy
    // -------------------------------------------------------------------------

    private int intraOptionAction(float[] state, Option option) {
        if (rng.nextFloat() < EPSILON_INTRA) return rng.nextInt(actionDim);

        int optIdx = option.ordinal();
        int bestAction = 0;
        float bestQ = Float.NEGATIVE_INFINITY;

        for (int a = 0; a < actionDim; a++) {
            float q = dotProduct(Q_intra[optIdx][a], state);
            if (q > bestQ) { bestQ = q; bestAction = a; }
        }

        // Option-specific action bias
        bestAction = applyOptionBias(option, bestAction, state);
        return bestAction;
    }

    private void updateIntraOption(float[] state, int action, float reward,
                                    float[] nextState, Option option, boolean done) {
        int optIdx = option.ordinal();

        // Bellman target
        float maxNextQ = 0f;
        if (!done) {
            for (int a = 0; a < actionDim; a++) {
                float q = dotProduct(Q_intra[optIdx][a], nextState);
                if (q > maxNextQ) maxNextQ = q;
            }
        }
        float target  = reward + GAMMA_INTRA * maxNextQ;
        float current = dotProduct(Q_intra[optIdx][action], state);
        float delta   = target - current;

        // Gradient update: dL/dW = −δ · state
        float[] grad = new float[stateDim];
        int dim = Math.min(state.length, stateDim);
        for (int s = 0; s < dim; s++) grad[s] = -delta * state[s];

        float[][] W   = new float[][]{Q_intra[optIdx][action]};
        float[][] dW  = new float[][]{grad};
        optimizer.step("intra_" + optIdx + "_" + action, W, dW);
        System.arraycopy(W[0], 0, Q_intra[optIdx][action], 0, stateDim);

        intraUpdates.incrementAndGet();
    }

    // -------------------------------------------------------------------------
    // Meta-controller update
    // -------------------------------------------------------------------------

    private void updateMetaController(float[] startState, Option option,
                                       float cumReward, float[] endState, boolean done) {
        if (startState == null) return;
        int optIdx = option.ordinal();

        // Best next option Q value
        float maxNextQ = 0f;
        if (!done) {
            for (int o = 0; o < NUM_OPTIONS; o++) {
                float q = dotProduct(Q_meta[o], endState);
                if (q > maxNextQ) maxNextQ = q;
            }
        }
        float target  = cumReward + GAMMA_META * maxNextQ;
        float current = dotProduct(Q_meta[optIdx], startState);
        float delta   = target - current;

        float[] grad = new float[stateDim];
        int dim = Math.min(startState.length, stateDim);
        for (int s = 0; s < dim; s++) grad[s] = -delta * startState[s];

        float[][] W  = new float[][]{Q_meta[optIdx]};
        float[][] dW = new float[][]{grad};
        optimizer.step("meta_" + optIdx, W, dW);
        System.arraycopy(W[0], 0, Q_meta[optIdx], 0, stateDim);

        metaUpdates.incrementAndGet();
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    private Option greedyMetaOption(float[] state) {
        Option best = ALL_OPTIONS[0];
        float  bestQ = Float.NEGATIVE_INFINITY;
        for (int o = 0; o < NUM_OPTIONS; o++) {
            float q = dotProduct(Q_meta[o], state);
            if (q > bestQ) { bestQ = q; best = ALL_OPTIONS[o]; }
        }
        return best;
    }

    /** Context-based option hint: shortcut for obvious situations. */
    private Option contextualOptionHint(Map<String, Object> ctx) {
        if (ctx == null) return null;
        float health = getFloat(ctx, "health");
        if (health < 0.15f) return Option.RETREAT;
        if (health < 0.4f)  return Option.DEFEND;
        if (getBoolean(ctx, "bossPresent")) return Option.USE_SKILL;
        if (getBoolean(ctx, "enemyPresent") && health > 0.5f) return Option.ATTACK_ENEMY;
        if (getBoolean(ctx, "resourceNearby")) return Option.COLLECT_ITEMS;
        return null;
    }

    /** Small bias to prefer actions relevant to the current option. */
    private int applyOptionBias(Option option, int bestAction, float[] state) {
        // For now, just return the Q-greedy action as-is
        // (In a real implementation, each option would have a sub-goal embedding)
        return bestAction;
    }

    private void updateOptionStats(Option option, float cumReward) {
        float[] s = optionStats.get(option);
        if (s == null) return;
        s[0] += cumReward;
        s[1]++;
        int steps = MAX_OPTION_STEPS - optionStepsRemaining;
        s[2] = s[1] > 0 ? (s[2] * (s[1] - 1) + steps) / s[1] : steps;
    }

    private float dotProduct(float[] w, float[] x) {
        float sum = 0;
        int   dim = Math.min(Math.min(w.length, x.length), stateDim);
        for (int i = 0; i < dim; i++) sum += w[i] * x[i];
        return sum;
    }

    private static float getFloat(Map<String, Object> m, String key) {
        Object v = m.get(key);
        if (v instanceof Number) return ((Number)v).floatValue();
        return 0f;
    }

    private static boolean getBoolean(Map<String, Object> m, String key) {
        Object v = m.get(key);
        if (v instanceof Boolean) return (Boolean)v;
        if (v instanceof Number)  return ((Number)v).floatValue() > 0f;
        return false;
    }

    // -------------------------------------------------------------------------
    // Monitoring
    // -------------------------------------------------------------------------

    public Option getCurrentOption() { return currentOption; }

    public Map<String, Object> getStats() {
        Map<String, Object> s = new HashMap<>();
        s.put("currentOption",    currentOption != null ? currentOption.name() : "none");
        s.put("optionSwitches",   optionSwitches.get());
        s.put("metaUpdates",      metaUpdates.get());
        s.put("intraUpdates",     intraUpdates.get());
        s.put("optionStepsLeft",  optionStepsRemaining);

        Map<String, Object> os = new HashMap<>();
        for (Map.Entry<Option, float[]> e : optionStats.entrySet()) {
            Map<String, Object> od = new HashMap<>();
            od.put("totalReward",  e.getValue()[0]);
            od.put("episodeCount", (int) e.getValue()[1]);
            od.put("avgSteps",     e.getValue()[2]);
            os.put(e.getKey().name(), od);
        }
        s.put("optionStats", os);
        return s;
    }
}
