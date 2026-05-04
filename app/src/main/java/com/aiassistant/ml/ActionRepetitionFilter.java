package com.aiassistant.ml;

import android.util.Log;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * ActionRepetitionFilter — detect and break action repetition loops in game agents.
 *
 * Many naive RL agents get stuck in repetitive action cycles (e.g., tapping the
 * same screen region repeatedly when stuck). This filter:
 *
 *   1. DETECTION:
 *      - Sliding window of recent actions.
 *      - Detects exact loops: a_t = a_{t-k} for all k in [minPeriod, maxPeriod].
 *      - Detects near-repetition: one dominant action > dominanceThresh fraction.
 *      - Detects low-diversity windows: Shannon entropy of action histogram < threshold.
 *
 *   2. INTERVENTION:
 *      - RANDOM_REPLACE: substitute repeated action with a random one.
 *      - EPSILON_BOOST:  temporarily increase ε to force exploration.
 *      - PENALTY:        add negative reward signal for repetitive steps.
 *      - BLOCK:          hard-block the most repeated action for cooldownSteps steps.
 *
 *   3. REPORTING:
 *      - Logs loop detection events with period and dominant action.
 *      - Tracks intervention history for analysis.
 *
 * Thread-safe.
 */
public class ActionRepetitionFilter {

    private static final String TAG = "ActionRepFilter";

    public enum Intervention { RANDOM_REPLACE, EPSILON_BOOST, PENALTY, BLOCK }

    // ─────────────────────────────────────────────────────────────────────────
    // Fields
    // ─────────────────────────────────────────────────────────────────────────
    private final int    actionDim;
    private final int    windowSize;
    private final int    minPeriod;
    private final int    maxPeriod;
    private final float  dominanceThresh;  // fraction to call dominant
    private final float  entropyThresh;    // entropy below → repetitive
    private final Intervention intervention;
    private final int    cooldownSteps;
    private final float  penaltyValue;     // reward deduction when PENALTY

    // Action window (circular buffer)
    private final int[]  window;
    private int          head = 0, count = 0;

    // Per-action block cooldown
    private final int[]  blockCooldown;   // steps remaining blocked per action

    // Epsilon boost state
    private float  baseEpsilon   = 0.1f;
    private float  boostedEpsilon= 0.5f;
    private int    boostSteps    = 0;
    private final int boostedDuration = 100;

    // Stats
    private final AtomicInteger detectCount     = new AtomicInteger(0);
    private final AtomicInteger interventionCnt = new AtomicInteger(0);
    private final AtomicInteger totalSteps      = new AtomicInteger(0);
    private float avgEntropy   = 1f;
    private int   lastPeriod   = -1;

    private final java.util.Random rng = new java.util.Random(233L);

    // ─────────────────────────────────────────────────────────────────────────
    // Construction
    // ─────────────────────────────────────────────────────────────────────────

    public ActionRepetitionFilter(int actionDim, int windowSize,
                                   int minPeriod, int maxPeriod,
                                   float dominanceThresh, float entropyThresh,
                                   Intervention intervention, int cooldownSteps,
                                   float penaltyValue) {
        this.actionDim       = actionDim;
        this.windowSize      = windowSize;
        this.minPeriod       = minPeriod;
        this.maxPeriod       = maxPeriod;
        this.dominanceThresh = dominanceThresh;
        this.entropyThresh   = entropyThresh;
        this.intervention    = intervention;
        this.cooldownSteps   = cooldownSteps;
        this.penaltyValue    = penaltyValue;

        window        = new int[windowSize];
        blockCooldown = new int[actionDim];

        Log.i(TAG, "ActionRepetitionFilter: win=" + windowSize
                + " period=[" + minPeriod + "," + maxPeriod + "] mode=" + intervention);
    }

