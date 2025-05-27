package utils;

import java.util.HashMap;
import java.util.Map;

/**
 * Helper class to provide compatibility between different prediction callback interfaces
 */
public class PredictionCallbackCompatHelper {

    /**
     * Convert a generic PredictionCallback to a compatible callback for other systems
     * 
     * @param callback Original PredictionCallback
     * @return Compatible callback object or original if conversion not needed
     */
    public static Object convertCallback(PredictionCallback callback) {
        if (callback == null) {
            return null;
        }
        
        // Return a wrapped version of the callback that adapts between interfaces
        return new CompatibleCallback(callback);
    }
    
    /**
     * Adapter that wraps a standard PredictionCallback
     */
    private static class CompatibleCallback implements PredictionCallback {
        private final PredictionCallback originalCallback;
        
        public CompatibleCallback(PredictionCallback callback) {
            this.originalCallback = callback;
        }
        
        @Override
        public void onPredictionSuccess(String predictionType, Map<String, Object> results) {
            originalCallback.onPredictionSuccess(predictionType, results);
        }
        
        @Override
        public void onPredictionFailure(String predictionType, int errorCode, String errorMessage) {
            originalCallback.onPredictionFailure(predictionType, errorCode, errorMessage);
        }
        
        @Override
        public void onPredictionProgress(String predictionType, int progress, String status) {
            originalCallback.onPredictionProgress(predictionType, progress, status);
        }
        
        @Override
        public void onIntermediateResults(String predictionType, Map<String, Object> intermediateResults, int stage) {
            originalCallback.onIntermediateResults(predictionType, intermediateResults, stage);
        }
    }
    
    /**
     * Forward a prediction result to the appropriate callback method
     * 
     * @param callback Callback to notify
     * @param predictionType Type of prediction
     * @param results Results data
     * @param success Whether the prediction was successful
     * @param errorCode Error code if not successful
     * @param errorMessage Error message if not successful
     */
    public static void forwardPredictionResult(PredictionCallback callback, String predictionType, 
                                               Map<String, Object> results, boolean success,
                                               int errorCode, String errorMessage) {
        if (callback == null) {
            return;
        }
        
        if (success) {
            callback.onPredictionSuccess(predictionType, results);
        } else {
            callback.onPredictionFailure(predictionType, errorCode, errorMessage);
        }
    }
    
    /**
     * Convert a prediction result map from one format to another
     * 
     * @param originalResults Original results map
     * @param targetFormat Target format identifier
     * @return Converted results map
     */
    public static Map<String, Object> convertResultFormat(Map<String, Object> originalResults, String targetFormat) {
        if (originalResults == null) {
            return null;
        }
        
        // If the target format matches the original format, no conversion needed
        if (originalResults.containsKey("format") && 
            targetFormat.equals(originalResults.get("format"))) {
            return originalResults;
        }
        
        // Create a new map for the converted results
        Map<String, Object> convertedResults = new HashMap<>();
        
        // Add the format identifier
        convertedResults.put("format", targetFormat);
        
        // Perform format-specific conversions
        switch (targetFormat) {
            case "standard":
                // Convert to standard format
                convertToStandardFormat(originalResults, convertedResults);
                break;
            case "android":
                // Convert to Android-specific format
                convertToAndroidFormat(originalResults, convertedResults);
                break;
            case "ios":
                // Convert to iOS-specific format
                convertToIOSFormat(originalResults, convertedResults);
                break;
            case "web":
                // Convert to web-specific format
                convertToWebFormat(originalResults, convertedResults);
                break;
            default:
                // Unknown format, just copy the original data
                convertedResults.putAll(originalResults);
                break;
        }
        
        return convertedResults;
    }
    
    /**
     * Convert results to the standard format
     * 
     * @param source Source results
     * @param target Target results map
     */
    private static void convertToStandardFormat(Map<String, Object> source, Map<String, Object> target) {
        // Copy standard fields directly
        copyIfPresent(source, target, "type");
        copyIfPresent(source, target, "confidence");
        copyIfPresent(source, target, "timestamp");
        copyIfPresent(source, target, "duration");
        
        // Copy results with standard key names
        if (source.containsKey("result")) {
            target.put("result", source.get("result"));
        } else if (source.containsKey("results")) {
            target.put("result", source.get("results"));
        } else if (source.containsKey("prediction")) {
            target.put("result", source.get("prediction"));
        } else if (source.containsKey("predictions")) {
            target.put("result", source.get("predictions"));
        }
        
        // Copy metadata if present
        copyIfPresent(source, target, "metadata");
    }
    
