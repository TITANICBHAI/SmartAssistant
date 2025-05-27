package com.aiassistant.models;

import androidx.annotation.NonNull;

/**
 * Model class representing the state of the AI assistant.
 */
public class AIModel {
    
    private boolean autoMode;
    private boolean copilotMode;
    private boolean inactivityTakeoverEnabled;
    private boolean learningEnabled;
    private int inactivityTimeoutSeconds;
    private String lastAction;
    private String lastSuggestion;
    private String currentFocus;
    private boolean debugMode;
    
    // Default constructor
    public AIModel() {
        this.autoMode = false;
        this.copilotMode = false;
        this.inactivityTakeoverEnabled = true;
        this.learningEnabled = true;
        this.inactivityTimeoutSeconds = 90; // 1.5 min by default
        this.lastAction = "No actions yet";
        this.lastSuggestion = "No suggestions yet";
        this.currentFocus = "None";
        this.debugMode = false;
    }
    
    // Constructor with parameters
    public AIModel(boolean autoMode, boolean copilotMode, boolean inactivityTakeoverEnabled,
                   boolean learningEnabled, int inactivityTimeoutSeconds) {
        this.autoMode = autoMode;
        this.copilotMode = copilotMode;
        this.inactivityTakeoverEnabled = inactivityTakeoverEnabled;
        this.learningEnabled = learningEnabled;
        this.inactivityTimeoutSeconds = inactivityTimeoutSeconds;
        this.lastAction = "No actions yet";
        this.lastSuggestion = "No suggestions yet";
        this.currentFocus = "None";
        this.debugMode = false;
    }
    
    // Getters and setters
    public boolean isAutoMode() {
        return autoMode;
    }
    
    public void setAutoMode(boolean autoMode) {
        this.autoMode = autoMode;
        // Auto mode and copilot mode are mutually exclusive
        if (autoMode) {
            this.copilotMode = false;
        }
    }
    
    public boolean isCopilotMode() {
        return copilotMode;
    }
    
    public void setCopilotMode(boolean copilotMode) {
        this.copilotMode = copilotMode;
        // Auto mode and copilot mode are mutually exclusive
        if (copilotMode) {
            this.autoMode = false;
        }
    }
    
    public boolean isInactivityTakeoverEnabled() {
        return inactivityTakeoverEnabled;
    }
    
    public void setInactivityTakeoverEnabled(boolean inactivityTakeoverEnabled) {
        this.inactivityTakeoverEnabled = inactivityTakeoverEnabled;
    }
    
    public boolean isLearningEnabled() {
        return learningEnabled;
    }
    
    public void setLearningEnabled(boolean learningEnabled) {
        this.learningEnabled = learningEnabled;
    }
    
    public int getInactivityTimeoutSeconds() {
        return inactivityTimeoutSeconds;
    }
    
    public void setInactivityTimeoutSeconds(int inactivityTimeoutSeconds) {
        this.inactivityTimeoutSeconds = inactivityTimeoutSeconds;
    }
    
    public String getLastAction() {
        return lastAction;
    }
    
    public void setLastAction(String lastAction) {
        this.lastAction = lastAction;
    }
    
    public String getLastSuggestion() {
        return lastSuggestion;
    }
    
    public void setLastSuggestion(String lastSuggestion) {
        this.lastSuggestion = lastSuggestion;
    }
    
    public String getCurrentFocus() {
        return currentFocus;
    }
    
    public void setCurrentFocus(String currentFocus) {
        this.currentFocus = currentFocus;
    }
    
    public boolean isDebugMode() {
        return debugMode;
    }
    
    public void setDebugMode(boolean debugMode) {
        this.debugMode = debugMode;
    }
    
    @NonNull
    @Override
    public String toString() {
        return "AIModel{" +
                "autoMode=" + autoMode +
                ", copilotMode=" + copilotMode +
                ", inactivityTakeoverEnabled=" + inactivityTakeoverEnabled +
                ", learningEnabled=" + learningEnabled +
                ", inactivityTimeoutSeconds=" + inactivityTimeoutSeconds +
                ", currentFocus='" + currentFocus + '\'' +
                '}';
    }
}
