package utils;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Rect;
import android.util.Log;
import java.util.List;
import java.util.ArrayList;
import java.util.Map;
import java.util.HashMap;

/**
 * Adapter for AI Controller functionality
 * Extends the AIController class to bridge between utils and models packages
 */
public class AIControllerAdapter extends AIController {
    private static final String TAG = "AIControllerAdapter";
    
    private static AIControllerAdapter instance;
    private Object nativeAIController;
    
    /**
     * Private constructor
     */
    private AIControllerAdapter() {
        // Attempt to get the native AI controller instance
        try {
            Class<?> aiControllerClass = Class.forName("com.aiassistant.AIController");
            java.lang.reflect.Method getInstance = aiControllerClass.getMethod("getInstance");
            nativeAIController = getInstance.invoke(null);
            Log.i(TAG, "Successfully got native AIController instance");
        } catch (Exception e) {
            Log.e(TAG, "Could not get native AIController: " + e.getMessage());
            nativeAIController = null;
        }
    }
    
    /**
     * Constructor that takes an AIControllerHelper
     * @param helper The AIControllerHelper instance
     */
    public AIControllerAdapter(AIControllerHelper helper) {
        this();
        if (helper != null) {
            // Store helper reference or perform any helper-specific initialization
            Log.i(TAG, "Initialized AIControllerAdapter with AIControllerHelper");
        }
    }
    
    /**
     * Get the singleton instance
     * @return The AIControllerAdapter instance
     */
    public static synchronized AIControllerAdapter getInstance() {
        if (instance == null) {
            instance = new AIControllerAdapter();
        }
        return instance;
    }
    
    /**
     * Set the native AI controller
     * @param controller The native controller
     */
    public void setNativeController(Object controller) {
        this.nativeAIController = controller;
    }
    
    /**
     * Get the native AI controller
     * @return The native controller
     */
    public Object getNativeController() {
        return nativeAIController;
    }
    
    /**
     * Initialize the AI controller
     * @param config Configuration parameters
     * @return True if initialization was successful, false otherwise
     */
    public boolean initialize(Map<String, Object> config) {
        if (nativeAIController == null) {
            Log.e(TAG, "Cannot initialize AIController: native controller is null");
            return false;
        }
        
        try {
            java.lang.reflect.Method method = nativeAIController.getClass().getMethod("initialize", Map.class);
            Object result = method.invoke(nativeAIController, config);
            return result != null && Boolean.parseBoolean(result.toString());
        } catch (Exception e) {
            Log.e(TAG, "Failed to initialize AIController: " + e.getMessage());
            return false;
        }
    }
    
    /**
     * Analyze a UI screen
     * @param elements The UI elements to analyze
     * @return Analysis results as a map
     */
    public Map<String, Object> analyzeScreen(UIElement[] elements) {
        if (nativeAIController == null) {
            Log.e(TAG, "Cannot analyze screen: native controller is null");
            return new HashMap<>();
        }
        
        try {
            // Convert UIElements to the format expected by the native controller
            List<Object> nativeElements = new ArrayList<>();
            for (UIElement element : elements) {
                // Check if element is an adapter with a wrapped element
                if (element instanceof UIElementAdapter) {
                    UIElementAdapter adapter = (UIElementAdapter)element;
                    models.UIElementInterface wrapped = adapter.getWrappedElement();
                    if (wrapped != null) {
                        nativeElements.add(wrapped);
                        continue;
                    }
                }
                
                // Check if element implements models.UIElementInterface
                if (element instanceof models.UIElementInterface) {
                    nativeElements.add(element);
                    continue;
                }
                
                // Otherwise, convert to a map
                nativeElements.add(UIElementHelper.toMap(element));
            }
            
            java.lang.reflect.Method method = nativeAIController.getClass().getMethod("analyzeScreen", List.class);
            Object result = method.invoke(nativeAIController, nativeElements);
            
            if (result instanceof Map) {
                return (Map<String, Object>) result;
            } else {
                Log.e(TAG, "AIController.analyzeScreen did not return a Map");
                return new HashMap<>();
            }
        } catch (Exception e) {
            Log.e(TAG, "Failed to analyze screen: " + e.getMessage());
            return new HashMap<>();
        }
    }
    
