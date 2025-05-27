package utils;

/**
 * Helper class for TaskTriggerType providing compatibility methods.
 */
public class TaskTriggerTypeHelper {
    // Define common trigger types as constants
    public static final int MANUAL = 0;
    public static final int SCHEDULED = 1;
    public static final int RECURRING = 2;
    public static final int EVENT_BASED = 3;
    public static final int CONDITION_BASED = 4;
    
    /**
     * Get the display name for a trigger type
     *
     * @param triggerType The TaskTriggerType to get display name for
     * @return String display name
     */
    public static String getDisplayName(TaskTriggerType triggerType) {
        if (triggerType == null) {
            return "Manual";
        }
        
        try {
            // Check if the triggerType has a getDisplayName method
            if (hasMethod(triggerType, "getDisplayName")) {
                return (String) triggerType.getClass().getMethod("getDisplayName").invoke(triggerType);
            }
            
            // Try alternative methods
            if (hasMethod(triggerType, "getName")) {
                return (String) triggerType.getClass().getMethod("getName").invoke(triggerType);
            }
            
            if (hasMethod(triggerType, "toString")) {
                return triggerType.toString();
            }
            
            // Fallback to enum name if it's an enum
            if (triggerType.getClass().isEnum()) {
                return formatEnumName(((Enum<?>) triggerType).name());
            }
        } catch (Exception e) {
            // Log the error but don't crash
            System.err.println("Error getting display name from trigger type: " + e.getMessage());
        }
        
        // Last resort fallback based on common trigger type values
        String triggerStr = triggerType.toString().toUpperCase();
        if (triggerStr.contains("MANUAL")) {
            return "Manual";
        } else if (triggerStr.contains("SCHEDULED")) {
            return "Scheduled";
        } else if (triggerStr.contains("RECURRING") || triggerStr.contains("REPEAT")) {
            return "Recurring";
        } else if (triggerStr.contains("EVENT")) {
            return "Event-based";
        } else if (triggerStr.contains("CONDITION")) {
            return "Condition-based";
        } else {
            return triggerType.toString();
        }
    }
    
    /**
     * Get the color resource ID for a trigger type
     *
     * @param triggerType The TaskTriggerType to get color for
     * @return Integer resource ID for the color
     */
    public static int getTriggerTypeColorResId(TaskTriggerType triggerType) {
        if (triggerType == null) {
            return R.color.default_trigger_color; // Assuming a default color exists
        }
        
        try {
            // Check if the triggerType has a getColorResId method
            if (hasMethod(triggerType, "getColorResId")) {
                return (Integer) triggerType.getClass().getMethod("getColorResId").invoke(triggerType);
            }
            
            if (hasMethod(triggerType, "getColor")) {
                return (Integer) triggerType.getClass().getMethod("getColor").invoke(triggerType);
            }
        } catch (Exception e) {
            // Log the error but don't crash
            System.err.println("Error getting color resource ID from trigger type: " + e.getMessage());
        }
        
        // Fallback to predefined colors based on common trigger type values
        String triggerStr = triggerType.toString().toUpperCase();
        if (triggerStr.contains("MANUAL")) {
            return R.color.trigger_manual;
        } else if (triggerStr.contains("SCHEDULED")) {
            return R.color.trigger_scheduled;
        } else if (triggerStr.contains("RECURRING") || triggerStr.contains("REPEAT")) {
            return R.color.trigger_recurring;
        } else if (triggerStr.contains("EVENT")) {
            return R.color.trigger_event;
        } else if (triggerStr.contains("CONDITION")) {
            return R.color.trigger_condition;
        }
        
        // Ultimate fallback
        return R.color.default_trigger_color;
    }
    
    /**
     * Get a TaskTriggerType constant from an integer value
     *
     * @param value Integer value
     * @return TaskTriggerType constant
     */
    public static TaskTriggerType fromInt(int value) {
        switch (value) {
            case MANUAL:
                return TaskTriggerType.MANUAL;
            case SCHEDULED:
                return TaskTriggerType.SCHEDULED;
            case RECURRING:
                return TaskTriggerType.RECURRING;
            case EVENT_BASED:
                return TaskTriggerType.EVENT;
            case CONDITION_BASED:
                return TaskTriggerType.CONDITION;
            default:
                return TaskTriggerType.MANUAL;
        }
    }
    
    /**
     * Format an enum name to make it more readable
     *
     * @param enumName The enum name
     * @return Formatted name
     */
    private static String formatEnumName(String enumName) {
        if (enumName == null || enumName.isEmpty()) {
            return "";
        }
        
        // Replace underscores with spaces and convert to title case
        String[] words = enumName.split("_");
        StringBuilder result = new StringBuilder();
        
        for (String word : words) {
            if (word.length() > 0) {
                result.append(Character.toUpperCase(word.charAt(0)));
                if (word.length() > 1) {
                    result.append(word.substring(1).toLowerCase());
                }
                result.append(" ");
            }
        }
        
        // Remove trailing space and return
        return result.toString().trim();
    }
    
    /**
     * Check if a method exists on the given object
     *
     * @param obj The object to check
     * @param methodName The name of the method to look for
     * @return True if the method exists, false otherwise
     */
    private static boolean hasMethod(Object obj, String methodName) {
        if (obj == null) {
            return false;
        }
        
        try {
            obj.getClass().getMethod(methodName);
            return true;
        } catch (NoSuchMethodException e) {
            return false;
        }
    }
}