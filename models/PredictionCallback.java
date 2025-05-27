package models;

import java.util.Map;

/**
 * Interface for receiving prediction callbacks
 * This was moved from PredictiveActionSystem.PredictionCallback to fix class naming issues
 */
public interface PredictionCallback {
    /**
     * Called when a prediction is available
     * 
     * @param prediction The prediction
     */
    void onPredictionAvailable(ActionPrediction prediction);
    
    /**
     * Called when a prediction fails
     * 
     * @param errorMessage The error message
     */
    void onPredictionFailed(String errorMessage);
    
    /**
     * Called when a prediction is generated with context data
     * 
     * @param action The predicted action
     * @param context The context data associated with the prediction
     */
    default void onPredictionGenerated(ActionPrediction action, Map<String, Object> context) {
        // Default implementation just calls the simpler method
        onPredictionAvailable(action);
    }
}