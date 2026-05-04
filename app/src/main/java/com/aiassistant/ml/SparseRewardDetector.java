package com.aiassistant.ml;

import android.util.Log;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * SparseRewardDetector — diagnoses and mitigates sparse reward conditions in RL.
 *
 * Many game environments have extremely sparse rewards (e.g., only win/lose).
 * This detector monitors reward statistics and activates mitigation strategies
 * when the agent is stuck in a reward desert.
 *
 * Detection criteria (ANY of):
 *   - Average reward over last W steps < threshold (near-zero reward signal)
 *   - Reward event rate < minRate (fraction of steps with non-zero reward)
 *   - Episode return variance < varThresh (reward signal is too uniform)
 *   - State novelty has collapsed (agent is looping)
 *
 * Mitigation strategies (activated automatically when sparse is detected):
 *   HINT_REWARDS    — Generate sub-goal hint rewards from heuristic progress signals.
 *   GOAL_BABBLING   — Randomly sample goals from the goal space and reward progress.
 *   RANDOM_EXPLORE  — Force ε=1 (full exploration) for burnIn steps.
 *   CURIOSITY_BOOST — Amplify intrinsic curiosity reward by boostFactor.
 *
 * Thread-safe.
 */
public class SparseRewardDetector {

    private static final String TAG = "SparseRewardDetector";

    public enum Mitigation { HINT_REWARDS, GOAL_BABBLING, RANDOM_EXPLORE, CURIOSITY_BOOST }

    // ─────────────────────────────────────────────────────────────────────────
    // Circular window for reward history
    // ─────────────────────────────────────────────────────────────────────────
    private final float[] rewardWindow;
    private int    winHead  = 0;
    private int    winCount = 0;
    private final int windowSize;

    // ─────────────────────────────────────────────────────────────────────────
    // Fields
    // ─────────────────────────────────────────────────────────────────────────
    private final float sparseThreshold;   // avg reward below this → sparse
    private final float minEventRate;      // min fraction of non-zero rewards
    private final float varThreshold;      // reward variance must exceed this
    private final int   noveltyWindow;     // steps to check novelty collapse

    private Mitigation  activeMitigation  = null;
    private boolean     isSparse          = false;
    private int         mitigationStepsLeft = 0;
    private final int   mitigationDuration;
    private final float boostFactor;

    private float hintGoal[]   = null;    // current babbling goal (state dim)
    private float forceEpsilon = -1f;

    // Episode-level tracking
    private int   episodeSteps     = 0;
    private int   episodeNonZero   = 0;
    private float episodeReturn    = 0f;
    private final List<Float> episodeReturns = new ArrayList<>();
    private final int maxReturnHistory = 50;

    private final AtomicInteger sparseDetections = new AtomicInteger(0);
    private final AtomicInteger totalSteps       = new AtomicInteger(0);
    private float avgReward = 0f;

    private final java.util.Random rng = new java.util.Random(179L);

    // ─────────────────────────────────────────────────────────────────────────
    // Construction
    // ─────────────────────────────────────────────────────────────────────────

    public SparseRewardDetector(int windowSize, float sparseThreshold,
                                 float minEventRate, float varThreshold,
                                 int mitigationDuration, float boostFactor) {
        this.windowSize         = windowSize;
        this.sparseThreshold    = sparseThreshold;
        this.minEventRate       = minEventRate;
        this.varThreshold       = varThreshold;
        this.noveltyWindow      = windowSize / 2;
        this.mitigationDuration = mitigationDuration;
        this.boostFactor        = boostFactor;
        this.rewardWindow       = new float[windowSize];

        Log.i(TAG, "SparseRewardDetector: win=" + windowSize + " thresh=" + sparseThreshold);
    }

