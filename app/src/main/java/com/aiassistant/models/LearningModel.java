package com.aiassistant.models;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Learning Model
 * Represents a machine learning model for the AI assistant
 */
public class LearningModel {
    // Model ID (unique identifier)
    private String modelId;
    
    // Model name
    private String name;
    
    // Model type
    private ModelType modelType;
    
    // Source of learning data
    private LearningSource learningSource;
    
    // Model version
    private String version;
    
    // Creation timestamp
    private long createdAt;
    
    // Last update timestamp
    private long lastUpdatedAt;
    
    // Training progress (0.0-1.0)
    private float trainingProgress;
    
    // Whether the model is ready for use
    private boolean ready;
    
    // Accuracy metrics
    private Map<String, Float> accuracyMetrics;
    
    // Model parameters
    private Map<String, Object> parameters;
    
    // Target application (if app-specific)
    private String targetPackage;
    
    // Model size in bytes
    private long sizeBytes;
    
    // Associated learning sessions
    private List<String> learningSessions;
    
    /**
     * Create a new learning model
     */
    public LearningModel(String name, ModelType modelType, LearningSource learningSource) {
        this.modelId = UUID.randomUUID().toString();
        this.name = name;
        this.modelType = modelType;
        this.learningSource = learningSource;
        this.version = "1.0";
        this.createdAt = System.currentTimeMillis();
        this.lastUpdatedAt = this.createdAt;
        this.trainingProgress = 0.0f;
        this.ready = false;
        this.accuracyMetrics = new HashMap<>();
        this.parameters = new HashMap<>();
        this.sizeBytes = 0;
        this.learningSessions = new ArrayList<>();
    }
    
    // Getters and setters
    public String getModelId() {
        return modelId;
    }
    
    public String getName() {
        return name;
    }
    
    public void setName(String name) {
        this.name = name;
    }
    
    public ModelType getModelType() {
        return modelType;
    }
    
    public LearningSource getLearningSource() {
        return learningSource;
    }
    
    public String getVersion() {
        return version;
    }
    
    public void setVersion(String version) {
        this.version = version;
    }
    
    public long getCreatedAt() {
        return createdAt;
    }
    
    public long getLastUpdatedAt() {
        return lastUpdatedAt;
    }
    
    public void setLastUpdatedAt(long lastUpdatedAt) {
        this.lastUpdatedAt = lastUpdatedAt;
    }
    
    public float getTrainingProgress() {
        return trainingProgress;
    }
    
    public void setTrainingProgress(float trainingProgress) {
        this.trainingProgress = trainingProgress;
    }
    
    public boolean isReady() {
        return ready;
    }
    
    public void setReady(boolean ready) {
        this.ready = ready;
    }
    
    public Map<String, Float> getAccuracyMetrics() {
        return accuracyMetrics;
    }
    
    public void setAccuracyMetrics(Map<String, Float> accuracyMetrics) {
        this.accuracyMetrics = accuracyMetrics;
    }
    
    public void addAccuracyMetric(String name, float value) {
        this.accuracyMetrics.put(name, value);
    }
    
    public Float getAccuracyMetric(String name) {
        return this.accuracyMetrics.get(name);
    }
    
    public Map<String, Object> getParameters() {
        return parameters;
    }
    
    public void setParameters(Map<String, Object> parameters) {
        this.parameters = parameters;
    }
    
    public void addParameter(String name, Object value) {
        this.parameters.put(name, value);
    }
    
    public Object getParameter(String name) {
        return this.parameters.get(name);
    }
    
    public String getTargetPackage() {
        return targetPackage;
    }
    
    public void setTargetPackage(String targetPackage) {
        this.targetPackage = targetPackage;
    }
    
    public long getSizeBytes() {
        return sizeBytes;
    }
    
    public void setSizeBytes(long sizeBytes) {
        this.sizeBytes = sizeBytes;
    }
    
    public List<String> getLearningSessions() {
        return learningSessions;
    }
    
    public void setLearningSessions(List<String> learningSessions) {
        this.learningSessions = learningSessions;
    }
    
    public void addLearningSession(String sessionId) {
        if (sessionId != null && !learningSessions.contains(sessionId)) {
            this.learningSessions.add(sessionId);
        }
    }
    
    /**
     * Update model timestamp and training progress
     */
    public void updateTraining(float progress) {
        this.trainingProgress = progress;
        this.lastUpdatedAt = System.currentTimeMillis();
        
        if (progress >= 1.0f) {
            this.ready = true;
        }
    }
    
    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        
        LearningModel model = (LearningModel) obj;
        return modelId.equals(model.modelId);
    }
    
    @Override
    public int hashCode() {
        return modelId.hashCode();
    }
}