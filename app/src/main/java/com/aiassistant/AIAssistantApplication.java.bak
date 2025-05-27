package com.aiassistant;

import android.app.Application;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.util.Log;

import androidx.work.Configuration;
import androidx.work.WorkManager;

import com.aiassistant.core.AIController;
import com.aiassistant.services.AIBackgroundService;

/**
 * Main application class for AI Assistant
 * Initializes core components and services
 */
public class AIAssistantApplication extends Application implements Configuration.Provider {
    private static final String TAG = "AIAssistantApp";
    
    private static Context appContext;
    
    @Override
    public void onCreate() {
        super.onCreate();
        appContext = getApplicationContext();
        
        // Initialize WorkManager for background tasks
        WorkManager.initialize(this, getWorkManagerConfiguration());
        
        // Initialize AI Controller in the background
        new Thread(() -> {
            try {
                AIController.getInstance(this);
                Log.i(TAG, "AIController initialized");
            } catch (Exception e) {
                Log.e(TAG, "Error initializing AIController: " + e.getMessage());
                // Use fallback initialization if needed
                initializeFallbackMode();
            }
        }).start();
        
        // Start background service if needed
        startBackgroundService();
    }
    
    /**
     * Get application context
     */
    public static Context getAppContext() {
        return appContext;
    }
    
    /**
     * Start the background service for persistence
     */
    private void startBackgroundService() {
        try {
            Intent serviceIntent = new Intent(this, AIBackgroundService.class);
            
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                startForegroundService(serviceIntent);
            } else {
                startService(serviceIntent);
            }
            
            Log.d(TAG, "Background service started");
        } catch (Exception e) {
            Log.e(TAG, "Error starting background service: " + e.getMessage());
        }
    }
    
    /**
     * Initialize fallback mode when regular initialization fails
     */
    private void initializeFallbackMode() {
        Log.w(TAG, "Initializing fallback mode");
        
        // In fallback mode, we use lighter components
        // and avoid features that require special permissions
        
        try {
            // Attempt to initialize with fallback components
            // This would typically involve loading simpler models
            // and avoiding features that require special permissions
            
            // For now, just log that we're in fallback mode
            Log.i(TAG, "Fallback mode initialized");
        } catch (Exception e) {
            Log.e(TAG, "Error initializing fallback mode: " + e.getMessage());
        }
    }
    
    @Override
    public Configuration getWorkManagerConfiguration() {
        return new Configuration.Builder()
                .setMinimumLoggingLevel(Log.INFO)
                .build();
    }
}