package com.aiassistant.models;

import android.graphics.Rect;

import java.util.HashMap;
import java.util.Map;

/**
 * Standardized Game Action
 * This class represents an action that can be performed in a game
 * Consolidates PredictiveActionSystem.GameAction and other game action implementations
 */
public class StandardizedGameAction {

    /**
     * Game action types
     */
    public enum ActionType {
        TAP("tap"),
        SWIPE("swipe"),
        HOLD("hold"),
        DRAG("drag"),
        MULTI_TAP("multi_tap"),
        ROTATE("rotate"),
        PINCH("pinch"),
        SPREAD("spread"),
        TEXT_INPUT("text_input"),
        WAIT("wait"),
        CUSTOM("custom");
        
        private final String value;
        
        ActionType(String value) {
            this.value = value;
        }
        
        public String getValue() {
            return value;
        }
        
        public static ActionType fromString(String text) {
            for (ActionType type : ActionType.values()) {
                if (type.value.equalsIgnoreCase(text)) {
                    return type;
                }
            }
            return CUSTOM;
        }
    }
    
    private String id;
    private ActionType actionType;
    private Rect targetArea;
    private Map<String, Object> parameters;
    private float priority;
    private float confidence;
    private long timestamp;
    private String description;
    private boolean isBatch;
    private StandardizedGameAction[] batchActions;
    
    /**
     * Basic constructor
     * 
     * @param id Action ID
     * @param actionType Action type
     */
    public StandardizedGameAction(String id, ActionType actionType) {
        this.id = id;
        this.actionType = actionType;
        this.parameters = new HashMap<>();
        this.priority = 1.0f;
        this.confidence = 1.0f;
        this.timestamp = System.currentTimeMillis();
        this.isBatch = false;
    }
    
    /**
     * Constructor with target area
     * 
     * @param id Action ID
     * @param actionType Action type
     * @param targetArea Target area for the action
     */
    public StandardizedGameAction(String id, ActionType actionType, Rect targetArea) {
        this(id, actionType);
        this.targetArea = targetArea;
    }
    
    /**
     * Full constructor
     * 
     * @param id Action ID
     * @param actionType Action type
     * @param targetArea Target area for the action
     * @param parameters Action parameters
     * @param priority Action priority
     * @param confidence Confidence in this action
     * @param description Action description
     */
    public StandardizedGameAction(
            String id, 
            ActionType actionType, 
            Rect targetArea,
            Map<String, Object> parameters,
            float priority,
            float confidence,
            String description) {
        
        this.id = id;
        this.actionType = actionType;
        this.targetArea = targetArea;
        this.parameters = parameters != null ? new HashMap<>(parameters) : new HashMap<>();
        this.priority = priority;
        this.confidence = confidence;
        this.timestamp = System.currentTimeMillis();
        this.description = description;
        this.isBatch = false;
    }
    
    /**
     * Create a batch action
     * 
     * @param id Batch ID
     * @param actions Actions in the batch
     * @return Batch action
     */
    public static StandardizedGameAction createBatch(String id, StandardizedGameAction[] actions) {
        StandardizedGameAction batch = new StandardizedGameAction(id, ActionType.CUSTOM);
        batch.isBatch = true;
        batch.batchActions = actions;
        return batch;
    }
    
    /**
     * Get action ID
     * 
     * @return Action ID
     */
    public String getId() {
        return id;
    }
    
    /**
     * Set action ID
     * 
     * @param id Action ID
     */
    public void setId(String id) {
        this.id = id;
    }
    
    /**
     * Get action type
     * 
     * @return Action type
     */
    public ActionType getActionType() {
        return actionType;
    }
    
    /**
     * Set action type
     * 
     * @param actionType Action type
     */
    public void setActionType(ActionType actionType) {
        this.actionType = actionType;
    }
    
    /**
     * Get target area
     * 
     * @return Target area
     */
    public Rect getTargetArea() {
        return targetArea;
    }
    
