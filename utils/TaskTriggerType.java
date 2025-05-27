package utils;

/**
 * Enumeration of task trigger types
 */
public enum TaskTriggerType {
    MANUAL(1),       // Manually triggered by user
    SCHEDULED(2),    // Triggered at a specific time
    EVENT(3),        // Triggered by an event
    RECURRING(4),    // Recurring task
    AUTOMATIC(5),    // Automatically triggered
    CONDITION(6);    // Triggered when a condition is met
    
    private final int value;
    
    TaskTriggerType(int value) {
        this.value = value;
    }
    
    /**
     * Get the numeric value
     * 
     * @return Numeric trigger type value
     */
    public int getValue() {
        return value;
    }
    
    /**
     * Get trigger type from numeric value
     * 
     * @param value Numeric value
     * @return Trigger type or MANUAL if not found
     */
    public static TaskTriggerType fromValue(int value) {
        for (TaskTriggerType triggerType : TaskTriggerType.values()) {
            if (triggerType.getValue() == value) {
                return triggerType;
            }
        }
        return MANUAL;
    }
    
    /**
     * Get trigger type from string name
     * 
     * @param triggerString Trigger type string
     * @return Trigger type or MANUAL if not found
     */
    public static TaskTriggerType fromString(String triggerString) {
        if (triggerString == null || triggerString.isEmpty()) {
            return MANUAL;
        }
        
        try {
            return TaskTriggerType.valueOf(triggerString.toUpperCase());
        } catch (IllegalArgumentException e) {
            // Try to match by numeric value
            try {
                int value = Integer.parseInt(triggerString);
                return fromValue(value);
            } catch (NumberFormatException nfe) {
                // Not a number
            }
            
            // Default to manual if no match
            return MANUAL;
        }
    }
    
    /**
     * Check if this trigger type requires scheduling
     * 
     * @return True if scheduling is required
     */
    public boolean requiresScheduling() {
        return this == SCHEDULED || this == RECURRING;
    }
    
    /**
     * Check if this trigger type is condition-based
     * 
     * @return True if condition-based
     */
    public boolean isConditionBased() {
        return this == CONDITION || this == EVENT;
    }
}