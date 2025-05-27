package utils;

import java.util.List;

/**
 * Interface for object detection listeners.
 */
public interface ObjectDetectionListener {
    /**
     * Called when objects are detected.
     * 
     * @param objects List of detected objects
     * @param timestamp Timestamp of the detection
     */
    void onObjectsDetected(List<DetectedObject> objects, long timestamp);
}