    /**
     * Set target area
     * 
     * @param targetArea Target area
     */
    public void setTargetArea(Rect targetArea) {
        this.targetArea = targetArea;
    }
    
    /**
     * Get parameters
     * 
     * @return Parameters
     */
    public Map<String, Object> getParameters() {
        return parameters;
    }
    
    /**
     * Set parameters
     * 
     * @param parameters Parameters
     */
    public void setParameters(Map<String, Object> parameters) {
        this.parameters = parameters != null ? new HashMap<>(parameters) : new HashMap<>();
    }
    
    /**
     * Get parameter
     * 
     * @param key Parameter key
     * @return Parameter value
     */
    public Object getParameter(String key) {
        return parameters.get(key);
    }
    
    /**
     * Set parameter
     * 
     * @param key Parameter key
     * @param value Parameter value
     */
    public void setParameter(String key, Object value) {
        parameters.put(key, value);
    }
    
    /**
     * Get priority
     * 
     * @return Priority
     */
    public float getPriority() {
        return priority;
    }
    
    /**
     * Set priority
     * 
     * @param priority Priority
     */
    public void setPriority(float priority) {
        this.priority = priority;
    }
    
    /**
     * Get confidence
     * 
     * @return Confidence
     */
    public float getConfidence() {
        return confidence;
    }
    
    /**
     * Set confidence
     * 
     * @param confidence Confidence
     */
    public void setConfidence(float confidence) {
        this.confidence = confidence;
    }
    
    /**
     * Get timestamp
     * 
     * @return Timestamp
     */
    public long getTimestamp() {
        return timestamp;
    }
    
    /**
     * Set timestamp
     * 
     * @param timestamp Timestamp
     */
    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }
    
    /**
     * Get description
     * 
     * @return Description
     */
    public String getDescription() {
        return description;
    }
    
    /**
     * Set description
     * 
     * @param description Description
     */
    public void setDescription(String description) {
        this.description = description;
    }
    
    /**
     * Check if this is a batch action
     * 
     * @return True if batch action
     */
    public boolean isBatch() {
        return isBatch;
    }
    
    /**
     * Get batch actions
     * 
     * @return Batch actions
     */
    public StandardizedGameAction[] getBatchActions() {
        return batchActions;
    }
    
    /**
     * Set batch actions
     * 
     * @param batchActions Batch actions
     */
    public void setBatchActions(StandardizedGameAction[] batchActions) {
        this.batchActions = batchActions;
        this.isBatch = batchActions != null && batchActions.length > 0;
    }
    
    /**
     * Get center X coordinate of target area
     * 
     * @return Center X
     */
    public int getCenterX() {
        return targetArea != null ? targetArea.centerX() : 0;
    }
    
    /**
     * Get center Y coordinate of target area
     * 
     * @return Center Y
     */
    public int getCenterY() {
        return targetArea != null ? targetArea.centerY() : 0;
    }
    
    /**
     * Generate a description for this action if not already set
     * 
     * @return Description of the action
     */
    public String generateDescription() {
        if (description != null && !description.isEmpty()) {
            return description;
        }
        
        StringBuilder builder = new StringBuilder();
        builder.append(actionType.name().toLowerCase());
        
        if (targetArea != null) {
            builder.append(" at (").append(targetArea.centerX()).append(",").append(targetArea.centerY()).append(")");
        }
        
        if (parameters.containsKey("duration")) {
            builder.append(" for ").append(parameters.get("duration")).append("ms");
        }
        
        if (parameters.containsKey("text")) {
            builder.append(" '").append(parameters.get("text")).append("'");
        }
        
        if (parameters.containsKey("direction")) {
            builder.append(" ").append(parameters.get("direction"));
        }
        
        return builder.toString();
    }
    
    @Override
    public String toString() {
        return "GameAction{" +
                "id='" + id + '\'' +
                ", type=" + actionType +
                ", targetArea=" + (targetArea != null ? targetArea.toShortString() : "null") +
                ", priority=" + priority +
                ", confidence=" + confidence +
                '}';
    }
}