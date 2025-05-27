package utils;

import android.graphics.Bitmap;
import java.util.List;

/**
 * Interface for recognizing UI elements in images.
 * Implementations of this interface can identify different types of UI elements such as buttons,
 * text fields, images, etc., from screenshots.
 */
public interface UIElementRecognizer {
    
    /**
     * Recognize UI elements in the given screenshot.
     * 
     * @param screenshot The screenshot to analyze for UI elements
     * @return A list of recognized UI elements
     */
    List<UIElement> recognizeElements(Bitmap screenshot);
    
    /**
     * Get the types of UI elements that this recognizer can identify.
     * 
     * @return An array of element types that this recognizer can identify
     */
    String[] getSupportedElementTypes();
    
    /**
     * Check if this recognizer supports a specific UI element type.
     * 
     * @param elementType The type of UI element to check
     * @return True if this recognizer supports the specified element type, false otherwise
     */
    default boolean supportsElementType(String elementType) {
        if (elementType == null) {
            return false;
        }
        
        String[] supportedTypes = getSupportedElementTypes();
        if (supportedTypes == null) {
            return false;
        }
        
        for (String type : supportedTypes) {
            if (elementType.equals(type)) {
                return true;
            }
        }
        
        return false;
    }
}