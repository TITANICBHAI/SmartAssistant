package com.aiassistant.monitoring;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import java.io.File;
import java.io.FileWriter;
import java.io.PrintWriter;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Crash detection and recovery manager for the AI assistant.
 *
 * Features:
 *  1. Installs an {@link Thread.UncaughtExceptionHandler} that logs crash info
 *     to a local file before forwarding to the default handler.
 *  2. Tracks crash frequency within a rolling time window to distinguish
 *     transient from persistent failures.
 *  3. Provides a "health-check" heartbeat: the background service calls
 *     {@link #heartbeat()} regularly; if it goes missing the monitor knows
 *     the processing loop has stalled.
 *  4. Exposes {@link #shouldRestartService()} so AIBackgroundService can
 *     decide to restart after a crash.
 *  5. Persists crash counts across restarts via SharedPreferences so the
 *     system can give up after {@link #MAX_RESTART_ATTEMPTS} failures.
 */
public class CrashRecoveryManager {

    private static final String TAG = "CrashRecoveryManager";

    private static final String PREFS_NAME          = "crash_recovery";
    private static final String KEY_CRASH_COUNT     = "crash_count";
    private static final String KEY_LAST_CRASH_TIME = "last_crash_time";
    private static final String KEY_SESSION_ID      = "session_id";

    private static final int  MAX_RESTART_ATTEMPTS  = 5;
    private static final long CRASH_WINDOW_MS       = 10 * 60 * 1000L; // 10 min
    private static final long STALL_THRESHOLD_MS    = 60_000L;          // 1 min
    private static final long HEARTBEAT_CHECK_MS    = 30_000L;          // 30 s

    // -----------------------------------------------------------------------
    // Fields
    // -----------------------------------------------------------------------

    private final Context                 context;
    private final SharedPreferences       prefs;
    private final File                    crashLogDir;
    private final ScheduledExecutorService scheduler;
    private final AtomicBoolean           running      = new AtomicBoolean(false);
    private final AtomicInteger           sessionCrashes = new AtomicInteger(0);

    private volatile long   lastHeartbeat = 0;
    private volatile String lastCrashInfo = null;

    private Thread.UncaughtExceptionHandler originalHandler;

    // Callback so AIBackgroundService can restart itself
    public interface RecoveryCallback {
        void onCrashDetected(String crashInfo);
        void onStallDetected(long stalledMs);
        void onGivingUp(int crashCount);
    }

    private RecoveryCallback recoveryCallback;

    // -----------------------------------------------------------------------
    // Constructor
    // -----------------------------------------------------------------------

    public CrashRecoveryManager(Context context) {
        this.context     = context.getApplicationContext();
        this.prefs       = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        this.crashLogDir = new File(context.getFilesDir(), "crash_logs");
        this.scheduler   = Executors.newSingleThreadScheduledExecutor();
        if (!crashLogDir.exists()) crashLogDir.mkdirs();
        resetSessionIfNeeded();
    }

    // -----------------------------------------------------------------------
    // Lifecycle
    // -----------------------------------------------------------------------

    public void install(RecoveryCallback callback) {
        this.recoveryCallback = callback;
        installUncaughtExceptionHandler();
        startHeartbeatWatcher();
        running.set(true);
        Log.i(TAG, "CrashRecoveryManager installed (session crashes: " +
                prefs.getInt(KEY_CRASH_COUNT, 0) + ")");
    }

    public void uninstall() {
        if (!running.compareAndSet(true, false)) return;
        // Restore original handler
        if (originalHandler != null) {
            Thread.setDefaultUncaughtExceptionHandler(originalHandler);
        }
        scheduler.shutdown();
        Log.i(TAG, "CrashRecoveryManager uninstalled");
    }

    // -----------------------------------------------------------------------
    // Heartbeat
    // -----------------------------------------------------------------------

    /** Call this regularly from the AI processing loop (e.g. every iteration). */
    public void heartbeat() {
        lastHeartbeat = System.currentTimeMillis();
    }

    // -----------------------------------------------------------------------
    // Queries
    // -----------------------------------------------------------------------

    /** Returns true if the service crashed recently and should attempt a restart. */
    public boolean shouldRestartService() {
        int total = prefs.getInt(KEY_CRASH_COUNT, 0);
        if (total >= MAX_RESTART_ATTEMPTS) {
            Log.w(TAG, "Too many crashes (" + total + "), giving up");
            return false;
        }
        return total > 0;
    }

    public int  getTotalCrashCount() { return prefs.getInt(KEY_CRASH_COUNT, 0); }
    public int  getSessionCrashCount() { return sessionCrashes.get(); }
    public String getLastCrashInfo()  { return lastCrashInfo; }

    // -----------------------------------------------------------------------
    // Manual crash report (for caught exceptions the service wants to record)
    // -----------------------------------------------------------------------

    public void recordNonFatalException(String tag, Throwable t) {
        String info = formatException(tag, t);
        writeCrashLog(info, false);
        Log.w(TAG, "Non-fatal exception recorded from " + tag);
    }

    // -----------------------------------------------------------------------
    // Reset
    // -----------------------------------------------------------------------

    /** Call after a successful service restart to clear the crash count. */
    public void clearCrashCount() {
        prefs.edit().putInt(KEY_CRASH_COUNT, 0).apply();
        sessionCrashes.set(0);
        Log.i(TAG, "Crash count cleared");
    }

    // -----------------------------------------------------------------------
    // Internal helpers
    // -----------------------------------------------------------------------

    private void installUncaughtExceptionHandler() {
        originalHandler = Thread.getDefaultUncaughtExceptionHandler();
        Thread.setDefaultUncaughtExceptionHandler((thread, throwable) -> {
            handleCrash(thread, throwable);
            if (originalHandler != null) originalHandler.uncaughtException(thread, throwable);
        });
    }

    private void handleCrash(Thread thread, Throwable t) {
        try {
            String info = formatException(thread.getName(), t);
            lastCrashInfo = info;
            writeCrashLog(info, true);

            int total = incrementCrashCount();
            sessionCrashes.incrementAndGet();

            Log.e(TAG, "Crash #" + total + " recorded");

            if (recoveryCallback != null) {
                if (total >= MAX_RESTART_ATTEMPTS) {
                    recoveryCallback.onGivingUp(total);
                } else {
                    recoveryCallback.onCrashDetected(info);
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Error handling crash", e);
        }
    }

    private void startHeartbeatWatcher() {
        lastHeartbeat = System.currentTimeMillis();
        scheduler.scheduleAtFixedRate(() -> {
            if (!running.get()) return;
            long now     = System.currentTimeMillis();
            long elapsed = now - lastHeartbeat;
            if (elapsed > STALL_THRESHOLD_MS) {
                Log.w(TAG, "Heartbeat stalled for " + elapsed + " ms");
                if (recoveryCallback != null) {
                    recoveryCallback.onStallDetected(elapsed);
                }
                // Reset so we don't fire repeatedly
                lastHeartbeat = now;
            }
        }, HEARTBEAT_CHECK_MS, HEARTBEAT_CHECK_MS, TimeUnit.MILLISECONDS);
    }

    private int incrementCrashCount() {
        int count = prefs.getInt(KEY_CRASH_COUNT, 0) + 1;
        prefs.edit()
             .putInt(KEY_CRASH_COUNT, count)
             .putLong(KEY_LAST_CRASH_TIME, System.currentTimeMillis())
             .apply();
        return count;
    }

    private void resetSessionIfNeeded() {
        long lastCrash = prefs.getLong(KEY_LAST_CRASH_TIME, 0);
        if (System.currentTimeMillis() - lastCrash > CRASH_WINDOW_MS) {
            // Crashes are old — reset the counter for a fresh session
            prefs.edit().putInt(KEY_CRASH_COUNT, 0).apply();
            Log.d(TAG, "Crash count reset (window expired)");
        }
    }

    private void writeCrashLog(String info, boolean fatal) {
        try {
            String ts       = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US)
                    .format(new Date());
            String filename = (fatal ? "crash_" : "nonfatal_") + ts + ".txt";
            File   logFile  = new File(crashLogDir, filename);
            try (PrintWriter pw = new PrintWriter(new FileWriter(logFile))) {
                pw.println(info);
            }
            // Keep crash dir tidy — max 20 files
            pruneOldLogs(20);
        } catch (Exception e) {
            Log.e(TAG, "Failed to write crash log", e);
        }
    }

    private void pruneOldLogs(int maxFiles) {
        File[] files = crashLogDir.listFiles();
        if (files == null || files.length <= maxFiles) return;
        // Sort by last modified (oldest first)
        java.util.Arrays.sort(files, (a, b) ->
                Long.compare(a.lastModified(), b.lastModified()));
        for (int i = 0; i < files.length - maxFiles; i++) {
            files[i].delete();
        }
    }

    private static String formatException(String source, Throwable t) {
        StringBuilder sb = new StringBuilder();
        sb.append("Source : ").append(source).append('\n');
        sb.append("Time   : ").append(new Date()).append('\n');
        sb.append("Thread : ").append(Thread.currentThread().getName()).append('\n');
        sb.append("Error  : ").append(t.getClass().getName())
                .append(": ").append(t.getMessage()).append('\n');
        sb.append("Stack  :\n");
        for (StackTraceElement e : t.getStackTrace()) {
            sb.append("  at ").append(e.toString()).append('\n');
        }
        Throwable cause = t.getCause();
        if (cause != null) {
            sb.append("Caused by: ").append(cause.getClass().getName())
                    .append(": ").append(cause.getMessage()).append('\n');
            for (StackTraceElement e : cause.getStackTrace()) {
                sb.append("  at ").append(e.toString()).append('\n');
            }
        }
        return sb.toString();
    }
}
