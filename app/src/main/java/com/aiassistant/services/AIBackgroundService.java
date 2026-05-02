package com.aiassistant.services;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Binder;
import android.os.Build;
import android.os.IBinder;
import android.os.PowerManager;
import android.util.Log;

import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;
import androidx.preference.PreferenceManager;

import com.aiassistant.MainActivity;
import com.aiassistant.R;
import com.aiassistant.core.AIController;
import com.aiassistant.learning.LearningEngine;
import com.aiassistant.monitoring.PerformanceMonitor;
import com.aiassistant.monitoring.NetworkStateMonitor;
import com.aiassistant.monitoring.SmartBatteryOptimizer;
import com.aiassistant.scheduler.TaskSchedulerManager;
import utils.PermissionManager;

import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Background foreground service for the AI Assistant.
 *
 * Improvements over the original stub:
 *  - initializeAIComponents() actually creates and starts AIController,
 *    LearningEngine, TaskSchedulerManager, PerformanceMonitor,
 *    NetworkStateMonitor, and SmartBatteryOptimizer.
 *  - startAIProcessing() runs a real adaptive loop that varies its polling
 *    frequency based on battery and performance state.
 *  - executeTask() and stopTask() delegate to TaskSchedulerManager.
 *  - updateAIMode() updates all live subsystems.
 *  - cleanupAIComponents() properly shuts down each subsystem in order.
 */
public class AIBackgroundService extends Service {

    private static final String TAG                    = "AIBackgroundService";
    private static final String CHANNEL_ID             = "ai_assistant_channel";
    private static final int    FOREGROUND_NOTIF_ID    = 1001;

    // Polling intervals (ms)
    private static final long POLL_ACTIVE_MS      =  1_000L;
    private static final long POLL_BALANCED_MS    =  3_000L;
    private static final long POLL_LOW_POWER_MS   = 10_000L;
    private static final long POLL_LEARNING_MS    =  5_000L;

    // -------------------------------------------------------------------------
    // Binder
    // -------------------------------------------------------------------------

    public class LocalBinder extends Binder {
        public AIBackgroundService getService() { return AIBackgroundService.this; }
    }

    private final IBinder binder = new LocalBinder();

    // -------------------------------------------------------------------------
    // State
    // -------------------------------------------------------------------------

    private ExecutorService executorService;
    private PowerManager.WakeLock wakeLock;
    private final AtomicBoolean isRunning    = new AtomicBoolean(false);

    // Preferences
    private String  currentAIMode  = "balanced";
    private boolean lowPowerMode   = false;
    private boolean privacyMode    = false;

    // Subsystems
    private AIController        aiController;
    private LearningEngine      learningEngine;
    private TaskSchedulerManager taskScheduler;
    private PerformanceMonitor  performanceMonitor;
    private NetworkStateMonitor  networkMonitor;
    private SmartBatteryOptimizer batteryOptimizer;

    // -------------------------------------------------------------------------
    // Lifecycle
    // -------------------------------------------------------------------------

    @Override
    public void onCreate() {
        super.onCreate();
        Log.i(TAG, "AIBackgroundService created");

        createNotificationChannel();
        executorService = Executors.newFixedThreadPool(3);

        PowerManager pm = (PowerManager) getSystemService(POWER_SERVICE);
        wakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "AIAssistant::BGService");

        loadPreferences();
        initializeAIComponents();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.i(TAG, "AIBackgroundService started");

