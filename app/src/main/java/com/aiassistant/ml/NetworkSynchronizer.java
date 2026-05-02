package com.aiassistant.ml;

import android.util.Log;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

/**
 * NetworkSynchronizer — manages weight synchronization between online and
 * target neural networks for stable RL training.
 *
 * Supports three synchronization strategies:
 *
 *   HARD_COPY   — copy online → target exactly every N steps (classic DQN)
 *
 *   POLYAK      — soft/Polyak update: θ_target ← τ·θ_online + (1−τ)·θ_target
 *                 Applied every step.  τ=0.005 is the TD3/SAC default.
 *
 *   SCHEDULED   — automatic background sync at a fixed time interval (ms).
 *                 Useful when training and inference run on different threads.
 *
 * Features:
 *   - Works with any float[][] weight matrix pair (online, target).
 *   - Multiple weight groups can be registered under unique keys.
 *   - Hard-copy count and total divergence (L2 norm of (online−target)) tracked.
 *   - Thread-safe; all operations synchronized.
 */
public class NetworkSynchronizer {

    private static final String TAG = "NetworkSynchronizer";

    // -------------------------------------------------------------------------
    // Sync strategy enum
    // -------------------------------------------------------------------------
    public enum SyncStrategy { HARD_COPY, POLYAK, SCHEDULED }

    // -------------------------------------------------------------------------
    // Registered weight group
    // -------------------------------------------------------------------------
    private static class WeightGroup {
        final String     key;
        float[][]        online;  // reference to the online network weights
        float[][]        target;  // reference to the target network weights
        final int        syncFreq; // for HARD_COPY: steps between syncs
        int              stepsSinceSync;

        WeightGroup(String key, float[][] online, float[][] target, int syncFreq) {
            this.key           = key;
            this.online        = online;
            this.target        = target;
            this.syncFreq      = syncFreq;
            this.stepsSinceSync = 0;
        }
    }

    // -------------------------------------------------------------------------
    // Fields
    // -------------------------------------------------------------------------
    private final SyncStrategy strategy;
    private final float         tau;            // Polyak coefficient (0 < τ ≪ 1)
    private final int           hardCopyFreq;   // steps between hard copies (HARD_COPY)

    private final Map<String, WeightGroup> groups = new HashMap<>();

    private final AtomicInteger syncCount     = new AtomicInteger(0);
    private final AtomicInteger stepCount     = new AtomicInteger(0);
    private final AtomicLong    totalDivergence = new AtomicLong(0); // as int bits of double

    private ScheduledExecutorService scheduler; // for SCHEDULED mode

    // -------------------------------------------------------------------------
    // Construction
    // -------------------------------------------------------------------------

    public NetworkSynchronizer(SyncStrategy strategy, float tau, int hardCopyFreq) {
        this.strategy    = strategy;
        this.tau         = Math.max(1e-4f, Math.min(1.0f, tau));
        this.hardCopyFreq = Math.max(1, hardCopyFreq);
    }

    /** Hard-copy every hardCopyFreq steps. */
    public NetworkSynchronizer(int hardCopyFreq) {
        this(SyncStrategy.HARD_COPY, 0f, hardCopyFreq);
    }

    /** Polyak update with given τ. */
    public NetworkSynchronizer(float tau) {
        this(SyncStrategy.POLYAK, tau, Integer.MAX_VALUE);
    }

    // -------------------------------------------------------------------------
    // Registration
    // -------------------------------------------------------------------------

    /**
     * Register a weight matrix pair for synchronization.
     *
     * @param key    Unique identifier (e.g. "policy_W1", "value_W2").
     * @param online Reference to the online network's weight matrix (modified by training).
     * @param target Reference to the target network's weight matrix (updated by sync).
     */
    public synchronized void register(String key, float[][] online, float[][] target) {
        groups.put(key, new WeightGroup(key, online, target, hardCopyFreq));
        Log.d(TAG, "Registered group '" + key + "' [" + online.length
                + "×" + online[0].length + "]");
    }

    /** Update the online reference (e.g. after reallocation). */
    public synchronized void setOnline(String key, float[][] online) {
        WeightGroup g = groups.get(key);
        if (g != null) g.online = online;
    }

    /** Update the target reference. */
    public synchronized void setTarget(String key, float[][] target) {
        WeightGroup g = groups.get(key);
        if (g != null) g.target = target;
    }

    // -------------------------------------------------------------------------
    // Sync API
    // -------------------------------------------------------------------------