    /**
     * Recognize text in an image
     * @param imageData The image data
     * @return Recognized text
     */
    public String recognizeText(byte[] imageData) {
        if (nativeAIController == null) {
            Log.e(TAG, "Cannot recognize text: native controller is null");
            return "";
        }
        
        try {
            java.lang.reflect.Method method = nativeAIController.getClass().getMethod("recognizeText", byte[].class);
            Object result = method.invoke(nativeAIController, imageData);
            return result != null ? result.toString() : "";
        } catch (Exception e) {
            Log.e(TAG, "Failed to recognize text: " + e.getMessage());
            return "";
        }
    }
    
    /**
     * Analyze an image
     * @param imageData The image data
     * @return Analysis results as a map
     */
    public Map<String, Object> analyzeImage(byte[] imageData) {
        if (nativeAIController == null) {
            Log.e(TAG, "Cannot analyze image: native controller is null");
            return new HashMap<>();
        }
        
        try {
            java.lang.reflect.Method method = nativeAIController.getClass().getMethod("analyzeImage", byte[].class);
            Object result = method.invoke(nativeAIController, imageData);
            
            if (result instanceof Map) {
                return (Map<String, Object>) result;
            } else {
                Log.e(TAG, "AIController.analyzeImage did not return a Map");
                return new HashMap<>();
            }
        } catch (Exception e) {
            Log.e(TAG, "Failed to analyze image: " + e.getMessage());
            return new HashMap<>();
        }
    }
    
    /**
     * Process natural language input
     * @param text The input text
     * @param context Optional context information
     * @return Processing results as a map
     */
    public Map<String, Object> processNaturalLanguage(String text, Map<String, Object> context) {
        if (nativeAIController == null) {
            Log.e(TAG, "Cannot process natural language: native controller is null");
            return new HashMap<>();
        }
        
        try {
            java.lang.reflect.Method method = nativeAIController.getClass().getMethod("processNaturalLanguage", String.class, Map.class);
            Object result = method.invoke(nativeAIController, text, context);
            
            if (result instanceof Map) {
                return (Map<String, Object>) result;
            } else {
                Log.e(TAG, "AIController.processNaturalLanguage did not return a Map");
                return new HashMap<>();
            }
        } catch (Exception e) {
            Log.e(TAG, "Failed to process natural language: " + e.getMessage());
            return new HashMap<>();
        }
    }
    
    /**
     * Generate suggestions based on the current context
     * @param context The current context
     * @return A list of suggestions
     */
    public List<String> generateSuggestions(Map<String, Object> context) {
        if (nativeAIController == null) {
            Log.e(TAG, "Cannot generate suggestions: native controller is null");
            return new ArrayList<>();
        }
        
        try {
            java.lang.reflect.Method method = nativeAIController.getClass().getMethod("generateSuggestions", Map.class);
            Object result = method.invoke(nativeAIController, context);
            
            if (result instanceof List) {
                return (List<String>) result;
            } else {
                Log.e(TAG, "AIController.generateSuggestions did not return a List");
                return new ArrayList<>();
            }
        } catch (Exception e) {
            Log.e(TAG, "Failed to generate suggestions: " + e.getMessage());
            return new ArrayList<>();
        }
    }
    
    /**
     * Analyze user behavior
     * @param actions The user actions to analyze
     * @return Analysis results as a map
     */
    public Map<String, Object> analyzeUserBehavior(List<Map<String, Object>> actions) {
        if (nativeAIController == null) {
            Log.e(TAG, "Cannot analyze user behavior: native controller is null");
            return new HashMap<>();
        }
        
        try {
            java.lang.reflect.Method method = nativeAIController.getClass().getMethod("analyzeUserBehavior", List.class);
            Object result = method.invoke(nativeAIController, actions);
            
            if (result instanceof Map) {
                return (Map<String, Object>) result;
            } else {
                Log.e(TAG, "AIController.analyzeUserBehavior did not return a Map");
                return new HashMap<>();
            }
        } catch (Exception e) {
            Log.e(TAG, "Failed to analyze user behavior: " + e.getMessage());
            return new HashMap<>();
        }
    }
    
