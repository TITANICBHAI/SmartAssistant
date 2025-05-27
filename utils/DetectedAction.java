package utils;

import java.util.HashMap;
import java.util.Map;

/**
 * Represents an action that has been detected in a video or game context
 */
public class DetectedAction {
    private String actionType;
    private float confidence;
    private long timestamp;
    private Map<String, Object> data;
    
    /**
     * Create a new detected action
     * 
     * @param actionType the type of action detected
     * @param confidence the confidence level (0.0-1.0)
     * @param timestamp the time when the action was detected
     */
    public DetectedAction(String actionType, float confidence, long timestamp) {
        this.actionType = actionType;
        this.confidence = confidence;
        this.timestamp = timestamp;
        this.data = new HashMap<>();
    }
    
    /**
     * Get the type of action detected
     * 
     * @return the action type
     */
    public String getActionType() {
        return actionType;
    }
    
    /**
     * Get the confidence level for the detection
     * 
     * @return the confidence (0.0-1.0)
     */
    public float getConfidence() {
        return confidence;
    }
    
    /**
     * Get the timestamp when the action was detected
     * 
     * @return the timestamp
     */
    public long getTimestamp() {
        return timestamp;
    }
    
    /**
     * Get additional data associated with the detected action
     * 
     * @return a map of data properties
     */
    public Map<String, Object> getData() {
        return data;
    }
    
    /**
     * Add additional data to the detected action
     * 
     * @param key the data key
     * @param value the data value
     */
    public void addData(String key, Object value) {
        this.data.put(key, value);
    }
}