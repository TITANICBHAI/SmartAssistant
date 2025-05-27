package utils;

import android.util.Log;
import java.lang.reflect.Method;

/**
 * Helper for ScheduledTask
 * This class provides methods for working with ScheduledTask objects
 */
public class ScheduledTaskHelper {
    private static final String TAG = "ScheduledTaskHelper";
    
    /**
     * Create a new ScheduledTask
     * @param name The task name
     * @param description The task description
     * @return The new ScheduledTask, or null if an error occurred
     */
    public static Object createScheduledTask(String name, String description) {
        try {
            // Try to load the ScheduledTask class
            Class<?> scheduledTaskClass = Class.forName("com.aiassistant.models.ScheduledTask");
            
            // Try to create a new instance with the constructor that takes two arguments
            try {
                java.lang.reflect.Constructor<?> constructor = scheduledTaskClass.getConstructor(String.class, String.class);
                return constructor.newInstance(name, description);
            } catch (Exception e) {
                // Constructor doesn't exist or failed to invoke
            }
            
            // Try to load the TaskType and TaskPriority classes
            Class<?> taskTypeClass = null;
            Class<?> taskPriorityClass = null;
            
            try {
                taskTypeClass = Class.forName("com.aiassistant.models.TaskType");
                taskPriorityClass = Class.forName("com.aiassistant.models.TaskPriority");
            } catch (Exception e) {
                // Classes don't exist
            }
            
            // If we couldn't load them, try to see if they're nested classes
            if (taskTypeClass == null) {
                for (Class<?> nestedClass : scheduledTaskClass.getDeclaredClasses()) {
                    if (nestedClass.getSimpleName().equals("TaskType")) {
                        taskTypeClass = nestedClass;
                    } else if (nestedClass.getSimpleName().equals("TaskPriority")) {
                        taskPriorityClass = nestedClass;
                    }
                }
            }
            
            // If we found the TaskType and TaskPriority classes, try to use them
            if (taskTypeClass != null && taskPriorityClass != null) {
                // Try to get the default values
                Object defaultType = null;
                Object defaultPriority = null;
                
                // Try to get the Enum constants
                if (taskTypeClass.isEnum() && taskPriorityClass.isEnum()) {
                    Object[] taskTypes = taskTypeClass.getEnumConstants();
                    Object[] taskPriorities = taskPriorityClass.getEnumConstants();
                    
                    if (taskTypes != null && taskTypes.length > 0) {
                        defaultType = taskTypes[0];
                    }
                    
                    if (taskPriorities != null && taskPriorities.length > 0) {
                        defaultPriority = taskPriorities[0];
                    }
                }
                
                // Try to create a new instance with the constructor that takes four arguments
                if (defaultType != null && defaultPriority != null) {
                    try {
                        java.lang.reflect.Constructor<?> constructor = scheduledTaskClass.getConstructor(
                            String.class, String.class, taskTypeClass, taskPriorityClass);
                        return constructor.newInstance(name, description, defaultType, defaultPriority);
                    } catch (Exception e) {
                        // Constructor doesn't exist or failed to invoke
                    }
                }
            }
            
            // Try to create a new instance with the default constructor and then set the properties
            try {
                java.lang.reflect.Constructor<?> constructor = scheduledTaskClass.getConstructor();
                Object task = constructor.newInstance();
                
                // Set the name
                Method setNameMethod = scheduledTaskClass.getMethod("setName", String.class);
                setNameMethod.invoke(task, name);
                
                // Set the description
                Method setDescriptionMethod = scheduledTaskClass.getMethod("setDescription", String.class);
                setDescriptionMethod.invoke(task, description);
                
                return task;
            } catch (Exception e) {
                // Constructor or methods don't exist or failed to invoke
            }
            
            Log.e(TAG, "Failed to create ScheduledTask");
            return null;
            
        } catch (Exception e) {
            Log.e(TAG, "Error creating ScheduledTask: " + e.getMessage());
            return null;
        }
    }
    
    /**
     * Create a new ScheduledTask with the specified task type and priority
     * @param name The task name
     * @param description The task description
     * @param taskType The task type (should be a TaskType enum value)
     * @param taskPriority The task priority (should be a TaskPriority enum value)
     * @return The new ScheduledTask, or null if an error occurred
     */
    public static Object createScheduledTask(String name, String description, Object taskType, Object taskPriority) {
        try {
            // Try to load the ScheduledTask class
            Class<?> scheduledTaskClass = Class.forName("com.aiassistant.models.ScheduledTask");
            
            // Try to create a new instance with the constructor that takes four arguments
            try {
                java.lang.reflect.Constructor<?> constructor = scheduledTaskClass.getConstructor(
                    String.class, String.class, taskType.getClass(), taskPriority.getClass());
                return constructor.newInstance(name, description, taskType, taskPriority);
            } catch (Exception e) {
                // Constructor doesn't exist or failed to invoke
            }
            
            // Try to create a new instance with the default constructor and then set the properties
            try {
                java.lang.reflect.Constructor<?> constructor = scheduledTaskClass.getConstructor();
                Object task = constructor.newInstance();
                
                // Set the name
                Method setNameMethod = scheduledTaskClass.getMethod("setName", String.class);
                setNameMethod.invoke(task, name);
                
                // Set the description
                Method setDescriptionMethod = scheduledTaskClass.getMethod("setDescription", String.class);
                setDescriptionMethod.invoke(task, description);
                
                // Set the task type
                Method setTaskTypeMethod = scheduledTaskClass.getMethod("setTaskType", taskType.getClass());
                setTaskTypeMethod.invoke(task, taskType);
                
                // Set the task priority
                Method setTaskPriorityMethod = scheduledTaskClass.getMethod("setTaskPriority", taskPriority.getClass());
                setTaskPriorityMethod.invoke(task, taskPriority);
                
                return task;
            } catch (Exception e) {
                // Constructor or methods don't exist or failed to invoke
            }
            
            Log.e(TAG, "Failed to create ScheduledTask with type and priority");
            return null;
            
        } catch (Exception e) {
            Log.e(TAG, "Error creating ScheduledTask with type and priority: " + e.getMessage());
            return null;
        }
    }
    
