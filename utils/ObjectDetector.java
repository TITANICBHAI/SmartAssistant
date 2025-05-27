package utils;

import java.util.List;
import java.util.Map;

/**
 * Interface for object detection functionality.
 */
public interface ObjectDetector {
    /**
     * Inner class to represent a detected object.
     */
    public static class DetectedObject {
        private final String objectType;
        private final Rect boundingBox;
        private final float confidence;
        private final Map<String, Object> properties;
        
        /**
         * Create a new detected object.
         * 
         * @param objectType The type of object detected
         * @param boundingBox The bounding rectangle of the object
         * @param confidence The confidence score (0.0 to 1.0)
         * @param properties Additional properties of the object
         */
        public DetectedObject(String objectType, Rect boundingBox, float confidence, Map<String, Object> properties) {
            this.objectType = objectType;
            this.boundingBox = boundingBox;
            this.confidence = confidence;
            this.properties = properties;
        }
        
        /**
         * Get the type of this object.
         */
        public String getObjectType() {
            return objectType;
        }
        
        /**
         * Get the bounding box of this object.
         */
        public Rect getBoundingBox() {
            return boundingBox;
        }
        
        /**
         * Get the confidence score.
         */
        public float getConfidence() {
            return confidence;
        }
        
        /**
         * Get the object properties.
         */
        public Map<String, Object> getProperties() {
            return properties;
        }
    }
    
    /**
     * Detect objects in an image.
     * 
     * @param image The image to analyze
     * @return A list of detected objects
     */
    List<utils.DetectedObject> detectObjects(Bitmap image);
    
    /**
     * Detect objects in an image with a minimum confidence threshold.
     * 
     * @param image The image to analyze
     * @param minConfidence The minimum confidence threshold (0.0 to 1.0)
     * @return A list of detected objects
     */
    List<utils.DetectedObject> detectObjects(Bitmap image, float minConfidence);
    
    /**
     * Configure the detector with the specified settings.
     * 
     * @param settings Configuration settings
     */
    void configure(Map<String, Object> settings);
    
    /**
     * Get the current detector configuration.
     * 
     * @return The current configuration settings
     */
    Map<String, Object> getConfiguration();
    
    /**
     * Register a listener for detection events.
     * 
     * @param listener The listener to register
     */
    void addDetectionListener(ObjectDetectionListener listener);
    
    /**
     * Unregister a detection listener.
     * 
     * @param listener The listener to unregister
     */
    void removeDetectionListener(ObjectDetectionListener listener);
}