package com.aiassistant.models;

import java.util.ArrayList;
import java.util.List;

/**
 * AI State
 * Represents the current state of the AI assistant
 */
public class AIState {
    // AI mode
    private AIMode aiMode = AIMode.PASSIVE;
    
    // Service state
    private boolean serviceEnabled = false;
    private boolean accessibilityEnabled = false;
    private boolean deviceAdminEnabled = false;
    
    // Learning state
    private boolean learningActive = false;
    private LearningSource currentLearningSource = null;
    private float learningProgress = 0.0f;
    
    // Permissions
    private boolean overlayPermissionEnabled = false;
    private boolean usageStatsPermissionEnabled = false;
    private boolean storagePermissionEnabled = false;
    
    // App state
    private List<AppState> activeApps = new ArrayList<>();
    
    // First launch
    private boolean firstLaunchCompleted = false;
    
    // Inactivity threshold in milliseconds (default: 2 minutes)
    private int inactivityThresholdMs = 2 * 60 * 1000;
    
    // Performance settings
    private PerformanceMode performanceMode = PerformanceMode.BALANCED;
    private boolean lowMemoryMode = false;
    
    // Algorithm settings
    private String rlAlgorithm = "Q_LEARNING";
    private boolean autoSelectAlgorithm = true;
    
    // Video learning
    private boolean videoLearningEnabled = true;
    
    // Getters and setters
    public AIMode getAiMode() {
        return aiMode;
    }
    
    public void setAiMode(AIMode aiMode) {
        this.aiMode = aiMode;
    }
    
    public boolean isServiceEnabled() {
        return serviceEnabled;
    }
    
    public void setServiceEnabled(boolean serviceEnabled) {
        this.serviceEnabled = serviceEnabled;
    }
    
    public boolean isAccessibilityEnabled() {
        return accessibilityEnabled;
    }
    
    public void setAccessibilityEnabled(boolean accessibilityEnabled) {
        this.accessibilityEnabled = accessibilityEnabled;
    }
    
    public boolean isDeviceAdminEnabled() {
        return deviceAdminEnabled;
    }
    
    public void setDeviceAdminEnabled(boolean deviceAdminEnabled) {
        this.deviceAdminEnabled = deviceAdminEnabled;
    }
    
    public boolean isLearningActive() {
        return learningActive;
    }
    
    public void setLearningActive(boolean learningActive) {
        this.learningActive = learningActive;
    }
    
    public LearningSource getCurrentLearningSource() {
        return currentLearningSource;
    }
    
    public void setCurrentLearningSource(LearningSource currentLearningSource) {
        this.currentLearningSource = currentLearningSource;
    }
    
    public float getLearningProgress() {
        return learningProgress;
    }
    
    public void setLearningProgress(float learningProgress) {
        this.learningProgress = learningProgress;
    }
    
    public boolean isOverlayPermissionEnabled() {
        return overlayPermissionEnabled;
    }
    
    public void setOverlayPermissionEnabled(boolean overlayPermissionEnabled) {
        this.overlayPermissionEnabled = overlayPermissionEnabled;
    }
    
    public boolean isUsageStatsPermissionEnabled() {
        return usageStatsPermissionEnabled;
    }
    
    public void setUsageStatsPermissionEnabled(boolean usageStatsPermissionEnabled) {
        this.usageStatsPermissionEnabled = usageStatsPermissionEnabled;
    }
    
    public boolean isStoragePermissionEnabled() {
        return storagePermissionEnabled;
    }
    
    public void setStoragePermissionEnabled(boolean storagePermissionEnabled) {
        this.storagePermissionEnabled = storagePermissionEnabled;
    }
    
    public List<AppState> getActiveApps() {
        return activeApps;
    }
    
    public void setActiveApps(List<AppState> activeApps) {
        this.activeApps = activeApps;
    }
    
    public boolean isFirstLaunchCompleted() {
        return firstLaunchCompleted;
    }
    
    public void setFirstLaunchCompleted(boolean firstLaunchCompleted) {
        this.firstLaunchCompleted = firstLaunchCompleted;
    }
    
    public int getInactivityThresholdMs() {
        return inactivityThresholdMs;
    }
    
    public void setInactivityThresholdMs(int inactivityThresholdMs) {
        this.inactivityThresholdMs = inactivityThresholdMs;
    }
    
    public PerformanceMode getPerformanceMode() {
        return performanceMode;
    }
    
    public void setPerformanceMode(PerformanceMode performanceMode) {
        this.performanceMode = performanceMode;
    }
    
    public boolean isLowMemoryMode() {
        return lowMemoryMode;
    }
    
    public void setLowMemoryMode(boolean lowMemoryMode) {
        this.lowMemoryMode = lowMemoryMode;
    }
    
    public String getRlAlgorithm() {
        return rlAlgorithm;
    }
    
    public void setRlAlgorithm(String rlAlgorithm) {
        this.rlAlgorithm = rlAlgorithm;
    }
    
    public boolean isAutoSelectAlgorithm() {
        return autoSelectAlgorithm;
    }
    
    public void setAutoSelectAlgorithm(boolean autoSelectAlgorithm) {
        this.autoSelectAlgorithm = autoSelectAlgorithm;
    }
    
    public boolean isVideoLearningEnabled() {
        return videoLearningEnabled;
    }
    
    public void setVideoLearningEnabled(boolean videoLearningEnabled) {
        this.videoLearningEnabled = videoLearningEnabled;
    }
}