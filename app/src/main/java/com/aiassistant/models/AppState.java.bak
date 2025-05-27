package com.aiassistant.models;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * App State
 * Represents the state of an application being monitored by the AI assistant
 */
public class AppState {
    // Package name (unique identifier)
    private String packageName;
    
    // App name
    private String appName;
    
    // Whether the app is currently in foreground
    private boolean foreground = false;
    
    // Last time the app was in foreground (timestamp)
    private long lastForegroundTime = 0;
    
    // Total foreground time (milliseconds)
    private long totalForegroundTime = 0;
    
    // UI elements detected in the app
    private List<UIElement> detectedElements = new ArrayList<>();
    
    // Learning progress for this app (0.0-1.0)
    private float learningProgress = 0.0f;
    
    // Known actions for this app
    private List<String> knownActions = new ArrayList<>();
    
    // App-specific settings
    private Map<String, Object> appSettings = new HashMap<>();
    
    /**
     * Create a new AppState
     */
    public AppState(String packageName, String appName) {
        this.packageName = packageName;
        this.appName = appName;
    }
    
    // Getters and setters
    public String getPackageName() {
        return packageName;
    }
    
    public void setPackageName(String packageName) {
        this.packageName = packageName;
    }
    
    public String getAppName() {
        return appName;
    }
    
    public void setAppName(String appName) {
        this.appName = appName;
    }
    
    public boolean isForeground() {
        return foreground;
    }
    
    public void setForeground(boolean foreground) {
        // Update foreground time tracking
        long currentTime = System.currentTimeMillis();
        
        if (this.foreground && !foreground) {
            // App is leaving foreground, update total time
            totalForegroundTime += (currentTime - lastForegroundTime);
        } else if (!this.foreground && foreground) {
            // App is entering foreground, update last time
            lastForegroundTime = currentTime;
        }
        
        this.foreground = foreground;
    }
    
    public long getLastForegroundTime() {
        return lastForegroundTime;
    }
    
    public long getTotalForegroundTime() {
        return totalForegroundTime;
    }
    
    public List<UIElement> getDetectedElements() {
        return detectedElements;
    }
    
    public void setDetectedElements(List<UIElement> detectedElements) {
        this.detectedElements = detectedElements;
    }
    
    public void addDetectedElement(UIElement element) {
        if (element != null && !detectedElements.contains(element)) {
            detectedElements.add(element);
        }
    }
    
    public float getLearningProgress() {
        return learningProgress;
    }
    
    public void setLearningProgress(float learningProgress) {
        this.learningProgress = learningProgress;
    }
    
    public List<String> getKnownActions() {
        return knownActions;
    }
    
    public void setKnownActions(List<String> knownActions) {
        this.knownActions = knownActions;
    }
    
    public void addKnownAction(String action) {
        if (action != null && !knownActions.contains(action)) {
            knownActions.add(action);
        }
    }
    
    public Map<String, Object> getAppSettings() {
        return appSettings;
    }
    
    public void setAppSettings(Map<String, Object> appSettings) {
        this.appSettings = appSettings;
    }
    
    public Object getAppSetting(String key) {
        return appSettings.get(key);
    }
    
    public void setAppSetting(String key, Object value) {
        appSettings.put(key, value);
    }
    
    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        
        AppState appState = (AppState) obj;
        return packageName != null ? packageName.equals(appState.packageName) : appState.packageName == null;
    }
    
    @Override
    public int hashCode() {
        return packageName != null ? packageName.hashCode() : 0;
    }
}