package com.aiassistant.ml;

import android.util.Log;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

/**
 * PerformanceProfiler — lightweight on-device profiler for measuring execution
 * time, memory pressure, and throughput of critical AI pipeline stages.
 *
 * Features:
 *   - Named timer sections with begin/end API (nestable).
 *   - Rolling statistics (mean, p50, p95, p99, max) over a configurable window.
 *   - Frame-rate estimation per section (calls/second).
 *   - Slow-call detection: logs a warning when a section exceeds its threshold.
 *   - Throughput counter: tracks items processed per second.
 *   - Memory snapshot helper: reads runtime free/total heap.
 *   - Thread-safe, minimal overhead.
 *   - getReport() returns a sorted summary map ready for display or logging.
 *
 * Usage:
 *   PerformanceProfiler profiler = new PerformanceProfiler();
 *   profiler.setSlowCallThreshold("perception", 80);
 *
 *   profiler.begin("perception");
 *   ... // do work
 *   profiler.end("perception");
 *
 *   Map<String, Object> report = profiler.getReport();
 */
public class PerformanceProfiler {

    private static final String TAG = "PerformanceProfiler";

    private static final int DEFAULT_WINDOW = 100; // rolling window size

    // -------------------------------------------------------------------------
    // Per-section state
    // -------------------------------------------------------------------------
    private static class SectionStats {
        final String           name;
        final Queue<Long>      timings;     // recent ms values
        final int              windowSize;
        final AtomicLong       totalMs      = new AtomicLong(0);
        final AtomicInteger    callCount    = new AtomicInteger(0);
        final AtomicLong       maxMs        = new AtomicLong(0);
        final AtomicInteger    slowCalls    = new AtomicInteger(0);
        volatile long          slowThreshMs = Long.MAX_VALUE;
        volatile long          startNs      = 0L;   // for nested begin/end

        SectionStats(String name, int windowSize) {
            this.name       = name;
            this.windowSize = windowSize;
            this.timings    = new LinkedList<>();
        }

        synchronized void record(long durationMs) {
            callCount.incrementAndGet();
            totalMs.addAndGet(durationMs);
            if (durationMs > maxMs.get()) maxMs.set(durationMs);
            if (durationMs > slowThreshMs) {
                slowCalls.incrementAndGet();
                Log.w(TAG, "Slow call in '" + name + "': " + durationMs
                        + " ms (threshold=" + slowThreshMs + " ms)");
            }
            timings.offer(durationMs);
            if (timings.size() > windowSize) timings.poll();
        }

        synchronized float mean() {
            if (timings.isEmpty()) return 0f;
            long sum = 0;
            for (long v : timings) sum += v;
            return (float) sum / timings.size();
        }

        synchronized long percentile(double p) {
            if (timings.isEmpty()) return 0L;
            List<Long> sorted = new ArrayList<>(timings);
            Collections.sort(sorted);
            int idx = (int)(p * sorted.size() / 100.0);
            return sorted.get(Math.min(idx, sorted.size() - 1));
        }

        float callsPerSecond() {
            int n = callCount.get();
            if (n == 0) return 0f;
            long totalS = totalMs.get();
            return totalS > 0 ? n / (totalS / 1000f) : 0f;
        }
    }

    // -------------------------------------------------------------------------
    // Throughput counter
    // -------------------------------------------------------------------------
    private static class ThroughputCounter {
        final String   name;
        final AtomicLong itemCount   = new AtomicLong(0);
        final AtomicLong windowStart = new AtomicLong(System.currentTimeMillis());
        volatile double  lastRate    = 0.0;

        ThroughputCounter(String name) { this.name = name; }

        void record(long count) { itemCount.addAndGet(count); }

        double getRate() {
            long now     = System.currentTimeMillis();
            long elapsed = now - windowStart.get();
            if (elapsed >= 1000L) {
                lastRate    = itemCount.get() / (elapsed / 1000.0);
                itemCount.set(0);
                windowStart.set(now);
            }
            return lastRate;
        }
    }

    // -------------------------------------------------------------------------
    // Fields
    // -------------------------------------------------------------------------
    private final int windowSize;
    private final ConcurrentHashMap<String, SectionStats>      sections    = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, ThroughputCounter> throughputs = new ConcurrentHashMap<>();
    private final long startMs = System.currentTimeMillis();

    // -------------------------------------------------------------------------
    // Construction
    // -------------------------------------------------------------------------

    public PerformanceProfiler(int windowSize) {
        this.windowSize = windowSize;
    }

    public PerformanceProfiler() {
        this(DEFAULT_WINDOW);
    }

    // -------------------------------------------------------------------------
    // Timer API
    // -------------------------------------------------------------------------

    /**
     * Mark the beginning of a named section.  Stores the current nanosecond timestamp.
     */
    public void begin(String section) {
        getSection(section).startNs = System.nanoTime();
    }

    /**
     * Mark the end of a named section, record the duration.
     * @return Duration in ms.
     */
    public long end(String section) {
        SectionStats s = getSection(section);
        long durationNs = System.nanoTime() - s.startNs;
        long durationMs = durationNs / 1_000_000L;
        s.record(durationMs);
        return durationMs;
    }

