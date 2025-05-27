package com.aiassistant.models;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

/**
 * Represents an action that can be executed by the AI assistant
 */
public class Action implements Serializable {
    private static final long serialVersionUID = 1L;
    
    private ActionType type;
    private Map<String, Object> parameters;
    
    /**
     * Create a new action with given type and parameters
     * 
     * @param type The type of action
     * @param parameters Parameters for the action
     */
    public Action(ActionType type, Map<String, Object> parameters) {
        this.type = type;
        this.parameters = parameters != null ? parameters : new HashMap<>();
    }
    
    /**
     * Create a new action with given type and no parameters
     * 
     * @param type The type of action
     */
    public Action(ActionType type) {
        this(type, new HashMap<>());
    }
    
    /**
     * Get the action type
     * 
     * @return The action type
     */
    public ActionType getType() {
        return type;
    }
    
    /**
     * Set the action type
     * 
     * @param type The action type
     */
    public void setType(ActionType type) {
        this.type = type;
    }
    
    /**
     * Get the action parameters
     * 
     * @return Map of parameters
     */
    public Map<String, Object> getParameters() {
        return parameters;
    }
    
    /**
     * Set the action parameters
     * 
     * @param parameters Map of parameters
     */
    public void setParameters(Map<String, Object> parameters) {
        this.parameters = parameters != null ? parameters : new HashMap<>();
    }
    
    /**
     * Add a parameter to this action
     * 
     * @param key Parameter key
     * @param value Parameter value
     */
    public void addParameter(String key, Object value) {
        if (parameters == null) {
            parameters = new HashMap<>();
        }
        parameters.put(key, value);
    }
    
    /**
     * Get a parameter value by key
     * 
     * @param key Parameter key
     * @return Parameter value or null if not found
     */
    public Object getParameter(String key) {
        return parameters != null ? parameters.get(key) : null;
    }
    
    /**
     * Get a parameter value as a specific type
     * 
     * @param key Parameter key
     * @param defaultValue Default value if parameter not found or wrong type
     * @param <T> Type to cast to
     * @return Parameter value or default if not found or wrong type
     */
    @SuppressWarnings("unchecked")
    public <T> T getParameterAs(String key, T defaultValue) {
        if (parameters == null) return defaultValue;
        
        Object value = parameters.get(key);
        if (value == null) return defaultValue;
        
        try {
            return (T) value;
        } catch (ClassCastException e) {
            return defaultValue;
        }
    }
    
    /**
     * Check if this action has a specific parameter
     * 
     * @param key Parameter key
     * @return True if parameter exists
     */
    public boolean hasParameter(String key) {
        return parameters != null && parameters.containsKey(key);
    }
    
    @Override
    public String toString() {
        return "Action{" +
                "type=" + type +
                ", parameters=" + parameters +
                '}';
    }
}