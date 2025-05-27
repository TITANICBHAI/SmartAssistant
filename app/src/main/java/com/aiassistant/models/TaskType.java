package com.aiassistant.models;

/**
 * Enumeration of task types supported by the system
 */
public enum TaskType {
    GAME_ANALYSIS("game_analysis", "Game Analysis"),
    UI_AUTOMATION("ui_automation", "UI Automation"),
    ACTION_SEQUENCE("action_sequence", "Action Sequence"),
    DATA_COLLECTION("data_collection", "Data Collection"),
    MODEL_TRAINING("model_training", "Model Training"),
    NOTIFICATION("notification", "Notification"),
    CUSTOM("custom", "Custom Task");
    
    private final String id;
    private final String displayName;
    
    TaskType(String id, String displayName) {
        this.id = id;
        this.displayName = displayName;
    }
    
    public String getId() {
        return id;
    }
    
    public String getDisplayName() {
        return displayName;
    }
    
    /**
     * Get TaskType from string ID
     */
    public static TaskType fromString(String text) {
        for (TaskType type : TaskType.values()) {
            if (type.id.equalsIgnoreCase(text)) {
                return type;
            }
        }
        return CUSTOM;
    }
}