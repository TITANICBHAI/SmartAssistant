package utils;

import android.graphics.Rect;
import android.graphics.RectF;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import models.StandardizedUIElement;
import models.StandardizedUIElementType;

/**
 * Utility class for converting between different UI element representations
 */
public class UIElementConverter {
    
    /**
     * Convert a UIElement to a StandardizedUIElement
     * 
     * @param element The UIElement to convert
     * @return Standardized UI element
     */
    public static StandardizedUIElement toStandardizedElement(UIElement element) {
        if (element == null) {
            return null;
        }
        
        StandardizedUIElement standardized = new StandardizedUIElement();
        standardized.setId(element.getId());
        standardized.setType(convertElementType(element.getType()));
        standardized.setText(element.getText());
        
        // Get content description from attributes since some implementations might not have it directly
        String contentDesc = null;
        if (element.getAttributes() != null && element.getAttributes().containsKey("contentDescription")) {
            Object descObj = element.getAttributes().get("contentDescription");
            if (descObj instanceof String) {
                contentDesc = (String) descObj;
            }
        }
        standardized.setContentDescription(contentDesc);
        
        standardized.setClickable(element.isClickable());
        standardized.setVisible(element.isVisible());
        standardized.setConfidence(element.getConfidence());
        
        // Handle bounds conversion
        int[] boundsArray = element.getBoundsArray();
        if (boundsArray != null && boundsArray.length >= 4) {
            standardized.setBounds(boundsArray);
        } else {
            // Fallback to other methods if available
            if (element instanceof UIElementInterface) {
                utils.Rect rect = ((UIElementInterface) element).getRectBounds();
                if (rect != null) {
                    standardized.setBounds(new int[] {rect.left, rect.top, rect.width(), rect.height()});
                }
            }
        }
        
        // Copy attributes
        Map<String, Object> attributes = element.getAttributes();
        if (attributes != null) {
            standardized.setAttributes(new HashMap<>(attributes));
        }
        
        return standardized;
    }
    
    /**
     * Convert a StandardizedUIElement to a UIElement
     * 
     * @param standardized The StandardizedUIElement to convert
     * @return UI element
     */
    public static UIElement fromStandardizedElement(StandardizedUIElement standardized) {
        if (standardized == null) {
            return null;
        }
        
        DefaultUIElement defaultElement = new DefaultUIElement();
        defaultElement.setId(standardized.getId());
        defaultElement.setType(convertToUIElementType(standardized.getType()));
        defaultElement.setText(standardized.getText());
        
        // Set attributes first
        Map<String, Object> attributes = standardized.getAttributes();
        if (attributes != null) {
            defaultElement.setAttributes(new HashMap<>(attributes));
        }
        
        // Set content description as an attribute
        String contentDesc = standardized.getContentDescription();
        if (contentDesc != null && !contentDesc.isEmpty()) {
            defaultElement.setAttribute("contentDescription", contentDesc);
        }
        
        defaultElement.setClickable(standardized.isClickable());
        
        // Set visible status
        defaultElement.setVisible(standardized.isVisible());
        
        // Set confidence value
        defaultElement.setConfidence(standardized.getConfidence());
        
        // Handle bounds conversion
        int[] bounds = standardized.getBoundsArray();
        if (bounds != null && bounds.length >= 4) {
            defaultElement.setBounds(bounds);
        }
        
        // Convert the DefaultUIElement to a UIElement using the adapter
        return defaultElement.convertToUIElement();
    }
    
    /**
     * Convert a list of UIElements to a list of StandardizedUIElements
     * 
     * @param elements List of UIElements to convert
     * @return List of standardized UI elements
     */
    public static List<StandardizedUIElement> toStandardizedElements(List<UIElement> elements) {
        if (elements == null) {
            return null;
        }
        
        List<StandardizedUIElement> standardizedList = new ArrayList<>(elements.size());
        for (UIElement element : elements) {
            StandardizedUIElement standardized = toStandardizedElement(element);
            if (standardized != null) {
                standardizedList.add(standardized);
            }
        }
        
        return standardizedList;
    }
    
    /**
     * Convert a list of StandardizedUIElements to a list of UIElements
     * 
     * @param standardizedElements List of StandardizedUIElements to convert
     * @return List of UI elements
     */
    public static List<UIElement> fromStandardizedElements(List<StandardizedUIElement> standardizedElements) {
        if (standardizedElements == null) {
            return null;
        }
        
        List<UIElement> elementsList = new ArrayList<>(standardizedElements.size());
        for (StandardizedUIElement standardized : standardizedElements) {
            UIElement element = fromStandardizedElement(standardized);
            if (element != null) {
                elementsList.add(element);
            }
        }
        
        return elementsList;
    }
    
