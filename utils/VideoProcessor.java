package utils;

import android.graphics.Bitmap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Interface for video processing functionality
 */
public interface VideoProcessor {
    /**
     * Callback interface for detection results
     */
    interface DetectionCallback {
        /**
         * Called when elements are detected in a frame
         * 
         * @param elements The detected UI elements
         * @param timestamp The timestamp of the detection
         */
        void onElementsDetected(List<UIElementInterface> elements, long timestamp);
        
        /**
         * Called when actions are detected in a frame
         * 
         * @param actions The detected actions
         * @param timestamp The timestamp of the detection
         */
        void onActionsDetected(List<DetectedAction> actions, long timestamp);
        
        /**
         * Called when an error occurs during detection
         * 
         * @param errorMessage The error message
         * @param timestamp The timestamp of the error
         */
        void onDetectionError(String errorMessage, long timestamp);
    }
    
    /**
     * Class representing a detected action
     */
    class DetectedAction {
        private String actionType;
        private UIElementInterface targetElement;
        private float confidence;
        private long timestamp;
        private Map<String, Object> data;
        
        /**
         * Create a new detected action
         * 
         * @param actionType The type of action
         * @param targetElement The target element
         * @param confidence The confidence score (0-1)
         * @param timestamp The timestamp of the detection
         */
        public DetectedAction(String actionType, UIElementInterface targetElement, float confidence, long timestamp) {
            this.actionType = actionType;
            this.targetElement = targetElement;
            this.confidence = confidence;
            this.timestamp = timestamp;
            this.data = new HashMap<>();
        }
        
        /**
         * Create a new detected action with additional data
         * 
         * @param actionType The type of action
         * @param targetElement The target element
         * @param confidence The confidence score (0-1)
         * @param timestamp The timestamp of the detection
         * @param data Additional data about the action
         */
        public DetectedAction(String actionType, UIElementInterface targetElement, float confidence, long timestamp, Map<String, Object> data) {
            this(actionType, targetElement, confidence, timestamp);
            if (data != null) {
                this.data.putAll(data);
            }
        }
        
        /**
         * Get the action type
         * 
         * @return The action type
         */
        public String getActionType() {
            return actionType;
        }
        
        /**
         * Get the target element
         * 
         * @return The target element
         */
        public UIElementInterface getTargetElement() {
            return targetElement;
        }
        
        /**
         * Get the confidence score
         * 
         * @return The confidence score (0-1)
         */
        public float getConfidence() {
            return confidence;
        }
        
        /**
         * Get the timestamp of the detection
         * 
         * @return The timestamp
         */
        public long getTimestamp() {
            return timestamp;
        }
        
        /**
         * Get additional data about the action
         * 
         * @return The data map
         */
        public Map<String, Object> getData() {
            return new HashMap<>(data);
        }
        
        /**
         * Add a data entry
         * 
         * @param key The data key
         * @param value The data value
         */
        public void addData(String key, Object value) {
            if (key != null) {
                data.put(key, value);
            }
        }
    }
    /**
     * Start video processing
     * 
     * @return True if started successfully, false otherwise
     */
    boolean start();
    
    /**
     * Stop video processing
     * 
     * @return True if stopped successfully, false otherwise
     */
    boolean stop();
    
    /**
     * Pause video processing
     * 
     * @return True if paused successfully, false otherwise
     */
    boolean pause();
    
    /**
     * Resume video processing
     * 
     * @return True if resumed successfully, false otherwise
     */
    boolean resume();
    
    /**
     * Get the current frame
     * 
     * @return The current frame as a Bitmap
     */
    Bitmap getCurrentFrame();
    
    /**
     * Process a specific frame
     * 
     * @param frame The frame to process
     * @return Processing results
     */
    Object processFrame(Bitmap frame);
    
    /**
     * Check if the processor is running
     * 
     * @return True if running, false otherwise
     */
    boolean isRunning();
    
    /**
     * Set the frame rate
     * 
     * @param fps Frames per second
     */
    void setFrameRate(int fps);
    
    /**
     * Get the current frame rate
     * 
     * @return Frames per second
     */
    int getFrameRate();
    
    /**
     * Set whether to process frames in real-time or batch mode
     * 
     * @param realTime True for real-time processing, false for batch mode
     */
    void setRealTimeProcessing(boolean realTime);
    
    /**
     * Check if real-time processing is enabled
     * 
     * @return True if real-time processing is enabled, false otherwise
     */
    boolean isRealTimeProcessing();
    
    /**
     * Check if the processor is ready for processing
     * 
     * @return True if ready, false otherwise
     */
    default boolean isReady() {
        return isRunning();
    }
    
    /**
     * Set a detection callback
     * 
     * @param callback The callback to set
     */
    default void setDetectionCallback(DetectionCallback callback) {
        // Default implementation does nothing
    }
    
    /**
     * Get the current detection callback
     * 
     * @return The current callback or null if none is set
     */
    default DetectionCallback getDetectionCallback() {
        return null;
    }
    
    /**
     * Release resources used by the processor
     */
    default void release() {
        // Default implementation does nothing
        // Subclasses should override to release resources
    }
}