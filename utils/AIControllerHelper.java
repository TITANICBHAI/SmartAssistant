package utils;

import android.content.Context;

/**
 * Helper class for AI Controller
 */
public class AIControllerHelper {
    private Context context;
    private AIControllerSettings settings;
    
    /**
     * Create a new AI Controller Helper
     * 
     * @param context The Android context
     */
    public AIControllerHelper(Context context) {
        this.context = context;
        this.settings = new AIControllerSettings();
    }
    
    /**
     * Create a new AI Controller Helper with settings
     * 
     * @param context The Android context
     * @param settings The AI controller settings
     */
    public AIControllerHelper(Context context, AIControllerSettings settings) {
        this.context = context;
        this.settings = settings != null ? settings : new AIControllerSettings();
    }
    
    /**
     * Get the Android context
     * 
     * @return The Android context
     */
    public Context getContext() {
        return context;
    }
    
    /**
     * Set the Android context
     * 
     * @param context The Android context
     */
    public void setContext(Context context) {
        this.context = context;
    }
    
    /**
     * Get the AI controller settings
     * 
     * @return The settings
     */
    public AIControllerSettings getSettings() {
        return settings;
    }
    
    /**
     * Set the AI controller settings
     * 
     * @param settings The settings
     */
    public void setSettings(AIControllerSettings settings) {
        this.settings = settings != null ? settings : new AIControllerSettings();
    }
    
    /**
     * Settings class for AI Controller
     */
    public static class AIControllerSettings {
        private boolean debugMode = false;
        private boolean enableLogging = true;
        private int modelType = 0;
        private String modelPath = "";
        private float confidenceThreshold = 0.5f;
        
        /**
         * Create new settings with default values
         */
        public AIControllerSettings() {
            // Default constructor
        }
        
        /**
         * Check if debug mode is enabled
         * 
         * @return True if debug mode is enabled, false otherwise
         */
        public boolean isDebugMode() {
            return debugMode;
        }
        
        /**
         * Set debug mode
         * 
         * @param debugMode True to enable debug mode, false to disable
         * @return This settings object for chaining
         */
        public AIControllerSettings setDebugMode(boolean debugMode) {
            this.debugMode = debugMode;
            return this;
        }
        
        /**
         * Check if logging is enabled
         * 
         * @return True if logging is enabled, false otherwise
         */
        public boolean isEnableLogging() {
            return enableLogging;
        }
        
        /**
         * Set logging
         * 
         * @param enableLogging True to enable logging, false to disable
         * @return This settings object for chaining
         */
        public AIControllerSettings setEnableLogging(boolean enableLogging) {
            this.enableLogging = enableLogging;
            return this;
        }
        
        /**
         * Get the model type
         * 
         * @return The model type
         */
        public int getModelType() {
            return modelType;
        }
        
        /**
         * Set the model type
         * 
         * @param modelType The model type
         * @return This settings object for chaining
         */
        public AIControllerSettings setModelType(int modelType) {
            this.modelType = modelType;
            return this;
        }
        
        /**
         * Get the model path
         * 
         * @return The model path
         */
        public String getModelPath() {
            return modelPath;
        }
        
        /**
         * Set the model path
         * 
         * @param modelPath The model path
         * @return This settings object for chaining
         */
        public AIControllerSettings setModelPath(String modelPath) {
            this.modelPath = modelPath != null ? modelPath : "";
            return this;
        }
        
        /**
         * Get the confidence threshold
         * 
         * @return The confidence threshold
         */
        public float getConfidenceThreshold() {
            return confidenceThreshold;
        }
        
        /**
         * Set the confidence threshold
         * 
         * @param confidenceThreshold The confidence threshold
         * @return This settings object for chaining
         */
        public AIControllerSettings setConfidenceThreshold(float confidenceThreshold) {
            this.confidenceThreshold = confidenceThreshold;
            return this;
        }
    }
}