    /**
     * Get the task type of a ScheduledTask
     * @param task The ScheduledTask
     * @return The task type, or null if an error occurred
     */
    public static Object getTaskType(Object task) {
        if (task == null) {
            return null;
        }
        
        try {
            // Try to use the getTaskType method
            try {
                Method getTaskTypeMethod = task.getClass().getMethod("getTaskType");
                return getTaskTypeMethod.invoke(task);
            } catch (Exception e) {
                // Method doesn't exist or failed to invoke
            }
            
            // Try to access the taskType field directly
            try {
                java.lang.reflect.Field taskTypeField = task.getClass().getDeclaredField("taskType");
                taskTypeField.setAccessible(true);
                return taskTypeField.get(task);
            } catch (Exception e) {
                // Field doesn't exist or couldn't be accessed
            }
            
            return null;
            
        } catch (Exception e) {
            Log.e(TAG, "Error getting task type: " + e.getMessage());
            return null;
        }
    }
    
    /**
     * Get the task priority of a ScheduledTask
     * @param task The ScheduledTask
     * @return The task priority, or null if an error occurred
     */
    public static Object getTaskPriority(Object task) {
        if (task == null) {
            return null;
        }
        
        try {
            // Try to use the getTaskPriority method
            try {
                Method getTaskPriorityMethod = task.getClass().getMethod("getTaskPriority");
                return getTaskPriorityMethod.invoke(task);
            } catch (Exception e) {
                // Method doesn't exist or failed to invoke
            }
            
            // Try to access the taskPriority field directly
            try {
                java.lang.reflect.Field taskPriorityField = task.getClass().getDeclaredField("taskPriority");
                taskPriorityField.setAccessible(true);
                return taskPriorityField.get(task);
            } catch (Exception e) {
                // Field doesn't exist or couldn't be accessed
            }
            
            return null;
            
        } catch (Exception e) {
            Log.e(TAG, "Error getting task priority: " + e.getMessage());
            return null;
        }
    }
    
    /**
     * Get the display name of a ScheduledTask
     * @param task The ScheduledTask
     * @return The display name
     */
    public static String getDisplayName(Object task) {
        if (task == null) {
            return "";
        }
        
        try {
            // Try to use the getDisplayName method
            try {
                Method getDisplayNameMethod = task.getClass().getMethod("getDisplayName");
                Object result = getDisplayNameMethod.invoke(task);
                if (result instanceof String) {
                    return (String) result;
                }
            } catch (Exception e) {
                // Method doesn't exist or returned non-String
            }
            
            // Try to use the getName method
            try {
                Method getNameMethod = task.getClass().getMethod("getName");
                Object result = getNameMethod.invoke(task);
                if (result instanceof String) {
                    return (String) result;
                }
            } catch (Exception e) {
                // Method doesn't exist or returned non-String
            }
            
            // Try to access the name field directly
            try {
                java.lang.reflect.Field nameField = task.getClass().getDeclaredField("name");
                nameField.setAccessible(true);
                Object result = nameField.get(task);
                if (result instanceof String) {
                    return (String) result;
                }
            } catch (Exception e) {
                // Field doesn't exist or returned non-String
            }
            
            // If all else fails, use toString
            return task.toString();
            
        } catch (Exception e) {
            Log.e(TAG, "Error getting display name: " + e.getMessage());
            return "";
        }
    }
    
    /**
     * Get the status color resource ID of a ScheduledTask
     * @param task The ScheduledTask
     * @return The status color resource ID, or 0 if an error occurred
     */
    public static int getStatusColorResId(Object task) {
        if (task == null) {
            return 0;
        }
        
        try {
            // Try to use the getStatusColorResId method
            try {
                Method getStatusColorResIdMethod = task.getClass().getMethod("getStatusColorResId");
                Object result = getStatusColorResIdMethod.invoke(task);
                if (result instanceof Integer) {
                    return (Integer) result;
                }
            } catch (Exception e) {
                // Method doesn't exist or returned non-Integer
            }
            
            // Try to get the status and derive the color from it
            Object status = null;
            
            try {
                Method getStatusMethod = task.getClass().getMethod("getStatus");
                status = getStatusMethod.invoke(task);
            } catch (Exception e) {
                // Method doesn't exist or failed to invoke
            }
            
            if (status != null) {
                // Get the string representation of the status
                String statusString = TaskStatusHelper.getStatusString(status);
                
                // Return different color values based on the status (using common Android color values)
                if (statusString.equals("RUNNING")) {
                    return 0xFF0099CC; // holo_blue_dark
                } else if (statusString.equals("COMPLETED")) {
                    return 0xFF669900; // holo_green_dark
                } else if (statusString.equals("FAILED")) {
                    return 0xFFCC0000; // holo_red_dark
                } else if (statusString.equals("PENDING")) {
                    return 0xFFFF8800; // holo_orange_dark
                } else if (statusString.equals("SCHEDULED")) {
                    return 0xFFAA66CC; // holo_purple
                } else {
                    return 0xFF666666; // darker_gray
                }
            }
            
            return 0;
            
        } catch (Exception e) {
            Log.e(TAG, "Error getting status color resource ID: " + e.getMessage());
            return 0;
        }
    }
}