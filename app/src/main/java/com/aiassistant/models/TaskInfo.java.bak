package com.aiassistant.models;

import com.aiassistant.R;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.UUID;

/**
 * Represents a task to be performed by the AI Assistant
 */
public class TaskInfo {
    private String id;
    private String name;
    private String description;
    private TaskType type;
    private TaskScheduleType scheduleType;
    private TaskPriority priority;
    private Date scheduledTime;
    private Date completedTime;
    private TaskStatus status;
    private String actionSequence; // JSON string representing sequence of actions
    private boolean repeating;
    private int repeatInterval; // in minutes, if repeating
    
    public TaskInfo() {
        this.id = UUID.randomUUID().toString();
        this.status = TaskStatus.PENDING;
    }
    
    public TaskInfo(String name, String description, TaskType type, 
                  TaskScheduleType scheduleType, TaskPriority priority) {
        this();
        this.name = name;
        this.description = description;
        this.type = type;
        this.scheduleType = scheduleType;
        this.priority = priority;
    }

    // Getters and Setters
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

    public TaskScheduleType getScheduleType() {
        return scheduleType;
    }

    public void setScheduleType(TaskScheduleType scheduleType) {
        this.scheduleType = scheduleType;
    }

    public TaskPriority getPriority() {
        return priority;
    }

    public void setPriority(TaskPriority priority) {
        this.priority = priority;
    }

    public Date getScheduledTime() {
        return scheduledTime;
    }

    public void setScheduledTime(Date scheduledTime) {
        this.scheduledTime = scheduledTime;
    }

    public Date getCompletedTime() {
        return completedTime;
    }

    public void setCompletedTime(Date completedTime) {
        this.completedTime = completedTime;
    }

    public TaskStatus getStatus() {
        return status;
    }

    public void setStatus(TaskStatus status) {
        this.status = status;
    }

    public String getActionSequence() {
        return actionSequence;
    }

    public void setActionSequence(String actionSequence) {
        this.actionSequence = actionSequence;
    }

    public boolean isRepeating() {
        return repeating;
    }

    public void setRepeating(boolean repeating) {
        this.repeating = repeating;
    }

    public int getRepeatInterval() {
        return repeatInterval;
    }

    public void setRepeatInterval(int repeatInterval) {
        this.repeatInterval = repeatInterval;
    }
    
    /**
     * Get the scheduled date for this task
     * 
     * @return Scheduled date or null if not scheduled
     */
    public Date getScheduledDate() {
        return scheduledTime;
    }
    
    /**
     * Set the scheduled date for this task
     * 
     * @param scheduledDate The date when the task is scheduled
     */
    public void setScheduledDate(Date scheduledDate) {
        this.scheduledTime = scheduledDate;
    }
    
    /**
     * Get the interval in minutes for recurring tasks
     * 
     * @return Interval in minutes
     */
    public int getIntervalMinutes() {
        return repeatInterval;
    }
    
    /**
     * Set the interval in minutes for recurring tasks
     * 
     * @param intervalMinutes Interval in minutes
     */
    public void setIntervalMinutes(int intervalMinutes) {
        this.repeatInterval = intervalMinutes;
    }
    
    /**
     * Format the scheduled date for display
     * 
     * @return Formatted date string
     */
    public String getFormattedScheduledDate() {
        if (scheduledTime == null) {
            return "Not scheduled";
        }
        
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
        return dateFormat.format(scheduledTime);
    }
    
    /**
     * Format the scheduled time for display
     * 
     * @return Formatted time string
     */
    public String getFormattedScheduledTime() {
        if (scheduledTime == null) {
            return "";
        }
        
        SimpleDateFormat timeFormat = new SimpleDateFormat("HH:mm", Locale.getDefault());
        return timeFormat.format(scheduledTime);
    }
    
    /**
     * Get a color code based on task priority
     * 
     * @return Color resource id to use for this task
     */
    public int getPriorityColorResource() {
        if (priority == null) {
            return android.R.color.holo_blue_light;
        }
        
        switch (priority) {
            case HIGH:
                return R.color.priority_high;
            case MEDIUM:
                return R.color.priority_medium;
            case LOW:
                return R.color.priority_low;
            default:
                return android.R.color.holo_blue_light;
        }
    }
    
    /**
     * Get a color code based on task status
     * 
     * @return Color resource id to use for this task's status
     */
    public int getStatusColorResource() {
        if (status == null) {
            return android.R.color.holo_blue_light;
        }
        
        switch (status) {
            case COMPLETED:
                return R.color.status_completed;
            case RUNNING:
                return R.color.status_running;
            case PENDING:
                return R.color.status_pending;
            case FAILED:
                return R.color.status_failed;
            case CANCELLED:
                return R.color.status_cancelled;
            case SCHEDULED:
                return R.color.status_scheduled;
            default:
                return android.R.color.holo_blue_light;
        }
    }
    
    /**
     * Mark this task as completed
     */
    public void markCompleted() {
        this.status = TaskStatus.COMPLETED;
        this.completedTime = new Date();
    }
    
    /**
     * Mark this task as in progress
     */
    public void markInProgress() {
        this.status = TaskStatus.RUNNING;
    }
    
    /**
     * Mark this task as failed
     */
    public void markFailed() {
        this.status = TaskStatus.FAILED;
    }
    
    /**
     * Mark this task as cancelled
     */
    public void markCancelled() {
        this.status = TaskStatus.CANCELLED;
    }
    
    /**
     * Reset task to pending status
     */
    public void resetStatus() {
        this.status = TaskStatus.PENDING;
        this.completedTime = null;
    }
}