    /**
     * Get AI configuration
     * @return The configuration as a map
     */
    public Map<String, Object> getConfiguration() {
        if (nativeAIController == null) {
            Log.e(TAG, "Cannot get configuration: native controller is null");
            return new HashMap<>();
        }
        
        try {
            java.lang.reflect.Method method = nativeAIController.getClass().getMethod("getConfiguration");
            Object result = method.invoke(nativeAIController);
            
            if (result instanceof Map) {
                return (Map<String, Object>) result;
            } else {
                Log.e(TAG, "AIController.getConfiguration did not return a Map");
                return new HashMap<>();
            }
        } catch (Exception e) {
            Log.e(TAG, "Failed to get configuration: " + e.getMessage());
            return new HashMap<>();
        }
    }
    
    /**
     * Set AI configuration
     * @param config The configuration as a map
     * @return True if successful, false otherwise
     */
    public boolean setConfiguration(Map<String, Object> config) {
        if (nativeAIController == null) {
            Log.e(TAG, "Cannot set configuration: native controller is null");
            return false;
        }
        
        try {
            java.lang.reflect.Method method = nativeAIController.getClass().getMethod("setConfiguration", Map.class);
            Object result = method.invoke(nativeAIController, config);
            return result != null && Boolean.parseBoolean(result.toString());
        } catch (Exception e) {
            Log.e(TAG, "Failed to set configuration: " + e.getMessage());
            return false;
        }
    }
    
    // AIController interface implementation
    
    @Override
    public boolean initialize(Context context) {
        if (context == null) {
            Log.e(TAG, "Cannot initialize with null context");
            return false;
        }
        
        Map<String, Object> config = new HashMap<>();
        config.put("context", context);
        return initialize(config);
    }
    
    @Override
    public Map<String, Object> performAction(String actionType, Map<String, Object> parameters) {
        if (nativeAIController == null) {
            Log.e(TAG, "Cannot perform action: native controller is null");
            return new HashMap<>();
        }
        
        try {
            java.lang.reflect.Method method = nativeAIController.getClass().getMethod("performAction", String.class, Map.class);
            Object result = method.invoke(nativeAIController, actionType, parameters);
            
            if (result instanceof Map) {
                return (Map<String, Object>) result;
            } else {
                Log.e(TAG, "AIController.performAction did not return a Map");
                return new HashMap<>();
            }
        } catch (Exception e) {
            Log.e(TAG, "Failed to perform action: " + e.getMessage());
            return new HashMap<>();
        }
    }
    
    @Override
    public boolean processVideoInput(VideoProcessor videoProcessor) {
        if (nativeAIController == null || videoProcessor == null) {
            Log.e(TAG, "Cannot process video input: controller is null or videoProcessor is null");
            return false;
        }
        
        try {
            java.lang.reflect.Method method = nativeAIController.getClass().getMethod("processVideoInput", Object.class);
            Object result = method.invoke(nativeAIController, videoProcessor);
            return result != null && Boolean.parseBoolean(result.toString());
        } catch (Exception e) {
            Log.e(TAG, "Failed to process video input: " + e.getMessage());
            return false;
        }
    }
    
    @Override
    public boolean processScreenshotInput(Bitmap screenshot) {
        if (nativeAIController == null || screenshot == null) {
            Log.e(TAG, "Cannot process screenshot input: controller is null or screenshot is null");
            return false;
        }
        
        try {
            java.lang.reflect.Method method = nativeAIController.getClass().getMethod("processScreenshotInput", Bitmap.class);
            Object result = method.invoke(nativeAIController, screenshot);
            return result != null && Boolean.parseBoolean(result.toString());
        } catch (Exception e) {
            Log.e(TAG, "Failed to process screenshot input: " + e.getMessage());
            return false;
        }
    }
    
    @Override
    public Map<String, Object> processTextInput(String text) {
        if (nativeAIController == null || text == null) {
            Log.e(TAG, "Cannot process text input: controller is null or text is null");
            return new HashMap<>();
        }
        
        try {
            java.lang.reflect.Method method = nativeAIController.getClass().getMethod("processTextInput", String.class);
            Object result = method.invoke(nativeAIController, text);
            
            if (result instanceof Map) {
                return (Map<String, Object>) result;
            } else {
                Log.e(TAG, "AIController.processTextInput did not return a Map");
                return new HashMap<>();
            }
        } catch (Exception e) {
            Log.e(TAG, "Failed to process text input: " + e.getMessage());
            return new HashMap<>();
        }
    }
    
