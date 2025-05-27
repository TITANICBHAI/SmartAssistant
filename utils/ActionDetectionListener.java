package utils;

import java.util.List;

/**
 * Interface for listening to action detection events
 */
public interface ActionDetectionListener {
    /**
     * Called when actions are detected
     * 
     * @param actions List of detected actions
     * @param timestamp Timestamp when actions were detected
     */
    void onActionsDetected(List<VideoProcessor.DetectedAction> actions, long timestamp);
    
    /**
     * Called when a specific action type is detected
     * 
     * @param actionType Type of action detected
     * @param confidence Confidence score of the detection
     * @param timestamp Timestamp when action was detected
     */
    void onActionTypeDetected(String actionType, float confidence, long timestamp);
    
    /**
     * Called when action detection fails
     * 
     * @param errorMessage Error message
     * @param timestamp Timestamp when error occurred
     */
    default void onActionDetectionError(String errorMessage, long timestamp) {
        // Default implementation does nothing
    }
}