        if (intent != null && intent.getAction() != null) {
            handleCommandAction(intent);
        } else {
            startForeground(FOREGROUND_NOTIF_ID, createNotification());
            if (!isRunning.getAndSet(true)) {
                startAIProcessing();
            }
        }
        return START_STICKY;
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) { return binder; }

    @Override
    public void onDestroy() {
        Log.i(TAG, "AIBackgroundService destroying");
        isRunning.set(false);

        if (wakeLock != null && wakeLock.isHeld()) wakeLock.release();
        if (executorService != null) {
            executorService.shutdown();
            try { executorService.awaitTermination(3, TimeUnit.SECONDS); }
            catch (InterruptedException ignored) {}
        }

        cleanupAIComponents();
        super.onDestroy();
    }

    // -------------------------------------------------------------------------
    // Command routing
    // -------------------------------------------------------------------------

    private void handleCommandAction(Intent intent) {
        String action = intent.getAction();
        if (action == null) return;
        switch (action) {
            case "UPDATE_AI_MODE":
                String newMode = intent.getStringExtra("mode");
                updateAIMode(newMode != null ? newMode : currentAIMode);
                break;
            case "START_TASK":
                String startId = intent.getStringExtra("task_id");
                if (startId != null) executeTask(startId);
                break;
            case "STOP_TASK":
                String stopId = intent.getStringExtra("task_id");
                if (stopId != null) stopTask(stopId);
                break;
            case "UPDATE_SETTINGS":
                loadPreferences();
                applyPreferencesToSubsystems();
                break;
            case "STOP_SERVICE":
                stopSelf();
                break;
        }
    }

    // -------------------------------------------------------------------------
    // Notification helpers
    // -------------------------------------------------------------------------

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel ch = new NotificationChannel(
                    CHANNEL_ID, "AI Assistant Service", NotificationManager.IMPORTANCE_LOW);
            ch.setDescription("Background AI processing");
            ch.enableVibration(false);
            ch.setSound(null, null);
            getSystemService(NotificationManager.class).createNotificationChannel(ch);
        }
    }

    private Notification createNotification() {
        Intent mainIntent = new Intent(this, MainActivity.class);
        PendingIntent pi   = PendingIntent.getActivity(this, 0, mainIntent,
                PendingIntent.FLAG_IMMUTABLE);
        String modeText = "Mode: " + currentAIMode + (lowPowerMode ? " [battery saver]" : "");
        return new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle(getString(R.string.app_name))
                .setContentText(modeText)
                .setSmallIcon(R.mipmap.ic_launcher)
                .setContentIntent(pi)
                .setOngoing(true)
                .build();
    }

    private void updateNotification() {
        NotificationManager nm = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        if (nm != null) nm.notify(FOREGROUND_NOTIF_ID, createNotification());
    }

    // -------------------------------------------------------------------------
    // Preferences
    // -------------------------------------------------------------------------

    private void loadPreferences() {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        currentAIMode = prefs.getString("ai_mode", "balanced");
        lowPowerMode  = prefs.getBoolean("low_power_mode", false);
        privacyMode   = prefs.getBoolean("privacy_mode", false);
        Log.d(TAG, "Preferences: mode=" + currentAIMode +
                ", lowPower=" + lowPowerMode + ", privacy=" + privacyMode);
    }

    private void applyPreferencesToSubsystems() {
        if (batteryOptimizer != null) batteryOptimizer.setLowPowerOverride(lowPowerMode);
        if (learningEngine   != null) learningEngine.setLowPowerMode(lowPowerMode);
        if (aiController     != null) {
            switch (currentAIMode) {
                case "gaming":   aiController.setMode(AIController.Mode.GAMING);   break;
                case "learning": aiController.setMode(AIController.Mode.LEARNING); break;
                case "passive":  aiController.setMode(AIController.Mode.PASSIVE);  break;
                default:         aiController.setMode(AIController.Mode.ACTIVE);   break;
            }
        }
        updateNotification();
    }

    // -------------------------------------------------------------------------
    // AI component lifecycle
    // -------------------------------------------------------------------------

    private void initializeAIComponents() {
        Log.i(TAG, "Initializing AI components");

        try {
            // 1. Performance monitor (first — others query it)
            performanceMonitor = new PerformanceMonitor(getApplicationContext());
            performanceMonitor.start();

            // 2. Battery optimizer
            batteryOptimizer = new SmartBatteryOptimizer(getApplicationContext());
            batteryOptimizer.setLowPowerOverride(lowPowerMode);

            // 3. Network monitor
            networkMonitor = new NetworkStateMonitor(getApplicationContext());
            networkMonitor.start();

            // 4. Learning engine
            learningEngine = new LearningEngine(getApplicationContext());
            learningEngine.setLowPowerMode(lowPowerMode);

            // 5. Task scheduler
            taskScheduler = new TaskSchedulerManager(getApplicationContext());
            taskScheduler.start();

            // 6. Central AI controller
            aiController = AIController.getInstance(getApplicationContext());
            aiController.initialize();
            aiController.startSuggestionPolling();

            // Apply current preferences
            applyPreferencesToSubsystems();

            // Acquire partial wake-lock
            if (!wakeLock.isHeld()) wakeLock.acquire();

            Log.i(TAG, "All AI components initialized successfully");

        } catch (Exception e) {
            Log.e(TAG, "Error initializing AI components", e);
        }
    }

    private void cleanupAIComponents() {
        Log.i(TAG, "Cleaning up AI components");

        if (aiController != null) {
            try { aiController.stopSuggestionPolling(); } catch (Exception ignored) {}
        }
        if (taskScheduler != null) {
            try { taskScheduler.stop(); } catch (Exception ignored) {}
        }
        if (networkMonitor != null) {
            try { networkMonitor.stop(); } catch (Exception ignored) {}
        }
        if (performanceMonitor != null) {
            try { performanceMonitor.stop(); } catch (Exception ignored) {}
        }
        // batteryOptimizer and learningEngine are passive — no teardown needed
    }

    // -------------------------------------------------------------------------
    // AI processing loop
    // -------------------------------------------------------------------------

    private void startAIProcessing() {
        Log.i(TAG, "Starting AI processing loop in mode: " + currentAIMode);

        executorService.execute(() -> {
            while (isRunning.get()) {
                try {
                    long sleepMs = computePollInterval();
                    Thread.sleep(sleepMs);

                    if (!isRunning.get()) break;

                    // Tick the learning engine with current performance data
                    if (learningEngine != null && performanceMonitor != null) {
                        Map<String, Object> perfSnapshot = performanceMonitor.getSnapshot();
                        if (!perfSnapshot.isEmpty()) {
                            learningEngine.processScreenAnalysis(perfSnapshot);
                        }
                    }

                    // Let battery optimizer advise whether to throttle
                    boolean throttle = batteryOptimizer != null && batteryOptimizer.shouldThrottle();
                    if (throttle) {
                        Log.d(TAG, "Battery optimizer: throttling AI processing");
                        continue;
                    }

                    // Trigger suggestions if in active/gaming/learning mode
                    if (aiController != null) {
                        AIController.Mode mode = aiController.getMode();
                        if (mode == AIController.Mode.ACTIVE   ||
                            mode == AIController.Mode.GAMING   ||
                            mode == AIController.Mode.LEARNING) {
                            aiController.getSuggestions();
                        }
                    }

                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                } catch (Exception e) {
                    Log.e(TAG, "Error in AI processing loop", e);
                }
            }
            Log.i(TAG, "AI processing loop exited");
        });
    }

    /** Determines the current poll interval based on mode and battery. */
    private long computePollInterval() {
        if (batteryOptimizer != null && batteryOptimizer.isCriticalBattery()) {
            return POLL_LOW_POWER_MS;
        }
        if (lowPowerMode) return POLL_LOW_POWER_MS;
        switch (currentAIMode) {
            case "active":   return POLL_ACTIVE_MS;
            case "learning": return POLL_LEARNING_MS;
            case "passive":  return POLL_LOW_POWER_MS;
            default:         return POLL_BALANCED_MS;
        }
    }

    // -------------------------------------------------------------------------
    // Task management
    // -------------------------------------------------------------------------

    private void updateAIMode(String mode) {
        Log.i(TAG, "Updating AI mode to: " + mode);
        currentAIMode = mode;
        applyPreferencesToSubsystems();
    }

    private void executeTask(String taskId) {
        Log.d(TAG, "Executing task: " + taskId);
        if (taskScheduler != null) {
            com.aiassistant.scheduler.model.ScheduledTask task = taskScheduler.getTask(taskId);
            if (task != null) {
                // Re-schedule immediately with current time
                task.setScheduledTime(new java.util.Date());
                taskScheduler.scheduleTask(task);
            }
        }
    }

    private void stopTask(String taskId) {
        Log.d(TAG, "Stopping task: " + taskId);
        if (taskScheduler != null) taskScheduler.cancelTask(taskId);
    }

    // -------------------------------------------------------------------------
    // Public API (for bound clients)
    // -------------------------------------------------------------------------

    public void setLowPowerMode(boolean enabled) {
        lowPowerMode = enabled;
        applyPreferencesToSubsystems();
        Log.d(TAG, "Low power mode set to: " + enabled);
    }

    public void setPrivacyMode(boolean enabled) {
        privacyMode = enabled;
        AIAccessibilityService svc = AIAccessibilityService.getInstance();
        if (svc != null) svc.setPrivacyMode(enabled);
        Log.d(TAG, "Privacy mode set to: " + enabled);
    }

    public boolean hasRequiredPermissions() {
        PermissionManager pm = new PermissionManager(this);
        return pm.hasAccessibilityPermission() &&
               pm.hasOverlayPermission()       &&
               pm.hasUsageStatsPermission();
    }

    public Map<String, Object> getStatusSnapshot() {
        java.util.HashMap<String, Object> status = new java.util.HashMap<>();
        status.put("mode", currentAIMode);
        status.put("lowPower", lowPowerMode);
        status.put("privacy", privacyMode);
        if (performanceMonitor != null) status.putAll(performanceMonitor.getSnapshot());
        if (batteryOptimizer   != null) status.put("batteryThrottle", batteryOptimizer.shouldThrottle());
        if (taskScheduler      != null) status.put("pendingTasks",
                taskScheduler.getTasksByStatus(
                        com.aiassistant.scheduler.model.TaskStatus.PENDING).size());
        return status;
    }
}
