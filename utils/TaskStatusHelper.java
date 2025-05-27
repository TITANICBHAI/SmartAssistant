package utils;

import android.util.Log;
import java.lang.reflect.Method;

/**
 * Helper for TaskStatus
 * This class provides methods for converting between different TaskStatus implementations
 */
public class TaskStatusHelper {
    private static final String TAG = "TaskStatusHelper";
    
    /**
     * Convert from ScheduledTask.TaskStatus to TaskStatus
     * @param status The ScheduledTask.TaskStatus
     * @return The TaskStatus
     */
    public static Object toTaskStatus(Object status) {
        if (status == null) {
            return null;
        }
        
        try {
            // Try to load the TaskStatus class
            Class<?> taskStatusClass = Class.forName("com.aiassistant.models.TaskStatus");
            
            // If the status is already a TaskStatus, return it
            if (taskStatusClass.isInstance(status)) {
                return status;
            }
            
            // Get the string representation of the status
            String statusString = getStatusString(status);
            
            // Try to use valueOf to convert to TaskStatus
            try {
                Method valueOfMethod = taskStatusClass.getMethod("valueOf", String.class);
                return valueOfMethod.invoke(null, statusString);
            } catch (Exception e) {
                // Method doesn't exist or failed to invoke
            }
            
            // Try to find a matching constant
            for (Object constant : taskStatusClass.getEnumConstants()) {
                String constantName = ((Enum<?>) constant).name();
                if (constantName.equals(statusString)) {
                    return constant;
                }
            }
            
            // If all else fails, return SCHEDULED as a default
            try {
                Method valueOfMethod = taskStatusClass.getMethod("valueOf", String.class);
                return valueOfMethod.invoke(null, "SCHEDULED");
            } catch (Exception e) {
                // Method doesn't exist or failed to invoke
            }
            
            Log.e(TAG, "Failed to convert to TaskStatus");
            return null;
            
        } catch (Exception e) {
            Log.e(TAG, "Error converting to TaskStatus: " + e.getMessage());
            return null;
        }
    }
    
    /**
     * Convert from TaskStatus to ScheduledTask.TaskStatus
     * @param status The TaskStatus
     * @return The ScheduledTask.TaskStatus
     */
    public static Object toScheduledTaskStatus(Object status) {
        if (status == null) {
            return null;
        }
        
        try {
            // Try to load the ScheduledTask.TaskStatus class
            Class<?> scheduledTaskClass = Class.forName("com.aiassistant.models.ScheduledTask");
            Class<?> scheduledTaskStatusClass = null;
            
            // Look for the nested TaskStatus class
            for (Class<?> nestedClass : scheduledTaskClass.getDeclaredClasses()) {
                if (nestedClass.getSimpleName().equals("TaskStatus")) {
                    scheduledTaskStatusClass = nestedClass;
                    break;
                }
            }
            
            if (scheduledTaskStatusClass == null) {
                throw new IllegalStateException("Could not find ScheduledTask.TaskStatus class");
            }
            
            // If the status is already a ScheduledTask.TaskStatus, return it
            if (scheduledTaskStatusClass.isInstance(status)) {
                return status;
            }
            
            // Get the string representation of the status
            String statusString = getStatusString(status);
            
            // Try to use valueOf to convert to ScheduledTask.TaskStatus
            try {
                Method valueOfMethod = scheduledTaskStatusClass.getMethod("valueOf", String.class);
                return valueOfMethod.invoke(null, statusString);
            } catch (Exception e) {
                // Method doesn't exist or failed to invoke
            }
            
            // Try to find a matching constant
            for (Object constant : scheduledTaskStatusClass.getEnumConstants()) {
                String constantName = ((Enum<?>) constant).name();
                if (constantName.equals(statusString)) {
                    return constant;
                }
            }
            
            // If all else fails, return SCHEDULED as a default
            try {
                Method valueOfMethod = scheduledTaskStatusClass.getMethod("valueOf", String.class);
                return valueOfMethod.invoke(null, "SCHEDULED");
            } catch (Exception e) {
                // Method doesn't exist or failed to invoke
            }
            
            Log.e(TAG, "Failed to convert to ScheduledTask.TaskStatus");
            return null;
            
        } catch (Exception e) {
            Log.e(TAG, "Error converting to ScheduledTask.TaskStatus: " + e.getMessage());
            return null;
        }
    }
    
    /**
     * Compare two status objects
     * @param status1 The first status
     * @param status2 The second status
     * @return True if the statuses are equal, false otherwise
     */
    public static boolean equals(Object status1, Object status2) {
        if (status1 == null || status2 == null) {
            return status1 == status2;
        }
        
        try {
            // Get the string representations of the statuses
            String status1String = getStatusString(status1);
            String status2String = getStatusString(status2);
            
            // Compare the strings
            return status1String.equals(status2String);
            
        } catch (Exception e) {
            Log.e(TAG, "Error comparing statuses: " + e.getMessage());
            return false;
        }
    }
    
    /**
     * Get the string representation of a status
     * @param status The status
     * @return The string representation
     */
    public static String getStatusString(Object status) {
        if (status == null) {
            return "UNKNOWN";
        }
        
        try {
            // If it's an enum, get the name
            if (status.getClass().isEnum()) {
                return ((Enum<?>) status).name();
            }
            
            // Try to use the name or toString methods
            try {
                Method getNameMethod = status.getClass().getMethod("name");
                Object result = getNameMethod.invoke(status);
                if (result instanceof String) {
                    return (String) result;
                }
            } catch (Exception e) {
                // Method doesn't exist or returned non-String
            }
            
            // Try to use toString
            return status.toString();
            
        } catch (Exception e) {
            Log.e(TAG, "Error getting status string: " + e.getMessage());
            return "UNKNOWN";
        }
    }
}