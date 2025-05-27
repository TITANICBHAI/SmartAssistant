package com.aiassistant.models;

/**
 * Defines when and how a task is triggered
 */
public enum TaskTriggerType {
    MANUAL("manual", "Manual"),
    SCHEDULED("scheduled", "Scheduled"),
    RECURRING("recurring", "Recurring"),
    EVENT_BASED("event", "Event Based"),
    CONDITION_BASED("condition", "Condition Based");
    
    private final String id;
    private final String displayName;
    
    TaskTriggerType(String id, String displayName) {
        this.id = id;
        this.displayName = displayName;
    }
    
    public String getId() {
        return id;
    }
    
    public String getDisplayName() {
        return displayName;
    }
    
    public static TaskTriggerType fromString(String text) {
        for (TaskTriggerType type : TaskTriggerType.values()) {
            if (type.id.equalsIgnoreCase(text)) {
                return type;
            }
        }
        return MANUAL;
    }
}