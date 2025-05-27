package com.aiassistant.models;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

/**
 * Result of an action execution
 */
public class ActionResult implements Serializable {
    private static final long serialVersionUID = 1L;
    
    private boolean success;
    private String message;
    private String error;
    private Map<String, Object> data;
    private long executionTimeMs;
    
    /**
     * Create a new action result
     * 
     * @param success Whether the action was successful
     * @param message Result message
     */
    public ActionResult(boolean success, String message) {
        this.success = success;
        this.message = message;
        this.error = success ? null : message;
        this.data = new HashMap<>();
        this.executionTimeMs = 0;
    }
    
    /**
     * Create a new successful action result
     * 
     * @param message Success message
     * @return Action result instance
     */
    public static ActionResult success(String message) {
        return new ActionResult(true, message);
    }
    
    /**
     * Create a new failed action result
     * 
     * @param error Error message
     * @return Action result instance
     */
    public static ActionResult failure(String error) {
        return new ActionResult(false, error);
    }
    
    /**
     * Check if the action was successful
     * 
     * @return True if successful
     */
    public boolean isSuccess() {
        return success;
    }
    
    /**
     * Set whether the action was successful
     * 
     * @param success True if successful
     */
    public void setSuccess(boolean success) {
        this.success = success;
    }
    
    /**
     * Get the result message
     * 
     * @return Result message
     */
    public String getMessage() {
        return message;
    }
    
    /**
     * Set the result message
     * 
     * @param message Result message
     */
    public void setMessage(String message) {
        this.message = message;
    }
    
    /**
     * Get the error message if the action failed
     * 
     * @return Error message or null if successful
     */
    public String getError() {
        return error;
    }
    
    /**
     * Set the error message
     * 
     * @param error Error message
     */
    public void setError(String error) {
        this.error = error;
        if (error != null && !error.isEmpty()) {
            this.success = false;
        }
    }
    
    /**
     * Get the result data
     * 
     * @return Map of result data
     */
    public Map<String, Object> getData() {
        return data;
    }
    
    /**
     * Set the result data
     * 
     * @param data Map of result data
     */
    public void setData(Map<String, Object> data) {
        this.data = data != null ? data : new HashMap<>();
    }
    
    /**
     * Add data to the result
     * 
     * @param key Data key
     * @param value Data value
     */
    public void addData(String key, Object value) {
        if (data == null) {
            data = new HashMap<>();
        }
        data.put(key, value);
    }
    
    /**
     * Get a data value
     * 
     * @param key Data key
     * @return Data value or null if not found
     */
    public Object getData(String key) {
        return data != null ? data.get(key) : null;
    }
    
    /**
     * Get a data value as a specific type
     * 
     * @param key Data key
     * @param defaultValue Default value if not found or wrong type
     * @param <T> Type to cast to
     * @return Data value or default if not found or wrong type
     */
    @SuppressWarnings("unchecked")
    public <T> T getDataAs(String key, T defaultValue) {
        if (data == null) return defaultValue;
        
        Object value = data.get(key);
        if (value == null) return defaultValue;
        
        try {
            return (T) value;
        } catch (ClassCastException e) {
            return defaultValue;
        }
    }
    
    /**
     * Get the action execution time in milliseconds
     * 
     * @return Execution time in ms
     */
    public long getExecutionTimeMs() {
        return executionTimeMs;
    }
    
    /**
     * Set the action execution time in milliseconds
     * 
     * @param executionTimeMs Execution time in ms
     */
    public void setExecutionTimeMs(long executionTimeMs) {
        this.executionTimeMs = executionTimeMs;
    }
    
    @Override
    public String toString() {
        return "ActionResult{" +
                "success=" + success +
                ", message='" + message + '\'' +
                ", error='" + error + '\'' +
                ", data=" + data +
                ", executionTimeMs=" + executionTimeMs +
                '}';
    }
}