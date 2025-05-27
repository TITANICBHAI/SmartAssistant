package com.aiassistant.models;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

/**
 * Represents a task scheduled to run at a specific time or on a specific trigger.
 */
public class ScheduledTask {
    private String id;
    private String name;
    private String packageName;
    private String taskType;
    private Date scheduledTime;
    private boolean repeating;
    private long repeatInterval;
    private String triggerType;
    private String triggerData;
    private String actionData;
    private boolean enabled;
    private Date lastRun;
    private boolean completed;
    private Map<String, Object> extraData;
    
    /**
     * Create a new scheduled task
     */
    public ScheduledTask() {
        this.id = generateId();
        this.enabled = true;
        this.completed = false;
        this.repeating = false;
        this.repeatInterval = 0;
        this.extraData = new HashMap<>();
    }
    
    /**
     * Generate a unique ID for this task
     * @return The generated ID
     */
    private String generateId() {
        return "task_" + System.currentTimeMillis() + "_" + Math.round(Math.random() * 1000);
    }
    
    /**
     * Get the task ID
     * @return The task ID
     */
    public String getId() {
        return id;
    }
    
    /**
     * Set the task ID
     * @param id The task ID
     */
    public void setId(String id) {
        this.id = id;
    }
    
    /**
     * Get the task name
     * @return The task name
     */
    public String getName() {
        return name;
    }
    
    /**
     * Set the task name
     * @param name The task name
     */
    public void setName(String name) {
        this.name = name;
    }
    
    /**
     * Get the package name associated with this task
     * @return The package name
     */
    public String getPackageName() {
        return packageName;
    }
    
    /**
     * Set the package name associated with this task
     * @param packageName The package name
     */
    public void setPackageName(String packageName) {
        this.packageName = packageName;
    }
    
    /**
     * Get the task type
     * @return The task type
     */
    public String getTaskType() {
        return taskType;
    }
    
    /**
     * Set the task type
     * @param taskType The task type
     */
    public void setTaskType(String taskType) {
        this.taskType = taskType;
    }
    
    /**
     * Get the scheduled time
     * @return The scheduled time
     */
    public Date getScheduledTime() {
        return scheduledTime;
    }
    
    /**
     * Set the scheduled time
     * @param scheduledTime The scheduled time
     */
    public void setScheduledTime(Date scheduledTime) {
        this.scheduledTime = scheduledTime;
    }
    
    /**
     * Check if this task is repeating
     * @return True if the task is repeating, false otherwise
     */
    public boolean isRepeating() {
        return repeating;
    }
    
    /**
     * Set whether this task is repeating
     * @param repeating True if the task is repeating, false otherwise
     */
    public void setRepeating(boolean repeating) {
        this.repeating = repeating;
    }
    
    /**
     * Get the repeat interval in milliseconds
     * @return The repeat interval
     */
    public long getRepeatInterval() {
        return repeatInterval;
    }
    
    /**
     * Set the repeat interval in milliseconds
     * @param repeatInterval The repeat interval
     */
    public void setRepeatInterval(long repeatInterval) {
        this.repeatInterval = repeatInterval;
    }
    
    /**
     * Get the trigger type
     * @return The trigger type
     */
    public String getTriggerType() {
        return triggerType;
    }
    
    /**
     * Set the trigger type
     * @param triggerType The trigger type
     */
    public void setTriggerType(String triggerType) {
        this.triggerType = triggerType;
    }
    
    /**
     * Get the trigger data
     * @return The trigger data
     */
    public String getTriggerData() {
        return triggerData;
    }
    
    /**
     * Set the trigger data
     * @param triggerData The trigger data
     */
    public void setTriggerData(String triggerData) {
        this.triggerData = triggerData;
    }
    
    /**
     * Get the action data
     * @return The action data
     */
    public String getActionData() {
        return actionData;
    }
    
    /**
     * Set the action data
     * @param actionData The action data
     */
    public void setActionData(String actionData) {
        this.actionData = actionData;
    }
    
    /**
     * Check if this task is enabled
     * @return True if the task is enabled, false otherwise
     */
    public boolean isEnabled() {
        return enabled;
    }
    
    /**
     * Set whether this task is enabled
     * @param enabled True if the task is enabled, false otherwise
     */
    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }
    
    /**
     * Get the last time this task was run
     * @return The last run time
     */
    public Date getLastRun() {
        return lastRun;
    }
    
    /**
     * Set the last time this task was run
     * @param lastRun The last run time
     */
    public void setLastRun(Date lastRun) {
        this.lastRun = lastRun;
    }
    
    /**
     * Check if this task is completed
     * @return True if the task is completed, false otherwise
     */
    public boolean isCompleted() {
        return completed;
    }
    
    /**
     * Set whether this task is completed
     * @param completed True if the task is completed, false otherwise
     */
    public void setCompleted(boolean completed) {
        this.completed = completed;
    }
    
    /**
     * Get extra data associated with this task
     * @param key The data key
     * @return The data value, or null if not found
     */
    public Object getExtraData(String key) {
        return extraData.get(key);
    }
    
    /**
     * Set extra data associated with this task
     * @param key The data key
     * @param value The data value
     */
    public void setExtraData(String key, Object value) {
        extraData.put(key, value);
    }
    
    /**
     * Get all extra data associated with this task
     * @return A map of all extra data
     */
    public Map<String, Object> getAllExtraData() {
        return new HashMap<>(extraData);
    }
    
    /**
     * Check if this task is due to run
     * @return True if the task is due to run, false otherwise
     */
    public boolean isDue() {
        if (!enabled || completed) {
            return false;
        }
        
        if (scheduledTime == null) {
            return false;
        }
        
        Date now = new Date();
        if (now.after(scheduledTime)) {
            // If it's a one-time task, it's due
            if (!repeating) {
                return true;
            }
            
            // If it's a repeating task, check if it should run again
            if (lastRun == null) {
                return true;
            }
            
            long timeSinceLastRun = now.getTime() - lastRun.getTime();
            return timeSinceLastRun >= repeatInterval;
        }
        
        return false;
    }
    
    @Override
    public String toString() {
        return "ScheduledTask{" +
                "id='" + id + '\'' +
                ", name='" + name + '\'' +
                ", packageName='" + packageName + '\'' +
                ", taskType='" + taskType + '\'' +
                ", scheduledTime=" + scheduledTime +
                ", repeating=" + repeating +
                ", repeatInterval=" + repeatInterval +
                ", enabled=" + enabled +
                ", completed=" + completed +
                '}';
    }
}