package com.aiassistant.models;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * Represents an action that can be performed in a game
 */
public class GameAction implements Serializable {
    
    private static final long serialVersionUID = 1L;
    
    public enum ActionType {
        TAP("tap"),
        SWIPE("swipe"),
        DRAG("drag"),
        MULTI_TAP("multi_tap"),
        WAIT("wait"),
        HOLD("hold"),
        GESTURE("gesture"),
        CUSTOM("custom");
        
        private final String value;
        
        ActionType(String value) {
            this.value = value;
        }
        
        public String getValue() {
            return value;
        }
        
        @Nullable
        public static ActionType fromValue(String value) {
            for (ActionType type : values()) {
                if (type.getValue().equals(value)) {
                    return type;
                }
            }
            return null;
        }
    }
    
    private String actionId;
    private ActionType actionType;
    private Map<String, Object> parameters;
    private float confidence;
    private long timestamp;
    private String gameType;
    private String context;
    
    /**
     * Default constructor for serialization
     */
    public GameAction() {
        this.parameters = new HashMap<>();
        this.confidence = 0.0f;
        this.timestamp = System.currentTimeMillis();
    }
    
    /**
     * Create a new game action
     * 
     * @param actionId Unique identifier for this action
     * @param actionType Type of action
     * @param parameters Action parameters
     */
    public GameAction(
            @NonNull String actionId,
            @NonNull ActionType actionType,
            @Nullable Map<String, Object> parameters) {
        this.actionId = actionId;
        this.actionType = actionType;
        this.parameters = parameters != null ? new HashMap<>(parameters) : new HashMap<>();
        this.confidence = 1.0f;
        this.timestamp = System.currentTimeMillis();
    }
    
    /**
     * Create a new game action with confidence
     * 
     * @param actionId Unique identifier for this action
     * @param actionType Type of action
     * @param parameters Action parameters
     * @param confidence Confidence level for this action (0.0-1.0)
     */
    public GameAction(
            @NonNull String actionId,
            @NonNull ActionType actionType,
            @Nullable Map<String, Object> parameters,
            float confidence) {
        this(actionId, actionType, parameters);
        this.confidence = confidence;
    }
    
    /**
     * Create a new game action with confidence and game type
     * 
     * @param actionId Unique identifier for this action
     * @param actionType Type of action
     * @param parameters Action parameters
     * @param confidence Confidence level for this action (0.0-1.0)
     * @param gameType Type of game
     */
    public GameAction(
            @NonNull String actionId,
            @NonNull ActionType actionType,
            @Nullable Map<String, Object> parameters,
            float confidence,
            @Nullable String gameType) {
        this(actionId, actionType, parameters, confidence);
        this.gameType = gameType;
    }
    
    /**
     * Create a new game action with all properties
     * 
     * @param actionId Unique identifier for this action
     * @param actionType Type of action
     * @param parameters Action parameters
     * @param confidence Confidence level for this action (0.0-1.0)
     * @param gameType Type of game
     * @param context Context for this action
     */
    public GameAction(
            @NonNull String actionId,
            @NonNull ActionType actionType,
            @Nullable Map<String, Object> parameters,
            float confidence,
            @Nullable String gameType,
            @Nullable String context) {
        this(actionId, actionType, parameters, confidence, gameType);
        this.context = context;
    }
    
    /**
     * Get the action ID
     * 
     * @return Action ID
     */
    @NonNull
    public String getActionId() {
        return actionId;
    }
    
    /**
     * Set the action ID
     * 
     * @param actionId Action ID
     */
    public void setActionId(@NonNull String actionId) {
        this.actionId = actionId;
    }
    
    /**
     * Get the action type
     * 
     * @return Action type
     */
    @NonNull
    public ActionType getActionType() {
        return actionType;
    }
    
    /**
     * Set the action type
     * 
     * @param actionType Action type
     */
    public void setActionType(@NonNull ActionType actionType) {
        this.actionType = actionType;
    }
    
    /**
     * Get the action parameters
     * 
     * @return Action parameters
     */
    @NonNull
    public Map<String, Object> getParameters() {
        return new HashMap<>(parameters);
    }
    
    /**
     * Set the action parameters
     * 
     * @param parameters Action parameters
     */
    public void setParameters(@Nullable Map<String, Object> parameters) {
        this.parameters = parameters != null ? new HashMap<>(parameters) : new HashMap<>();
    }
    
