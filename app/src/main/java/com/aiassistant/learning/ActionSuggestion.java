package com.aiassistant.learning;

import androidx.annotation.NonNull;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

/**
 * Represents an action suggestion based on learned patterns
 */
public class ActionSuggestion implements Serializable, Comparable<ActionSuggestion> {
    private static final long serialVersionUID = 1L;
    
    private final String actionType;
    private final Map<String, Object> parameters;
    private final float confidence;
    private final long lastUsed;
    private final float successRate;
    
    /**
     * Create a new action suggestion
     * 
     * @param actionType Type of action being suggested
     * @param parameters Parameters for the action
     * @param confidence Confidence level (0.0-1.0)
     * @param lastUsed Timestamp when the pattern was last used
     * @param successRate Success rate of this action (0.0-1.0)
     */
    public ActionSuggestion(
            String actionType,
            Map<String, Object> parameters,
            float confidence,
            long lastUsed,
            float successRate) {
        this.actionType = actionType;
        this.parameters = parameters != null ? new HashMap<>(parameters) : new HashMap<>();
        this.confidence = Math.max(0.0f, Math.min(1.0f, confidence));
        this.lastUsed = lastUsed;
        this.successRate = Math.max(0.0f, Math.min(1.0f, successRate));
    }
    
    /**
     * Get the action type
     * 
     * @return The action type
     */
    public String getActionType() {
        return actionType;
    }
    
    /**
     * Get the action parameters
     * 
     * @return Map of parameter name to value
     */
    public Map<String, Object> getParameters() {
        return new HashMap<>(parameters);
    }
    
    /**
     * Get the confidence level for this suggestion
     * 
     * @return Confidence (0.0-1.0)
     */
    public float getConfidence() {
        return confidence;
    }
    
    /**
     * Get the timestamp when this pattern was last used
     * 
     * @return Last used timestamp
     */
    public long getLastUsed() {
        return lastUsed;
    }
    
    /**
     * Get the success rate for this action
     * 
     * @return Success rate (0.0-1.0)
     */
    public float getSuccessRate() {
        return successRate;
    }
    
    /**
     * Calculate a combined score based on confidence, recency, and success rate
     * 
     * @return Combined score
     */
    public float getCombinedScore() {
        // Calculate recency score (diminishing with age)
        long now = System.currentTimeMillis();
        long ageMs = now - lastUsed;
        float recencyScore = (float) Math.exp(-ageMs / (7 * 24 * 60 * 60 * 1000.0)); // Decay over a week
        
        // Combined score with weights
        return (0.6f * confidence) + (0.2f * recencyScore) + (0.2f * successRate);
    }
    
    @Override
    public int compareTo(@NonNull ActionSuggestion other) {
        // Compare by combined score, higher is better
        return Float.compare(other.getCombinedScore(), this.getCombinedScore());
    }
    
    @Override
    public String toString() {
        return "ActionSuggestion{" +
                "actionType='" + actionType + '\'' +
                ", confidence=" + confidence +
                ", successRate=" + successRate +
                ", parameterCount=" + parameters.size() +
                '}';
    }
}