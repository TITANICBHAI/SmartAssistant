package com.aiassistant;

import android.app.Application;
import android.content.Context;
import android.os.StrictMode;
import android.util.Log;

import androidx.work.Configuration;
import androidx.work.WorkManager;

/**
 * Application class for the Self Learning AI Assistant.
 * Initializes global app components and manages app-wide settings.
 */
public class SelfLearningAIApplication extends Application implements Configuration.Provider {
    
    private static final String TAG = "SelfLearningAI";
    private static SelfLearningAIApplication instance;
    
    @Override
    public void onCreate() {
        super.onCreate();
        
        // Store application instance
        instance = this;
        
        // Initialize app components
        initializeComponents();
        
        // Set up debugging tools for development builds
        if (BuildConfig.DEBUG) {
            setupDebugTools();
        }
        
        Log.i(TAG, "Application initialized successfully");
    }
    
    /**
     * Get the application instance.
     *
     * @return The application instance
     */
    public static SelfLearningAIApplication getInstance() {
        return instance;
    }
    
    /**
     * Get the application context.
     *
     * @return The application context
     */
    public static Context getAppContext() {
        return instance.getApplicationContext();
    }
    
    /**
     * Initialize application components.
     */
    private void initializeComponents() {
        // Initialize WorkManager manually
        WorkManager.initialize(
                this,
                getWorkManagerConfiguration()
        );
        
        // Initialize other app components here
        // ...
    }
    
    /**
     * Set up debugging tools for development builds.
     */
    private void setupDebugTools() {
        // Enable strict mode for development builds
        StrictMode.setThreadPolicy(new StrictMode.ThreadPolicy.Builder()
                .detectAll()
                .penaltyLog()
                .build());
        
        StrictMode.setVmPolicy(new StrictMode.VmPolicy.Builder()
                .detectLeakedSqlLiteObjects()
                .detectLeakedClosableObjects()
                .penaltyLog()
                .build());
    }
    
    /**
     * Provide the WorkManager configuration.
     *
     * @return The WorkManager configuration
     */
    @Override
    public Configuration getWorkManagerConfiguration() {
        return new Configuration.Builder()
                .setMinimumLoggingLevel(Log.INFO)
                .build();
    }
}