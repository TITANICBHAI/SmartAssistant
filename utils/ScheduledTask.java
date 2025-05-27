package utils;

/**
 * Implementation of ScheduledTask that provides missing methods
 * needed by the Android app's TaskSchedulerManager
 */
public class ScheduledTask {
    private String taskId;
    private long lastExecutedAt;
    private int priority;
    private boolean enabled;
    private long intervalMs;
    
    /**
     * Create a new scheduled task
     * 
     * @param taskId Task identifier
     */
    public ScheduledTask(String taskId) {
        this.taskId = taskId;
        this.lastExecutedAt = 0;
        this.priority = 0;
        this.enabled = true;
        this.intervalMs = 60000; // Default 1 minute
    }
    
    /**
     * Get the task ID
     * 
     * @return The task ID
     */
    public String getTaskId() {
        return taskId;
    }
    
    /**
     * Set the task ID
     * 
     * @param taskId The new task ID
     */
    public void setTaskId(String taskId) {
        this.taskId = taskId;
    }
    
    /**
     * Get the last execution time
     * 
     * @return The last execution timestamp
     */
    public long getLastExecutedAt() {
        return lastExecutedAt;
    }
    
    /**
     * Set the last execution time
     * 
     * @param timestamp The execution timestamp
     */
    public void setLastExecutedAt(long timestamp) {
        this.lastExecutedAt = timestamp;
    }
    
    /**
     * Get the task priority
     * 
     * @return The priority value
     */
    public int getPriority() {
        return priority;
    }
    
    /**
     * Set the task priority
     * 
     * @param priority The priority value
     */
    public void setPriority(int priority) {
        this.priority = priority;
    }
    
    /**
     * Check if the task is enabled
     * 
     * @return True if enabled, false otherwise
     */
    public boolean isEnabled() {
        return enabled;
    }
    
    /**
     * Set whether the task is enabled
     * 
     * @param enabled The enabled state
     */
    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }
    
    /**
     * Get the execution interval in milliseconds
     * 
     * @return The interval
     */
    public long getIntervalMs() {
        return intervalMs;
    }
    
    /**
     * Set the execution interval
     * 
     * @param intervalMs The interval in milliseconds
     */
    public void setIntervalMs(long intervalMs) {
        this.intervalMs = intervalMs;
    }
}