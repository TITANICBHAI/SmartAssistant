package com.aiassistant.models;

/**
 * Task status enumeration
 * This class is provided as a standalone enum for compatibility with code
 * that uses a separate TaskStatus class instead of ScheduledTask.TaskStatus
 */
public enum TaskStatus {
    PENDING("pending"),
    SCHEDULED("scheduled"),
    RUNNING("running"),
    COMPLETED("completed"),
    FAILED("failed"),
    CANCELLED("cancelled"),
    PAUSED("paused");
    
    private final String value;
    
    TaskStatus(String value) {
        this.value = value;
    }
    
    public String getValue() {
        return value;
    }
    
    /**
     * Get a display name suitable for UI display
     */
    public String getDisplayName() {
        // Capitalize first letter
        if (value.isEmpty()) return "";
        return value.substring(0, 1).toUpperCase() + value.substring(1);
    }
    
    /**
     * Get color resource ID for this status
     */
    public int getStatusColorResId() {
        // This would return a color resource ID
        // For now, return 0 as a placeholder
        return 0;
    }
    
    public static TaskStatus fromString(String text) {
        for (TaskStatus status : TaskStatus.values()) {
            if (status.value.equalsIgnoreCase(text)) {
                return status;
            }
        }
        return PENDING;
    }
    
    /**
     * Convert from ScheduledTask.TaskStatus to TaskStatus
     */
    public static TaskStatus fromScheduledTaskStatus(ScheduledTask.TaskStatus status) {
        if (status == null) return PENDING;
        
        switch (status) {
            case PENDING: return PENDING;
            case SCHEDULED: return SCHEDULED;
            case RUNNING: return RUNNING;
            case COMPLETED: return COMPLETED;
            case FAILED: return FAILED;
            case CANCELLED: return CANCELLED;
            case PAUSED: return PAUSED;
            default: return PENDING;
        }
    }
    
    /**
     * Convert from TaskStatus to ScheduledTask.TaskStatus
     */
    public static ScheduledTask.TaskStatus toScheduledTaskStatus(TaskStatus status) {
        if (status == null) return ScheduledTask.TaskStatus.PENDING;
        
        switch (status) {
            case PENDING: return ScheduledTask.TaskStatus.PENDING;
            case SCHEDULED: return ScheduledTask.TaskStatus.SCHEDULED;
            case RUNNING: return ScheduledTask.TaskStatus.RUNNING;
            case COMPLETED: return ScheduledTask.TaskStatus.COMPLETED;
            case FAILED: return ScheduledTask.TaskStatus.FAILED;
            case CANCELLED: return ScheduledTask.TaskStatus.CANCELLED;
            case PAUSED: return ScheduledTask.TaskStatus.PAUSED;
            default: return ScheduledTask.TaskStatus.PENDING;
        }
    }
}