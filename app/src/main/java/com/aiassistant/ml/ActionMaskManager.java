package com.aiassistant.ml;

import android.util.Log;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * ActionMaskManager — dynamic action masking for safe and valid action selection.
 *
 * Maintains a binary mask over the action space. Masked actions are invalid in
 * the current state (e.g., cannot scroll when already at top, cannot click
 * a disabled button) and should NOT be selected by the policy.
 *
 * Integration with Q-learning:
 *   Masked actions get Q = -∞ before argmax, ensuring they are never selected.
 *
 * Integration with policy gradient:
 *   Masked action logits → -∞ before softmax, zeroing their probability.
 *
 * Mask sources:
 *   1. STATIC_RULES:   hard-coded logical constraints (always apply).
 *   2. LEARNED_MASK:   learned from past constraint violations (soft penalty → hard block).
 *   3. ACCESSIBILITY:  derived from Android accessibility node properties.
 *   4. TEMPORAL_BLOCK: cooldown-based block after a recent action.
 *
 * Provides action validity scoring (0=blocked, 1=freely allowed, 0<x<1=discouraged).
 *
 * Thread-safe.
 */
public class ActionMaskManager {

    private static final String TAG = "ActionMaskManager";

    public enum MaskSource { STATIC_RULES, LEARNED_MASK, ACCESSIBILITY, TEMPORAL_BLOCK }

    // ─────────────────────────────────────────────────────────────────────────
    // Fields
    // ─────────────────────────────────────────────────────────────────────────
    private final int   actionDim;
    // Combined mask: true = BLOCKED
    private final boolean[] hardMask;      // hard block (action completely invalid)
    private final float[]   softMask;      // soft discouragement score [0,1]
    private final int[]     cooldown;      // steps remaining blocked
    private final float[]   violationRate; // learned: fraction of times action caused problem

    // Mask sources active
    private final boolean[] sourceActive = new boolean[MaskSource.values().length];

    // Stats
    private final AtomicInteger maskApplications = new AtomicInteger(0);
    private final AtomicInteger violationsRecorded= new AtomicInteger(0);
    private int   numMasked = 0;

    private float learnedThreshold = 0.7f;   // violation rate above which → hard block

    // ─────────────────────────────────────────────────────────────────────────
    // Construction
    // ─────────────────────────────────────────────────────────────────────────

