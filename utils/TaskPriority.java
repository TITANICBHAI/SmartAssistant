package utils;

/**
 * Enumeration of task priorities
 */
public enum TaskPriority {
    LOW(1, 0xFF9E9E9E, "Low", "#9E9E9E", false),       // Light Gray
    MEDIUM(2, 0xFF2196F3, "Medium", "#2196F3", false),    // Blue 
    HIGH(3, 0xFFF44336, "High", "#F44336", true),      // Red
    CRITICAL(4, 0xFF7B1FA2, "Critical", "#7B1FA2", true);  // Purple
    
    private final int value;
    private final int colorResId;
    private final String displayName;
    private final String hexColor;
    private final boolean highPriority;
    
    TaskPriority(int value, int colorResId, String displayName, String hexColor, boolean highPriority) {
        this.value = value;
        this.colorResId = colorResId;
        this.displayName = displayName;
        this.hexColor = hexColor;
        this.highPriority = highPriority;
    }
    
    /**
     * Get the numeric value
     * 
     * @return Numeric priority value
     */
    public int getValue() {
        return value;
    }
    
    /**
     * Get the priority color resource ID
     * 
     * @return Color resource ID
     */
    public int getPriorityColorResId() {
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
     * Get the hex color code
     * 
     * @return Hex color code
     */
    public String getHexColor() {
        return hexColor;
    }
    
    /**
     * Check if this is a high priority
     * 
     * @return True if high priority
     */
    public boolean isHighPriority() {
        return highPriority;
    }
    
    /**
     * Get the priority level
     * 
     * @return Priority level
     */
    public int getLevel() {
        return value;
    }
    
    /**
     * Compare priorities by level
     * 
     * @param other Other priority
     * @return -1 if less than, 0 if equal, 1 if greater than
     */
    public int compareByLevel(TaskPriority other) {
        if (other == null) {
            return 1;
        }
        return Integer.compare(this.value, other.value);
    }
    
    /**
     * Get priority from numeric value
     * 
     * @param value Numeric value
     * @return Priority or MEDIUM if not found
     */
    public static TaskPriority fromValue(int value) {
        for (TaskPriority priority : TaskPriority.values()) {
            if (priority.getValue() == value) {
                return priority;
            }
        }
        return MEDIUM;
    }
    
    /**
     * Get priority from string name
     * 
     * @param priorityString Priority string
     * @return Priority or MEDIUM if not found
     */
    public static TaskPriority fromString(String priorityString) {
        if (priorityString == null || priorityString.isEmpty()) {
            return MEDIUM;
        }
        
        try {
            return TaskPriority.valueOf(priorityString.toUpperCase());
        } catch (IllegalArgumentException e) {
            // Try to match by display name
            String upperStr = priorityString.toUpperCase();
            for (TaskPriority priority : TaskPriority.values()) {
                if (priority.getDisplayName().toUpperCase().equals(upperStr)) {
                    return priority;
                }
            }
            
            // Try to match by numeric value
            try {
                int value = Integer.parseInt(priorityString);
                return fromValue(value);
            } catch (NumberFormatException nfe) {
                // Not a number
            }
            
            // Default to medium if no match
            return MEDIUM;
        }
    }
}