package com.aiassistant.models;

import com.aiassistant.R;

/**
 * Enum representing task priority levels
 */
public enum TaskPriority {
    LOW,        // Low priority task
    MEDIUM,     // Medium priority task
    HIGH,       // High priority task
    CRITICAL;   // Critical priority task
    
    /**
     * Get a user-friendly display name for the task priority
     * 
     * @return Display name for the priority
     */
    public String getDisplayName() {
        switch (this) {
            case LOW:
                return "Low";
            case MEDIUM:
                return "Medium";
            case HIGH:
                return "High";
            case CRITICAL:
                return "Critical";
            default:
                return "Unknown";
        }
    }
    
    /**
     * Get the color resource ID associated with this priority
     * 
     * @return Color resource ID
     */
    public int getPriorityColorResId() {
        switch (this) {
            case LOW:
                return R.color.priority_low;
            case MEDIUM:
                return R.color.priority_medium;
            case HIGH:
                return R.color.priority_high;
            case CRITICAL:
                return R.color.priority_critical;
            default:
                return R.color.priority_medium;
        }
    }
}