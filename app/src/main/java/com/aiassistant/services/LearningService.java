package com.aiassistant.services;

import android.content.Context;
import android.media.MediaMetadataRetriever;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.aiassistant.database.AppDatabase;
import models.LearnedData;
import utils.AppDetector;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Service responsible for the AI learning capabilities.
 */
public class LearningService {
    
    private static final String TAG = "LearningService";
    
    private static LearningService instance;
    
    private final Context context;
    private final AppDatabase database;
    private final AppDetector appDetector;
    private final ExecutorService executor;
    private final Handler mainHandler;
    
    private final MutableLiveData<Boolean> learningEnabled = new MutableLiveData<>(true);
    private final MutableLiveData<String> learningStatus = new MutableLiveData<>("Ready");
    private final MutableLiveData<Integer> learningProgress = new MutableLiveData<>(0);
    private final MutableLiveData<List<LearnedData>> allLearnedData = new MutableLiveData<>(new ArrayList<>());
    
    private float learningRate = 0.5f; // Default learning rate (0-1)
    private boolean collectUserData = true;
    
    private LearningService(Context context) {
        this.context = context.getApplicationContext();
        this.database = AppDatabase.getInstance(context);
        this.appDetector = new AppDetector(context);
        this.executor = Executors.newCachedThreadPool();
        this.mainHandler = new Handler(Looper.getMainLooper());
        
        // Load initial data
        loadAllLearnedData();
    }
    
    public static synchronized LearningService getInstance(Context context) {
        if (instance == null) {
            instance = new LearningService(context);
        }
        return instance;
    }
    
    /**
     * Load all learned data from the database
     */
    private void loadAllLearnedData() {
        executor.execute(() -> {
            List<LearnedData> learnedDataList = database.learnedDataDao().getAllLearnedData();
            allLearnedData.postValue(learnedDataList);
        });
    }
    
