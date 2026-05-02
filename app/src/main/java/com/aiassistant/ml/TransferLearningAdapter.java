package com.aiassistant.ml;

import android.util.Log;

import com.aiassistant.rl.RLAgent;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Transfer Learning Adapter — enables knowledge transfer between game types.
 *
 * Problem: training a new game from scratch requires thousands of episodes.
 * Transfer learning re-uses policies learned for similar source games as a
 * warm start for the target game.
 *
 * Mechanism:
 *  1. Feature mapping — maps source-game state features onto target-game
 *     features using a hand-crafted or learned alignment table.  Features with
 *     no mapping are initialised to 0.
 *
 *  2. Policy transfer — the source agent is queried for a Q-value estimate
 *     over the mapped state; the result is blended with the target agent's
 *     own Q-values using a transfer coefficient τ that decays toward 0 as
 *     the target accumulates experience.
 *
 *  3. Progressive fine-tuning — τ decays as:
 *         τ(n) = τ₀ · exp(−n / DECAY_STEPS)
 *     where n is the number of steps the target agent has trained.
 *     After DECAY_STEPS × 3 steps τ ≈ 0 and the target is independent.
 *
 *  4. Negative transfer protection — per-action transfer scores are tracked.
 *     If a transferred action's success rate falls below NEGATIVE_THRESHOLD,
 *     its τ contribution is zeroed out for that action index.
 *
 *  5. State normalisation — each mapped feature is min-max normalised using
 *     running statistics to prevent scale mismatch between games.
 *
 *  6. Alignment API — callers provide a {@link FeatureAlignment} that maps
 *     source feature index → target feature index (or −1 if no mapping).
 */
public class TransferLearningAdapter {
    private static final String TAG            = "TransferLearningAdapter";
    private static final int    DECAY_STEPS    = 500;
    private static final float  TAU_INITIAL    = 0.8f;
    private static final float  NEGATIVE_THRESHOLD = 0.35f; // below this success rate → disable

    // -----------------------------------------------------------------------
    // Feature alignment
    // -----------------------------------------------------------------------

    /**
     * Maps source feature index → target feature index.
     * Return −1 to indicate "no mapping" for a given source index.
     */
    public interface FeatureAlignment {
        int mapFeature(int sourceIndex, int sourceSize, int targetSize);
    }

    /** Default alignment: wraps source indices modulo target size. */
    public static final FeatureAlignment MODULO_ALIGNMENT =
            (src, srcSize, tgtSize) -> src % tgtSize;

    /** Identity alignment (both games use same feature layout). */
    public static final FeatureAlignment IDENTITY_ALIGNMENT =
            (src, srcSize, tgtSize) -> src < tgtSize ? src : -1;

    /** No alignment — transfer is disabled. */
    public static final FeatureAlignment NO_ALIGNMENT = (src, srcSize, tgtSize) -> -1;

    // -----------------------------------------------------------------------
    // Per-action transfer stats
    // -----------------------------------------------------------------------
    private static class ActionTransferStats {
        int   uses      = 0;
        int   successes = 0;
        boolean disabled = false;

        void record(boolean success) {
            uses++; if (success) successes++;
            if (uses >= 10 && successRate() < NEGATIVE_THRESHOLD) disabled = true;
        }
        float successRate() { return uses > 0 ? (float) successes / uses : 0.5f; }
    }

    // -----------------------------------------------------------------------
    // Fields
    // -----------------------------------------------------------------------
    private final RLAgent         sourceAgent;
    private final RLAgent         targetAgent;
    private final int             sourceStateSize;
    private final int             targetStateSize;
    private final int             actionSize;
    private final FeatureAlignment alignment;

    private float                 tau            = TAU_INITIAL;
    private int                   targetSteps    = 0;

    private final ActionTransferStats[] actionStats;
    private final AtomicInteger   transferCount  = new AtomicInteger(0);

    // Running min/max for source feature normalisation
    private final float[] srcMin, srcMax;

    // -----------------------------------------------------------------------
    // Constructor
    // -----------------------------------------------------------------------
    public TransferLearningAdapter(RLAgent sourceAgent, int sourceStateSize,
                                   RLAgent targetAgent, int targetStateSize,
                                   int actionSize) {
        this(sourceAgent, sourceStateSize, targetAgent, targetStateSize,
             actionSize, MODULO_ALIGNMENT);
    }

    public TransferLearningAdapter(RLAgent sourceAgent, int sourceStateSize,
                                   RLAgent targetAgent, int targetStateSize,
                                   int actionSize, FeatureAlignment alignment) {
        this.sourceAgent     = sourceAgent;
        this.sourceStateSize = sourceStateSize;
        this.targetAgent     = targetAgent;
        this.targetStateSize = targetStateSize;
        this.actionSize      = Math.max(1, actionSize);
        this.alignment       = alignment != null ? alignment : MODULO_ALIGNMENT;

        this.actionStats = new ActionTransferStats[this.actionSize];
        for (int i = 0; i < this.actionSize; i++) actionStats[i] = new ActionTransferStats();

        this.srcMin = new float[sourceStateSize];
        this.srcMax = new float[sourceStateSize];
        Arrays.fill(srcMin, Float.MAX_VALUE);
        Arrays.fill(srcMax, Float.MIN_VALUE);

        Log.i(TAG, "TransferLearningAdapter created τ=" + TAU_INITIAL
                + " srcDim=" + sourceStateSize + " tgtDim=" + targetStateSize
                + " actions=" + actionSize);
    }

