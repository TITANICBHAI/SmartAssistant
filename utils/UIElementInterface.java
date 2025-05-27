package utils;

import java.util.Map;

/**
 * Interface for UI elements.
 * This interface defines the common functionality for all UI elements.
 */
public interface UIElementInterface {
    /**
     * Get the ID of the element.
     * 
     * @return The ID
     */
    String getId();
    
    /**
     * Set the ID of the element.
     * 
     * @param id The ID
     */
    default void setId(String id) {
        // Optional implementation
    }
    
    /**
     * Get the text of the element.
     * 
     * @return The text
     */
    String getText();
    
    /**
     * Set the text of the element.
     * 
     * @param text The text
     */
    default void setText(String text) {
        // Optional implementation
    }
    
    /**
     * Get the type of the element.
     * 
     * @return The type
     */
    String getType();
    
    /**
     * Set the type of the element.
     * 
     * @param type The type
     */
    default void setType(String type) {
        // Optional implementation
    }
    
    /**
     * Set the type of the element using an object.
     * This is useful for handling different type enums.
     * 
     * @param type The type object
     */
    default void setType(Object type) {
        if (type instanceof String) {
            setType((String) type);
        } else if (type != null) {
            setType(type.toString());
        }
    }
    
    /**
     * Get the bounds of the element as a Rect.
     * 
     * @return The bounds
     */
    Rect getRectBounds();
    
    /**
     * Set the bounds of the element.
     * 
     * @param bounds The bounds
     */
    default void setBounds(Rect bounds) {
        // Optional implementation
    }
    
    /**
     * Set the rect bounds of the element.
     * This is the same as setBounds but with a different method name for clarity.
     * 
     * @param bounds The bounds
     */
    default void setRectBounds(Rect bounds) {
        setBounds(bounds);
    }
    
    /**
     * Set the bounds of the element from an array.
     * 
     * @param boundsArray The bounds array (left, top, right, bottom)
     */
    default void setBounds(int[] boundsArray) {
        // Optional implementation
    }
    
    /**
     * Get the bounds of the element as an array.
     * 
     * @return The bounds array (left, top, right, bottom)
     */
    int[] getBoundsArray();
    
    /**
     * Check if the element is clickable.
     * 
     * @return True if clickable
     */
    boolean isClickable();
    
    /**
     * Set whether the element is clickable.
     * 
     * @param clickable True if clickable
     */
    default void setClickable(boolean clickable) {
        // Optional implementation
    }
    
    /**
     * Check if the element is visible.
     * 
     * @return True if visible
     */
    boolean isVisible();
    
    /**
     * Set whether the element is visible.
     * 
     * @param visible True if visible
     */
    default void setVisible(boolean visible) {
        // Optional implementation
    }
    
    /**
     * Check if the element is enabled.
     * 
     * @return True if enabled
     */
    boolean isEnabled();
    
    /**
     * Set whether the element is enabled.
     * 
     * @param enabled True if enabled
     */
    default void setEnabled(boolean enabled) {
        // Optional implementation
    }
    
    /**
     * Check if the element is selectable.
     * 
     * @return True if selectable
     */
    boolean isSelectable();
    
    /**
     * Set whether the element is selectable.
     * 
     * @param selectable True if selectable
     */
    default void setSelectable(boolean selectable) {
        // Optional implementation
    }
    
    /**
     * Check if the element is selected.
     * 
     * @return True if selected
     */
    boolean isSelected();
    
    /**
     * Set whether the element is selected.
     * 
     * @param selected True if selected
     */
    default void setSelected(boolean selected) {
        // Optional implementation
    }
    
    /**
     * Check if the element is scrollable.
     * 
     * @return True if scrollable
     */
    boolean isScrollable();
    
    /**
     * Set whether the element is scrollable.
     * 
     * @param scrollable True if scrollable
     */
    default void setScrollable(boolean scrollable) {
        // Optional implementation
    }
    
    /**
     * Check if the element is editable.
     * 
     * @return True if editable
     */
    boolean isEditable();
    
