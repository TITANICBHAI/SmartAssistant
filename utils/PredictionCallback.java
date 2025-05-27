package utils;

import java.util.List;
import java.util.Map;

/**
 * Interface for callbacks that handle prediction results
 */
public interface PredictionCallback {
    /**
     * Called when a prediction is successful
     * 
     * @param predictionType Type of prediction that was made
     * @param results Prediction results
     */
    void onPredictionSuccess(String predictionType, Map<String, Object> results);
    
    /**
     * Called when a prediction fails
     * 
     * @param predictionType Type of prediction that was attempted
     * @param errorCode Error code indicating the reason for failure
     * @param errorMessage Detailed error message
     */
    void onPredictionFailure(String predictionType, int errorCode, String errorMessage);
    
    /**
     * Called with progress updates during long-running predictions
     * 
     * @param predictionType Type of prediction in progress
     * @param progress Progress value (0-100)
     * @param status Status message
     */
    void onPredictionProgress(String predictionType, int progress, String status);
    
    /**
     * Called with intermediate results during multi-stage predictions
     * 
     * @param predictionType Type of prediction in progress
     * @param intermediateResults Partial results
     * @param stage Current stage of the prediction
     */
    void onIntermediateResults(String predictionType, Map<String, Object> intermediateResults, int stage);
}