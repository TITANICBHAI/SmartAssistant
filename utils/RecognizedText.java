package utils;

import android.graphics.Rect;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Represents text recognized in an image, with position and confidence information.
 */
public class RecognizedText implements Comparable<RecognizedText> {
    private String text;
    private Rect boundingBox;
    private float confidence;
    private Map<String, Object> metadata;
    
    /**
     * Default constructor.
     */
    public RecognizedText() {
        text = "";
        boundingBox = new Rect();
        confidence = 0f;
        metadata = new HashMap<>();
    }
    
    /**
     * Constructor with text and bounding box.
     * 
     * @param text The recognized text
     * @param boundingBox The bounding box of the text in the image
     */
    public RecognizedText(String text, Rect boundingBox) {
        this.text = text != null ? text : "";
        this.boundingBox = boundingBox != null ? boundingBox : new Rect();
        confidence = 1.0f;
        metadata = new HashMap<>();
    }
    
    /**
     * Constructor with text, bounding box, and confidence.
     * 
     * @param text The recognized text
     * @param boundingBox The bounding box of the text in the image
     * @param confidence The confidence value (0-1)
     */
    public RecognizedText(String text, Rect boundingBox, float confidence) {
        this(text, boundingBox);
        this.confidence = confidence;
    }
    
    /**
     * Get the recognized text.
     * 
     * @return The text
     */
    public String getText() {
        return text;
    }
    
    /**
     * Set the recognized text.
     * 
     * @param text The text to set
     */
    public void setText(String text) {
        this.text = text != null ? text : "";
    }
    
    /**
     * Get the bounding box of the text in the image.
     * 
     * @return The bounding box
     */
    public Rect getBoundingBox() {
        return boundingBox;
    }
    
    /**
     * Set the bounding box of the text in the image.
     * 
     * @param boundingBox The bounding box to set
     */
    public void setBoundingBox(Rect boundingBox) {
        this.boundingBox = boundingBox != null ? boundingBox : new Rect();
    }
    
    /**
     * Get the confidence value of the recognition.
     * 
     * @return The confidence value (0-1)
     */
    public float getConfidence() {
        return confidence;
    }
    
    /**
     * Set the confidence value of the recognition.
     * 
     * @param confidence The confidence value to set (0-1)
     */
    public void setConfidence(float confidence) {
        this.confidence = confidence;
    }
    
    /**
     * Get a metadata value.
     * 
     * @param key The metadata key
     * @return The metadata value or null if not found
     */
    public Object getMetadata(String key) {
        return metadata.get(key);
    }
    
    /**
     * Set a metadata value.
     * 
     * @param key The metadata key
     * @param value The metadata value
     */
    public void setMetadata(String key, Object value) {
        metadata.put(key, value);
    }
    
    /**
     * Get all metadata.
     * 
     * @return The metadata map
     */
    public Map<String, Object> getMetadataMap() {
        return metadata;
    }
    
    /**
     * Get the width of the bounding box.
     * 
     * @return The width
     */
    public int getWidth() {
        return boundingBox.width();
    }
    
    /**
     * Get the height of the bounding box.
     * 
     * @return The height
     */
    public int getHeight() {
        return boundingBox.height();
    }
    
    /**
     * Get the X coordinate of the center of the bounding box.
     * 
     * @return The center X coordinate
     */
    public int getCenterX() {
        return (int)RectHelper.centerX(boundingBox);
    }
    
    /**
     * Get the Y coordinate of the center of the bounding box.
     * 
     * @return The center Y coordinate
     */
    public int getCenterY() {
        return (int)RectHelper.centerY(boundingBox);
    }
    
    /**
     * Check if this text area intersects with another.
     * 
     * @param other The other recognized text
     * @return True if they intersect
     */
    public boolean intersects(RecognizedText other) {
        if (other == null) {
            return false;
        }
        return RectHelper.intersects(boundingBox, other.boundingBox);
    }
    
    /**
     * Calculate the distance between the center of this text and the center of another.
     * 
     * @param other The other recognized text
     * @return The distance between centers
     */
    public float distanceTo(RecognizedText other) {
        if (other == null) {
            return Float.MAX_VALUE;
        }
        return RectHelper.distanceBetweenCenters(boundingBox, other.boundingBox);
    }
    
    /**
     * Create a new RecognizedText that represents the union of this one and another.
     * 
     * @param other The other recognized text
     * @return A new RecognizedText representing the union
     */
    public RecognizedText union(RecognizedText other) {
        if (other == null) {
            return new RecognizedText(text, boundingBox, confidence);
        }
        
        Rect unionRect = RectHelper.union(boundingBox, other.boundingBox);
        String unionText = text;
        if (!other.text.isEmpty()) {
            unionText = unionText.isEmpty() ? other.text : unionText + " " + other.text;
        }
        
        float unionConfidence = (confidence + other.confidence) / 2.0f;
        
        return new RecognizedText(unionText, unionRect, unionConfidence);
    }
    
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("RecognizedText{");
        sb.append("text='").append(text).append('\'');
        sb.append(", bounds=").append(RectHelper.toShortString(boundingBox));
        sb.append(", confidence=").append(confidence);
        sb.append('}');
        return sb.toString();
    }
    
    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null || getClass() != obj.getClass()) {
            return false;
        }
        
        RecognizedText other = (RecognizedText) obj;
        return text.equals(other.text) &&
                RectHelper.equals(boundingBox, other.boundingBox) &&
                Math.abs(confidence - other.confidence) < 0.001f;
    }
    
    @Override
    public int hashCode() {
        int result = text.hashCode();
        result = 31 * result + boundingBox.hashCode();
        result = 31 * result + Float.floatToIntBits(confidence);
        return result;
    }
    
    @Override
    public int compareTo(RecognizedText other) {
        if (other == null) {
            return 1;
        }
        
        // Compare by Y position first (top to bottom)
        int yDiff = boundingBox.top - other.boundingBox.top;
        if (Math.abs(yDiff) > 10) { // Small threshold to group items on the same line
            return yDiff;
        }
        
        // If on roughly the same line, compare by X position (left to right)
        return boundingBox.left - other.boundingBox.left;
    }
    
    /**
     * Create a list of RecognizedText objects from OCR results.
     * 
     * @param text Array of recognized text strings
     * @param bounds Array of bounding boxes for each text
     * @param confidences Array of confidence values for each text
     * @return List of RecognizedText objects
     */
    public static List<RecognizedText> fromOcrResults(String[] text, Rect[] bounds, float[] confidences) {
        List<RecognizedText> results = new ArrayList<>();
        
        if (text == null || bounds == null) {
            return results;
        }
        
        int count = Math.min(text.length, bounds.length);
        
        for (int i = 0; i < count; i++) {
            float confidence = (confidences != null && i < confidences.length) ? confidences[i] : 1.0f;
            results.add(new RecognizedText(text[i], bounds[i], confidence));
        }
        
        return results;
    }
}