    /**
     * Set whether the element is editable.
     * 
     * @param editable True if editable
     */
    default void setEditable(boolean editable) {
        // Optional implementation
    }
    
    /**
     * Check if the element is focusable.
     * 
     * @return True if focusable
     */
    boolean isFocusable();
    
    /**
     * Set whether the element is focusable.
     * 
     * @param focusable True if focusable
     */
    default void setFocusable(boolean focusable) {
        // Optional implementation
    }
    
    /**
     * Check if the element is focused.
     * 
     * @return True if focused
     */
    boolean isFocused();
    
    /**
     * Set whether the element is focused.
     * 
     * @param focused True if focused
     */
    default void setFocused(boolean focused) {
        // Optional implementation
    }
    
    /**
     * Get the confidence of the element.
     * 
     * @return The confidence (0.0 to 1.0)
     */
    double getConfidence();
    
    /**
     * Get the confidence of the element as a float.
     * 
     * @return The confidence (0.0f to 1.0f)
     */
    default float getConfidenceFloat() {
        return (float)getConfidence();
    }
    
    /**
     * Set the confidence of the element.
     * 
     * @param confidence The confidence
     */
    default void setConfidence(double confidence) {
        // Optional implementation
    }
    
    /**
     * Set the confidence of the element as a float.
     * 
     * @param confidence The confidence
     */
    default void setConfidence(float confidence) {
        setConfidence((double)confidence);
    }
    
    /**
     * Get the properties of the element.
     * 
     * @return The properties
     */
    Map<String, Object> getProperties();
    
    /**
     * Get a property of the element.
     * 
     * @param key The property key
     * @return The property value, or null if not found
     */
    default Object getProperty(String key) {
        Map<String, Object> props = getProperties();
        return props != null ? props.get(key) : null;
    }
    
    /**
     * Set a property of the element.
     * 
     * @param key The property key
     * @param value The property value
     */
    default void setProperty(String key, Object value) {
        // Optional implementation
    }
    
    /**
     * Set the properties of the element.
     * 
     * @param properties The properties
     */
    default void setProperties(Map<String, Object> properties) {
        // Optional implementation
    }
    
    /**
     * Get the attributes of the element.
     * 
     * @return The attributes
     */
    Map<String, Object> getAttributes();
    
    /**
     * Get an attribute of the element.
     * 
     * @param name The attribute name
     * @return The attribute value, or null if not found
     */
    default Object getAttribute(String name) {
        Map<String, Object> attrs = getAttributes();
        return attrs != null ? attrs.get(name) : null;
    }
    
    /**
     * Set an attribute of the element.
     * 
     * @param key The attribute key
     * @param value The attribute value
     */
    default void setAttribute(String key, Object value) {
        // Optional implementation
    }
    
    /**
     * Set the attributes of the element.
     * 
     * @param attributes The attributes
     */
    default void setAttributes(Map<String, Object> attributes) {
        // Optional implementation
    }
    
    /**
     * Get the children of the element.
     * 
     * @return The children
     */
    UIElementInterface[] getChildren();
    
    /**
     * Set the children of the element.
     * 
     * @param children The children
     */
    default void setChildren(UIElementInterface[] children) {
        // Optional implementation
    }
    
    /**
     * Check if the element contains a point.
     * 
     * @param x The x coordinate
     * @param y The y coordinate
     * @return True if the element contains the point
     */
    default boolean contains(int x, int y) {
        Rect bounds = getRectBounds();
        return bounds != null && bounds.contains(x, y);
    }
    
    /**
     * Check if the element intersects with another element.
     * 
     * @param element The other element
     * @return True if the elements intersect
     */
    default boolean intersects(UIElementInterface element) {
        if (element == null) {
            return false;
        }
        
        Rect thisBounds = getRectBounds();
        Rect otherBounds = element.getRectBounds();
        
        return thisBounds != null && otherBounds != null && thisBounds.intersects(otherBounds);
    }
}