    @Override
    public Map<String, Object> getCurrentState() {
        if (nativeAIController == null) {
            Log.e(TAG, "Cannot get current state: native controller is null");
            return new HashMap<>();
        }
        
        try {
            java.lang.reflect.Method method = nativeAIController.getClass().getMethod("getCurrentState");
            Object result = method.invoke(nativeAIController);
            
            if (result instanceof Map) {
                return (Map<String, Object>) result;
            } else {
                Log.e(TAG, "AIController.getCurrentState did not return a Map");
                return new HashMap<>();
            }
        } catch (Exception e) {
            Log.e(TAG, "Failed to get current state: " + e.getMessage());
            return new HashMap<>();
        }
    }
    
    @Override
    public Map<String, Object> analyzeCurrentState() {
        if (nativeAIController == null) {
            Log.e(TAG, "Cannot analyze current state: native controller is null");
            return new HashMap<>();
        }
        
        try {
            java.lang.reflect.Method method = nativeAIController.getClass().getMethod("analyzeCurrentState");
            Object result = method.invoke(nativeAIController);
            
            if (result instanceof Map) {
                return (Map<String, Object>) result;
            } else {
                Log.e(TAG, "AIController.analyzeCurrentState did not return a Map");
                return new HashMap<>();
            }
        } catch (Exception e) {
            Log.e(TAG, "Failed to analyze current state: " + e.getMessage());
            return new HashMap<>();
        }
    }
    
    @Override
    public boolean updateModel(Map<String, Object> modelData) {
        if (nativeAIController == null) {
            Log.e(TAG, "Cannot update model: native controller is null");
            return false;
        }
        
        try {
            java.lang.reflect.Method method = nativeAIController.getClass().getMethod("updateModel", Map.class);
            Object result = method.invoke(nativeAIController, modelData);
            return result != null && Boolean.parseBoolean(result.toString());
        } catch (Exception e) {
            Log.e(TAG, "Failed to update model: " + e.getMessage());
            return false;
        }
    }
    
    @Override
    public boolean trainModel(List<Map<String, Object>> trainingData) {
        if (nativeAIController == null) {
            Log.e(TAG, "Cannot train model: native controller is null");
            return false;
        }
        
        try {
            java.lang.reflect.Method method = nativeAIController.getClass().getMethod("trainModel", List.class);
            Object result = method.invoke(nativeAIController, trainingData);
            return result != null && Boolean.parseBoolean(result.toString());
        } catch (Exception e) {
            Log.e(TAG, "Failed to train model: " + e.getMessage());
            return false;
        }
    }
    
    @Override
    public boolean saveModel(String filePath) {
        if (nativeAIController == null) {
            Log.e(TAG, "Cannot save model: native controller is null");
            return false;
        }
        
        try {
            java.lang.reflect.Method method = nativeAIController.getClass().getMethod("saveModel", String.class);
            Object result = method.invoke(nativeAIController, filePath);
            return result != null && Boolean.parseBoolean(result.toString());
        } catch (Exception e) {
            Log.e(TAG, "Failed to save model: " + e.getMessage());
            return false;
        }
    }
    
    @Override
    public boolean loadModel(String filePath) {
        if (nativeAIController == null) {
            Log.e(TAG, "Cannot load model: native controller is null");
            return false;
        }
        
        try {
            java.lang.reflect.Method method = nativeAIController.getClass().getMethod("loadModel", String.class);
            Object result = method.invoke(nativeAIController, filePath);
            return result != null && Boolean.parseBoolean(result.toString());
        } catch (Exception e) {
            Log.e(TAG, "Failed to load model: " + e.getMessage());
            return false;
        }
    }
    
    @Override
    public Object predictNextAction() {
        if (nativeAIController == null) {
            Log.e(TAG, "Cannot predict next action: native controller is null");
            return null;
        }
        
        try {
            java.lang.reflect.Method method = nativeAIController.getClass().getMethod("predictNextAction");
            return method.invoke(nativeAIController);
        } catch (Exception e) {
            Log.e(TAG, "Failed to predict next action: " + e.getMessage());
            return null;
        }
    }
    
