package utils;

import android.graphics.Rect;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

/**
 * Helper class for UI element detection
 * Provides methods to process and analyze UI elements
 */
public class UIElementDetectionHelper {
    private Context context;
    private UIElementDetector detector;
    private boolean isInitialized = false;
    
    /**
     * Initialize the helper with application context
     * 
     * @param context Application context
     */
    public void initialize(Object context) {
        if (isInitialized) {
            return;
        }
        
        if (context instanceof Context) {
            this.context = (Context) context;
        } else {
            // Create an empty context if the passed object is not a Context
            this.context = ContextCompatHelper.createEmptyContext();
        }
        
        this.detector = new UIElementDetector();
        this.detector.initialize(this.context);
        this.isInitialized = true;
    }
    
    /**
     * Detect UI elements in a screenshot
     * 
     * @param screenshot Screenshot bitmap to analyze
     * @return List of detected UI elements
     */
    public List<UIElement> detectUIElements(Object screenshot) {
        if (!isInitialized || screenshot == null) {
            return new ArrayList<>();
        }
        
        // Handle both Bitmap and BufferedImage types
        if (screenshot instanceof utils.Bitmap) {
            // Convert utils.Bitmap to android.graphics.Bitmap
            android.graphics.Bitmap androidBitmap = BitmapConverter.toAndroidBitmap((utils.Bitmap)screenshot);
            // We'll use the OTHER game type as default
            return detector.detectElements(androidBitmap, GameType.OTHER);
        } else if (screenshot instanceof java.awt.image.BufferedImage) {
            // Convert BufferedImage to utils.Bitmap, then to android.graphics.Bitmap
            utils.Bitmap bitmap = BitmapConverter.fromBufferedImage((java.awt.image.BufferedImage) screenshot);
            android.graphics.Bitmap androidBitmap = BitmapConverter.toAndroidBitmap(bitmap);
            return detector.detectElements(androidBitmap, GameType.OTHER);
        }
        
        return new ArrayList<>();
    }
    
    /**
     * Detect UI elements in a screenshot (legacy method)
     * 
     * @param screenshot Screenshot bitmap to analyze
     * @return List of detected UI elements
     */
    public List<UIElement> detectUIElements(Bitmap screenshot) {
        return detectUIElements((Object)screenshot);
    }
    
    /**
     * Find text elements that overlap with UI elements
     * 
     * @param elements List of UI elements
     * @param texts List of recognized text elements
     * @return List of UI elements with associated text
     */
    public static List<UIElement> findElementsWithText(List<UIElement> elements, 
                                                   List<RecognizedText> texts) {
        if (elements == null || texts == null) {
            return new ArrayList<>();
        }
        
        List<UIElement> result = new ArrayList<>();
        
        for (UIElement element : elements) {
            for (RecognizedText text : texts) {
                // Convert bounds to Rect objects for intersection test
                utils.Rect elementRect = null;
                
                // Check if element.getBounds() returns android.graphics.Rect or utils.Rect
                Object bounds = element.getBounds();
                if (bounds instanceof android.graphics.Rect) {
                    elementRect = BitmapConverter.toUtilsRect((android.graphics.Rect)bounds);
                } else if (bounds instanceof utils.Rect) {
                    // Already a utils.Rect, no conversion needed
                    elementRect = (utils.Rect)bounds;
                } else {
                    // Unknown type, skip this element
                    continue;
                }
                
                utils.Rect textRect = null;
                
                // Get the bounding box from the recognized text
                // RecognizedText.getBoundingBox() returns android.graphics.Rect, need to convert
                android.graphics.Rect boundingBox = text.getBoundingBox();
                if (boundingBox != null) {
                    textRect = BitmapConverter.toUtilsRect(boundingBox);
                } else {
                    // Skip if there's no bounding box
                    continue;
                }
                
                if (RectHelper.intersects(elementRect, textRect)) {
                    // Create a copy of the element with the recognized text
                    // Create a new UIElement (not StandardizedUIElement) to match return type
                    UIElement elementWithText = new UIElement();
                    // Set properties
                    elementWithText.setId(element.getId());
                    elementWithText.setBounds(elementRect);
                    elementWithText.setType(element.getType());
                    elementWithText.setText(text.getText());
                    elementWithText.setClickable(element.isClickable());
                    elementWithText.setConfidence(element.getConfidence());
                    // Set content description using reflection to avoid compile errors
                    try {
                        Method getContentDescMethod = element.getClass().getMethod("getContentDescription");
                        Object contentDesc = getContentDescMethod.invoke(element);
                        if (contentDesc != null) {
                            elementWithText.setProperty("contentDescription", contentDesc.toString());
                        }
                    } catch (Exception e) {
                        // Ignore if content description methods aren't available
                    }
                    
                    // No need to cast since elementWithText is already a utils.UIElement
                    result.add(elementWithText);
                }
            }
        }
        
        return result;
    }
    
    /**
     * Find all UI elements that match a specific query
     * 
     * @param elements List of UI elements to search
     * @param query Search query
     * @return List of matching UI elements
     */
    public static List<UIElement> findMatchingElements(List<UIElement> elements, String query) {
        if (elements == null || query == null || query.isEmpty()) {
            return new ArrayList<>();
        }
        
        List<UIElement> result = new ArrayList<>();
        
        for (UIElement element : elements) {
            // Check for matching text
            if (element.getText() != null && element.getText().toLowerCase().contains(query.toLowerCase())) {
                result.add(element);
                continue;
            }
            
            // Try to get content description via reflection
            try {
                Method getContentDescMethod = element.getClass().getMethod("getContentDescription");
                Object contentDesc = getContentDescMethod.invoke(element);
                if (contentDesc != null && contentDesc.toString().toLowerCase().contains(query.toLowerCase())) {
                    result.add(element);
                }
            } catch (Exception e) {
                // Ignore if content description method isn't available
            }
        }
        
        return result;
    }
    
    /**
     * Group UI elements by their type
     * 
     * @param elements List of UI elements
     * @return Map of element types to lists of elements
     */
    public static java.util.Map<String, List<UIElement>> groupElementsByType(List<UIElement> elements) {
        java.util.Map<String, List<UIElement>> result = new java.util.HashMap<>();
        
        if (elements == null) {
            return result;
        }
        
        for (UIElement element : elements) {
            utils.ElementType elementType = element.getUtilsElementType();
            String type = (elementType != null) ? elementType.toString() : "UNKNOWN";
            
            if (!result.containsKey(type)) {
                result.put(type, new ArrayList<>());
            }
            
            result.get(type).add(element);
        }
        
        return result;
    }
    
    /**
     * Find UI elements at a specific point
     * 
     * @param elements List of UI elements
     * @param x X coordinate
     * @param y Y coordinate
     * @return List of elements containing the point
     */
    public static List<UIElement> findElementsAtPoint(List<UIElement> elements, int x, int y) {
        if (elements == null) {
            return new ArrayList<>();
        }
        
        List<UIElement> result = new ArrayList<>();
        
        for (UIElement element : elements) {
            if (element.getBounds() != null && element.contains(x, y)) {
                result.add(element);
            }
        }
        
        return result;
    }
}