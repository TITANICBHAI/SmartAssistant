package utils;

/**
 * Enumeration of task statuses
 */
public enum TaskStatus {
    PENDING(1, 0xFF808080, "Pending"),       // Gray
    SCHEDULED(2, 0xFF2196F3, "Scheduled"),   // Blue
    IN_PROGRESS(3, 0xFFFFA000, "In Progress"), // Amber
    COMPLETED(4, 0xFF4CAF50, "Completed"),   // Green
    FAILED(5, 0xFFF44336, "Failed"),         // Red
    CANCELED(6, 0xFF9E9E9E, "Canceled");     // Light Gray
    
    private final int value;
    private final int colorResId;
    private final String displayName;
    
    TaskStatus(int value, int colorResId, String displayName) {
        this.value = value;
        this.colorResId = colorResId;
        this.displayName = displayName;
    }
    
    /**
     * Get the numeric value
     * 
     * @return Numeric status value
     */
    public int getValue() {
        return value;
    }
    
    /**
     * Get the status color resource ID
     * 
     * @return Color resource ID
     */
    public int getStatusColorResId() {
        return colorResId;
    }
    
    /**
     * Get the display name
     * 
     * @return Display name
     */
    public String getDisplayName() {
        return displayName;
    }
    
    /**
     * Get status from numeric value
     * 
     * @param value Numeric value
     * @return Status or PENDING if not found
     */
    public static TaskStatus fromValue(int value) {
        for (TaskStatus status : TaskStatus.values()) {
            if (status.getValue() == value) {
                return status;
            }
        }
        return PENDING;
    }
    
    /**
     * Get status from string name
     * 
     * @param statusString Status string
     * @return Status or PENDING if not found
     */
    public static TaskStatus fromString(String statusString) {
        if (statusString == null || statusString.isEmpty()) {
            return PENDING;
        }
        
        try {
            return TaskStatus.valueOf(statusString.toUpperCase().replace(' ', '_'));
        } catch (IllegalArgumentException e) {
            // Try to match by display name
            String upperStr = statusString.toUpperCase();
            for (TaskStatus status : TaskStatus.values()) {
                if (status.getDisplayName().toUpperCase().equals(upperStr)) {
                    return status;
                }
            }
            
            // Try to match by numeric value
            try {
                int value = Integer.parseInt(statusString);
                return fromValue(value);
            } catch (NumberFormatException nfe) {
                // Not a number
            }
            
            // Default to pending if no match
            return PENDING;
        }
    }
    
    /**
     * Check if status represents an active state
     * 
     * @return True if active
     */
    public boolean isActive() {
        return this == PENDING || this == SCHEDULED || this == IN_PROGRESS;
    }
    
    /**
     * Check if status represents a completed state
     * 
     * @return True if completed
     */
    public boolean isCompleted() {
        return this == COMPLETED;
    }
    
    /**
     * Check if status represents a terminated state
     * 
     * @return True if terminated
     */
    public boolean isTerminated() {
        return this == COMPLETED || this == FAILED || this == CANCELED;
    }
}