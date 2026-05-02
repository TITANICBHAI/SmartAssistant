package com.aiassistant.learning;

import android.util.Log;

import com.aiassistant.monitoring.PerformanceMonitor;
import com.aiassistant.monitoring.SmartBatteryOptimizer;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Adaptive learning scheduler for the AI assistant.
 *
 * Automatically schedules training rounds for RL agents and the learning
 * engine based on current resource availability (CPU, memory, battery).
 *
 * Features:
 *  1. Resource-aware timing — uses {@link PerformanceMonitor} and
 *     {@link SmartBatteryOptimizer} to decide whether now is a good time
 *     to run a training round.
 *  2. Backoff on overload — doubles the interval up to MAX_INTERVAL_MS when
 *     resources are constrained; resets to BASE_INTERVAL_MS when they recover.
 *  3. Task priority queue — multiple {@link LearningTask} implementations can
 *     be registered with different priorities; higher-priority tasks run first.
 *  4. Min interval guard — individual tasks cannot run more frequently than
 *     their declared minIntervalMs regardless of resource state.
 *  5. Statistics — tracks total rounds, skipped rounds, and last-run times.
 *  6. Thread-safe — all fields accessed from a single scheduler thread or
 *     behind atomic variables.
 */
public class AdaptiveLearningScheduler {
    private static final String TAG = "AdaptiveLearningScheduler";

    private static final long BASE_INTERVAL_MS = 5_000L;   // 5 s initial check
    private static final long MAX_INTERVAL_MS  = 120_000L; // 2 min backoff cap

    // -----------------------------------------------------------------------
    // Task interface
    // -----------------------------------------------------------------------

    public interface LearningTask {
        /** Human-readable name for logging. */
        String getName();

        /** Priority: higher = runs first when multiple tasks are due. */
        int getPriority();

        /** Minimum milliseconds between consecutive runs of this task. */
        long getMinIntervalMs();

        /** Executes the training round. Called on the scheduler thread. */
        void run();

        /** Returns true if the task should be skipped (e.g., no data yet). */
        default boolean isReady() { return true; }
    }

    // -----------------------------------------------------------------------
    // Task wrapper (tracks last-run time)
    // -----------------------------------------------------------------------
    private static class TaskEntry implements Comparable<TaskEntry> {
        final LearningTask task;
        volatile long      lastRunMs = 0;
        final AtomicInteger runCount  = new AtomicInteger(0);
        final AtomicInteger skipCount = new AtomicInteger(0);

        TaskEntry(LearningTask t) { task = t; }

        boolean isDue() {
            return System.currentTimeMillis() - lastRunMs >= task.getMinIntervalMs();
        }

        @Override
        public int compareTo(TaskEntry other) {
            // Higher priority first; if equal, longest-overdue first
            int pc = Integer.compare(other.task.getPriority(), task.getPriority());
            if (pc != 0) return pc;
            return Long.compare(lastRunMs, other.lastRunMs); // smaller = older = higher urgency
        }
    }

    // -----------------------------------------------------------------------
    // Fields
    // -----------------------------------------------------------------------
    private final PerformanceMonitor    perfMonitor;
    private final SmartBatteryOptimizer batteryOpt;

    private final List<TaskEntry>          tasks     = new ArrayList<>();
    private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();
    private ScheduledFuture<?>             ticker;

    private final AtomicBoolean running       = new AtomicBoolean(false);
    private final AtomicLong    currentInterval = new AtomicLong(BASE_INTERVAL_MS);
    private final AtomicInteger totalRounds   = new AtomicInteger(0);
    private final AtomicInteger skippedRounds = new AtomicInteger(0);

    // -----------------------------------------------------------------------
    // Constructor
    // -----------------------------------------------------------------------
    public AdaptiveLearningScheduler(PerformanceMonitor perfMonitor,
                                     SmartBatteryOptimizer batteryOpt) {
        this.perfMonitor = perfMonitor;
        this.batteryOpt  = batteryOpt;
    }

    // -----------------------------------------------------------------------
    // Lifecycle
    // -----------------------------------------------------------------------

    public void start() {
        if (!running.compareAndSet(false, true)) return;
        scheduleTick(BASE_INTERVAL_MS);
        Log.i(TAG, "AdaptiveLearningScheduler started");
    }