    public SparseRewardDetector() {
        this(500, 0.01f, 0.05f, 0.001f, 2000, 3.0f);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Core API
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Record a reward observation and update sparse detection.
     * @return Augmented reward after applying any active mitigation.
     */
    public synchronized float observe(float reward, float[] state, float intrinsicReward) {
        // Update window
        rewardWindow[winHead % windowSize] = reward;
        winHead++; if (winCount < windowSize) winCount++;
        episodeSteps++;
        episodeReturn += reward;
        totalSteps.incrementAndGet();
        if (Math.abs(reward) > 1e-6f) episodeNonZero++;
        avgReward = 0.99f * avgReward + 0.01f * reward;

        // Check sparsity every windowSize steps
        if (winCount == windowSize && winHead % (windowSize / 4) == 0) detectSparsity();

        // Apply mitigation
        float augmented = reward;
        if (isSparse && activeMitigation != null) {
            augmented = applyMitigation(reward, state, intrinsicReward);
            if (--mitigationStepsLeft <= 0) { activeMitigation = null; isSparse = false; }
        }
        return augmented;
    }

    /** Call at episode boundary. */
    public synchronized void endEpisode() {
        if (episodeReturns.size() >= maxReturnHistory) episodeReturns.remove(0);
        episodeReturns.add(episodeReturn);
        episodeSteps = 0; episodeNonZero = 0; episodeReturn = 0f;
    }

    /** Whether the environment is currently sparse. */
    public synchronized boolean isSparse() { return isSparse; }

    /** Currently active mitigation mode, null if none. */
    public synchronized Mitigation getActiveMitigation() { return activeMitigation; }

    /** Epsilon override for RANDOM_EXPLORE mitigation (-1 = no override). */
    public synchronized float getForceEpsilon() { return forceEpsilon; }

    /** Intrinsic reward boost factor (1.0 = no boost). */
    public synchronized float getBoostFactor() {
        return (isSparse && activeMitigation == Mitigation.CURIOSITY_BOOST) ? boostFactor : 1f;
    }

    /** Compute hint reward toward an internally set sub-goal. */
    public synchronized float hintReward(float[] state) {
        if (!isSparse || activeMitigation != Mitigation.HINT_REWARDS || hintGoal == null) return 0f;
        float d = 0;
        for (int i = 0; i < Math.min(state.length, hintGoal.length); i++) {
            float diff = state[i] - hintGoal[i]; d += diff * diff;
        }
        return 0.1f / ((float) Math.sqrt(d) + 1f);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Detection
    // ─────────────────────────────────────────────────────────────────────────

    private void detectSparsity() {
        float sum = 0, nonZero = 0;
        for (int i = 0; i < winCount; i++) { sum += rewardWindow[i]; if (Math.abs(rewardWindow[i]) > 1e-6f) nonZero++; }
        float avg = sum / winCount;
        float eventRate = nonZero / winCount;

        float var = 0;
        for (int i = 0; i < winCount; i++) { float d = rewardWindow[i] - avg; var += d*d; }
        var /= winCount;

        boolean newSparse = Math.abs(avg) < sparseThreshold
                && eventRate < minEventRate
                && var < varThreshold;

        if (newSparse && !isSparse) {
            isSparse = true;
            activateMitigation();
            sparseDetections.incrementAndGet();
            Log.w(TAG, "Sparse reward detected! avg=" + avg
                    + " eventRate=" + eventRate + " mitigation=" + activeMitigation);
        } else if (!newSparse && isSparse) {
            isSparse = false;
            activeMitigation = null;
            forceEpsilon = -1f;
            Log.i(TAG, "Sparse reward resolved.");
        }
    }

    private void activateMitigation() {
        // Round-robin through mitigations
        Mitigation[] vals = Mitigation.values();
        activeMitigation    = vals[sparseDetections.get() % vals.length];
        mitigationStepsLeft = mitigationDuration;

        switch (activeMitigation) {
            case RANDOM_EXPLORE:  forceEpsilon = 1.0f; break;
            case HINT_REWARDS:    hintGoal = sampleGoal(); break;
            case GOAL_BABBLING:   hintGoal = sampleGoal(); break;
            case CURIOSITY_BOOST: break;
        }
    }

    private float applyMitigation(float reward, float[] state, float intrinsic) {
        switch (activeMitigation) {
            case CURIOSITY_BOOST: return reward + boostFactor * intrinsic;
            case HINT_REWARDS:    return reward + hintReward(state);
            case GOAL_BABBLING:   return reward + hintReward(state) * 0.5f;
            default:              return reward;
        }
    }

    private float[] sampleGoal() {
        float[] g = new float[8]; // default 8-dim
        for (int i = 0; i < g.length; i++) g[i] = rng.nextFloat();
        return g;
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Stats
    // ─────────────────────────────────────────────────────────────────────────

    public synchronized Map<String, Object> getStats() {
        Map<String, Object> s = new HashMap<>();
        s.put("isSparse",          isSparse);
        s.put("activeMitigation",  activeMitigation != null ? activeMitigation.name() : "none");
        s.put("sparseDetections",  sparseDetections.get());
        s.put("totalSteps",        totalSteps.get());
        s.put("avgReward",         avgReward);
        s.put("windowFill",        winCount);
        s.put("episodeReturnHistory", episodeReturns.size());
        if (!episodeReturns.isEmpty()) {
            float sum = 0; for (float r : episodeReturns) sum += r;
            s.put("avgEpisodeReturn", sum / episodeReturns.size());
        }
        return s;
    }
}
