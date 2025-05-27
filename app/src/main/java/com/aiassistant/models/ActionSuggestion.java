package com.aiassistant.models;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * Model class representing a suggested action from the AI system
 */
public class ActionSuggestion {
    private final String id;
    private final String title;
    private final String description;
    private final String actionType;
    private final Map<String, Object> parameters;
    private final float confidence;
    private final long timestamp;
    private final String sourceType;
    private final String sourceId;
    
    /**
     * Create a new ActionSuggestion
     * 
     * @param id Unique identifier for this suggestion
     * @param title Title of the suggestion
     * @param description Description of the suggestion
     * @param actionType Type of action being suggested
     * @param parameters Parameters for the action
     * @param confidence Confidence score (0.0-1.0)
     * @param timestamp Creation timestamp
     * @param sourceType Type of source that generated this suggestion (e.g. "learning", "rule")
     * @param sourceId Identifier of the source that generated this suggestion
     */
    public ActionSuggestion(
            @NonNull String id,
            @NonNull String title,
            @NonNull String description,
            @NonNull String actionType,
            @Nullable Map<String, Object> parameters,
            float confidence,
            long timestamp,
            @Nullable String sourceType,
            @Nullable String sourceId) {
        this.id = id;
        this.title = title;
        this.description = description;
        this.actionType = actionType;
        this.parameters = parameters != null ? new HashMap<>(parameters) : new HashMap<>();
        this.confidence = confidence;
        this.timestamp = timestamp;
        this.sourceType = sourceType != null ? sourceType : "unknown";
        this.sourceId = sourceId != null ? sourceId : "";
    }
    
    /**
     * Create a simple ActionSuggestion with default values
     * 
     * @param id Unique identifier for this suggestion
     * @param title Title of the suggestion
     * @param description Description of the suggestion
     * @param actionType Type of action being suggested
     */
    public ActionSuggestion(
            @NonNull String id,
            @NonNull String title,
            @NonNull String description,
            @NonNull String actionType) {
        this(id, title, description, actionType, null, 0.5f, 
                System.currentTimeMillis(), "manual", null);
    }

    @NonNull
    public String getId() {
        return id;
    }

    @NonNull
    public String getTitle() {
        return title;
    }

    @NonNull
    public String getDescription() {
        return description;
    }

    @NonNull
    public String getActionType() {
        return actionType;
    }

    @NonNull
    public Map<String, Object> getParameters() {
        return new HashMap<>(parameters);
    }

    public float getConfidence() {
        return confidence;
    }

    public long getTimestamp() {
        return timestamp;
    }

    @NonNull
    public String getSourceType() {
        return sourceType;
    }

    @NonNull
    public String getSourceId() {
        return sourceId;
    }
    
    /**
     * Get a specific parameter value
     * 
     * @param key Parameter key
     * @return Parameter value or null if not found
     */
    @Nullable
    public Object getParameter(String key) {
        return parameters.get(key);
    }
    
    /**
     * Create a new ActionSuggestion with updated confidence
     * 
     * @param newConfidence New confidence score
     * @return New ActionSuggestion instance
     */
    @NonNull
    public ActionSuggestion withConfidence(float newConfidence) {
        return new ActionSuggestion(
                id, title, description, actionType, parameters,
                newConfidence, timestamp, sourceType, sourceId);
    }
    
    /**
     * Create a new ActionSuggestion with updated parameters
     * 
     * @param newParameters New parameters
     * @return New ActionSuggestion instance
     */
    @NonNull
    public ActionSuggestion withParameters(@NonNull Map<String, Object> newParameters) {
        return new ActionSuggestion(
                id, title, description, actionType, newParameters,
                confidence, timestamp, sourceType, sourceId);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ActionSuggestion that = (ActionSuggestion) o;
        return Float.compare(that.confidence, confidence) == 0 &&
                timestamp == that.timestamp &&
                id.equals(that.id) &&
                title.equals(that.title) &&
                description.equals(that.description) &&
                actionType.equals(that.actionType) &&
                parameters.equals(that.parameters) &&
                sourceType.equals(that.sourceType) &&
                sourceId.equals(that.sourceId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, title, description, actionType, parameters,
                confidence, timestamp, sourceType, sourceId);
    }

    @Override
    public String toString() {
        return "ActionSuggestion{" +
                "id='" + id + '\'' +
                ", title='" + title + '\'' +
                ", actionType='" + actionType + '\'' +
                ", confidence=" + confidence +
                ", sourceType='" + sourceType + '\'' +
                '}';
    }
}