    /**
     * Attempt to extract a RectF from an unknown rectangle object (private method)
     * 
     * @param rect Rectangle object of unknown type
     * @return RectF representation or null if conversion failed
     */
    private static RectF extractRectFPrivate(Object rect) {
        if (rect == null) {
            return null;
        }
        
        // Use RectHelper to convert different rectangle types
        return RectHelper.toRectF(rect);
    }
    
    /**
     * Convert a UI element type string to a StandardizedUIElementType
     * 
     * @param typeStr Type string
     * @return StandardizedUIElementType
     */
    public static StandardizedUIElementType convertElementType(String typeStr) {
        if (typeStr == null || typeStr.isEmpty()) {
            return StandardizedUIElementType.UNKNOWN;
        }
        
        return StandardizedUIElementType.fromString(typeStr);
    }
    
    /**
     * Convert a UIElement.ElementType to a StandardizedUIElementType
     * 
     * @param elementType UIElement.ElementType
     * @return StandardizedUIElementType
     */
    public static StandardizedUIElementType convertElementType(UIElement.ElementType elementType) {
        if (elementType == null) {
            return StandardizedUIElementType.UNKNOWN;
        }
        
        try {
            return StandardizedUIElementType.valueOf(elementType.name());
        } catch (IllegalArgumentException e) {
            return StandardizedUIElementType.UNKNOWN;
        }
    }
    
    /**
     * Convert a StandardizedUIElementType to a UIElement.ElementType
     * 
     * @param standardizedType StandardizedUIElementType
     * @return UIElement.ElementType
     */
    public static UIElement.ElementType convertToUIElementType(StandardizedUIElementType standardizedType) {
        if (standardizedType == null) {
            return UIElement.ElementType.UNKNOWN;
        }
        
        try {
            return UIElement.ElementType.valueOf(standardizedType.name());
        } catch (IllegalArgumentException e) {
            return UIElement.ElementType.UNKNOWN;
        }
    }
    
    /**
     * Convert a String type to an ElementType
     * 
     * @param typeStr The type as a string
     * @return The converted ElementType
     */
    public static UIElement.ElementType convertToUIElementType(String typeStr) {
        if (typeStr == null || typeStr.isEmpty()) {
            return UIElement.ElementType.UNKNOWN;
        }
        
        try {
            return UIElement.ElementType.valueOf(typeStr.toUpperCase());
        } catch (IllegalArgumentException e) {
            // If the direct conversion fails, try matching by name
            for (UIElement.ElementType type : UIElement.ElementType.values()) {
                if (type.name().equalsIgnoreCase(typeStr) || 
                    type.toString().equalsIgnoreCase(typeStr)) {
                    return type;
                }
            }
            return UIElement.ElementType.UNKNOWN;
        }
    }
    
    /**
     * Convert any object to a RectF using RectHelper
     * 
     * @param rect Object representing a rectangle
     * @return RectF object or null if conversion fails
     */
    public static android.graphics.RectF extractRectF(Object rect) {
        if (rect == null) {
            return null;
        }
        
        try {
            return RectHelper.toRectF(rect);
        } catch (Exception e) {
            // If conversion fails, return null
            return null;
        }
    }
    
    /**
     * Alias for toStandardizedElement for backward compatibility
     * 
     * @param element The UIElement to convert
     * @return Standardized UI element
     */
    public static StandardizedUIElement toStandardized(UIElement element) {
        return toStandardizedElement(element);
    }
    
    /**
     * Alias for fromStandardizedElement for backward compatibility
     * 
     * @param standardized The StandardizedUIElement to convert
     * @return UI element
     */
    public static UIElement fromStandardized(StandardizedUIElement standardized) {
        return fromStandardizedElement(standardized);
    }
    
    /**
     * Alias for toStandardizedElements for backward compatibility
     * 
     * @param elements List of UIElements to convert
     * @return List of standardized UI elements
     */
    public static List<StandardizedUIElement> toStandardizedList(List<UIElement> elements) {
        return toStandardizedElements(elements);
    }
    
    /**
     * Alias for fromStandardizedElements for backward compatibility
     * 
     * @param standardizedElements List of StandardizedUIElements to convert
     * @return List of UI elements
     */
    public static List<UIElement> fromStandardizedList(List<StandardizedUIElement> standardizedElements) {
        return fromStandardizedElements(standardizedElements);
    }
}