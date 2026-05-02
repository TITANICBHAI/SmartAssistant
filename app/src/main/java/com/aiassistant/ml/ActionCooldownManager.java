package com.aiassistant.ml;

import android.util.Log;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * ActionCooldownManager — prevents action spam and coordinates timing constraints.
 *
 * Responsibilities:
 *   1. Per-action cooldown: each action type can have a mandatory rest period in ms
 *      before it can be issued again.
 *   2. Global action rate limit: maximum N actions per second across all types.
 *   3. Failure-based backoff: when an action fails repeatedly, its cooldown
 *      increases exponentially (up to a configurable maximum).
 *   4. Combo readiness: tracks whether a combo's constituent actions are all off
 *      cooldown simultaneously.
 *   5. Priority bypass: CRITICAL priority actions may skip cooldown checks.
 *
 * Thread-safe — all public methods are synchronized or use atomic operations.
 */
public class ActionCooldownManager {

    private static final String TAG = "ActionCooldownManager";

    // -------------------------------------------------------------------------
    // Constants
    // -------------------------------------------------------------------------
    private static final float BACKOFF_MULTIPLIER = 2.0f;
    private static final float BACKOFF_MAX_RATIO  = 8.0f;  // max backoff = 8× base cooldown

    // -------------------------------------------------------------------------
    // Inner types
    // -------------------------------------------------------------------------

    public static class CooldownEntry {
        final String  actionType;
        final long    baseCooldownMs;  // configured minimum rest time
        long          lastUsedMs;      // last time the action was dispatched
        int           consecutiveFails;
        float         currentMultiplier; // backoff multiplier [1, BACKOFF_MAX_RATIO]

        CooldownEntry(String actionType, long baseCooldownMs) {
            this.actionType        = actionType;
            this.baseCooldownMs    = baseCooldownMs;
            this.lastUsedMs        = 0L;
            this.consecutiveFails  = 0;
            this.currentMultiplier = 1.0f;
        }

        long effectiveCooldownMs() {
            return (long)(baseCooldownMs * currentMultiplier);
        }

        boolean isReady(long nowMs) {
            return (nowMs - lastUsedMs) >= effectiveCooldownMs();
        }

        long remainingMs(long nowMs) {
            long elapsed = nowMs - lastUsedMs;
            long remain  = effectiveCooldownMs() - elapsed;
            return Math.max(0L, remain);
        }
    }

    // -------------------------------------------------------------------------
    // Fields
    // -------------------------------------------------------------------------

    private final Map<String, CooldownEntry> cooldowns      = new ConcurrentHashMap<>();
    private final Map<String, Long>          globalCooldown = new ConcurrentHashMap<>();

    // Global rate limiter
    private final int  maxActionsPerSecond;
    private final long[] actionTimestamps;  // ring buffer of recent action times
    private int          ringHead = 0;

    private final AtomicInteger totalDispatched  = new AtomicInteger(0);
    private final AtomicInteger totalBlocked     = new AtomicInteger(0);
    private final AtomicInteger totalBackoffBumps = new AtomicInteger(0);

    // -------------------------------------------------------------------------
    // Construction
    // -------------------------------------------------------------------------

    /**
     * @param maxActionsPerSecond Global throughput cap (0 = unlimited).
     */
    public ActionCooldownManager(int maxActionsPerSecond) {
        this.maxActionsPerSecond = maxActionsPerSecond;
        this.actionTimestamps    = maxActionsPerSecond > 0
                ? new long[maxActionsPerSecond]
                : new long[0];

        // Register default cooldowns for common action types
        registerCooldown("TAP",        50L);
        registerCooldown("SWIPE",     120L);
        registerCooldown("LONG_PRESS", 500L);
        registerCooldown("SCROLL",    200L);
        registerCooldown("DRAG",      300L);
        registerCooldown("attack",    200L);
        registerCooldown("heal",     1000L);
        registerCooldown("special",  3000L);
        registerCooldown("dodge",     400L);
        registerCooldown("block",     150L);
    }

    public ActionCooldownManager() {
        this(20); // default: 20 actions/second cap
    }

    // -------------------------------------------------------------------------
    // Configuration
    // -------------------------------------------------------------------------

    /** Register or update a cooldown for an action type. */
    public void registerCooldown(String actionType, long cooldownMs) {
        cooldowns.put(actionType, new CooldownEntry(actionType, cooldownMs));
    }

    /** Register a shared cooldown across a group of action types (e.g. skill buttons). */
    public void registerGroupCooldown(String groupKey, long cooldownMs, String... actionTypes) {
        for (String t : actionTypes) registerCooldown(t, cooldownMs);
        globalCooldown.put(groupKey, 0L);
    }

    // -------------------------------------------------------------------------
    // Readiness checks
    // -------------------------------------------------------------------------

    /**
     * Check whether an action is ready to fire.
     *
     * @param actionType     Action type string.
     * @param criticalBypass If true, skip cooldown checks for CRITICAL priority actions.
     * @return true when the action may be dispatched.
     */
    public synchronized boolean isReady(String actionType, boolean criticalBypass) {
        if (criticalBypass) return true;
        long now = System.currentTimeMillis();

        // Global rate limit check
        if (!globalRateOk(now)) return false;

        // Per-action cooldown
        CooldownEntry entry = getOrCreate(actionType);
        return entry.isReady(now);
    }

