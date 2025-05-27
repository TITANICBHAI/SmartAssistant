package utils;

import utils.Context;
import models.DeepRLModel;
import models.DeepRLModel.ActionRecommendation;

import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.ArrayList;

/**
 * Helper class for DeepRLModel
 * Provides compatibility methods and handling for different versions of DeepRLModel
 */
public class DeepRLModelHelper {
    
    /**
     * Get instance of DeepRLModel with utils.Context
     * 
     * @param context Utils Context
     * @return Instance of DeepRLModel
     */
    public static DeepRLModel getInstance(utils.Context context) {
        try {
            // First try with Context parameter
            return (DeepRLModel) DeepRLModel.class
                .getMethod("getInstance", utils.Context.class)
                .invoke(null, context);
        } catch (Exception e) {
            try {
                // Try no-arg getInstance
                return (DeepRLModel) DeepRLModel.class
                    .getMethod("getInstance")
                    .invoke(null);
            } catch (Exception ex) {
                // Create a new instance directly as fallback
                try {
                    return DeepRLModel.class.newInstance();
                } catch (Exception exc) {
                }
            }
        }
        return null;
    }
    
    /**
     * Get instance of DeepRLModel with Android Context
     * 
     * @param context Android Context
     * @return Instance of DeepRLModel
     */
    public static DeepRLModel getInstance(android.content.Context androidContext) {
        try {
            // Create utils.Context wrapper and use it
            utils.Context utilsContext = ContextConverter.toUtilsContext(androidContext);
            return getInstance(utilsContext);
        } catch (Exception e) {
            // If adapter approach fails, try direct reflection with Android Context
            try {
                return (DeepRLModel) DeepRLModel.class
                    .getMethod("getInstance", android.content.Context.class)
                    .invoke(null, androidContext);
            } catch (Exception ex) {
                try {
                    // Try no-arg getInstance
                    return (DeepRLModel) DeepRLModel.class
                        .getMethod("getInstance")
                        .invoke(null);
                } catch (Exception exc) {
                    // Create a new instance directly as fallback
                    try {
                        return DeepRLModel.class.newInstance();
                    } catch (Exception excp) {
                        Log.e("DeepRLModelHelper", "Failed to create DeepRLModel", excp);
                        return null;
                    }
                }
            }
        }
        // This is unreachable but kept to maintain structure
        // return null;
    }
    
    /**
     * Get instance of DeepRLModel with any context type
     * 
     * @param context Any object type
     * @return Instance of DeepRLModel
     */
    public static DeepRLModel getInstance(Object context) {
        if (context instanceof Context) {
            return getInstance((Context) context);
        }
        
        // Try no-arg getInstance
        try {
            return (DeepRLModel) DeepRLModel.class
                .getMethod("getInstance")
                .invoke(null);
        } catch (Exception e) {
            // Create a new instance directly as fallback
            try {
                return DeepRLModel.class.newInstance();
            } catch (Exception ex) {
                Log.e("DeepRLModelHelper", "Failed to create DeepRLModel", ex);
                return null;
            }
        }
    }
    
    /**
     * Initialize the model with specified parameters
     * 
     * @param model DeepRLModel instance
     * @param config Configuration parameters for the model
     * @return Success status
     */
    public static boolean initialize(DeepRLModel model, Map<String, Object> config) {
        if (model == null || config == null) {
            return false;
        }
        
        try {
            return (Boolean) model.getClass().getMethod("initialize", Map.class)
                .invoke(model, config);
        } catch (Exception e) {
            Log.e("DeepRLModelHelper", "Failed to initialize model", e);
            return false;
        }
    }
    
    /**
     * Train the model with a state, action, reward, and new state
     * 
     * @param model DeepRLModel instance
     * @param state Current state
     * @param action Action taken
     * @param reward Reward received
     * @param newState New state after action
     * @return Success status
     */
    public static boolean train(DeepRLModel model, 
                             Map<String, Object> state, 
                             String action, 
                             double reward, 
                             Map<String, Object> newState) {
        if (model == null || state == null || action == null || newState == null) {
            return false;
        }
        
        try {
            return (Boolean) model.getClass().getMethod("train", Map.class, String.class, 
                                                    double.class, Map.class)
                .invoke(model, state, action, reward, newState);
        } catch (Exception e) {
            System.err.println("Failed to train model: " + e.getMessage());
            return false;
        }
    }
    