    /**
     * Record a pre-measured duration for a section.
     */
    public void record(String section, long durationMs) {
        getSection(section).record(durationMs);
    }

    /**
     * Set a slow-call warning threshold (ms) for a section.
     */
    public void setSlowCallThreshold(String section, long threshMs) {
        getSection(section).slowThreshMs = threshMs;
    }

    // -------------------------------------------------------------------------
    // Throughput API
    // -------------------------------------------------------------------------

    /**
     * Record that {@code count} items were processed (e.g. frames, actions, samples).
     */
    public void recordThroughput(String counter, long count) {
        getThroughput(counter).record(count);
    }

    /**
     * Get the current throughput rate (items/second).
     */
    public double getThroughputRate(String counter) {
        return getThroughput(counter).getRate();
    }

    // -------------------------------------------------------------------------
    // Memory snapshot
    // -------------------------------------------------------------------------

    /**
     * Returns a map with current JVM heap statistics.
     */
    public Map<String, Long> getMemorySnapshot() {
        Runtime rt = Runtime.getRuntime();
        Map<String, Long> m = new LinkedHashMap<>();
        m.put("heapUsedMb",  (rt.totalMemory() - rt.freeMemory()) / (1024 * 1024));
        m.put("heapTotalMb", rt.totalMemory()  / (1024 * 1024));
        m.put("heapMaxMb",   rt.maxMemory()    / (1024 * 1024));
        m.put("heapFreeMb",  rt.freeMemory()   / (1024 * 1024));
        return m;
    }

    // -------------------------------------------------------------------------
    // Reporting
    // -------------------------------------------------------------------------

    /**
     * Get a full performance report, sorted by mean latency (descending).
     */
    public Map<String, Object> getReport() {
        Map<String, Object> report = new LinkedHashMap<>();
        report.put("uptimeMs",   System.currentTimeMillis() - startMs);
        report.put("memory",     getMemorySnapshot());
        report.put("windowSize", windowSize);

        // Section stats
        List<Map<String, Object>> sectionList = new ArrayList<>();
        for (SectionStats s : sections.values()) {
            Map<String, Object> sd = new LinkedHashMap<>();
            sd.put("name",       s.name);
            sd.put("calls",      s.callCount.get());
            sd.put("meanMs",     s.mean());
            sd.put("p50Ms",      s.percentile(50));
            sd.put("p95Ms",      s.percentile(95));
            sd.put("p99Ms",      s.percentile(99));
            sd.put("maxMs",      s.maxMs.get());
            sd.put("slowCalls",  s.slowCalls.get());
            sd.put("callsPerSec", s.callsPerSecond());
            sectionList.add(sd);
        }
        sectionList.sort((a, b) -> Float.compare(
                ((Number)b.get("meanMs")).floatValue(),
                ((Number)a.get("meanMs")).floatValue()));
        report.put("sections", sectionList);

        // Throughput counters
        Map<String, Double> rates = new LinkedHashMap<>();
        for (ThroughputCounter tc : throughputs.values()) rates.put(tc.name, tc.getRate());
        report.put("throughputRates", rates);

        return report;
    }

    /** Get stats for a single section (or empty map if not found). */
    public Map<String, Object> getSectionStats(String section) {
        SectionStats s = sections.get(section);
        if (s == null) return new HashMap<>();
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("calls",       s.callCount.get());
        m.put("meanMs",      s.mean());
        m.put("p95Ms",       s.percentile(95));
        m.put("maxMs",       s.maxMs.get());
        m.put("slowCalls",   s.slowCalls.get());
        m.put("callsPerSec", s.callsPerSecond());
        return m;
    }

    /** Reset all section statistics (keep configuration). */
    public void reset() {
        for (SectionStats s : sections.values()) {
            synchronized (s) { s.timings.clear(); }
            s.totalMs.set(0);
            s.callCount.set(0);
            s.maxMs.set(0);
            s.slowCalls.set(0);
        }
        for (ThroughputCounter tc : throughputs.values()) {
            tc.itemCount.set(0);
            tc.windowStart.set(System.currentTimeMillis());
        }
        Log.i(TAG, "PerformanceProfiler reset.");
    }

    /** Log a compact summary to Logcat. */
    public void logSummary() {
        StringBuilder sb = new StringBuilder("--- Performance Summary ---\n");
        for (SectionStats s : sections.values()) {
            sb.append(String.format("  %-25s mean=%5.1f ms  p95=%5d ms  calls=%d  slow=%d\n",
                    s.name, s.mean(), s.percentile(95),
                    s.callCount.get(), s.slowCalls.get()));
        }
        Log.i(TAG, sb.toString());
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    private SectionStats getSection(String name) {
        return sections.computeIfAbsent(name, n -> new SectionStats(n, windowSize));
    }

    private ThroughputCounter getThroughput(String name) {
        return throughputs.computeIfAbsent(name, ThroughputCounter::new);
    }
}
