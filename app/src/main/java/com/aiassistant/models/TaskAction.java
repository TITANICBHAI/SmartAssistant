package com.aiassistant.models;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Task Action
 * Represents an action to be performed by a scheduled task
 */
public class TaskAction {
    // Action ID (unique identifier)
    private String actionId;
    
    // Action type (can be ActionType enum or raw string)
    private String actionType;
    
    // Action parameters
    private Map<String, Object> parameters;
    
    // Target element ID (for UI interaction actions)
    private String targetElementId;
    
    // Action description
    private String description;
    
    // Timeout in milliseconds
    private long timeoutMs;
    
    // Whether the action has been executed
    private boolean executed;
    
    // Execution result
    private boolean success;
    
    // Error message (if any)
    private String errorMessage;
    
    // Last execution timestamp
    private long lastExecutedAt;
    
    // Execution duration in milliseconds
    private long executionDurationMs;
    
    /**
     * Create a new task action using the enum type
     */
    public TaskAction(ActionType actionType) {
        this.actionId = UUID.randomUUID().toString();
        this.actionType = actionType.toString();
        this.parameters = new HashMap<>();
        this.timeoutMs = 5000; // Default timeout: 5 seconds
        this.executed = false;
        this.success = false;
    }
    
    /**
     * Create a new task action using string type
     */
    public TaskAction(String actionId, String actionType) {
        this.actionId = actionId;
        this.actionType = actionType;
        this.parameters = new HashMap<>();
        this.timeoutMs = 5000; // Default timeout: 5 seconds
        this.executed = false;
        this.success = false;
    }
    
    // Getters and setters
    public String getActionId() {
        return actionId;
    }
    
    public String getActionType() {
        return actionType;
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
        return parameters.get(key);
    }
    
    public String getTargetElementId() {
        return targetElementId;
    }
    
    public void setTargetElementId(String targetElementId) {
        this.targetElementId = targetElementId;
    }
    
    public String getDescription() {
        return description;
    }
    
    public void setDescription(String description) {
        this.description = description;
    }
    
    public long getTimeoutMs() {
        return timeoutMs;
    }
    
    public void setTimeoutMs(long timeoutMs) {
        this.timeoutMs = timeoutMs;
    }
    
    public boolean isExecuted() {
        return executed;
    }
    
    public void setExecuted(boolean executed) {
        this.executed = executed;
    }
    
    public boolean isSuccess() {
        return success;
    }
    
    public void setSuccess(boolean success) {
        this.success = success;
    }
    
    public String getErrorMessage() {
        return errorMessage;
    }
    
    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }
    
    public long getLastExecutedAt() {
        return lastExecutedAt;
    }
    
    public void setLastExecutedAt(long lastExecutedAt) {
        this.lastExecutedAt = lastExecutedAt;
    }
    
    public long getExecutionDurationMs() {
        return executionDurationMs;
    }
    
    public void setExecutionDurationMs(long executionDurationMs) {
        this.executionDurationMs = executionDurationMs;
    }
    
    /**
     * Record execution result
     */
    public void recordExecution(boolean success, String errorMessage, long durationMs) {
        this.executed = true;
        this.success = success;
        this.errorMessage = errorMessage;
        this.lastExecutedAt = System.currentTimeMillis();
        this.executionDurationMs = durationMs;
    }
    
    /**
     * Reset execution state
     */
    public void resetExecution() {
        this.executed = false;
        this.success = false;
        this.errorMessage = null;
        this.executionDurationMs = 0;
    }
    
    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        
        TaskAction action = (TaskAction) obj;
        return actionId.equals(action.actionId);
    }
    
    @Override
    public int hashCode() {
        return actionId.hashCode();
    }
}