    public void stop() {
        if (!running.compareAndSet(true, false)) return;
        if (ticker != null) ticker.cancel(false);
        scheduler.shutdown();
        Log.i(TAG, "AdaptiveLearningScheduler stopped. rounds=" + totalRounds.get()
                + " skipped=" + skippedRounds.get());
    }

    // -----------------------------------------------------------------------
    // Task management
    // -----------------------------------------------------------------------

    public synchronized void registerTask(LearningTask task) {
        if (task == null) return;
        tasks.add(new TaskEntry(task));
        Collections.sort(tasks);
        Log.d(TAG, "Registered task: " + task.getName() + " priority=" + task.getPriority());
    }

    public synchronized void unregisterTask(String name) {
        tasks.removeIf(e -> e.task.getName().equals(name));
    }

    // -----------------------------------------------------------------------
    // Tick
    // -----------------------------------------------------------------------

    private void scheduleTick(long delayMs) {
        if (!running.get()) return;
        ticker = scheduler.schedule(this::tick, delayMs, TimeUnit.MILLISECONDS);
    }

    private void tick() {
        try {
            if (!running.get()) return;

            boolean throttle = shouldThrottle();
            if (throttle) {
                // Back off
                long next = Math.min(currentInterval.get() * 2, MAX_INTERVAL_MS);
                currentInterval.set(next);
                skippedRounds.incrementAndGet();
                Log.d(TAG, "Resource pressure — backing off to " + next + " ms");
                scheduleTick(next);
                return;
            }

            // Resources are fine — reset interval
            currentInterval.set(BASE_INTERVAL_MS);

            // Run due tasks in priority order
            List<TaskEntry> due;
            synchronized (this) {
                due = new ArrayList<>(tasks);
                Collections.sort(due);
            }

            boolean ranAny = false;
            for (TaskEntry entry : due) {
                if (!entry.isDue() || !entry.task.isReady()) {
                    entry.skipCount.incrementAndGet();
                    continue;
                }
                try {
                    Log.d(TAG, "Running task: " + entry.task.getName());
                    long t0 = System.currentTimeMillis();
                    entry.task.run();
                    long elapsed = System.currentTimeMillis() - t0;
                    entry.lastRunMs = System.currentTimeMillis();
                    entry.runCount.incrementAndGet();
                    totalRounds.incrementAndGet();
                    ranAny = true;
                    Log.d(TAG, "Task " + entry.task.getName() + " done in " + elapsed + " ms");

                    // Re-check resources after each task
                    if (shouldThrottle()) break;

                } catch (Exception e) {
                    Log.e(TAG, "Task " + entry.task.getName() + " threw", e);
                }
            }

            scheduleTick(currentInterval.get());

        } catch (Exception e) {
            Log.e(TAG, "Tick error", e);
            scheduleTick(currentInterval.get());
        }
    }

    // -----------------------------------------------------------------------
    // Resource check
    // -----------------------------------------------------------------------

    private boolean shouldThrottle() {
        if (batteryOpt != null && batteryOpt.isCriticalBattery()) return true;
        if (batteryOpt != null && batteryOpt.shouldThrottle())    return true;
        if (perfMonitor != null && perfMonitor.shouldThrottle())   return true;
        return false;
    }

    // -----------------------------------------------------------------------
    // Statistics
    // -----------------------------------------------------------------------

    public int getTotalRounds()    { return totalRounds.get(); }
    public int getSkippedRounds()  { return skippedRounds.get(); }
    public long getCurrentInterval() { return currentInterval.get(); }

    public Map<String, Object> getStats() {
        Map<String, Object> m = new java.util.HashMap<>();
        m.put("totalRounds",     totalRounds.get());
        m.put("skippedRounds",   skippedRounds.get());
        m.put("currentInterval", currentInterval.get());
        m.put("isRunning",       running.get());
        m.put("taskCount",       tasks.size());
        List<Map<String, Object>> taskStats = new ArrayList<>();
        for (TaskEntry e : tasks) {
            Map<String, Object> ts = new java.util.HashMap<>();
            ts.put("name",      e.task.getName());
            ts.put("priority",  e.task.getPriority());
            ts.put("runCount",  e.runCount.get());
            ts.put("skipCount", e.skipCount.get());
            ts.put("lastRunMs", e.lastRunMs);
            taskStats.add(ts);
        }
        m.put("tasks", taskStats);
        return m;
    }
}