    public ActionMaskManager(int actionDim) {
        this.actionDim   = actionDim;
        this.hardMask    = new boolean[actionDim];
        this.softMask    = new float[actionDim];   // all 1.0 initially
        this.cooldown    = new int[actionDim];
        this.violationRate = new float[actionDim];
        Arrays.fill(softMask, 1f);
        Arrays.fill(sourceActive, true);
        Log.i(TAG, "ActionMaskManager: actions=" + actionDim);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Core API
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Apply the current mask to Q-values: blocked actions → -1e9.
     * @param Q Input Q-values [actionDim].
     * @return Masked Q-values.
     */
    public synchronized float[] maskQValues(float[] Q) {
        float[] masked = Q.clone();
        for (int a = 0; a < Math.min(actionDim, Q.length); a++) {
            if (isBlocked(a)) masked[a] = -1e9f;
            else masked[a] *= softMask[Math.min(a, softMask.length-1)];
        }
        maskApplications.incrementAndGet();
        return masked;
    }

    /**
     * Apply mask to policy logits: blocked → -1e9 before softmax.
     */
    public synchronized float[] maskLogits(float[] logits) {
        float[] masked = logits.clone();
        for (int a = 0; a < Math.min(actionDim, logits.length); a++) {
            if (isBlocked(a)) masked[a] = -1e9f;
        }
        return masked;
    }

    /** Check if action is currently blocked. */
    public synchronized boolean isBlocked(int action) {
        if (action < 0 || action >= actionDim) return true;
        return hardMask[action] || cooldown[action] > 0
                || (sourceActive[MaskSource.LEARNED_MASK.ordinal()]
                    && violationRate[action] >= learnedThreshold);
    }

    /** Get validity score for action (0=blocked, 1=full). */
    public synchronized float validity(int action) {
        if (isBlocked(action)) return 0f;
        return softMask[action] * (1f - Math.min(1f, violationRate[action] / learnedThreshold));
    }

    /** Advance cooldowns by 1 step. Call once per environment step. */
    public synchronized void step() {
        numMasked = 0;
        for (int a = 0; a < actionDim; a++) {
            if (cooldown[a] > 0) cooldown[a]--;
            if (isBlocked(a)) numMasked++;
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Mask manipulation
    // ─────────────────────────────────────────────────────────────────────────

    /** Hard-block an action permanently. */
    public synchronized void hardBlock(int action) {
        if (action >= 0 && action < actionDim) hardMask[action] = true;
    }

    /** Unblock a hard-blocked action. */
    public synchronized void unblock(int action) {
        if (action >= 0 && action < actionDim) { hardMask[action] = false; cooldown[action] = 0; }
    }

    /** Block action for N steps (temporal cooldown). */
    public synchronized void blockFor(int action, int steps) {
        if (action >= 0 && action < actionDim) cooldown[action] = steps;
    }

    /** Set soft discouragement score (0=banned, 1=free). */
    public synchronized void setSoftScore(int action, float score) {
        if (action >= 0 && action < actionDim) softMask[action] = Math.max(0f, Math.min(1f, score));
    }

    /** Set entire hard mask at once. */
    public synchronized void setMask(boolean[] mask) {
        System.arraycopy(mask, 0, hardMask, 0, Math.min(mask.length, actionDim));
    }

    /** Record that an action caused a constraint violation. */
    public synchronized void recordViolation(int action) {
        if (action < 0 || action >= actionDim) return;
        violationRate[action] = 0.9f * violationRate[action] + 0.1f * 1f;
        violationsRecorded.incrementAndGet();
    }

    /** Record that an action succeeded (no violation). */
    public synchronized void recordSuccess(int action) {
        if (action < 0 || action >= actionDim) return;
        violationRate[action] = 0.9f * violationRate[action];  // decay toward 0
    }

    // ─────────────────────────────────────────────────────────────────────────
    // From accessibility info
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Update mask from accessibility node enabled flags.
     * @param enabled boolean[actionDim] — true if action target is enabled/clickable.
     */
    public synchronized void updateFromAccessibility(boolean[] enabled) {
        if (!sourceActive[MaskSource.ACCESSIBILITY.ordinal()]) return;
        for (int a = 0; a < Math.min(actionDim, enabled.length); a++) {
            if (!enabled[a]) hardBlock(a);
            else if (hardMask[a]) unblock(a);
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Queries
    // ─────────────────────────────────────────────────────────────────────────

    /** All currently unblocked actions. */
    public synchronized int[] validActions() {
        List<Integer> v = new ArrayList<>();
        for (int a = 0; a < actionDim; a++) if (!isBlocked(a)) v.add(a);
        int[] arr = new int[v.size()];
        for (int i=0;i<arr.length;i++) arr[i]=v.get(i);
        return arr;
    }

    /** All blocked actions. */
    public synchronized int[] blockedActions() {
        List<Integer> b = new ArrayList<>();
        for (int a = 0; a < actionDim; a++) if (isBlocked(a)) b.add(a);
        int[] arr = new int[b.size()];
        for (int i=0;i<arr.length;i++) arr[i]=b.get(i);
        return arr;
    }

    public synchronized void setSourceActive(MaskSource src, boolean active) {
        sourceActive[src.ordinal()] = active;
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Stats
    // ─────────────────────────────────────────────────────────────────────────

    public synchronized Map<String, Object> getStats() {
        Map<String, Object> s = new HashMap<>();
        s.put("actionDim",         actionDim);
        s.put("numMasked",         numMasked);
        s.put("numValid",          actionDim - numMasked);
        s.put("maskFraction",      (float) numMasked / actionDim);
        s.put("maskApplications",  maskApplications.get());
        s.put("violationsRecorded",violationsRecorded.get());
        s.put("learnedThreshold",  learnedThreshold);
        return s;
    }
}
