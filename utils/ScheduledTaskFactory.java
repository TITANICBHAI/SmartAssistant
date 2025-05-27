package utils;

import android.util.Log;
import java.lang.reflect.Constructor;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import com.aiassistant.models.ScheduledTask;

/**
 * Factory for creating ScheduledTask instances
 * This class provides methods for creating ScheduledTask instances
 */
public class ScheduledTaskFactory {
    private static final String TAG = "ScheduledTaskFactory";
    
    /**
     * Create a new ScheduledTask
     * @param name The task name
     * @param description The task description
     * @return The task
     */
    public static ScheduledTask createTask(String name, String description) {
        try {
            // Try to find a constructor that accepts name and description
            try {
                Constructor<ScheduledTask> constructor = ScheduledTask.class.getConstructor(String.class, String.class);
                return constructor.newInstance(name, description);
            } catch (Exception e) {
                // Constructor doesn't exist, try other approaches
            }
            
            // Try to create a task with different constructors
            ScheduledTask task = null;
            
            // Try with empty constructor first
            try {
                task = ScheduledTask.class.newInstance();
            } catch (Exception e) {
                // Try with the most commonly available constructor
                try {
                    // Look for a constructor that accepts name, description, type, and priority
                    Class<?> taskTypeClass = Class.forName("com.aiassistant.models.TaskType");
                    Class<?> taskPriorityClass = Class.forName("com.aiassistant.models.TaskPriority");
                    
                    Constructor<ScheduledTask> constructor = ScheduledTask.class.getConstructor(String.class, String.class, taskTypeClass, taskPriorityClass);
                    
                    // Get default values for type and priority
                    Object defaultType = getDefaultTaskType();
                    Object defaultPriority = getDefaultTaskPriority();
                    
                    task = constructor.newInstance(name, description, defaultType, defaultPriority);
                } catch (Exception ex) {
                    Log.e(TAG, "Error creating task with constructor: " + ex.getMessage());
                    return null;
                }
            }
            
            // Set the name and description manually
            if (task != null) {
                try {
                    java.lang.reflect.Method setNameMethod = task.getClass().getMethod("setName", String.class);
                    setNameMethod.invoke(task, name);
                } catch (Exception e) {
                    Log.e(TAG, "Error setting task name: " + e.getMessage());
                }
                
                try {
                    java.lang.reflect.Method setDescriptionMethod = task.getClass().getMethod("setDescription", String.class);
                    setDescriptionMethod.invoke(task, description);
                } catch (Exception e) {
                    Log.e(TAG, "Error setting task description: " + e.getMessage());
                }
            }
            
            return task;
            
        } catch (Exception e) {
            Log.e(TAG, "Error creating task: " + e.getMessage());
            return null;
        }
    }
    
    /**
     * Create a task with full details
     * @param name The task name
     * @param description The task description
     * @param type The task type
     * @param priority The task priority
     * @param status The task status
     * @param scheduledFor The scheduled date
     * @param actionType The action type
     * @param actionParams The action parameters
     * @return The task
     */
    public static ScheduledTask createTaskWithDetails(
        String name, 
        String description, 
        Object type, 
        Object priority, 
        Object status, 
        Date scheduledFor, 
        String actionType, 
        Map<String, Object> actionParams
    ) {
        try {
            ScheduledTask task = createTask(name, description);
            if (task == null) {
                return null;
            }
            
            // Set the type
            if (type != null) {
                try {
                    java.lang.reflect.Method setTypeMethod = task.getClass().getMethod("setType", type.getClass());
                    setTypeMethod.invoke(task, type);
                } catch (Exception e) {
                    try {
                        java.lang.reflect.Method setTaskTypeMethod = task.getClass().getMethod("setTaskType", type.getClass());
                        setTaskTypeMethod.invoke(task, type);
                    } catch (Exception ex) {
                        Log.e(TAG, "Error setting task type: " + ex.getMessage());
                    }
                }
            }
            
            // Set the priority
            if (priority != null) {
                try {
                    java.lang.reflect.Method setPriorityMethod = task.getClass().getMethod("setPriority", priority.getClass());
                    setPriorityMethod.invoke(task, priority);
                } catch (Exception e) {
                    Log.e(TAG, "Error setting task priority: " + e.getMessage());
                }
            }
            
            // Set the status
            if (status != null) {
                try {
                    java.lang.reflect.Method setStatusMethod = task.getClass().getMethod("setStatus", status.getClass());
                    setStatusMethod.invoke(task, status);
                } catch (Exception e) {
                    Log.e(TAG, "Error setting task status: " + e.getMessage());
                }
            }
            
            // Set the scheduled date
            if (scheduledFor != null) {
                try {
                    java.lang.reflect.Method setScheduledForMethod = task.getClass().getMethod("setScheduledFor", Date.class);
                    setScheduledForMethod.invoke(task, scheduledFor);
                } catch (Exception e) {
                    try {
                        java.lang.reflect.Method setScheduledDateMethod = task.getClass().getMethod("setScheduledDate", Date.class);
                        setScheduledDateMethod.invoke(task, scheduledFor);
                    } catch (Exception ex) {
                        try {
                            java.lang.reflect.Method setDateMethod = task.getClass().getMethod("setDate", Date.class);
                            setDateMethod.invoke(task, scheduledFor);
                        } catch (Exception exc) {
                            Log.e(TAG, "Error setting task scheduled date: " + exc.getMessage());
                        }
                    }
                }
            }
            
            // Set the action type
            if (actionType != null) {
                try {
                    java.lang.reflect.Method setActionTypeMethod = task.getClass().getMethod("setActionType", String.class);
                    setActionTypeMethod.invoke(task, actionType);
                } catch (Exception e) {
                    Log.e(TAG, "Error setting task action type: " + e.getMessage());
                }
            }
            
            // Set the action parameters
            if (actionParams != null) {
                try {
                    java.lang.reflect.Method setActionParamsMethod = task.getClass().getMethod("setActionParams", Map.class);
                    setActionParamsMethod.invoke(task, actionParams);
                } catch (Exception e) {
                    try {
                        java.lang.reflect.Method setParamsMethod = task.getClass().getMethod("setParams", Map.class);
                        setParamsMethod.invoke(task, actionParams);
                    } catch (Exception ex) {
                        Log.e(TAG, "Error setting task action params: " + ex.getMessage());
                    }
                }
            }
            
            return task;
            
        } catch (Exception e) {
            Log.e(TAG, "Error creating task with details: " + e.getMessage());
            return null;
        }
    }
    
