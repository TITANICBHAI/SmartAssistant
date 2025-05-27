package models;

/**
 * Interface for receiving game prediction callbacks
 * This was moved from PredictiveActionSystem.PredictionCallback to fix class naming issues
 */
public interface GamePredictionCallback {
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
}