    @Override
    public float getPredictionConfidence(Map<String, Object> prediction) {
        if (nativeAIController == null) {
            Log.e(TAG, "Cannot get prediction confidence: native controller is null");
            return 0.0f;
        }
        
        try {
            java.lang.reflect.Method method = nativeAIController.getClass().getMethod("getPredictionConfidence", Map.class);
            Object result = method.invoke(nativeAIController, prediction);
            return result != null ? Float.parseFloat(result.toString()) : 0.0f;
        } catch (Exception e) {
            Log.e(TAG, "Failed to get prediction confidence: " + e.getMessage());
            return 0.0f;
        }
    }
    
    @Override
    public void shutdown() {
        if (nativeAIController == null) {
            Log.e(TAG, "Cannot shutdown: native controller is null");
            return;
        }
        
        try {
            java.lang.reflect.Method method = nativeAIController.getClass().getMethod("shutdown");
            method.invoke(nativeAIController);
        } catch (Exception e) {
            Log.e(TAG, "Failed to shutdown: " + e.getMessage());
        }
    }
    
    @Override
    public void setCurrentGameTargetLives(int lives) {
        if (nativeAIController == null) {
            Log.e(TAG, "Cannot set game target lives: native controller is null");
            return;
        }
        
        try {
            java.lang.reflect.Method method = nativeAIController.getClass().getMethod("setCurrentGameTargetLives", int.class);
            method.invoke(nativeAIController, lives);
        } catch (Exception e) {
            Log.e(TAG, "Failed to set game target lives: " + e.getMessage());
        }
    }
    
    @Override
    public boolean clickAction(float x, float y, ActionCallback callback) {
        if (nativeAIController == null) {
            Log.e(TAG, "Cannot perform click action: native controller is null");
            if (callback != null) {
                callback.onActionError("Native controller is null");
            }
            return false;
        }
        
        try {
            java.lang.reflect.Method method = nativeAIController.getClass().getMethod("clickAction", float.class, float.class, Object.class);
            Object result = method.invoke(nativeAIController, x, y, new ActionCallbackWrapper(callback));
            return result != null && Boolean.parseBoolean(result.toString());
        } catch (Exception e) {
            Log.e(TAG, "Failed to perform click action: " + e.getMessage());
            if (callback != null) {
                callback.onActionError("Error: " + e.getMessage());
            }
            return false;
        }
    }
    
    @Override
    public boolean longPressAction(float x, float y, ActionCallback callback) {
        if (nativeAIController == null) {
            Log.e(TAG, "Cannot perform long press action: native controller is null");
            if (callback != null) {
                callback.onActionError("Native controller is null");
            }
            return false;
        }
        
        try {
            java.lang.reflect.Method method = nativeAIController.getClass().getMethod("longPressAction", float.class, float.class, Object.class);
            Object result = method.invoke(nativeAIController, x, y, new ActionCallbackWrapper(callback));
            return result != null && Boolean.parseBoolean(result.toString());
        } catch (Exception e) {
            Log.e(TAG, "Failed to perform long press action: " + e.getMessage());
            if (callback != null) {
                callback.onActionError("Error: " + e.getMessage());
            }
            return false;
        }
    }
    
    @Override
    public boolean swipeAction(float startX, float startY, float endX, float endY, long duration, ActionCallback callback) {
        if (nativeAIController == null) {
            Log.e(TAG, "Cannot perform swipe action: native controller is null");
            if (callback != null) {
                callback.onActionError("Native controller is null");
            }
            return false;
        }
        
        try {
            java.lang.reflect.Method method = nativeAIController.getClass().getMethod("swipeAction", 
                float.class, float.class, float.class, float.class, long.class, Object.class);
            Object result = method.invoke(nativeAIController, startX, startY, endX, endY, duration, new ActionCallbackWrapper(callback));
            return result != null && Boolean.parseBoolean(result.toString());
        } catch (Exception e) {
            Log.e(TAG, "Failed to perform swipe action: " + e.getMessage());
            if (callback != null) {
                callback.onActionError("Error: " + e.getMessage());
            }
            return false;
        }
    }
    
    /**
     * Wrapper class to convert between ActionCallback interfaces
     */
    private class ActionCallbackWrapper {
        private final ActionCallback callback;
        
        public ActionCallbackWrapper(ActionCallback callback) {
            this.callback = callback;
        }
        
        public void onActionComplete(boolean success) {
            if (callback != null) {
                callback.onActionComplete(success);
            }
        }
        
        public void onActionError(String error) {
            if (callback != null) {
                callback.onActionError(error);
            }
        }
    }
}