    /**
     * Call once per training step.  Applies the configured sync strategy.
     */
    public synchronized void onStep() {
        int step = stepCount.incrementAndGet();
        switch (strategy) {
            case HARD_COPY:
                for (WeightGroup g : groups.values()) {
                    g.stepsSinceSync++;
                    if (g.stepsSinceSync >= g.syncFreq) {
                        hardCopy(g);
                        g.stepsSinceSync = 0;
                    }
                }
                break;

            case POLYAK:
                for (WeightGroup g : groups.values()) polyakUpdate(g);
                break;

            case SCHEDULED:
                // handled by background scheduler; nothing to do here
                break;
        }
    }

    /**
     * Force an immediate hard copy for all registered groups.
     */
    public synchronized void forceSync() {
        for (WeightGroup g : groups.values()) hardCopy(g);
        Log.i(TAG, "Forced sync of " + groups.size() + " groups at step=" + stepCount.get());
    }

    /**
     * Start a background scheduler that syncs all groups every {@code intervalMs} ms.
     * Only valid for SCHEDULED strategy.
     */
    public void startScheduled(long intervalMs) {
        if (strategy != SyncStrategy.SCHEDULED) {
            Log.w(TAG, "startScheduled called but strategy is " + strategy);
        }
        if (scheduler != null && !scheduler.isShutdown()) return;
        scheduler = Executors.newSingleThreadScheduledExecutor();
        scheduler.scheduleAtFixedRate(() -> {
            synchronized (NetworkSynchronizer.this) {
                for (WeightGroup g : groups.values()) hardCopy(g);
            }
        }, intervalMs, intervalMs, TimeUnit.MILLISECONDS);
        Log.i(TAG, "Scheduled sync every " + intervalMs + " ms");
    }

    public void stopScheduled() {
        if (scheduler != null) scheduler.shutdownNow();
    }

    // -------------------------------------------------------------------------
    // Divergence measurement
    // -------------------------------------------------------------------------

    /**
     * Compute the L2 norm of (online − target) for a registered group.
     * Useful for monitoring how far the target has drifted.
     */
    public synchronized double computeDivergence(String key) {
        WeightGroup g = groups.get(key);
        if (g == null) return 0.0;
        double sum = 0.0;
        for (int i = 0; i < g.online.length; i++) {
            for (int j = 0; j < g.online[i].length; j++) {
                double d = g.online[i][j] - g.target[i][j];
                sum += d * d;
            }
        }
        return Math.sqrt(sum);
    }

    /** Compute mean divergence across all registered groups. */
    public synchronized double meanDivergence() {
        if (groups.isEmpty()) return 0.0;
        double total = 0.0;
        for (String key : groups.keySet()) total += computeDivergence(key);
        return total / groups.size();
    }

    // -------------------------------------------------------------------------
    // Private sync implementations
    // -------------------------------------------------------------------------

    private void hardCopy(WeightGroup g) {
        for (int i = 0; i < g.online.length; i++) {
            if (i < g.target.length) {
                int cols = Math.min(g.online[i].length, g.target[i].length);
                System.arraycopy(g.online[i], 0, g.target[i], 0, cols);
            }
        }
        syncCount.incrementAndGet();
    }

    private void polyakUpdate(WeightGroup g) {
        // θ_target ← τ·θ_online + (1−τ)·θ_target
        float oneMinusTau = 1f - tau;
        for (int i = 0; i < g.online.length; i++) {
            if (i >= g.target.length) break;
            int cols = Math.min(g.online[i].length, g.target[i].length);
            for (int j = 0; j < cols; j++) {
                g.target[i][j] = tau * g.online[i][j] + oneMinusTau * g.target[i][j];
            }
        }
    }

    // -------------------------------------------------------------------------
    // Monitoring
    // -------------------------------------------------------------------------

    public synchronized Map<String, Object> getStats() {
        Map<String, Object> s = new HashMap<>();
        s.put("strategy",      strategy.name());
        s.put("tau",           tau);
        s.put("hardCopyFreq",  hardCopyFreq);
        s.put("syncCount",     syncCount.get());
        s.put("stepCount",     stepCount.get());
        s.put("groupCount",    groups.size());
        s.put("meanDivergence", meanDivergence());
        Map<String, Double> divMap = new HashMap<>();
        for (String key : groups.keySet()) divMap.put(key, computeDivergence(key));
        s.put("divergences",   divMap);
        return s;
    }
}