    public ActionRepetitionFilter(int actionDim) {
        this(actionDim, 20, 2, 5, 0.7f, 0.8f, Intervention.RANDOM_REPLACE, 50, -0.1f);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Core API
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Record action and check for repetition.
     * Apply intervention if needed.
     *
     * @param action Raw action from the agent.
     * @return Filtered action (may differ from input under intervention).
     */
    public synchronized int filter(int action) {
        recordAction(action);
        decayBlocks();
        totalSteps.incrementAndGet();
        if (boostSteps > 0) boostSteps--;

        boolean rep = isRepetitive();
        if (rep) {
            detectCount.incrementAndGet();
            return intervene(action);
        }
        return action;
    }

    /**
     * Get a penalty reward if current step is repetitive.
     * Call AFTER filter().
     */
    public synchronized float penaltyReward() {
        if (intervention == Intervention.PENALTY && isRepetitive()) return penaltyValue;
        return 0f;
    }

    /** Current effective epsilon (boosted if in boost mode). */
    public synchronized float effectiveEpsilon(float baseEps) {
        this.baseEpsilon = baseEps;
        return (boostSteps > 0) ? Math.max(baseEps, boostedEpsilon) : baseEps;
    }

    /** Whether a specific action is currently blocked. */
    public synchronized boolean isBlocked(int action) {
        return action >= 0 && action < actionDim && blockCooldown[action] > 0;
    }

    /** Reset the action window (call at episode start). */
    public synchronized void reset() { head = 0; count = 0; boostSteps = 0; }

    // ─────────────────────────────────────────────────────────────────────────
    // Detection
    // ─────────────────────────────────────────────────────────────────────────

    private boolean isRepetitive() {
        if (count < minPeriod * 2) return false;

        // Check periodic loops
        for (int period = minPeriod; period <= Math.min(maxPeriod, count / 2); period++) {
            if (hasLoop(period)) { lastPeriod = period; return true; }
        }

        // Check dominance
        int[]  hist = histogram();
        float  total = count;
        for (int a = 0; a < actionDim; a++) {
            if (hist[a] / total > dominanceThresh) { lastPeriod = 1; return true; }
        }

        // Check entropy
        float ent = entropy(hist, total);
        avgEntropy = 0.99f * avgEntropy + 0.01f * ent;
        float maxEnt = (float)(Math.log(actionDim) / Math.log(2));
        if (ent < entropyThresh * maxEnt) { lastPeriod = -1; return true; }

        return false;
    }

    private boolean hasLoop(int period) {
        int n = Math.min(count, windowSize);
        for (int i = 0; i < period; i++) {
            int a = window[(head - 1 - i + windowSize) % windowSize];
            int b = window[(head - 1 - i - period + windowSize) % windowSize];
            if (a != b) return false;
        }
        return true;
    }

    private int[] histogram() {
        int[] h = new int[actionDim];
        int n = Math.min(count, windowSize);
        for (int i = 0; i < n; i++) h[Math.min(window[i], actionDim-1)]++;
        return h;
    }

    private float entropy(int[] hist, float total) {
        if (total <= 0) return 0f;
        float ent = 0;
        for (int c : hist) if (c > 0) { float p = c/total; ent -= p*(float)Math.log(p)/Math.log(2); }
        return ent;
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Intervention
    // ─────────────────────────────────────────────────────────────────────────

    private int intervene(int action) {
        interventionCnt.incrementAndGet();
        switch (intervention) {
            case RANDOM_REPLACE:
                return randomUnblocked(action);
            case EPSILON_BOOST:
                boostSteps = boostedDuration;
                return action;  // epsilon will handle exploration
            case BLOCK:
                blockCooldown[action] = cooldownSteps;
                return randomUnblocked(action);
            case PENALTY:
            default:
                return action;  // reward penalty applied separately
        }
    }

    private int randomUnblocked(int excluded) {
        for (int tries = 0; tries < actionDim * 3; tries++) {
            int a = rng.nextInt(actionDim);
            if (a != excluded && !isBlocked(a)) return a;
        }
        return rng.nextInt(actionDim);
    }

    private void recordAction(int action) {
        window[head % windowSize] = Math.max(0, Math.min(actionDim - 1, action));
        head++; if (count < windowSize) count++;
    }

    private void decayBlocks() {
        for (int a = 0; a < actionDim; a++) if (blockCooldown[a] > 0) blockCooldown[a]--;
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Stats
    // ─────────────────────────────────────────────────────────────────────────

    public synchronized Map<String, Object> getStats() {
        Map<String, Object> s = new HashMap<>();
        s.put("detectCount",     detectCount.get());
        s.put("interventionCnt", interventionCnt.get());
        s.put("totalSteps",      totalSteps.get());
        s.put("detectRate",      totalSteps.get() > 0 ? (float)detectCount.get()/totalSteps.get() : 0f);
        s.put("avgEntropy",      avgEntropy);
        s.put("lastPeriod",      lastPeriod);
        s.put("boostSteps",      boostSteps);
        s.put("intervention",    intervention.name());
        return s;
    }
}
