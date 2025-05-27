package utils;

import android.content.Context;
import android.graphics.Bitmap;
import java.util.List;
import java.util.Map;

/**
 * Interface defining the AI controller functionality
 */
public interface AIControllerInterface {
    /**
     * Initialize the AI controller
     * 
     * @param context The Android context
     * @return True if initialization was successful, false otherwise
     */
    boolean initialize(Context context);
    
    /**
     * Process video input from a video processor
     * 
     * @param processor The video processor to use
     * @return True if processing was successful, false otherwise
     */
    boolean processVideoInput(VideoProcessor processor);
    
    /**
     * Process a screenshot
     * 
     * @param screenshot The screenshot to process
     * @return True if processing was successful, false otherwise
     */
    boolean processScreenshotInput(Bitmap screenshot);
    
    /**
     * Process text input
     * 
     * @param text The text to process
     * @return Results of the text processing
     */
    Map<String, Object> processTextInput(String text);
    
    /**
     * Get the current state of the AI controller
     * 
     * @return The current state
     */
    Map<String, Object> getCurrentState();
    
    /**
     * Analyze the current state
     * 
     * @return Analysis results
     */
    Map<String, Object> analyzeCurrentState();
    
    /**
     * Update the AI model with new data
     * 
     * @param modelData The data to update with
     * @return True if update was successful, false otherwise
     */
    boolean updateModel(Map<String, Object> modelData);
    
    /**
     * Train the AI model with new data
     * 
     * @param trainingData The data to train with
     * @return True if training was successful, false otherwise
     */
    boolean trainModel(List<Map<String, Object>> trainingData);
    
    /**
     * Save the AI model to a file
     * 
     * @param filePath The file path to save to
     * @return True if save was successful, false otherwise
     */
    boolean saveModel(String filePath);
    
    /**
     * Load the AI model from a file
     * 
     * @param filePath The file path to load from
     * @return True if load was successful, false otherwise
     */
    boolean loadModel(String filePath);
    
    /**
     * Predict the next action
     * 
     * @return The predicted action
     */
    Object predictNextAction();
    
    /**
     * Get the confidence of a prediction
     * 
     * @param prediction The prediction to check
     * @return The confidence value (0-1)
     */
    float getPredictionConfidence(Map<String, Object> prediction);
    
    /**
     * Perform an action
     * 
     * @param actionType The type of action to perform
     * @param parameters The parameters for the action
     * @return Results of the action
     */
    Map<String, Object> performAction(String actionType, Map<String, Object> parameters);
    
    /**
     * Shut down the AI controller
     */
    void shutdown();
    
    /**
     * Click at a specific location
     * 
     * @param x The X coordinate
     * @param y The Y coordinate
     * @param callback Callback for click status
     * @return True if click was initiated, false otherwise
     */
    boolean clickAction(float x, float y, ActionCallback callback);
    
    /**
     * Long press at a specific location
     * 
     * @param x The X coordinate
     * @param y The Y coordinate
     * @param callback Callback for long press status
     * @return True if long press was initiated, false otherwise
     */
    boolean longPressAction(float x, float y, ActionCallback callback);
    
    /**
     * Swipe from one location to another
     * 
     * @param startX Starting X coordinate
     * @param startY Starting Y coordinate
     * @param endX Ending X coordinate
     * @param endY Ending Y coordinate
     * @param duration Duration of the swipe in milliseconds
     * @param callback Callback for swipe status
     * @return True if swipe was initiated, false otherwise
     */
    boolean swipeAction(float startX, float startY, float endX, float endY, long duration, ActionCallback callback);
    
    /**
     * Set the number of target lives for the current game
     * 
     * @param lives The number of lives
     */
    void setCurrentGameTargetLives(int lives);
}