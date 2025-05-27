package utils;

import java.util.HashMap;
import java.util.Map;

/**
 * Helper class for ScheduledTask operations to provide compatibility
 * with different versions of the class.
 */
public class TaskSchedulerHelper {
    
    /**
     * Get task ID from a ScheduledTask
     * 
     * @param task The scheduled task
     * @return The task ID or empty string if null
     */
    public static String getTaskId(ScheduledTask task) {
        if (task == null) {
            return "";
        }
        return task.getTaskId();
    }
    
    /**
     * Set the last execution timestamp for a task
     * 
     * @param task The scheduled task
     * @param timestamp The timestamp in milliseconds
     */
    public static void setLastExecutedAt(ScheduledTask task, long timestamp) {
        if (task != null) {
            task.setLastExecutedAt(timestamp);
        }
    }
    
    /**
     * Get the last execution timestamp for a task
     * 
     * @param task The scheduled task
     * @return The timestamp in milliseconds or 0 if not available
     */
    public static long getLastExecutedAt(ScheduledTask task) {
        if (task == null) {
            return 0L;
        }
        return task.getLastExecutedAt();
    }
    
    /**
     * Get task priority
     * 
     * @param task The scheduled task
     * @return The priority value or 0 if not available
     */
    public static int getPriority(ScheduledTask task) {
        if (task == null) {
            return 0;
        }
        return task.getPriority();
    }
    
    /**
     * Set task priority
     * 
     * @param task The scheduled task
     * @param priority The priority value
     */
    public static void setPriority(ScheduledTask task, int priority) {
        if (task != null) {
            task.setPriority(priority);
        }
    }
    
    /**
     * Check if a task is enabled
     * 
     * @param task The scheduled task
     * @return True if enabled, false otherwise
     */
    public static boolean isEnabled(ScheduledTask task) {
        if (task == null) {
            return false;
        }
        return task.isEnabled();
    }
    
    /**
     * Set task enabled state
     * 
     * @param task The scheduled task
     * @param enabled The enabled state
     */
    public static void setEnabled(ScheduledTask task, boolean enabled) {
        if (task != null) {
            task.setEnabled(enabled);
        }
    }
    
    /**
     * Get task execution interval
     * 
     * @param task The scheduled task
     * @return The interval in milliseconds or 60000 (1 minute) as default
     */
    public static long getIntervalMs(ScheduledTask task) {
        if (task == null) {
            return 60000L; // Default 1 minute
        }
        return task.getIntervalMs();
    }
    
    /**
     * Set task execution interval
     * 
     * @param task The scheduled task
     * @param intervalMs The interval in milliseconds
     */
    public static void setIntervalMs(ScheduledTask task, long intervalMs) {
        if (task != null) {
            task.setIntervalMs(intervalMs);
        }
    }
    
    /**
     * Convert task to a Map representation
     * 
     * @param task The scheduled task
     * @return Map containing task properties
     */
    public static Map<String, Object> toMap(ScheduledTask task) {
        Map<String, Object> map = new HashMap<>();
        if (task == null) {
            return map;
        }
        
        map.put("taskId", task.getTaskId());
        map.put("lastExecutedAt", task.getLastExecutedAt());
        map.put("priority", task.getPriority());
        map.put("enabled", task.isEnabled());
        map.put("intervalMs", task.getIntervalMs());
        
        return map;
    }
}