package com.aiassistant.models;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.UUID;

/**
 * Model class representing a scheduled task in the AI assistant.
 * This class holds information about tasks that can be scheduled for execution.
 */
public class ScheduledTask {
    
    /**
     * Status of a scheduled task
     */
    public enum TaskStatus {
        PENDING("pending"),
        SCHEDULED("scheduled"),
        RUNNING("running"),
        COMPLETED("completed"),
        FAILED("failed"),
        CANCELLED("cancelled"),
        PAUSED("paused");
        
        private final String value;
        
        TaskStatus(String value) {
            this.value = value;
        }
        
        public String getValue() {
            return value;
        }
        
        /**
         * Get a display name suitable for UI display
         */
        public String getDisplayName() {
            // Capitalize first letter
            if (value.isEmpty()) return "";
            return value.substring(0, 1).toUpperCase() + value.substring(1);
        }
        
        /**
         * Get color resource ID for this status
         */
        public int getStatusColorResId() {
            // This would return a color resource ID
            // For now, return 0 as a placeholder
            return 0;
        }
        
        public static TaskStatus fromString(String text) {
            for (TaskStatus status : TaskStatus.values()) {
                if (status.value.equalsIgnoreCase(text)) {
                    return status;
                }
            }
            return PENDING;
        }
    }
    
    /**
     * Priority of a scheduled task
     */
    public enum TaskPriority {
        LOW("low"),
        MEDIUM("medium"),
        HIGH("high"),
        CRITICAL("critical");
        
        private final String value;
        
        TaskPriority(String value) {
            this.value = value;
        }
        
        public String getValue() {
            return value;
        }
        
        /**
         * Get color resource ID for this priority
         */
        public int getPriorityColorResId() {
            // This would return a color resource ID
            // For now, return 0 as a placeholder
            return 0;
        }
        
        public static TaskPriority fromString(String text) {
            for (TaskPriority priority : TaskPriority.values()) {
                if (priority.value.equalsIgnoreCase(text)) {
                    return priority;
                }
            }
            return MEDIUM;
        }
    }
    
    // Task properties
    private String id;
    private String name;
    private String description;
    private TaskType type;
    private TaskStatus status;
    private TaskPriority priority;
    private Date scheduledDate;
    private Date completedDate;
    private boolean enabled;
    private long lastExecutedAt;
    private List<TaskAction> actions;
    
    /**
     * Default constructor
     */
    public ScheduledTask() {
        this.id = UUID.randomUUID().toString();
        this.status = TaskStatus.PENDING;
        this.priority = TaskPriority.MEDIUM;
        this.enabled = true;
        this.actions = new ArrayList<>();
    }
    
    /**
     * Constructor with basic task information
     */
    public ScheduledTask(String name, String description, TaskType type, TaskPriority priority) {
        this();
        this.name = name;
        this.description = description;
        this.type = type;
        this.priority = priority;
    }
    
    // Getters and setters
    
    public String getId() {
        return id;
    }
    
    public void setId(String id) {
        this.id = id;
    }
    
    public String getName() {
        return name;
    }
    
    public void setName(String name) {
        this.name = name;
    }
    
    public String getDescription() {
        return description;
    }
    
    public void setDescription(String description) {
        this.description = description;
    }
    
    public TaskType getType() {
        return type;
    }
    
    public void setType(TaskType type) {
        this.type = type;
    }
    
    public TaskStatus getStatus() {
        return status;
    }
    
    public void setStatus(TaskStatus status) {
        this.status = status;
    }
    
    public TaskPriority getPriority() {
        return priority;
    }
    
    public void setPriority(TaskPriority priority) {
        this.priority = priority;
    }
    
    public Date getScheduledDate() {
        return scheduledDate;
    }
    
    public void setScheduledDate(Date scheduledDate) {
        this.scheduledDate = scheduledDate;
    }
    
    public Date getCompletedDate() {
        return completedDate;
    }
    
    public void setCompletedDate(Date completedDate) {
        this.completedDate = completedDate;
    }
    
    public boolean isEnabled() {
        return enabled;
    }
    
    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }
    
    /**
     * Get the last time this task was executed
     * 
     * @return The timestamp when the task was last executed
     */
    public long getLastExecutedAt() {
        return lastExecutedAt;
    }
    
    /**
     * Set the last time this task was executed
     * 
     * @param lastExecutedAt The timestamp when the task was executed
     */
    public void setLastExecutedAt(long lastExecutedAt) {
        this.lastExecutedAt = lastExecutedAt;
    }
    
    /**
     * Get the list of actions to be performed by this task
     * 
     * @return The list of task actions
     */
    public List<TaskAction> getActions() {
        return actions;
    }
    
    /**
     * Set the list of actions to be performed by this task
     * 
     * @param actions The list of task actions
     */
    public void setActions(List<TaskAction> actions) {
        this.actions = actions != null ? actions : new ArrayList<>();
    }
    
    /**
     * Add an action to this task
     * 
     * @param action The action to add
     */
    public void addAction(TaskAction action) {
        if (action != null) {
            if (actions == null) {
                actions = new ArrayList<>();
            }
            actions.add(action);
        }
    }
    
    /**
     * Check if this task is due to be executed
     * 
     * @return true if the task is due, false otherwise
     */
    public boolean isDue() {
        // A task is due if:
        // 1. It has a scheduled date that is in the past or present
        // 2. Its status is PENDING or SCHEDULED
        if (scheduledDate == null) {
            return false;
        }
        
        boolean isPendingOrScheduled = 
            status == TaskStatus.PENDING || 
            status == TaskStatus.SCHEDULED;
            
        boolean scheduledTimeReached = 
            scheduledDate.getTime() <= System.currentTimeMillis();
            
        return enabled && isPendingOrScheduled && scheduledTimeReached;
    }
    
    @Override
    public String toString() {
        return "ScheduledTask{" +
               "id='" + id + '\'' +
               ", name='" + name + '\'' +
               ", status=" + status +
               ", priority=" + priority +
               '}';
    }
}