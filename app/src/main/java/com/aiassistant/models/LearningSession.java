package com.aiassistant.models;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Learning Session
 * Represents a session where the AI is learning from a specific source
 */
public class LearningSession {
    // Session ID (unique identifier)
    private String sessionId;
    
    // Session name
    private String name;
    
    // Learning source
    private LearningSource source;
    
    // Target package (if app-specific)
    private String targetPackage;
    
    // Start timestamp
    private long startTime;
    
    // End timestamp (0 if ongoing)
    private long endTime;
    
    // Session status
    private SessionStatus status;
    
    // Session parameters
    private Map<String, Object> parameters;
    
    // Data points collected
    private int dataPointsCollected;
    
    // Session metrics
    private Map<String, Float> metrics;
    
    // Associated models
    private List<String> modelIds;
    
    // Media source path (for video learning)
    private String mediaSourcePath;
    
    /**
     * Create a new learning session
     */
    public LearningSession(String name, LearningSource source) {
        this.sessionId = UUID.randomUUID().toString();
        this.name = name;
        this.source = source;
        this.startTime = System.currentTimeMillis();
        this.endTime = 0;
        this.status = SessionStatus.RUNNING;
        this.parameters = new HashMap<>();
        this.dataPointsCollected = 0;
        this.metrics = new HashMap<>();
        this.modelIds = new ArrayList<>();
    }
    
    // Getters and setters
    public String getSessionId() {
        return sessionId;
    }
    
    public String getName() {
        return name;
    }
    
    public void setName(String name) {
        this.name = name;
    }
    
    public LearningSource getSource() {
        return source;
    }
    
    public String getTargetPackage() {
        return targetPackage;
    }
    
    public void setTargetPackage(String targetPackage) {
        this.targetPackage = targetPackage;
    }
    
    public long getStartTime() {
        return startTime;
    }
    
    public long getEndTime() {
        return endTime;
    }
    
    public void setEndTime(long endTime) {
        this.endTime = endTime;
    }
    
    public SessionStatus getStatus() {
        return status;
    }
    
    public void setStatus(SessionStatus status) {
        this.status = status;
        
        if (status == SessionStatus.COMPLETED || status == SessionStatus.FAILED) {
            // Set end time if session is terminal
            this.endTime = System.currentTimeMillis();
        }
    }
    
    public Map<String, Object> getParameters() {
        return parameters;
    }
    
    public void setParameters(Map<String, Object> parameters) {
        this.parameters = parameters;
    }
    
    public void addParameter(String key, Object value) {
        this.parameters.put(key, value);
    }
    
    public Object getParameter(String key) {
        return this.parameters.get(key);
    }
    
    public int getDataPointsCollected() {
        return dataPointsCollected;
    }
    
    public void setDataPointsCollected(int dataPointsCollected) {
        this.dataPointsCollected = dataPointsCollected;
    }
    
    public void incrementDataPoints(int count) {
        this.dataPointsCollected += count;
    }
    
    public Map<String, Float> getMetrics() {
        return metrics;
    }
    
    public void setMetrics(Map<String, Float> metrics) {
        this.metrics = metrics;
    }
    
    public void addMetric(String key, float value) {
        this.metrics.put(key, value);
    }
    
    public Float getMetric(String key) {
        return this.metrics.get(key);
    }
    
    public List<String> getModelIds() {
        return modelIds;
    }
    
    public void setModelIds(List<String> modelIds) {
        this.modelIds = modelIds;
    }
    
    public void addModelId(String modelId) {
        if (modelId != null && !modelIds.contains(modelId)) {
            this.modelIds.add(modelId);
        }
    }
    
    public String getMediaSourcePath() {
        return mediaSourcePath;
    }
    
    public void setMediaSourcePath(String mediaSourcePath) {
        this.mediaSourcePath = mediaSourcePath;
    }
    
    /**
     * Get the duration of the session in milliseconds
     */
    public long getDurationMs() {
        long end = endTime > 0 ? endTime : System.currentTimeMillis();
        return end - startTime;
    }
    
    /**
     * Check if the session is active
     */
    public boolean isActive() {
        return status == SessionStatus.RUNNING || status == SessionStatus.PAUSED;
    }
    
    /**
     * Complete the session
     */
    public void complete() {
        this.status = SessionStatus.COMPLETED;
        this.endTime = System.currentTimeMillis();
    }
    
    /**
     * Fail the session
     */
    public void fail(String reason) {
        this.status = SessionStatus.FAILED;
        this.endTime = System.currentTimeMillis();
        this.addParameter("failureReason", reason);
    }
    
    /**
     * Pause the session
     */
    public void pause() {
        this.status = SessionStatus.PAUSED;
    }
    
    /**
     * Resume the session
     */
    public void resume() {
        this.status = SessionStatus.RUNNING;
    }
    
    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        
        LearningSession session = (LearningSession) obj;
        return sessionId.equals(session.sessionId);
    }
    
    @Override
    public int hashCode() {
        return sessionId.hashCode();
    }
}