    public boolean isReady(String actionType) {
        return isReady(actionType, false);
    }

    /**
     * Remaining cooldown in ms for an action type (0 = ready).
     */
    public synchronized long remainingCooldownMs(String actionType) {
        CooldownEntry entry = cooldowns.get(actionType);
        if (entry == null) return 0L;
        return entry.remainingMs(System.currentTimeMillis());
    }

    /**
     * Check whether all action types in a combo are simultaneously ready.
     */
    public synchronized boolean isComboReady(List<String> comboActionTypes) {
        long now = System.currentTimeMillis();
        for (String t : comboActionTypes) {
            CooldownEntry e = getOrCreate(t);
            if (!e.isReady(now)) return false;
        }
        return globalRateOk(now);
    }

    // -------------------------------------------------------------------------
    // Dispatch recording
    // -------------------------------------------------------------------------

    /**
     * Record that an action was successfully dispatched.
     * Starts the cooldown timer and resets failure backoff.
     */
    public synchronized void recordDispatched(String actionType) {
        long now = System.currentTimeMillis();
        CooldownEntry entry = getOrCreate(actionType);
        entry.lastUsedMs        = now;
        entry.consecutiveFails  = 0;
        entry.currentMultiplier = 1.0f;
        recordGlobalTimestamp(now);
        totalDispatched.incrementAndGet();
    }

    /**
     * Record a failed or blocked action.  Increases backoff multiplier.
     */
    public synchronized void recordFailed(String actionType) {
        CooldownEntry entry = getOrCreate(actionType);
        entry.consecutiveFails++;
        entry.currentMultiplier = Math.min(
                BACKOFF_MAX_RATIO,
                entry.currentMultiplier * BACKOFF_MULTIPLIER);
        entry.lastUsedMs = System.currentTimeMillis(); // restart cooldown on failure too
        totalBackoffBumps.incrementAndGet();
        Log.d(TAG, actionType + " backoff=" + String.format("%.1f", entry.currentMultiplier)
                + "× (" + entry.consecutiveFails + " consecutive fails)");
    }

    /**
     * Record that an action was blocked by the cooldown.
     */
    public synchronized void recordBlocked(String actionType) {
        totalBlocked.incrementAndGet();
    }

    // -------------------------------------------------------------------------
    // Cooldown reset
    // -------------------------------------------------------------------------

    /** Immediately reset a specific action's cooldown (e.g. after a power-up). */
    public synchronized void reset(String actionType) {
        CooldownEntry e = cooldowns.get(actionType);
        if (e != null) {
            e.lastUsedMs        = 0L;
            e.consecutiveFails  = 0;
            e.currentMultiplier = 1.0f;
        }
    }

    /** Reset all action cooldowns. */
    public synchronized void resetAll() {
        for (CooldownEntry e : cooldowns.values()) {
            e.lastUsedMs        = 0L;
            e.consecutiveFails  = 0;
            e.currentMultiplier = 1.0f;
        }
    }

    // -------------------------------------------------------------------------
    // Stats
    // -------------------------------------------------------------------------

    public Map<String, Object> getStats() {
        Map<String, Object> s = new HashMap<>();
        s.put("totalDispatched",   totalDispatched.get());
        s.put("totalBlocked",      totalBlocked.get());
        s.put("totalBackoffBumps", totalBackoffBumps.get());
        s.put("maxActionsPerSec",  maxActionsPerSecond);

        long now = System.currentTimeMillis();
        List<Map<String, Object>> cd = new ArrayList<>();
        for (CooldownEntry e : cooldowns.values()) {
            Map<String, Object> m = new HashMap<>();
            m.put("type",           e.actionType);
            m.put("baseCooldownMs", e.baseCooldownMs);
            m.put("effectiveMs",    e.effectiveCooldownMs());
            m.put("remainingMs",    e.remainingMs(now));
            m.put("fails",          e.consecutiveFails);
            m.put("backoff",        e.currentMultiplier);
            m.put("ready",          e.isReady(now));
            cd.add(m);
        }
        s.put("cooldowns", cd);
        return s;
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    private CooldownEntry getOrCreate(String actionType) {
        return cooldowns.computeIfAbsent(actionType,
                t -> new CooldownEntry(t, 100L)); // default 100 ms
    }

    private boolean globalRateOk(long nowMs) {
        if (maxActionsPerSecond <= 0 || actionTimestamps.length == 0) return true;
        // Check the oldest timestamp in the ring buffer
        long oldest = actionTimestamps[ringHead];
        return oldest == 0L || (nowMs - oldest) >= 1000L;
    }

    private void recordGlobalTimestamp(long nowMs) {
        if (actionTimestamps.length == 0) return;
        actionTimestamps[ringHead] = nowMs;
        ringHead = (ringHead + 1) % actionTimestamps.length;
    }
}