    // -----------------------------------------------------------------------
    // Primary API
    // -----------------------------------------------------------------------

    /**
     * Returns the best action using the blended policy.
     *
     * @param targetState  State vector in target-game space
     * @return             Action index in [0, actionSize)
     */
    public int selectAction(float[] targetState) {
        updateTau();

        // Target agent Q-values (primary)
        float[] targetQ  = safeGetQValues(targetAgent, targetState);

        // Source agent Q-values over mapped state (secondary)
        float[] mappedState = mapToSourceSpace(targetState);
        float[] sourceQ     = safeGetQValues(sourceAgent, mappedState);

        // Blend: blendedQ[a] = (1-τ)·targetQ[a] + τ·sourceQ[a]
        float[] blended = new float[actionSize];
        for (int a = 0; a < actionSize; a++) {
            float tq = a < targetQ.length ? targetQ[a] : 0f;
            float sq = a < sourceQ.length ? sourceQ[a] : 0f;

            // Disable transfer for negatively-transferred actions
            float effectiveTau = actionStats[a].disabled ? 0f : tau;
            blended[a] = (1f - effectiveTau) * tq + effectiveTau * sq;
        }

        int best = 0; float bv = blended[0];
        for (int i = 1; i < blended.length; i++) if (blended[i] > bv) { bv = blended[i]; best = i; }
        transferCount.incrementAndGet();
        return best;
    }

    /**
     * Records the outcome of a transferred action.
     * @param action   Action that was taken
     * @param reward   Reward received (0-1)
     * @param isTarget Whether this outcome is from the target game (not source)
     */
    public void recordOutcome(int action, float reward, boolean isTarget) {
        if (action >= 0 && action < actionSize) {
            actionStats[action].record(reward > 0.5f);
        }
        if (isTarget) {
            targetSteps++;
            targetAgent.update(
                new float[targetStateSize], action, reward,
                new float[targetStateSize], false);
        }
    }

    // -----------------------------------------------------------------------
    // Feature mapping
    // -----------------------------------------------------------------------

    /** Maps a target-space state vector into source-game feature space. */
    private float[] mapToSourceSpace(float[] targetState) {
        float[] sourceState = new float[sourceStateSize];
        // Reverse mapping: for each source dimension, find its aligned target index
        for (int si = 0; si < sourceStateSize; si++) {
            int ti = alignment.mapFeature(si, sourceStateSize, targetStateSize);
            if (ti >= 0 && ti < targetState.length) {
                float v = targetState[ti];
                // Update running normalisation statistics for source
                if (v < srcMin[si]) srcMin[si] = v;
                if (v > srcMax[si]) srcMax[si] = v;
                float span = srcMax[si] - srcMin[si];
                sourceState[si] = span > 1e-6f ? (v - srcMin[si]) / span : 0.5f;
            }
        }
        return sourceState;
    }

    // -----------------------------------------------------------------------
    // Helpers
    // -----------------------------------------------------------------------

    private float[] safeGetQValues(RLAgent agent, float[] state) {
        if (agent == null) return new float[actionSize];
        try {
            float[] probs = agent.getActionProbabilities(
                    state, buildActionIndices(actionSize));
            return probs != null ? probs : new float[actionSize];
        } catch (Exception e) {
            return new float[actionSize];
        }
    }

    private int[] buildActionIndices(int n) {
        int[] idx = new int[n];
        for (int i = 0; i < n; i++) idx[i] = i;
        return idx;
    }

    private void updateTau() {
        tau = TAU_INITIAL * (float) Math.exp(-(double) targetSteps / DECAY_STEPS);
    }

    // -----------------------------------------------------------------------
    // Diagnostics
    // -----------------------------------------------------------------------
    public float getCurrentTau()      { return tau; }
    public int   getTargetSteps()     { return targetSteps; }
    public int   getTransferCount()   { return transferCount.get(); }

    public Map<String, Object> getStats() {
        Map<String, Object> m = new HashMap<>();
        m.put("tau",           tau);
        m.put("targetSteps",   targetSteps);
        m.put("transferCount", transferCount.get());
        List<Map<String, Object>> actionList = new ArrayList<>();
        for (int i = 0; i < actionSize; i++) {
            Map<String, Object> as = new HashMap<>();
            as.put("action",      i);
            as.put("uses",        actionStats[i].uses);
            as.put("successRate", actionStats[i].successRate());
            as.put("disabled",    actionStats[i].disabled);
            actionList.add(as);
        }
        m.put("actionStats", actionList);
        return m;
    }
}
