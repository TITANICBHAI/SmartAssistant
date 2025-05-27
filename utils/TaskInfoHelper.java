package utils;

import java.util.Date;

/**
 * Helper class for TaskInfo providing compatibility methods.
 */
public class TaskInfoHelper {
    
    /**
     * Get the scheduled date for a task
     *
     * @param task The task to get the scheduled date from
     * @return The scheduled date or null if not scheduled
     */
    public static Date getScheduledDate(TaskInfo task) {
        if (task == null) {
            return null;
        }
        
        try {
            // Check if the task has a getScheduledDate method
            if (hasMethod(task, "getScheduledDate")) {
                return (Date) task.getClass().getMethod("getScheduledDate").invoke(task);
            }
            
            // Try alternative methods
            if (hasMethod(task, "getScheduledTime")) {
                return (Date) task.getClass().getMethod("getScheduledTime").invoke(task);
            }
            
            if (hasMethod(task, "getSchedule")) {
                Object schedule = task.getClass().getMethod("getSchedule").invoke(task);
                if (schedule != null && schedule instanceof Date) {
                    return (Date) schedule;
                }
            }
            
            // As a last resort, check for timestamp
            if (hasMethod(task, "getScheduledTimestamp")) {
                Long timestamp = (Long) task.getClass().getMethod("getScheduledTimestamp").invoke(task);
                if (timestamp != null) {
                    return new Date(timestamp);
                }
            }
        } catch (Exception e) {
            // Log the error but don't crash
            System.err.println("Error getting scheduled date from task: " + e.getMessage());
        }
        
        return null;
    }
    
    /**
     * Get the interval in minutes for a recurring task
     *
     * @param task The task to get the interval from
     * @return The interval in minutes or 0 if not recurring
     */
    public static int getIntervalMinutes(TaskInfo task) {
        if (task == null) {
            return 0;
        }
        
        try {
            // Try various method names that might exist
            if (hasMethod(task, "getIntervalMinutes")) {
                return (Integer) task.getClass().getMethod("getIntervalMinutes").invoke(task);
            }
            
            if (hasMethod(task, "getInterval")) {
                Object interval = task.getClass().getMethod("getInterval").invoke(task);
                if (interval instanceof Integer) {
                    return (Integer) interval;
                }
            }
            
            if (hasMethod(task, "getRepeatIntervalMinutes")) {
                return (Integer) task.getClass().getMethod("getRepeatIntervalMinutes").invoke(task);
            }
        } catch (Exception e) {
            // Log the error but don't crash
            System.err.println("Error getting interval minutes from task: " + e.getMessage());
        }
        
        return 0;
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