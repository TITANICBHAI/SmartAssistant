package utils;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Helper class for working with StandardizedUIElement objects.
 */
public class StandardizedUIElementHelper {
    /**
     * Convert an array of UIElementInterface objects to StandardizedUIElement objects.
     * 
     * @param elements The array of UIElementInterface objects
     * @return An array of StandardizedUIElement objects
     */
    public static StandardizedUIElement[] toStandardizedElements(UIElementInterface[] elements) {
        if (elements == null || elements.length == 0) {
            return new StandardizedUIElement[0];
        }
        
        List<StandardizedUIElement> result = new ArrayList<>();
        for (UIElementInterface element : elements) {
            if (element == null) {
                continue;
            }
            
            StandardizedUIElement standardized = toStandardizedElement(element);
            if (standardized != null) {
                result.add(standardized);
            }
        }
        
        return result.toArray(new StandardizedUIElement[0]);
    }
    
    /**
     * Convert a UIElementInterface to a StandardizedUIElement.
     * 
     * @param element The UIElementInterface to convert
     * @return A StandardizedUIElement
     */
    public static StandardizedUIElement toStandardizedElement(UIElementInterface element) {
        if (element == null) {
            return null;
        }
        
        // If it's already a StandardizedUIElement, return it
        if (element instanceof StandardizedUIElement) {
            return (StandardizedUIElement) element;
        }
        
        // If it's a wrapper that contains a StandardizedUIElement, unwrap it
        if (element instanceof UIElementWrapper && ((UIElementWrapper) element).getWrappedElement() instanceof StandardizedUIElement) {
            return (StandardizedUIElement) ((UIElementWrapper) element).getWrappedElement();
        }
        
        // Create a new StandardizedUIElement with the properties from the input element
        StandardizedUIElement result = new StandardizedUIElement(
            element.getId(), 
            element.getText(), 
            element.getType(), 
            element.getRectBounds()
        );
        
        result.setClickable(element.isClickable());
        result.setVisible(element.isVisible());
        result.setEnabled(element.isEnabled());
        result.setConfidence(element.getConfidence());
        
        // Copy attributes and properties
        if (element.getAttributes() != null) {
            result.setAttributes(element.getAttributes());
        }
        
        if (element.getProperties() != null) {
            result.setProperties(element.getProperties());
        }
        
        // Convert child elements if available
        UIElementInterface[] children = element.getChildren();
        if (children != null && children.length > 0) {
            result.setChildren(children);
        }
        
        return result;
    }
    
    /**
     * Convert a StandardizedUIElement to a DefaultUIElement.
     * 
     * @param standardized The StandardizedUIElement to convert
     * @return A DefaultUIElement
     */
    public static DefaultUIElement toDefaultUIElement(StandardizedUIElement standardized) {
        if (standardized == null) {
            return null;
        }
        
        DefaultUIElement element = new DefaultUIElement(standardized.getId(), standardized.getText(), standardized.getRectBounds());
        
        // Set type
        String typeStr = standardized.getType();
        if (typeStr != null && !typeStr.isEmpty()) {
            element.setType(typeStr);
        }
        
        // Set bounds
        Rect bounds = standardized.getRectBounds();
        if (bounds != null) {
            element.setBounds(bounds);
        } else {
            int[] boundsArray = standardized.getBoundsArray();
            if (boundsArray != null && boundsArray.length >= 4) {
                element.setBounds(boundsArray);
            }
        }
        
        // Set properties
        element.setClickable(standardized.isClickable());
        element.setVisible(standardized.isVisible());
        element.setEnabled(standardized.isEnabled());
        element.setSelectable(standardized.isSelectable());
        element.setSelected(standardized.isSelected());
        element.setScrollable(standardized.isScrollable());
        element.setEditable(standardized.isEditable());
        element.setFocusable(standardized.isFocusable());
        element.setConfidence(standardized.getConfidence());
        
        // Copy properties
        for (Map.Entry<String, Object> entry : standardized.getProperties().entrySet()) {
            element.setProperty(entry.getKey(), entry.getValue());
        }
        
        return element;
    }
    
    /**
     * Create a UIElementAdapter from a StandardizedUIElement.
     * 
     * @param standardized The StandardizedUIElement to convert
     * @return A UIElementAdapter
     */
    public static UIElementAdapter toUIElementAdapter(StandardizedUIElement standardized) {
        if (standardized == null) {
            return null;
        }
        
        // Convert StandardizedUIElement to DefaultUIElement first
        DefaultUIElement defaultElement = toDefaultUIElement(standardized);
        // Then use the DefaultUIElement.convertToUIElement method
        UIElement uiElement = defaultElement.convertToUIElement();
        return new UIElementAdapter(uiElement);
    }
    
