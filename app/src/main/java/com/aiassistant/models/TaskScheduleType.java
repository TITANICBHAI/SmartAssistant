package com.aiassistant.models;

/**
 * Defines how a task is scheduled to run
 */
public enum TaskScheduleType {
    IMMEDIATE("immediate", "Immediate"),
    ONCE("once", "One Time"),
    DAILY("daily", "Daily"),
    WEEKLY("weekly", "Weekly"),
    MONTHLY("monthly", "Monthly"),
    INTERVAL("interval", "Interval");
    
    private final String id;
    private final String displayName;
    
    TaskScheduleType(String id, String displayName) {
        this.id = id;
        this.displayName = displayName;
    }
    
    public String getId() {
        return id;
    }
    
    public String getDisplayName() {
        return displayName;
    }
    
    public static TaskScheduleType fromString(String text) {
        for (TaskScheduleType type : TaskScheduleType.values()) {
            if (type.id.equalsIgnoreCase(text)) {
                return type;
            }
        }
        return ONCE;
    }
}