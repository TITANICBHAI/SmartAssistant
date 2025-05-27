package com.aiassistant.models;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/**
 * Model class for storing learned data from user interaction or video analysis.
 */
@Entity(tableName = "learned_data")
public class LearnedData {
    
    @PrimaryKey(autoGenerate = true)
    private int id;
    
    private String appName;
    private String packageName;
    private String patternType;
    private String patternDescription;
    private String actionSequence;
    private float confidence;
    private long learnedAt;
    private int usageCount;
    private boolean isUserApproved;
    
    // Constructor for creating new learned data
    public LearnedData(String appName, String packageName, String patternType,
                       String patternDescription, String actionSequence, float confidence) {
        this.appName = appName;
        this.packageName = packageName;
        this.patternType = patternType;
        this.patternDescription = patternDescription;
        this.actionSequence = actionSequence;
        this.confidence = confidence;
        this.learnedAt = System.currentTimeMillis();
        this.usageCount = 0;
        this.isUserApproved = false;
    }
    
    // Getters and setters
    public int getId() {
        return id;
    }
    
    public void setId(int id) {
        this.id = id;
    }
    
    public String getAppName() {
        return appName;
    }
    
    public void setAppName(String appName) {
        this.appName = appName;
    }
    
    public String getPackageName() {
        return packageName;
    }
    
    public void setPackageName(String packageName) {
        this.packageName = packageName;
    }
    
    public String getPatternType() {
        return patternType;
    }
    
    public void setPatternType(String patternType) {
        this.patternType = patternType;
    }
    
    public String getPatternDescription() {
        return patternDescription;
    }
    
    public void setPatternDescription(String patternDescription) {
        this.patternDescription = patternDescription;
    }
    
    public String getActionSequence() {
        return actionSequence;
    }
    
    public void setActionSequence(String actionSequence) {
        this.actionSequence = actionSequence;
    }
    
    public float getConfidence() {
        return confidence;
    }
    
    public void setConfidence(float confidence) {
        this.confidence = confidence;
    }
    
    public long getLearnedAt() {
        return learnedAt;
    }
    
    public void setLearnedAt(long learnedAt) {
        this.learnedAt = learnedAt;
    }
    
    public int getUsageCount() {
        return usageCount;
    }
    
    public void setUsageCount(int usageCount) {
        this.usageCount = usageCount;
    }
    
    public void incrementUsageCount() {
        this.usageCount++;
    }
    
    public boolean isUserApproved() {
        return isUserApproved;
    }
    
    public void setUserApproved(boolean userApproved) {
        isUserApproved = userApproved;
    }
    
    // Helper methods
    public String getFormattedDate() {
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault());
        return dateFormat.format(new Date(learnedAt));
    }
    
    public String getConfidencePercentage() {
        return String.format(Locale.getDefault(), "%.1f%%", confidence * 100);
    }
    
    public boolean isHighConfidence() {
        return confidence >= 0.8f; // 80% confidence threshold
    }
    
    @NonNull
    @Override
    public String toString() {
        return "LearnedData{" +
                "id=" + id +
                ", appName='" + appName + '\'' +
                ", patternType='" + patternType + '\'' +
                ", confidence=" + getConfidencePercentage() +
                ", usageCount=" + usageCount +
                ", learnedAt=" + getFormattedDate() +
                '}';
    }
}
