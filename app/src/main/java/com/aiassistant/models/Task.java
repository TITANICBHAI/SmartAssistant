package com.aiassistant.models;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/**
 * Model class for task scheduling.
 */
@Entity(tableName = "tasks")
public class Task {
    
    @PrimaryKey(autoGenerate = true)
    private int id;
    
    private String title;
    private String description;
    private long scheduledTime;
    private String repeatPattern;
    private String actionType;
    private boolean isActive;
    private long createdAt;
    private long lastExecutedAt;
    
    // Constructor for creating a new task
    public Task(String title, String description, long scheduledTime,
                String repeatPattern, String actionType, boolean isActive) {
        this.title = title;
        this.description = description;
        this.scheduledTime = scheduledTime;
        this.repeatPattern = repeatPattern;
        this.actionType = actionType;
        this.isActive = isActive;
        this.createdAt = System.currentTimeMillis();
        this.lastExecutedAt = 0; // Not executed yet
    }
    
    // Getters and setters
    public int getId() {
        return id;
    }
    
    public void setId(int id) {
        this.id = id;
    }
    
    public String getTitle() {
        return title;
    }
    
    public void setTitle(String title) {
        this.title = title;
    }
    
    public String getDescription() {
        return description;
    }
    
    public void setDescription(String description) {
        this.description = description;
    }
    
    public long getScheduledTime() {
        return scheduledTime;
    }
    
    public void setScheduledTime(long scheduledTime) {
        this.scheduledTime = scheduledTime;
    }
    
    public String getRepeatPattern() {
        return repeatPattern;
    }
    
    public void setRepeatPattern(String repeatPattern) {
        this.repeatPattern = repeatPattern;
    }
    
    public String getActionType() {
        return actionType;
    }
    
    public void setActionType(String actionType) {
        this.actionType = actionType;
    }
    
    public boolean isActive() {
        return isActive;
    }
    
    public void setActive(boolean active) {
        isActive = active;
    }
    
    public long getCreatedAt() {
        return createdAt;
    }
    
    public void setCreatedAt(long createdAt) {
        this.createdAt = createdAt;
    }
    
    public long getLastExecutedAt() {
        return lastExecutedAt;
    }
    
    public void setLastExecutedAt(long lastExecutedAt) {
        this.lastExecutedAt = lastExecutedAt;
    }
    
    // Helper methods
    public String getFormattedTime() {
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault());
        return dateFormat.format(new Date(scheduledTime));
    }
    
    public String getFormattedCreatedAt() {
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault());
        return dateFormat.format(new Date(createdAt));
    }
    
    public String getFormattedLastExecutedAt() {
        if (lastExecutedAt == 0) {
            return "Never";
        }
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault());
        return dateFormat.format(new Date(lastExecutedAt));
    }
    
    public boolean isOverdue() {
        return scheduledTime < System.currentTimeMillis() && lastExecutedAt == 0;
    }
    
    public long getNextScheduledTime() {
        if ("None".equals(repeatPattern) || repeatPattern == null) {
            return scheduledTime;
        }
        
        // If task has been executed at least once, calculate next time based on repeat pattern
        long baseTime = lastExecutedAt > 0 ? lastExecutedAt : scheduledTime;
        
        switch (repeatPattern) {
            case "Daily":
                return baseTime + (24 * 60 * 60 * 1000); // Add 1 day
            case "Weekly":
                return baseTime + (7 * 24 * 60 * 60 * 1000); // Add 7 days
            case "Monthly":
                return baseTime + (30 * 24 * 60 * 60 * 1000); // Add ~30 days
            case "Yearly":
                return baseTime + (365 * 24 * 60 * 60 * 1000); // Add ~365 days
            case "Hourly":
                return baseTime + (60 * 60 * 1000); // Add 1 hour
            default:
                return scheduledTime;
        }
    }
    
    @NonNull
    @Override
    public String toString() {
        return "Task{" +
                "id=" + id +
                ", title='" + title + '\'' +
                ", scheduledTime=" + getFormattedTime() +
                ", repeatPattern='" + repeatPattern + '\'' +
                ", actionType='" + actionType + '\'' +
                ", isActive=" + isActive +
                '}';
    }
}
