package utils;

import android.util.Log;
import utils.AndroidColorConstants;

/**
 * Helper for TaskPriority enum
 * This class provides methods for working with TaskPriority enums
 */
public class TaskPriorityHelper {
    private static final String TAG = "TaskPriorityHelper";
    
    /**
     * Create a TaskPriority enum value
     * @param name The name of the priority
     * @return The TaskPriority value
     */
    public static Object createTaskPriority(String name) {
        try {
            // Try to get the enum class
            Class<?> taskPriorityClass = Class.forName("com.aiassistant.models.TaskPriority");
            
            // Try to use valueOf method
            try {
                java.lang.reflect.Method valueOfMethod = taskPriorityClass.getMethod("valueOf", String.class);
                return valueOfMethod.invoke(null, name.toUpperCase());
            } catch (Exception e) {
                // Method doesn't exist or failed, try to get the enum constants
                Object[] constants = taskPriorityClass.getEnumConstants();
                if (constants != null) {
                    for (Object constant : constants) {
                        if (constant.toString().equalsIgnoreCase(name)) {
                            return constant;
                        }
                    }
                }
            }
            
            // Return a default value if available
            Object[] constants = taskPriorityClass.getEnumConstants();
            if (constants != null && constants.length > 0) {
                return constants[0];
            }
            
            Log.e(TAG, "Could not create TaskPriority for name: " + name);
            return null;
            
        } catch (Exception e) {
            Log.e(TAG, "Error creating TaskPriority: " + e.getMessage());
            return null;
        }
    }
    
    /**
     * Get the integer value of a TaskPriority
     * @param priority The TaskPriority
     * @return The integer value
     */
    public static int getValue(Object priority) {
        if (priority == null) {
            return 0;
        }
        
        try {
            // Try to use getValue method
            try {
                java.lang.reflect.Method getValueMethod = priority.getClass().getMethod("getValue");
                return (int) getValueMethod.invoke(priority);
            } catch (Exception e) {
                // Method doesn't exist, try to get ordinal
                java.lang.reflect.Method ordinalMethod = priority.getClass().getMethod("ordinal");
                return (int) ordinalMethod.invoke(priority);
            }
            
        } catch (Exception e) {
            Log.e(TAG, "Error getting TaskPriority value: " + e.getMessage());
            return 0;
        }
    }
    
    /**
     * Get the display name of a TaskPriority
     * @param priority The TaskPriority
     * @return The display name
     */
    public static String getDisplayName(Object priority) {
        if (priority == null) {
            return "";
        }
        
        try {
            // Try to use getDisplayName method
            try {
                java.lang.reflect.Method getDisplayNameMethod = priority.getClass().getMethod("getDisplayName");
                return (String) getDisplayNameMethod.invoke(priority);
            } catch (Exception e) {
                // Method doesn't exist, use toString and format it
                String name = priority.toString();
                
                // Capitalize first letter, lowercase the rest
                if (name.length() > 0) {
                    name = name.substring(0, 1).toUpperCase() + name.substring(1).toLowerCase();
                }
                
                return name;
            }
            
        } catch (Exception e) {
            Log.e(TAG, "Error getting TaskPriority display name: " + e.getMessage());
            return priority.toString();
        }
    }
    
    /**
     * Get the color resource ID for a TaskPriority
     * @param priority The TaskPriority
     * @return The color resource ID
     */
    public static int getColorResId(Object priority) {
        if (priority == null) {
            return AndroidColorConstants.darker_gray;
        }
        
        try {
            // Try to use getColorResId method
            try {
                java.lang.reflect.Method getColorResIdMethod = priority.getClass().getMethod("getColorResId");
                return (int) getColorResIdMethod.invoke(priority);
            } catch (Exception e) {
                // Method doesn't exist, determine color based on name
                String name = priority.toString();
                
                if (name.equalsIgnoreCase("HIGH") || name.equalsIgnoreCase("CRITICAL") || name.equalsIgnoreCase("URGENT")) {
                    return AndroidColorConstants.holo_red_dark;
                } else if (name.equalsIgnoreCase("MEDIUM") || name.equalsIgnoreCase("NORMAL")) {
                    return AndroidColorConstants.holo_orange_light;
                } else if (name.equalsIgnoreCase("LOW")) {
                    return AndroidColorConstants.holo_green_light;
                } else {
                    return AndroidColorConstants.darker_gray;
                }
            }
            
        } catch (Exception e) {
            Log.e(TAG, "Error getting TaskPriority color resource ID: " + e.getMessage());
            return AndroidColorConstants.darker_gray;
        }
    }
    
    /**
     * Compare two TaskPriority objects
     * @param priority1 The first TaskPriority
     * @param priority2 The second TaskPriority
     * @return negative if priority1 < priority2, 0 if equal, positive if priority1 > priority2
     */
    public static int compare(Object priority1, Object priority2) {
        if (priority1 == null && priority2 == null) {
            return 0;
        }
        
        if (priority1 == null) {
            return -1;
        }
        
        if (priority2 == null) {
            return 1;
        }
        
        try {
            // If they're from the same enum class, use compareTo
            if (priority1.getClass() == priority2.getClass()) {
                java.lang.reflect.Method compareToMethod = priority1.getClass().getMethod("compareTo", priority1.getClass());
                return (int) compareToMethod.invoke(priority1, priority2);
            }
            
            // Otherwise, compare by value
            int value1 = getValue(priority1);
            int value2 = getValue(priority2);
            
            return Integer.compare(value1, value2);
            
        } catch (Exception e) {
            Log.e(TAG, "Error comparing TaskPriority: " + e.getMessage());
            
            // Compare by string representation as a fallback
            return priority1.toString().compareTo(priority2.toString());
        }
    }
}