    /**
     * Learn from video content.
     * This analyzes a video to extract patterns of app usage.
     */
    public void learnFromVideo(String videoPath) {
        if (videoPath == null || videoPath.isEmpty()) {
            learningStatus.postValue("Error: Invalid video path");
            return;
        }
        
        learningStatus.postValue("Analyzing video...");
        learningProgress.postValue(10);
        
        executor.execute(() -> {
            try {
                // Analyze video frames and identify apps shown
                MediaMetadataRetriever retriever = new MediaMetadataRetriever();
                retriever.setDataSource(context, Uri.parse(videoPath));
                
                // Get video duration
                String durationStr = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION);
                long duration = Long.parseLong(durationStr);
                
                // Sample frames at regular intervals
                int numSamples = 20;
                long interval = duration / numSamples;
                
                List<String> detectedPackages = new ArrayList<>();
                
                for (int i = 0; i < numSamples; i++) {
                    // Update progress
                    int progress = 10 + (i * 80 / numSamples);
                    mainHandler.post(() -> learningProgress.postValue(progress));
                    
                    // In a real implementation, this would use ML to identify the app
                    // shown in each frame. For demo, we'll simulate detection.
                    simulateAppDetection(i, detectedPackages);
                    
                    Thread.sleep(100); // Simulate processing time
                }
                
                // Process detected apps
                String primaryApp = determinePrimaryApp(detectedPackages);
                
                if (primaryApp != null) {
                    String appName = appDetector.getAppNameFromPackage(primaryApp);
                    
                    // Create learned data entry
                    LearnedData learnedData = new LearnedData(
                            appName,
                            primaryApp,
                            "video_analysis",
                            "App usage pattern from video",
                            "tap,swipe,scroll", // Simulated action sequence
                            0.85f // Confidence
                    );
                    
                    // Save to database
                    database.learnedDataDao().insertLearnedData(learnedData);
                    
                    // Reload learned data
                    loadAllLearnedData();
                    
                    mainHandler.post(() -> {
                        learningStatus.postValue("Learned from video: " + appName);
                        learningProgress.postValue(100);
                        
                        // Reset progress after a delay
                        mainHandler.postDelayed(() -> learningProgress.postValue(0), 3000);
                    });
                } else {
                    mainHandler.post(() -> {
                        learningStatus.postValue("Could not identify app in video");
                        learningProgress.postValue(0);
                    });
                }
                
                retriever.release();
                
            } catch (Exception e) {
                Log.e(TAG, "Error learning from video", e);
                mainHandler.post(() -> {
                    learningStatus.postValue("Error analyzing video: " + e.getMessage());
                    learningProgress.postValue(0);
                });
            }
        });
    }
    
    /**
     * Simulate detection of apps in video frames.
     * In a real implementation, this would use computer vision / ML.
     */
    private void simulateAppDetection(int frameIndex, List<String> detectedPackages) {
        // Simulate detecting different apps in the video
        String[] commonPackages = {
                "com.android.settings",
                "com.google.android.youtube",
                "com.google.android.gm",
                "com.whatsapp",
                "com.instagram.android"
        };
        
        // Simulate that a specific app is shown more frequently
        int primaryAppIndex = frameIndex % commonPackages.length;
        String detectedPackage = commonPackages[primaryAppIndex];
        
        detectedPackages.add(detectedPackage);
    }
    
    /**
     * Determine the primary app shown in the video
     */
    private String determinePrimaryApp(List<String> detectedPackages) {
        if (detectedPackages.isEmpty()) {
            return null;
        }
        
        // Count occurrences of each package
        java.util.Map<String, Integer> packageCounts = new java.util.HashMap<>();
        
        for (String pkg : detectedPackages) {
            packageCounts.put(pkg, packageCounts.getOrDefault(pkg, 0) + 1);
        }
        
        // Find the most frequent package
        String primaryPackage = null;
        int maxCount = 0;
        
        for (java.util.Map.Entry<String, Integer> entry : packageCounts.entrySet()) {
            if (entry.getValue() > maxCount) {
                maxCount = entry.getValue();
                primaryPackage = entry.getKey();
            }
        }
        
        return primaryPackage;
    }
    
    /**
     * Learn from a pattern of user interactions
     */
    public void learnFromInteractions(String packageName, List<String> interactions) {
        if (!collectUserData || !learningEnabled.getValue()) {
            return;
        }
        
        if (packageName == null || interactions.isEmpty()) {
            return;
        }
        
        executor.execute(() -> {
            try {
                String appName = appDetector.getAppNameFromPackage(packageName);
                
                // Analyze interaction pattern
                String patternType = determinePatternType(interactions);
                String patternDesc = generatePatternDescription(interactions);
                
                // Create action sequence from interactions
                StringBuilder actionSequence = new StringBuilder();
                for (String interaction : interactions) {
                    actionSequence.append(interaction).append(",");
                }
                
                // Calculate confidence based on number of interactions and learning rate
                float confidence = Math.min(0.5f + (interactions.size() * 0.05f * learningRate), 0.95f);
                
                // Create learned data entry
                LearnedData learnedData = new LearnedData(
                        appName,
                        packageName,
                        patternType,
                        patternDesc,
                        actionSequence.toString(),
                        confidence
                );
                
                // Save to database
                database.learnedDataDao().insertLearnedData(learnedData);
                
                // Reload learned data
                loadAllLearnedData();
                
            } catch (Exception e) {
                Log.e(TAG, "Error learning from interactions", e);
            }
        });
    }
    
    /**
     * Determine the type of interaction pattern
     */
    private String determinePatternType(List<String> interactions) {
        // Simple pattern type determination
        if (interactions.size() > 5) {
            return "complex_sequence";
        } else if (interactions.stream().anyMatch(i -> i.contains("scroll"))) {
            return "navigation";
        } else if (interactions.stream().anyMatch(i -> i.contains("text"))) {
            return "data_entry";
        } else {
            return "simple_interaction";
        }
    }
    
    /**
     * Generate a human-readable description of the pattern
     */
    private String generatePatternDescription(List<String> interactions) {
        // Generate a simple description based on the interactions
        int clicks = 0;
        int scrolls = 0;
        int textInputs = 0;
        
        for (String interaction : interactions) {
            if (interaction.contains("click")) clicks++;
            if (interaction.contains("scroll")) scrolls++;
            if (interaction.contains("text")) textInputs++;
        }
        
        StringBuilder description = new StringBuilder();
        
        if (clicks > 0) {
            description.append(clicks).append(" taps");
        }
        
        if (scrolls > 0) {
            if (description.length() > 0) description.append(", ");
            description.append(scrolls).append(" scrolls");
        }
        
        if (textInputs > 0) {
            if (description.length() > 0) description.append(", ");
            description.append(textInputs).append(" text inputs");
        }
        
        if (description.length() > 0) {
            description.append(" pattern");
        } else {
            description.append("Unknown pattern");
        }
        
        return description.toString();
    }
    
    /**
     * Clear all learned data
     */
    public void clearAllLearnedData() {
        executor.execute(() -> {
            database.learnedDataDao().deleteAllLearnedData();
            loadAllLearnedData();
        });
    }
    
    // Getters and setters
    
    public LiveData<Boolean> getLearningEnabled() {
        return learningEnabled;
    }
    
    public void setLearningEnabled(boolean enabled) {
        learningEnabled.postValue(enabled);
    }
    
    public LiveData<String> getLearningStatus() {
        return learningStatus;
    }
    
    public LiveData<Integer> getLearningProgress() {
        return learningProgress;
    }
    
    public LiveData<List<LearnedData>> getAllLearnedData() {
        return allLearnedData;
    }
    
    public void setLearningRate(float rate) {
        this.learningRate = Math.max(0.1f, Math.min(1.0f, rate));
    }
    
    public float getLearningRate() {
        return learningRate;
    }
    
    public void setCollectUserData(boolean collect) {
        this.collectUserData = collect;
    }
    
    public boolean isCollectingUserData() {
        return collectUserData;
    }
}