    /**
     * Get a specific parameter
     * 
     * @param key Parameter key
     * @return Parameter value or null if not found
     */
    @Nullable
    public Object getParameter(@NonNull String key) {
        return parameters.get(key);
    }
    
    /**
     * Add a parameter
     * 
     * @param key Parameter key
     * @param value Parameter value
     */
    public void addParameter(@NonNull String key, @Nullable Object value) {
        parameters.put(key, value);
    }
    
    /**
     * Get the confidence level
     * 
     * @return Confidence level (0.0-1.0)
     */
    public float getConfidence() {
        return confidence;
    }
    
    /**
     * Set the confidence level
     * 
     * @param confidence Confidence level (0.0-1.0)
     */
    public void setConfidence(float confidence) {
        this.confidence = confidence;
    }
    
    /**
     * Get the timestamp when this action was created
     * 
     * @return Timestamp in milliseconds
     */
    public long getTimestamp() {
        return timestamp;
    }
    
    /**
     * Set the timestamp
     * 
     * @param timestamp Timestamp in milliseconds
     */
    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }
    
    /**
     * Get the game type
     * 
     * @return Game type or null if not set
     */
    @Nullable
    public String getGameType() {
        return gameType;
    }
    
    /**
     * Set the game type
     * 
     * @param gameType Game type
     */
    public void setGameType(@Nullable String gameType) {
        this.gameType = gameType;
    }
    
    /**
     * Get the context
     * 
     * @return Context or null if not set
     */
    @Nullable
    public String getContext() {
        return context;
    }
    
    /**
     * Set the context
     * 
     * @param context Context
     */
    public void setContext(@Nullable String context) {
        this.context = context;
    }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        GameAction that = (GameAction) o;
        return Float.compare(that.confidence, confidence) == 0 &&
                timestamp == that.timestamp &&
                Objects.equals(actionId, that.actionId) &&
                actionType == that.actionType &&
                Objects.equals(parameters, that.parameters) &&
                Objects.equals(gameType, that.gameType) &&
                Objects.equals(context, that.context);
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(actionId, actionType, parameters, confidence, timestamp, gameType, context);
    }
    
    @Override
    public String toString() {
        return "GameAction{" +
                "actionId='" + actionId + '\'' +
                ", actionType=" + actionType +
                ", parameters=" + parameters +
                ", confidence=" + confidence +
                ", gameType='" + gameType + '\'' +
                ", context='" + context + '\'' +
                '}';
    }
    
    /**
     * Convert this action to a map
     * 
     * @return Map representation of this action
     */
    @NonNull
    public Map<String, Object> toMap() {
        Map<String, Object> map = new HashMap<>();
        map.put("actionId", actionId);
        map.put("actionType", actionType.getValue());
        map.put("parameters", parameters);
        map.put("confidence", confidence);
        map.put("timestamp", timestamp);
        if (gameType != null) {
            map.put("gameType", gameType);
        }
        if (context != null) {
            map.put("context", context);
        }
        return map;
    }
    
    /**
     * Create a game action from a map
     * 
     * @param map Map representing a game action
     * @return GameAction instance or null if map is invalid
     */
    @Nullable
    public static GameAction fromMap(@Nullable Map<String, Object> map) {
        if (map == null || !map.containsKey("actionId") || !map.containsKey("actionType")) {
            return null;
        }
        
        GameAction action = new GameAction();
        
        action.setActionId((String) map.get("actionId"));
        
        String actionTypeStr = (String) map.get("actionType");
        ActionType actionType = ActionType.fromValue(actionTypeStr);
        if (actionType != null) {
            action.setActionType(actionType);
        } else {
            action.setActionType(ActionType.CUSTOM);
        }
        
        if (map.containsKey("parameters") && map.get("parameters") instanceof Map) {
            @SuppressWarnings("unchecked")
            Map<String, Object> parameters = (Map<String, Object>) map.get("parameters");
            action.setParameters(parameters);
        }
        
        if (map.containsKey("confidence") && map.get("confidence") instanceof Number) {
            action.setConfidence(((Number) map.get("confidence")).floatValue());
        }
        
        if (map.containsKey("timestamp") && map.get("timestamp") instanceof Number) {
            action.setTimestamp(((Number) map.get("timestamp")).longValue());
        }
        
        if (map.containsKey("gameType") && map.get("gameType") instanceof String) {
            action.setGameType((String) map.get("gameType"));
        }
        
        if (map.containsKey("context") && map.get("context") instanceof String) {
            action.setContext((String) map.get("context"));
        }
        
        return action;
    }
}