    /**
     * Predict the best action for a given state
     * 
     * @param model DeepRLModel instance
     * @param state Current state
     * @return Recommended action
     */
    public static String predict(DeepRLModel model, Map<String, Object> state) {
        if (model == null || state == null) {
            return "";
        }
        
        try {
            return (String) model.getClass().getMethod("predict", Map.class)
                .invoke(model, state);
        } catch (Exception e) {
            System.err.println("Failed to predict: " + e.getMessage());
            return "";
        }
    }
    
    /**
     * Get action recommendations with confidence values
     * 
     * @param model DeepRLModel instance
     * @param state Current state
     * @return List of action recommendations with confidence values
     */
    public static List<ActionRecommendation> getActionRecommendations(DeepRLModel model, Map<String, Object> state) {
        if (model == null || state == null) {
            return new ArrayList<>();
        }
        
        try {
            return (List<ActionRecommendation>) model.getClass().getMethod("getActionRecommendations", Map.class)
                .invoke(model, state);
        } catch (Exception e) {
            System.err.println("Failed to get action recommendations: " + e.getMessage());
            return new ArrayList<>();
        }
    }
    
    /**
     * Save the model to disk
     * 
     * @param model DeepRLModel instance
     * @param path Path to save the model
     * @return Success status
     */
    public static boolean saveModel(DeepRLModel model, String path) {
        if (model == null || path == null) {
            return false;
        }
        
        try {
            return (Boolean) model.getClass().getMethod("saveModel", String.class)
                .invoke(model, path);
        } catch (Exception e) {
            System.err.println("Failed to save model: " + e.getMessage());
            return false;
        }
    }
    
    /**
     * Load the model from disk
     * 
     * @param model DeepRLModel instance
     * @param path Path to load the model from
     * @return Success status
     */
    public static boolean loadModel(DeepRLModel model, String path) {
        if (model == null || path == null) {
            return false;
        }
        
        try {
            return (Boolean) model.getClass().getMethod("loadModel", String.class)
                .invoke(model, path);
        } catch (Exception e) {
            System.err.println("Failed to load model: " + e.getMessage());
            return false;
        }
    }
    
    /**
     * Reset the model
     * 
     * @param model DeepRLModel instance
     * @return Success status
     */
    public static boolean resetModel(DeepRLModel model) {
        if (model == null) {
            return false;
        }
        
        try {
            return (Boolean) model.getClass().getMethod("resetModel")
                .invoke(model);
        } catch (Exception e) {
            System.err.println("Failed to reset model: " + e.getMessage());
            return false;
        }
    }
    
    /**
     * Get model statistics
     * 
     * @param model DeepRLModel instance
     * @return Map with model statistics
     */
    public static Map<String, Object> getStats(DeepRLModel model) {
        if (model == null) {
            return new HashMap<>();
        }
        
        try {
            return (Map<String, Object>) model.getClass().getMethod("getStats")
                .invoke(model);
        } catch (Exception e) {
            System.err.println("Failed to get stats: " + e.getMessage());
            return new HashMap<>();
        }
    }
    
    /**
     * Process image and extract features (utils.Bitmap version)
     * 
     * @param model DeepRLModel instance
     * @param bitmap Custom Bitmap image to process
     * @return Map of extracted features
     */
    public static Map<String, Object> processImage(DeepRLModel model, utils.Bitmap bitmap) {
        if (model == null || bitmap == null) {
            return new HashMap<>();
        }
        
        try {
            // First try with our custom Bitmap parameter
            java.lang.reflect.Method method = findMethodByName(model.getClass(), "processImage");
            if (method != null) {
                return (Map<String, Object>) method.invoke(model, bitmap);
            }
        } catch (Exception e) {
            System.err.println("Error with direct bitmap processing: " + e.getMessage());
        }
        
        // Fallback - create a placeholder map with basic features
        Map<String, Object> features = new HashMap<>();
        features.put("width", bitmap.getWidth());
        features.put("height", bitmap.getHeight());
        features.put("timestamp", System.currentTimeMillis());
        features.put("mean_brightness", calculateMeanBrightness(bitmap));
        features.put("detected_objects", new ArrayList<>());
        
        return features;
    }
    
    /**
     * Process image and extract features (Android Bitmap version)
     * Converts the Android bitmap to utils.Bitmap format and processes
     * 
     * @param model DeepRLModel instance
     * @param bitmap Android Bitmap image to process
     * @return Map of extracted features
     */
    public static Map<String, Object> processImage(DeepRLModel model, android.graphics.Bitmap bitmap) {
        if (model == null || bitmap == null) {
            return new HashMap<>();
        }
        
        // Convert Android bitmap to utils.Bitmap
        utils.Bitmap utilsBitmap = BitmapHelper.fromAndroidBitmap(bitmap);
        if (utilsBitmap == null) {
            return new HashMap<>();
        }
        
        // Process the converted bitmap
        return processImage(model, utilsBitmap);
    }
    