    /**
     * Create a UIElementAdapter from any UIElementInterface.
     * 
     * @param element The UIElementInterface to convert
     * @return A UIElementAdapter
     */
    public static UIElementAdapter toUIElementAdapter(UIElementInterface element) {
        if (element == null) {
            return null;
        }
        
        if (element instanceof StandardizedUIElement) {
            return toUIElementAdapter((StandardizedUIElement) element);
        }
        
        // Convert to StandardizedUIElement first
        StandardizedUIElement standardized = toStandardizedElement(element);
        return toUIElementAdapter(standardized);
    }
    
    /**
     * Create a StandardizedUIElement from a Rect.
     * 
     * @param rect The Rect to convert
     * @param type The element type
     * @return A StandardizedUIElement
     */
    public static StandardizedUIElement fromRect(Rect rect, String type) {
        if (rect == null) {
            return null;
        }
        
        String id = "rect_" + System.currentTimeMillis();
        StandardizedUIElement element = new StandardizedUIElement(id, "", type, rect);
        element.setVisible(true);
        element.setClickable(true);
        
        return element;
    }
    
    /**
     * Create a StandardizedUIElement from coordinates.
     * 
     * @param left The left coordinate
     * @param top The top coordinate
     * @param right The right coordinate
     * @param bottom The bottom coordinate
     * @param type The element type
     * @return A StandardizedUIElement
     */
    public static StandardizedUIElement fromCoordinates(int left, int top, int right, int bottom, String type) {
        Rect rect = new Rect(left, top, right, bottom);
        return fromRect(rect, type);
    }
    
    /**
     * Get a description of an element.
     * 
     * @param element The element to describe
     * @return A description string
     */
    public static String getElementDescription(UIElementInterface element) {
        if (element == null) {
            return "null";
        }
        
        StringBuilder sb = new StringBuilder();
        sb.append(element.getType()).append(": ");
        
        String text = element.getText();
        if (text != null && !text.isEmpty()) {
            sb.append("\"").append(text).append("\" ");
        }
        
        String id = element.getId();
        if (id != null && !id.isEmpty()) {
            sb.append("(ID: ").append(id).append(") ");
        }
        
        Rect bounds = element.getRectBounds();
        sb.append(bounds != null ? bounds.toString() : "null bounds");
        
        if (element.isClickable()) {
            sb.append(" [clickable]");
        }
        
        if (!element.isVisible()) {
            sb.append(" [invisible]");
        }
        
        if (!element.isEnabled()) {
            sb.append(" [disabled]");
        }
        
        return sb.toString();
    }
    
    /**
     * Create a StandardizedUIElement with a simple button.
     * 
     * @param id The button ID
     * @param text The button text
     * @param x The X coordinate
     * @param y The Y coordinate
     * @param width The width
     * @param height The height
     * @return A StandardizedUIElement representing a button
     */
    public static StandardizedUIElement createButton(String id, String text, int x, int y, int width, int height) {
        Rect bounds = new Rect(x, y, x + width, y + height);
        StandardizedUIElement element = new StandardizedUIElement(id, text, "BUTTON", bounds);
        element.setClickable(true);
        element.setVisible(true);
        element.setEnabled(true);
        
        return element;
    }
    
    /**
     * Create a StandardizedUIElement with a simple text element.
     * 
     * @param id The text element ID
     * @param text The text content
     * @param x The X coordinate
     * @param y The Y coordinate
     * @param width The width
     * @param height The height
     * @return A StandardizedUIElement representing a text element
     */
    public static StandardizedUIElement createText(String id, String text, int x, int y, int width, int height) {
        Rect bounds = new Rect(x, y, x + width, y + height);
        StandardizedUIElement element = new StandardizedUIElement(id, text, "TEXT", bounds);
        element.setClickable(false);
        element.setVisible(true);
        
        return element;
    }
    
    /**
     * Create a StandardizedUIElement with a simple image element.
     * 
     * @param id The image element ID
     * @param x The X coordinate
     * @param y The Y coordinate
     * @param width The width
     * @param height The height
     * @return A StandardizedUIElement representing an image element
     */
    public static StandardizedUIElement createImage(String id, int x, int y, int width, int height) {
        Rect bounds = new Rect(x, y, x + width, y + height);
        StandardizedUIElement element = new StandardizedUIElement(id, "", "IMAGE", bounds);
        element.setClickable(false);
        element.setVisible(true);
        
        return element;
    }
    
    /**
     * Create a StandardizedUIElement with a simple container element.
     * 
     * @param id The container element ID
     * @param x The X coordinate
     * @param y The Y coordinate
     * @param width The width
     * @param height The height
     * @param children The child elements
     * @return A StandardizedUIElement representing a container element
     */
    public static StandardizedUIElement createContainer(String id, int x, int y, int width, int height, UIElementInterface[] children) {
        Rect bounds = new Rect(x, y, x + width, y + height);
        StandardizedUIElement element = new StandardizedUIElement(id, "", "CONTAINER", bounds);
        element.setClickable(false);
        element.setVisible(true);
        
        if (children != null && children.length > 0) {
            element.setChildren(children);
        }
        
        return element;
    }
}