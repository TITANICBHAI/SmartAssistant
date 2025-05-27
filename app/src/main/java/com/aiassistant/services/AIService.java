package com.aiassistant.services;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.Binder;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.PowerManager;
import android.util.Log;

import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;

import com.aiassistant.core.AIController;
import models.AIMode;
import models.AIState;
import models.AppState;
import models.PerformanceMode;
import com.aiassistant.ui.MainActivity;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * AI Service
 * Background service for the AI assistant
 */
public class AIService extends Service {
    private static final String TAG = "AIService";
    
    // Notification IDs
    private static final int NOTIFICATION_ID = 1;
    private static final String CHANNEL_ID = "AIServiceChannel";
    
    // Actions
    public static final String ACTION_START = "com.aiassistant.action.START";
    public static final String ACTION_STOP = "com.aiassistant.action.STOP";
    public static final String ACTION_UPDATE_STATE = "com.aiassistant.action.UPDATE_STATE";
    public static final String ACTION_SET_AI_MODE = "com.aiassistant.action.SET_AI_MODE";
    public static final String ACTION_SET_PERFORMANCE_MODE = "com.aiassistant.action.SET_PERFORMANCE_MODE";
    public static final String ACTION_PACKAGE_CHANGED = "com.aiassistant.action.PACKAGE_CHANGED";
    
    // Extras
    public static final String EXTRA_AI_MODE = "com.aiassistant.extra.AI_MODE";
    public static final String EXTRA_PERFORMANCE_MODE = "com.aiassistant.extra.PERFORMANCE_MODE";
    public static final String EXTRA_PACKAGE_NAME = "com.aiassistant.extra.PACKAGE_NAME";
    public static final String EXTRA_APP_NAME = "com.aiassistant.extra.APP_NAME";
    
    // Binder for service clients
    private final IBinder binder = new LocalBinder();
    
    // Controller
    private AIController controller;
    
    // Scheduler for background tasks
    private ScheduledExecutorService scheduler;
    
    // Handler for UI thread operations
    private Handler mainHandler;
    
    // Power manager wake lock
    private PowerManager.WakeLock wakeLock;
    