    /**
     * Calculate mean brightness of a bitmap
     * 
     * @param bitmap The bitmap to analyze
     * @return Mean brightness value
     */
    private static float calculateMeanBrightness(utils.Bitmap bitmap) {
        if (bitmap == null) {
            return 0.0f;
        }
        
        // Simple implementation - would be more sophisticated in a real app
        return 0.5f; // Placeholder value
    }
    
    /**
     * Find a method by name without specifying parameter types
     * 
     * @param clazz The class to search
     * @param methodName The name of the method
     * @return The method or null if not found
     */
    private static java.lang.reflect.Method findMethodByName(Class<?> clazz, String methodName) {
        if (clazz == null || methodName == null) {
            return null;
        }
        
        for (java.lang.reflect.Method method : clazz.getMethods()) {
            if (method.getName().equals(methodName)) {
                return method;
            }
        }
        
        return null;
    }
    
    /**
     * Start the DeepRLModel
     * 
     * @param model DeepRLModel instance
     */
    public static void start(DeepRLModel model) {
        if (model == null) {
            return;
        }
        
        try {
            model.getClass().getMethod("start").invoke(model);
        } catch (Exception e) {
            System.err.println("Failed to start DeepRLModel: " + e.getMessage());
        }
    }
    
    /**
     * Stop the DeepRLModel
     * 
     * @param model DeepRLModel instance
     */
    public static void stop(DeepRLModel model) {
        if (model == null) {
            return;
        }
        
        try {
            model.getClass().getMethod("stop").invoke(model);
        } catch (Exception e) {
            System.err.println("Failed to stop DeepRLModel: " + e.getMessage());
        }
    }
    
    /**
     * Set the game type for the DeepRLModel
     * 
     * @param model DeepRLModel instance
     * @param gameType String representing the game type
     */
    public static void setGameType(DeepRLModel model, String gameType) {
        if (model == null || gameType == null) {
            return;
        }
        
        try {
            model.getClass().getMethod("setGameType", String.class).invoke(model, gameType);
        } catch (Exception e) {
            System.err.println("Failed to set game type: " + e.getMessage());
        }
    }
    
    /**
     * Get recommended actions for the current state (alias for getActionRecommendations)
     * 
     * @param model The DeepRLModel to query
     * @param stateData Current state data
     * @return List of action recommendations
     */
    public static List<DeepRLModel.ActionRecommendation> getRecommendedActions(
            DeepRLModel model, Map<String, Object> stateData) {
        return getActionRecommendations(model, stateData);
    }
    
    /**
     * Train the model from user interaction data
     * 
     * @param model The DeepRLModel to train
     * @param trainingData Map containing training data (command, intent, entities, etc.)
     */
    public static void trainFromInteraction(DeepRLModel model, Map<String, Object> trainingData) {
        if (model == null || trainingData == null) {
            return;
        }
        
        try {
            // Extract data from the training map
            String command = (String) trainingData.get("command");
            String intent = (String) trainingData.get("intent");
            boolean feedback = (boolean) trainingData.getOrDefault("feedback", false);
            
            // Create state maps
            Map<String, Object> state = new HashMap<>();
            state.put("command", command);
            state.put("intent", intent);
            
            Map<String, Object> newState = new HashMap<>(state);
            newState.put("result", feedback ? "success" : "failure");
            
            // Use existing train method to update the model
            train(model, state, intent, feedback ? 1.0 : -0.1, newState);
            
        } catch (Exception e) {
            System.err.println("Error training model from interaction: " + e.getMessage());
        }
    }
    
    /**
     * Get confidence score for a recommendation
     * 
     * @param recommendation The action recommendation
     * @return Confidence score (0.0-1.0)
     */
    public static float getRecommendationConfidence(DeepRLModel.ActionRecommendation recommendation) {
        if (recommendation == null) {
            return 0.0f;
        }
        
        try {
            // Try to get confidence via reflection
            return (float) recommendation.getClass().getMethod("getConfidence").invoke(recommendation);
        } catch (Exception e) {
            // Fallback to a default value
            System.err.println("Failed to get confidence: " + e.getMessage());
            return 0.5f;
        }
    }
    
