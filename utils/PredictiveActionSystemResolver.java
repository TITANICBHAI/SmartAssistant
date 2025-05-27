package utils;

import android.util.Log;
import java.lang.reflect.Method;

/**
 * Resolver for ambiguous PredictiveActionSystem references
 * This class provides methods to resolve ambiguous references between different PredictiveActionSystem implementations
 */
public class PredictiveActionSystemResolver {
    private static final String TAG = "PredictiveActionSystemResolver";
    
    /**
     * Get a GameState from a package
     * @param packageName The package name (e.g., "com.aiassistant.ml" or "com.aiassistant.models")
     * @return A GameState from the specified package, or null if not found
     */
    public static Object getGameStateFromPackage(String packageName) {
        try {
            Class<?> predictiveSystemClass = Class.forName(packageName + ".PredictiveActionSystem");
            Class<?> gameStateClass = Class.forName(packageName + ".PredictiveActionSystem$GameState");
            
            // Try to create a new instance
            try {
                return gameStateClass.newInstance();
            } catch (Exception e) {
                // Try to find a factory method
                try {
                    Method createMethod = predictiveSystemClass.getMethod("createGameState");
                    return createMethod.invoke(null);
                } catch (Exception ex) {
                    // Try to get a field
                    try {
                        return predictiveSystemClass.getField("EMPTY_STATE").get(null);
                    } catch (Exception exc) {
                        Log.e(TAG, "Could not create GameState from package " + packageName);
                        return null;
                    }
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Error getting GameState from package " + packageName + ": " + e.getMessage());
            return null;
        }
    }
    
    /**
     * Get an ActionPrediction from a package
     * @param packageName The package name (e.g., "com.aiassistant.ml" or "com.aiassistant.models")
     * @return An ActionPrediction from the specified package, or null if not found
     */
    public static Object getActionPredictionFromPackage(String packageName) {
        try {
            Class<?> predictiveSystemClass = Class.forName(packageName + ".PredictiveActionSystem");
            Class<?> actionPredictionClass = Class.forName(packageName + ".PredictiveActionSystem$ActionPrediction");
            
            // Try to create a new instance
            try {
                return actionPredictionClass.newInstance();
            } catch (Exception e) {
                // Try to find a factory method
                try {
                    Method createMethod = predictiveSystemClass.getMethod("createActionPrediction");
                    return createMethod.invoke(null);
                } catch (Exception ex) {
                    // Try to get a field
                    try {
                        return predictiveSystemClass.getField("EMPTY_PREDICTION").get(null);
                    } catch (Exception exc) {
                        Log.e(TAG, "Could not create ActionPrediction from package " + packageName);
                        return null;
                    }
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Error getting ActionPrediction from package " + packageName + ": " + e.getMessage());
            return null;
        }
    }
    
    /**
     * Convert a GameState from one package to another
     * @param sourceState The source GameState
     * @param targetPackage The target package
     * @return A GameState in the target package, or null if conversion failed
     */
    public static Object convertGameState(Object sourceState, String targetPackage) {
        if (sourceState == null) {
            return null;
        }
        
        try {
            // Determine source and target packages
            String sourcePackage = sourceState.getClass().getName();
            if (sourcePackage.lastIndexOf('.') > 0) {
                sourcePackage = sourcePackage.substring(0, sourcePackage.lastIndexOf('.'));
            }
            
            // If source and target are the same, return the source
            if (sourcePackage.equals(targetPackage)) {
                return sourceState;
            }
            
            // Get a new instance of the target GameState
            Object targetState = getGameStateFromPackage(targetPackage);
            if (targetState == null) {
                return null;
            }
            
            // Copy the properties
            // Common property names to try
            String[] propertyNames = {
                "currentScreen", "elements", "gameType", "deviceInfo", "timestamp", "screenshot", "actionHistory"
            };
            
            for (String propertyName : propertyNames) {
                try {
                    // Try to get the property
                    String capitalizedProperty = Character.toUpperCase(propertyName.charAt(0)) + propertyName.substring(1);
                    Method getMethod = sourceState.getClass().getMethod("get" + capitalizedProperty);
                    Object value = getMethod.invoke(sourceState);
                    
                    // Try to set the property
                    Method setMethod = targetState.getClass().getMethod("set" + capitalizedProperty, value.getClass());
                    setMethod.invoke(targetState, value);
                } catch (Exception e) {
                    // Property doesn't exist or types don't match, skip it
                }
            }
            
            return targetState;
            
        } catch (Exception e) {
            Log.e(TAG, "Error converting GameState: " + e.getMessage());
            return null;
        }
    }
    
    /**
     * Convert an ActionPrediction from one package to another
     * @param sourcePrediction The source ActionPrediction
     * @param targetPackage The target package
     * @return An ActionPrediction in the target package, or null if conversion failed
     */
    public static Object convertActionPrediction(Object sourcePrediction, String targetPackage) {
        if (sourcePrediction == null) {
            return null;
        }
        
        try {
            // Determine source and target packages
            String sourcePackage = sourcePrediction.getClass().getName();
            if (sourcePackage.lastIndexOf('.') > 0) {
                sourcePackage = sourcePackage.substring(0, sourcePackage.lastIndexOf('.'));
            }
            
            // If source and target are the same, return the source
            if (sourcePackage.equals(targetPackage)) {
                return sourcePrediction;
            }
            
            // Get a new instance of the target ActionPrediction
            Object targetPrediction = getActionPredictionFromPackage(targetPackage);
            if (targetPrediction == null) {
                return null;
            }
            
            // Copy the properties
            // Common property names to try
            String[] propertyNames = {
                "action", "targetElement", "confidence", "actionType", "parameters", "expectedOutcome"
            };
            
            for (String propertyName : propertyNames) {
                try {
                    // Try to get the property
                    String capitalizedProperty = Character.toUpperCase(propertyName.charAt(0)) + propertyName.substring(1);
                    Method getMethod = sourcePrediction.getClass().getMethod("get" + capitalizedProperty);
                    Object value = getMethod.invoke(sourcePrediction);
                    
                    // Try to set the property
                    Method setMethod = targetPrediction.getClass().getMethod("set" + capitalizedProperty, value.getClass());
                    setMethod.invoke(targetPrediction, value);
                } catch (Exception e) {
                    // Property doesn't exist or types don't match, skip it
                }
            }
            
            return targetPrediction;
            
        } catch (Exception e) {
            Log.e(TAG, "Error converting ActionPrediction: " + e.getMessage());
            return null;
        }
    }
}