    @Override
    public void onCreate() {
        super.onCreate();
        Log.i(TAG, "Service created");
        
        // Initialize controller
        controller = AIController.getInstance(this);
        
        // Initialize scheduler
        scheduler = Executors.newScheduledThreadPool(2);
        
        // Initialize handler
        mainHandler = new Handler(Looper.getMainLooper());
        
        // Acquire wake lock
        PowerManager powerManager = (PowerManager) getSystemService(POWER_SERVICE);
        wakeLock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "AIAssistant:AIServiceWakeLock");
        wakeLock.acquire();
    }
    
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent != null) {
            String action = intent.getAction();
            Log.i(TAG, "Received action: " + action);
            
            if (action != null) {
                switch (action) {
                    case ACTION_START:
                        startService();
                        break;
                        
                    case ACTION_STOP:
                        stopSelf();
                        break;
                        
                    case ACTION_UPDATE_STATE:
                        updateState(intent);
                        break;
                        
                    case ACTION_SET_AI_MODE:
                        setAIMode(intent);
                        break;
                        
                    case ACTION_SET_PERFORMANCE_MODE:
                        setPerformanceMode(intent);
                        break;
                        
                    case ACTION_PACKAGE_CHANGED:
                        handlePackageChanged(intent);
                        break;
                }
            }
        }
        
        // Make service sticky (restart if killed)
        return START_STICKY;
    }
    
    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return binder;
    }
    
    @Override
    public void onDestroy() {
        Log.i(TAG, "Service destroyed");
        
        // Release wake lock
        if (wakeLock != null && wakeLock.isHeld()) {
            wakeLock.release();
        }
        
        // Shutdown scheduler
        if (scheduler != null && !scheduler.isShutdown()) {
            scheduler.shutdown();
        }
        
        // Call controller
        controller.getState().setServiceEnabled(false);
        
        super.onDestroy();
    }
    
    /**
     * Start the service
     */
    private void startService() {
        Log.i(TAG, "Starting AI service");
        
        // Create notification channel for Android 8.0+
        createNotificationChannel();
        
        // Start foreground with notification
        startForeground(NOTIFICATION_ID, createNotification());
        
        // Start periodic tasks
        startPeriodicTasks();
        
        // Update controller state
        controller.getState().setServiceEnabled(true);
    }
    
    /**
     * Update service state
     */
    private void updateState(Intent intent) {
        if (intent.hasExtra(EXTRA_AI_MODE)) {
            String modeStr = intent.getStringExtra(EXTRA_AI_MODE);
            try {
                AIMode mode = AIMode.valueOf(modeStr);
                updateAIMode(mode);
            } catch (IllegalArgumentException e) {
                Log.e(TAG, "Invalid AI mode: " + modeStr);
            }
        }
        
        if (intent.hasExtra(EXTRA_PERFORMANCE_MODE)) {
            String modeStr = intent.getStringExtra(EXTRA_PERFORMANCE_MODE);
            try {
                PerformanceMode mode = PerformanceMode.valueOf(modeStr);
                updatePerformanceMode(mode);
            } catch (IllegalArgumentException e) {
                Log.e(TAG, "Invalid performance mode: " + modeStr);
            }
        }
        
        // Update notification to reflect current state
        updateNotification();
    }
    
    /**
     * Set AI mode
     */
    private void setAIMode(Intent intent) {
        if (intent.hasExtra(EXTRA_AI_MODE)) {
            String modeStr = intent.getStringExtra(EXTRA_AI_MODE);
            try {
                AIMode mode = AIMode.valueOf(modeStr);
                updateAIMode(mode);
                
                // Update controller
                controller.setAIMode(mode);
                
                // Update notification
                updateNotification();
            } catch (IllegalArgumentException e) {
                Log.e(TAG, "Invalid AI mode: " + modeStr);
            }
        }
    }
    
    /**
     * Update AI mode in service
     */
    private void updateAIMode(AIMode mode) {
        Log.i(TAG, "Updating AI mode to: " + mode);
        
        // Handle mode change
        switch (mode) {
            case AUTO_AI:
                // Start more aggressive AI operations
                break;
                
            case COPILOT:
                // Start suggestion mode
                break;
                
            case PASSIVE:
                // Just observe and learn
                break;
        }
    }
    
    /**
     * Set performance mode
     */
    private void setPerformanceMode(Intent intent) {
        if (intent.hasExtra(EXTRA_PERFORMANCE_MODE)) {
            String modeStr = intent.getStringExtra(EXTRA_PERFORMANCE_MODE);
            try {
                PerformanceMode mode = PerformanceMode.valueOf(modeStr);
                updatePerformanceMode(mode);
                
                // Update controller
                controller.setPerformanceMode(mode);
            } catch (IllegalArgumentException e) {
                Log.e(TAG, "Invalid performance mode: " + modeStr);
            }
        }
    }
    
    /**
     * Update performance mode in service
     */
    private void updatePerformanceMode(PerformanceMode mode) {
        Log.i(TAG, "Updating performance mode to: " + mode);
        
        // Adjust service behavior based on performance mode
        switch (mode) {
            case BATTERY_SAVER:
                // Reduce background tasks
                break;
                
            case BALANCED:
                // Default behavior
                break;
                
            case HIGH_PERFORMANCE:
                // More aggressive AI processing
                break;
        }
    }
    
    /**
     * Handle package change
     */
    private void handlePackageChanged(Intent intent) {
        String packageName = intent.getStringExtra(EXTRA_PACKAGE_NAME);
        String appName = intent.getStringExtra(EXTRA_APP_NAME);
        
        if (packageName != null) {
            Log.i(TAG, "Package change detected: " + packageName);
            
            // Update AI state with active app
            AIState state = controller.getState();
            
            // Find or create app state
            AppState appState = null;
            for (AppState app : state.getActiveApps()) {
                if (packageName.equals(app.getPackageName())) {
                    appState = app;
                    break;
                }
            }
            
            // If app not found, create new state
            if (appState == null) {
                appState = new AppState(packageName, appName != null ? appName : packageName);
                state.getActiveApps().add(appState);
            }
            
            // Set as foreground app
            for (AppState app : state.getActiveApps()) {
                app.setForeground(packageName.equals(app.getPackageName()));
            }
        }
    }
    
    /**
     * Start periodic tasks
     */
    private void startPeriodicTasks() {
        // State update task (every 5 seconds)
        scheduler.scheduleAtFixedRate(new Runnable() {
            @Override
            public void run() {
                updateSystemState();
            }
        }, 5, 5, TimeUnit.SECONDS);
        
        // Learning task (every 30 seconds)
        scheduler.scheduleAtFixedRate(new Runnable() {
            @Override
            public void run() {
                processLearningData();
            }
        }, 30, 30, TimeUnit.SECONDS);
    }
    
    /**
     * Update system state
     */
    private void updateSystemState() {
        // This would update device state, battery level, etc.
        Log.d(TAG, "Updating system state");
    }
    
    /**
     * Process learning data
     */
    private void processLearningData() {
        // This would process collected data for AI learning
        Log.d(TAG, "Processing learning data");
    }
    
    /**
     * Create notification channel
     */
    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID,
                    "AI Assistant Service",
                    NotificationManager.IMPORTANCE_LOW);
            
            channel.setDescription("Keeps the AI assistant running in the background");
            
            NotificationManager notificationManager = getSystemService(NotificationManager.class);
            notificationManager.createNotificationChannel(channel);
        }
    }
    
    /**
     * Create service notification
     */
    private Notification createNotification() {
        // Create intent for notification click
        Intent notificationIntent = new Intent(this, MainActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(
                this, 0, notificationIntent, PendingIntent.FLAG_IMMUTABLE);
        
        // Get current AI mode
        AIMode aiMode = controller.getAIMode();
        String modeText = getAIModeName(aiMode);
        
        // Create notification
        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle("AI Assistant Active")
                .setContentText("Mode: " + modeText)
                .setSmallIcon(android.R.drawable.ic_dialog_info)
                .setContentIntent(pendingIntent)
                .setPriority(NotificationCompat.PRIORITY_LOW);
        
        return builder.build();
    }
    
    /**
     * Update notification
     */
    private void updateNotification() {
        NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        notificationManager.notify(NOTIFICATION_ID, createNotification());
    }
    
    /**
     * Get human-readable AI mode name
     */
    private String getAIModeName(AIMode mode) {
        switch (mode) {
            case AUTO_AI:
                return "Autonomous";
            case COPILOT:
                return "Co-Pilot";
            case PASSIVE:
                return "Passive";
            default:
                return "Unknown";
        }
    }
    
    /**
     * Local binder class
     */
    public class LocalBinder extends Binder {
        public AIService getService() {
            return AIService.this;
        }
    }
}