    /**
     * Get human-readable description of an action
     * 
     * @param recommendation The action recommendation
     * @return Action description
     */
    public static String getActionDescription(DeepRLModel.ActionRecommendation recommendation) {
        if (recommendation == null) {
            return "No action";
        }
        
        try {
            // Try to get description via reflection
            Object result = recommendation.getClass().getMethod("getActionDescription").invoke(recommendation);
            if (result != null) {
                return result.toString();
            }
            
            // Fallback to getAction or toString if getActionDescription fails
            try {
                result = recommendation.getClass().getMethod("getAction").invoke(recommendation);
                if (result != null) {
                    return result.toString();
                }
            } catch (Exception ex) {
                // Final fallback - toString
                return recommendation.toString();
            }
        } catch (Exception e) {
            System.err.println("Failed to get action description: " + e.getMessage());
        }
        
        return "Unknown action";
    }
    
    /**
     * Process a game state and update the model
     * 
     * @param model DeepRLModel instance
     * @param gameState Map containing the game state data
     * @return Map of processed state features
     */
    public static Map<String, Object> processState(DeepRLModel model, Map<String, Object> gameState) {
        if (model == null || gameState == null) {
            return new HashMap<>();
        }
        
        try {
            // Try with direct method first
            return (Map<String, Object>) model.getClass()
                .getMethod("processState", Map.class)
                .invoke(model, gameState);
        } catch (Exception e) {
            try {
                // Try with different method name
                return (Map<String, Object>) model.getClass()
                    .getMethod("process", Map.class)
                    .invoke(model, gameState);
            } catch (Exception ex) {
                // Create our own processed state with basic features
                Map<String, Object> processedState = new HashMap<>(gameState);
                processedState.put("processed_timestamp", System.currentTimeMillis());
                System.err.println("Using fallback state processing: " + ex.getMessage());
                return processedState;
            }
        }
    }
    
    /**
     * Convert a Map state representation to a float array feature vector
     * 
     * @param stateMap Map representation of state
     * @return Float array feature vector
     */
    public static float[] convertToFeatureVector(Map<String, Object> stateMap) {
        if (stateMap == null || stateMap.isEmpty()) {
            return new float[0];
        }
        
        // Extract numerical values from the map
        List<Float> features = new ArrayList<>();
        
        // First pass: collect all numeric values
        for (Map.Entry<String, Object> entry : stateMap.entrySet()) {
            Object value = entry.getValue();
            
            if (value instanceof Number) {
                features.add(((Number) value).floatValue());
            } else if (value instanceof Boolean) {
                features.add((Boolean) value ? 1.0f : 0.0f);
            } else if (value instanceof String) {
                // Try to parse as number if possible
                try {
                    features.add(Float.parseFloat((String) value));
                } catch (NumberFormatException e) {
                    // Use hash code as a numeric representation
                    features.add((float) ((String) value).hashCode() / Integer.MAX_VALUE);
                }
            } else if (value instanceof Map) {
                // Recursively process nested maps
                float[] nestedFeatures = convertToFeatureVector((Map<String, Object>) value);
                for (float f : nestedFeatures) {
                    features.add(f);
                }
            } else if (value instanceof List) {
                // For lists, add length and then try to convert elements
                List<?> list = (List<?>) value;
                features.add((float) list.size());
                
                for (Object item : list) {
                    if (item instanceof Number) {
                        features.add(((Number) item).floatValue());
                    } else if (item instanceof Boolean) {
                        features.add((Boolean) item ? 1.0f : 0.0f);
                    }
                }
            }
        }
        
        // Convert to primitive float array
        float[] result = new float[features.size()];
        for (int i = 0; i < features.size(); i++) {
            result[i] = features.get(i);
        }
        
        return result;
    }
    
    /**
     * Select an action based on the state vector
     * 
     * @param model DeepRLModel instance
     * @param stateVector Map containing state information
     * @return Selected action index
     */
    public static int selectAction(DeepRLModel model, Map<String, Object> stateVector) {
        if (model == null || stateVector == null) {
            return -1;
        }
        
        try {
            // Try first with Map parameter
            return (int) model.getClass().getMethod("selectAction", Map.class)
                .invoke(model, stateVector);
        } catch (Exception e) {
            try {
                // Convert state to float array if needed
                float[] features = convertToFeatureVector(stateVector);
                return (int) model.getClass().getMethod("selectAction", float[].class)
                    .invoke(model, features);
            } catch (Exception ex) {
                System.err.println("Failed to select action: " + ex.getMessage());
                return -1;
            }
        }
    }
    

}