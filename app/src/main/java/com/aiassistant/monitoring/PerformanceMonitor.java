package com.aiassistant.monitoring;

import android.app.ActivityManager;
import android.content.Context;
import android.os.Debug;
import android.os.SystemClock;
import android.util.Log;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Monitors CPU and memory usage of the AI assistant process and provides
 * throttle signals so the service can back off when resources are tight.
 *
 * Sampling is done on a background thread every {@link #SAMPLE_INTERVAL_MS} ms.
 * Callers can read the latest snapshot via {@link #getSnapshot()} at any time
 * without blocking.
 */
public class PerformanceMonitor {

    private static final String TAG = "PerformanceMonitor";

    private static final long SAMPLE_INTERVAL_MS  = 5_000L;
    private static final int  CPU_HIGH_THRESHOLD  = 70;   // percent
    private static final int  MEM_LOW_MB_THRESHOLD = 100; // available MB

    // -----------------------------------------------------------------------
    // State
    // -----------------------------------------------------------------------

    private final Context          context;
    private final ActivityManager  activityManager;
    private final ScheduledExecutorService scheduler;
    private ScheduledFuture<?>     future;
    private final AtomicBoolean    running = new AtomicBoolean(false);

    // Latest sample (written by background thread, read by any thread)
    private volatile float  cpuPercent      = 0f;
    private volatile long   availableMemMB  = 0L;
    private volatile long   totalMemMB      = 0L;
    private volatile long   usedMemMB       = 0L;
    private volatile long   nativeHeapKB    = 0L;
    private volatile long   lastSampleTime  = 0L;

    // Previous CPU counters for delta calculation
    private long prevIdleTime  = 0;
    private long prevTotalTime = 0;

    // -----------------------------------------------------------------------
    // Constructor
    // -----------------------------------------------------------------------

    public PerformanceMonitor(Context context) {
        this.context         = context.getApplicationContext();
        this.activityManager = (ActivityManager) this.context.getSystemService(Context.ACTIVITY_SERVICE);
        this.scheduler       = Executors.newSingleThreadScheduledExecutor();
    }

    // -----------------------------------------------------------------------
    // Lifecycle
    // -----------------------------------------------------------------------

    public void start() {
        if (!running.compareAndSet(false, true)) return;
        // Take an initial sample immediately, then every SAMPLE_INTERVAL_MS
        future = scheduler.scheduleAtFixedRate(
                this::sample, 0, SAMPLE_INTERVAL_MS, TimeUnit.MILLISECONDS);
        Log.i(TAG, "PerformanceMonitor started");
    }

    public void stop() {
        if (!running.compareAndSet(true, false)) return;
        if (future != null) future.cancel(false);
        scheduler.shutdown();
        Log.i(TAG, "PerformanceMonitor stopped");
    }

    // -----------------------------------------------------------------------
    // Sampling
    // -----------------------------------------------------------------------

    private void sample() {
        sampleMemory();
        sampleCpu();
        lastSampleTime = System.currentTimeMillis();
    }

    private void sampleMemory() {
        try {
            ActivityManager.MemoryInfo mi = new ActivityManager.MemoryInfo();
            activityManager.getMemoryInfo(mi);
            availableMemMB = mi.availMem  / (1024 * 1024);
            totalMemMB     = mi.totalMem  / (1024 * 1024);
            usedMemMB      = totalMemMB - availableMemMB;
            nativeHeapKB   = Debug.getNativeHeapAllocatedSize() / 1024;
        } catch (Exception e) {
            Log.w(TAG, "Memory sample failed", e);
        }
    }

    private void sampleCpu() {
        try {
            // Parse /proc/stat for system-wide CPU usage
            File stat = new File("/proc/stat");
            if (!stat.exists()) return;
            String line;
            try (BufferedReader br = new BufferedReader(new FileReader(stat))) {
                line = br.readLine(); // first line: cpu totals
            }
            if (line == null || !line.startsWith("cpu ")) return;
            String[] parts = line.trim().split("\\s+");
            if (parts.length < 8) return;

            long user    = Long.parseLong(parts[1]);
            long nice    = Long.parseLong(parts[2]);
            long system  = Long.parseLong(parts[3]);
            long idle    = Long.parseLong(parts[4]);
            long iowait  = Long.parseLong(parts[5]);
            long irq     = Long.parseLong(parts[6]);
            long softirq = Long.parseLong(parts[7]);

            long idleTime  = idle + iowait;
            long totalTime = user + nice + system + idle + iowait + irq + softirq;

            long deltaIdle  = idleTime  - prevIdleTime;
            long deltaTotal = totalTime - prevTotalTime;

            if (deltaTotal > 0) {
                cpuPercent = 100f * (1f - (float) deltaIdle / deltaTotal);
            }

            prevIdleTime  = idleTime;
            prevTotalTime = totalTime;
        } catch (Exception e) {
            Log.w(TAG, "CPU sample failed", e);
        }
    }

    // -----------------------------------------------------------------------
    // Public API
    // -----------------------------------------------------------------------

    /** Returns a snapshot of the latest performance metrics. */
    public Map<String, Object> getSnapshot() {
        Map<String, Object> snap = new HashMap<>();
        snap.put("cpu_percent",       cpuPercent);
        snap.put("available_mem_mb",  availableMemMB);
        snap.put("total_mem_mb",      totalMemMB);
        snap.put("used_mem_mb",       usedMemMB);
        snap.put("native_heap_kb",    nativeHeapKB);
        snap.put("sample_age_ms",     System.currentTimeMillis() - lastSampleTime);
        snap.put("high_cpu",          isHighCpu());
        snap.put("low_memory",        isLowMemory());
        return snap;
    }

    public float getCpuPercent()     { return cpuPercent; }
    public long  getAvailableMemMB() { return availableMemMB; }
    public long  getUsedMemMB()      { return usedMemMB; }

    /** Returns true when CPU load exceeds {@link #CPU_HIGH_THRESHOLD}. */
    public boolean isHighCpu() { return cpuPercent > CPU_HIGH_THRESHOLD; }

    /** Returns true when available RAM is below {@link #MEM_LOW_MB_THRESHOLD}. */
    public boolean isLowMemory() { return availableMemMB < MEM_LOW_MB_THRESHOLD; }

    /** Returns true when the system should throttle AI work. */
    public boolean shouldThrottle() { return isHighCpu() || isLowMemory(); }
}