    /**
     * Get the default task type
     * @return The default task type
     */
    public static Object getDefaultTaskType() {
        try {
            Class<?> taskTypeClass = Class.forName("com.aiassistant.models.TaskType");
            
            // Try to get the DEFAULT constant
            try {
                java.lang.reflect.Field defaultField = taskTypeClass.getField("DEFAULT");
                return defaultField.get(null);
            } catch (Exception e) {
                // Field doesn't exist
            }
            
            // Get the first enum constant
            Object[] constants = taskTypeClass.getEnumConstants();
            if (constants != null && constants.length > 0) {
                return constants[0];
            }
            
            return null;
            
        } catch (Exception e) {
            Log.e(TAG, "Error getting default task type: " + e.getMessage());
            return null;
        }
    }
    
    /**
     * Get the default task priority
     * @return The default task priority
     */
    public static Object getDefaultTaskPriority() {
        try {
            Class<?> taskPriorityClass = Class.forName("com.aiassistant.models.TaskPriority");
            
            // Try to get the DEFAULT or MEDIUM constant
            try {
                java.lang.reflect.Field defaultField = taskPriorityClass.getField("DEFAULT");
                return defaultField.get(null);
            } catch (Exception e) {
                try {
                    java.lang.reflect.Field mediumField = taskPriorityClass.getField("MEDIUM");
                    return mediumField.get(null);
                } catch (Exception ex) {
                    try {
                        java.lang.reflect.Field normalField = taskPriorityClass.getField("NORMAL");
                        return normalField.get(null);
                    } catch (Exception exc) {
                        // Fields don't exist
                    }
                }
            }
            
            // Get the first enum constant
            Object[] constants = taskPriorityClass.getEnumConstants();
            if (constants != null && constants.length > 0) {
                return constants[0];
            }
            
            return null;
            
        } catch (Exception e) {
            Log.e(TAG, "Error getting default task priority: " + e.getMessage());
            return null;
        }
    }
    
    /**
     * Get the default task status
     * @return The default task status
     */
    public static Object getDefaultTaskStatus() {
        try {
            Class<?> taskStatusClass = Class.forName("com.aiassistant.models.TaskStatus");
            
            // Try to get the PENDING constant
            try {
                java.lang.reflect.Field pendingField = taskStatusClass.getField("PENDING");
                return pendingField.get(null);
            } catch (Exception e) {
                try {
                    java.lang.reflect.Field scheduledField = taskStatusClass.getField("SCHEDULED");
                    return scheduledField.get(null);
                } catch (Exception ex) {
                    try {
                        java.lang.reflect.Field newField = taskStatusClass.getField("NEW");
                        return newField.get(null);
                    } catch (Exception exc) {
                        // Fields don't exist
                    }
                }
            }
            
            // Get the first enum constant
            Object[] constants = taskStatusClass.getEnumConstants();
            if (constants != null && constants.length > 0) {
                return constants[0];
            }
            
            return null;
            
        } catch (Exception e) {
            Log.e(TAG, "Error getting default task status: " + e.getMessage());
            return null;
        }
    }
}