    /**
     * Convert results to Android format
     * 
     * @param source Source results
     * @param target Target results map
     */
    private static void convertToAndroidFormat(Map<String, Object> source, Map<String, Object> target) {
        // Android-specific field names
        copyIfPresent(source, target, "type", "predictorType");
        copyIfPresent(source, target, "confidence", "confidenceScore");
        copyIfPresent(source, target, "timestamp", "predictionTimestamp");
        copyIfPresent(source, target, "duration", "processingTimeMs");
        
        // Copy results with Android-specific key names
        if (source.containsKey("result")) {
            target.put("predictions", source.get("result"));
        } else if (source.containsKey("results")) {
            target.put("predictions", source.get("results"));
        } else if (source.containsKey("prediction")) {
            target.put("predictions", source.get("prediction"));
        } else if (source.containsKey("predictions")) {
            target.put("predictions", source.get("predictions"));
        }
        
        // Copy metadata if present, or create empty map
        if (source.containsKey("metadata")) {
            target.put("extraData", source.get("metadata"));
        } else {
            target.put("extraData", new HashMap<String, Object>());
        }
    }
    
    /**
     * Convert results to iOS format
     * 
     * @param source Source results
     * @param target Target results map
     */
    private static void convertToIOSFormat(Map<String, Object> source, Map<String, Object> target) {
        // iOS-specific field names
        copyIfPresent(source, target, "type", "predictionType");
        copyIfPresent(source, target, "confidence", "confidenceLevel");
        copyIfPresent(source, target, "timestamp", "timestamp");
        copyIfPresent(source, target, "duration", "executionTime");
        
        // Copy results with iOS-specific key names
        if (source.containsKey("result")) {
            target.put("predictionResults", source.get("result"));
        } else if (source.containsKey("results")) {
            target.put("predictionResults", source.get("results"));
        } else if (source.containsKey("prediction")) {
            target.put("predictionResults", source.get("prediction"));
        } else if (source.containsKey("predictions")) {
            target.put("predictionResults", source.get("predictions"));
        }
        
        // Copy metadata if present
        if (source.containsKey("metadata")) {
            target.put("additionalInfo", source.get("metadata"));
        }
    }
    
    /**
     * Convert results to web format
     * 
     * @param source Source results
     * @param target Target results map
     */
    private static void convertToWebFormat(Map<String, Object> source, Map<String, Object> target) {
        // Web-specific field names (camelCase)
        copyIfPresent(source, target, "type", "type");
        copyIfPresent(source, target, "confidence", "confidence");
        copyIfPresent(source, target, "timestamp", "timestamp");
        copyIfPresent(source, target, "duration", "duration");
        
        // Copy results with web-specific key names
        if (source.containsKey("result")) {
            target.put("data", source.get("result"));
        } else if (source.containsKey("results")) {
            target.put("data", source.get("results"));
        } else if (source.containsKey("prediction")) {
            target.put("data", source.get("prediction"));
        } else if (source.containsKey("predictions")) {
            target.put("data", source.get("predictions"));
        }
        
        // Copy metadata if present
        if (source.containsKey("metadata")) {
            target.put("meta", source.get("metadata"));
        }
    }
    
    /**
     * Copy a field from source to target if present
     * 
     * @param source Source map
     * @param target Target map
     * @param key Key to copy
     */
    private static void copyIfPresent(Map<String, Object> source, Map<String, Object> target, String key) {
        if (source.containsKey(key)) {
            target.put(key, source.get(key));
        }
    }
    
    /**
     * Copy a field from source to target with a different key name
     * 
     * @param source Source map
     * @param target Target map
     * @param sourceKey Key in source map
     * @param targetKey Key to use in target map
     */
    private static void copyIfPresent(Map<String, Object> source, Map<String, Object> target, 
                                      String sourceKey, String targetKey) {
        if (source.containsKey(sourceKey)) {
            target.put(targetKey, source.get(sourceKey));
        }
    }
}