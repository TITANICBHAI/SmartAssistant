package utils;

import android.graphics.Rect;

/**
 * Class representing an object detected in a video frame
 */
public class DetectedObject {
    private String type;
    private Rect bounds;
    private float confidence;
    private long timestamp;
    private UIElementInterface uiElement;
    
    /**
     * Create a new detected object
     * 
     * @param type The type of object
     * @param bounds The bounding rectangle
     * @param confidence The confidence score (0-1)
     * @param timestamp The timestamp of the detection
     */
    public DetectedObject(String type, Rect bounds, float confidence, long timestamp) {
        this.type = type;
        this.bounds = bounds;
        this.confidence = confidence;
        this.timestamp = timestamp;
    }
    
    /**
     * Create a new detected object with UI element
     * 
     * @param type The type of object
     * @param bounds The bounding rectangle
     * @param confidence The confidence score (0-1)
     * @param timestamp The timestamp of the detection
     * @param uiElement The associated UI element
     */
    public DetectedObject(String type, Rect bounds, float confidence, long timestamp, UIElementInterface uiElement) {
        this(type, bounds, confidence, timestamp);
        this.uiElement = uiElement;
    }

    /**
     * Create a new detected object from a RectF with current timestamp
     * 
     * @param type The type of object
     * @param confidence The confidence score (0-1)
     * @param rectf The bounding rectangle as RectF
     */
    public DetectedObject(String type, float confidence, android.graphics.RectF rectf) {
        this(type, new Rect(
            Math.round(rectf.left),
            Math.round(rectf.top),
            Math.round(rectf.right),
            Math.round(rectf.bottom)
        ), confidence, System.currentTimeMillis());
    }
    
    /**
     * Get the object type
     * 
     * @return The object type
     */
    public String getType() {
        return type;
    }
    
    /**
     * Get the bounding rectangle
     * 
     * @return The bounding rectangle
     */
    public Rect getBounds() {
        return bounds;
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
     * Get the associated UI element
     * 
     * @return The UI element or null if none
     */
    public UIElementInterface getUIElement() {
        return uiElement;
    }
    
    /**
     * Set the associated UI element
     * 
     * @param uiElement The UI element to associate
     */
    public void setUIElement(UIElementInterface uiElement) {
        this.uiElement = uiElement;
    }
}