package com.aiassistant.learning;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Represents a sequence pattern discovered by the learning system.
 * This class stores information about action sequences the AI has learned.
 */
public class SequencePattern {
    
    private String id;
    private String name;
    private String description;
    private List<Map<String, Object>> actions;
    private Map<String, Object> contextConditions;
    private float confidence;
    private int observationCount;
    private int successCount;
    private Date firstObserved;
    private Date lastObserved;
    private String source;
    private float utilityScore;
    private boolean enabled;
    
    /**
     * Default constructor
     */
    public SequencePattern() {
        this.id = UUID.randomUUID().toString();
        this.actions = new ArrayList<>();
        this.contextConditions = new HashMap<>();
        this.firstObserved = new Date();
        this.lastObserved = new Date();
        this.confidence = 0.0f;
        this.observationCount = 0;
        this.successCount = 0;
        this.utilityScore = 0.0f;
        this.enabled = true;
    }
    
    /**
     * Constructor with name and description
     * 
     * @param name Pattern name
     * @param description Pattern description
     */
    public SequencePattern(String name, String description) {
        this();
        this.name = name;
        this.description = description;
    }
    
    /**
     * Get pattern ID
     * 
     * @return Pattern ID
     */
    public String getId() {
        return id;
    }
    
    /**
     * Set pattern ID
     * 
     * @param id Pattern ID
     */
    public void setId(String id) {
        this.id = id;
    }
    
    /**
     * Get pattern name
     * 
     * @return Pattern name
     */
    public String getName() {
        return name;
    }
    
    /**
     * Set pattern name
     * 
     * @param name Pattern name
     */
    public void setName(String name) {
        this.name = name;
    }
    
    /**
     * Get pattern description
     * 
     * @return Pattern description
     */
    public String getDescription() {
        return description;
    }
    
    /**
     * Set pattern description
     * 
     * @param description Pattern description
     */
    public void setDescription(String description) {
        this.description = description;
    }
    
    /**
     * Get actions in this sequence
     * 
     * @return List of actions
     */
    public List<Map<String, Object>> getActions() {
        return actions;
    }
    
    /**
     * Set actions in this sequence
     * 
     * @param actions List of actions
     */
    public void setActions(List<Map<String, Object>> actions) {
        this.actions = actions;
    }
    
    /**
     * Add an action to this sequence
     * 
     * @param action Action to add
     */
    public void addAction(Map<String, Object> action) {
        if (this.actions == null) {
            this.actions = new ArrayList<>();
        }
        this.actions.add(action);
    }
    
    /**
     * Get context conditions
     * 
     * @return Context conditions
     */
    public Map<String, Object> getContextConditions() {
        return contextConditions;
    }
    
    /**
     * Set context conditions
     * 
     * @param contextConditions Context conditions
     */
    public void setContextConditions(Map<String, Object> contextConditions) {
        this.contextConditions = contextConditions;
    }
    
    /**
     * Get confidence score
     * 
     * @return Confidence score
     */
    public float getConfidence() {
        return confidence;
    }
    
    /**
     * Set confidence score
     * 
     * @param confidence Confidence score
     */
    public void setConfidence(float confidence) {
        this.confidence = confidence;
    }
    
    /**
     * Get observation count
     * 
     * @return Observation count
     */
    public int getObservationCount() {
        return observationCount;
    }
    
    /**
     * Set observation count
     * 
     * @param observationCount Observation count
     */
    public void setObservationCount(int observationCount) {
        this.observationCount = observationCount;
    }
    
    /**
     * Increment observation count
     */
    public void incrementObservationCount() {
        this.observationCount++;
    }
    
    /**
     * Get success count
     * 
     * @return Success count
     */
    public int getSuccessCount() {
        return successCount;
    }
    
    /**
     * Set success count
     * 
     * @param successCount Success count
     */
    public void setSuccessCount(int successCount) {
        this.successCount = successCount;
    }
    
    /**
     * Increment success count
     */
    public void incrementSuccessCount() {
        this.successCount++;
    }
    
    /**
     * Get first observed date
     * 
     * @return First observed date
     */
    public Date getFirstObserved() {
        return firstObserved;
    }
    
    /**
     * Set first observed date
     * 
     * @param firstObserved First observed date
     */
    public void setFirstObserved(Date firstObserved) {
        this.firstObserved = firstObserved;
    }
    
    /**
     * Get last observed date
     * 
     * @return Last observed date
     */
    public Date getLastObserved() {
        return lastObserved;
    }
    
    /**
     * Set last observed date
     * 
     * @param lastObserved Last observed date
     */
    public void setLastObserved(Date lastObserved) {
        this.lastObserved = lastObserved;
    }
    
    /**
     * Update last observed date to now
     */
    public void updateLastObserved() {
        this.lastObserved = new Date();
    }
    
    /**
     * Get pattern source
     * 
     * @return Pattern source
     */
    public String getSource() {
        return source;
    }
    
    /**
     * Set pattern source
     * 
     * @param source Pattern source
     */
    public void setSource(String source) {
        this.source = source;
    }
    
    /**
     * Get utility score
     * 
     * @return Utility score
     */
    public float getUtilityScore() {
        return utilityScore;
    }
    
    /**
     * Set utility score
     * 
     * @param utilityScore Utility score
     */
    public void setUtilityScore(float utilityScore) {
        this.utilityScore = utilityScore;
    }
    
    /**
     * Check if pattern is enabled
     * 
     * @return true if enabled
     */
    public boolean isEnabled() {
        return enabled;
    }
    
    /**
     * Set enabled status
     * 
     * @param enabled Enabled status
     */
    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }
    
    /**
     * Calculate success rate of this pattern
     * 
     * @return Success rate (0.0-1.0)
     */
    public float getSuccessRate() {
        if (observationCount == 0) {
            return 0.0f;
        }
        return (float) successCount / (float) observationCount;
    }
}