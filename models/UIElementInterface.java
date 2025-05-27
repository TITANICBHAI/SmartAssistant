package models;

import utils.Rect;
import java.util.Map;

/**
 * Interface for UI elements in the models package
 */
public interface UIElementInterface {
    /**
     * Get the wrapped element if this is a wrapper, otherwise return null
     * 
     * @return The wrapped utils.UIElementInterface or null
     */
    default utils.UIElementInterface getWrappedElement() {
        return null;
    }
    /**
     * Get the element type as a string
     * 
     * @return The element type as a string
     */
    String getType();
    
    /**
     * Get the element type as an enum
     * 
     * @return The element type as an enum
     */
    default ElementType getElementType() {
        String typeStr = getType();
        try {
            return ElementType.valueOf(typeStr.toUpperCase());
        } catch (IllegalArgumentException e) {
            return ElementType.UNKNOWN;
        }
    }
    
    /**
     * Get the value of an attribute
     * 
     * @param name The attribute name
     * @return The attribute value
     */
    Object getAttribute(String name);
    
    /**
     * Get the value of an attribute with a default value
     * 
     * @param name The attribute name
     * @param defaultValue The default value to return if the attribute is not found
     * @return The attribute value or the default value if not found
     */
    default Object getAttribute(String name, Object defaultValue) {
        Object value = getAttribute(name);
        return value != null ? value : defaultValue;
    }
    
    /**
     * Set the value of an attribute
     * 
     * @param name The attribute name
     * @param value The attribute value
     */
    default void setAttribute(String name, Object value) {
        // Default implementation is a no-op
    }
    
    /**
     * Get all attributes
     * 
     * @return Map of attribute names to values
     */
    Map<String, Object> getAttributes();
    
    /**
     * Set all attributes
     * 
     * @param attributes Map of attribute names to values
     */
    default void setAttributes(Map<String, Object> attributes) {
        // Default implementation is a no-op
    }
    
    /**
     * Get the element's bounding rectangle
     * 
     * @return The bounding rectangle
     */
    Rect getBounds();
    
    /**
     * Alias for getBounds() for compatibility with utils.UIElementInterface
     * 
     * @return The bounding rectangle
     */
    default Rect getRectBounds() {
        return getBounds();
    }
    
    /**
     * Get the element's bounds as an array
     * 
     * @return The bounds as an array [x, y, width, height]
     */
    default int[] getBoundsArray() {
        Rect rect = getBounds();
        if (rect == null) {
            return new int[]{0, 0, 0, 0};
        }
        return new int[]{rect.left, rect.top, rect.width(), rect.height()};
    }
    
    /**
     * Set the element's bounds using a rectangle
     * 
     * @param bounds The bounds rectangle
     */
    default void setBounds(Rect bounds) {
        // Default implementation is a no-op
    }
    
    /**
     * Set the element's bounds using an array [x, y, width, height]
     * 
     * @param bounds The bounds array
     */
    default void setBounds(int[] bounds) {
        if (bounds != null && bounds.length >= 4) {
            Rect rect = new Rect(
                bounds[0], 
                bounds[1], 
                bounds[0] + bounds[2], 
                bounds[1] + bounds[3]
            );
            setBounds(rect);
        }
    }
    
    /**
     * Get the confidence score for this element detection
     * 
     * @return The confidence score (0-1)
     */
    double getConfidence();
    
    /**
     * Get the confidence score for this element detection as a float
     * 
     * @return The confidence score (0-1)
     */
    default float getConfidenceFloat() {
        return (float)getConfidence();
    }
    
    /**
     * Set the confidence score for this element detection
     * 
     * @param confidence The confidence score (0-1)
     */
    default void setConfidence(double confidence) {
        // Default implementation is a no-op
    }
    
    /**
     * Set the confidence score for this element detection
     * 
     * @param confidence The confidence score (0-1)
     */
    default void setConfidence(float confidence) {
        setConfidence((double)confidence);
    }
    
    /**
     * Get the element's ID
     * 
     * @return The element ID
     */
    String getId();
    
    /**
     * Set the element's ID
     * 
     * @param id The ID to set
     */
    default void setId(String id) {
        // Default implementation is a no-op
    }
    
    /**
     * Get the element's text
     * 
     * @return The element text
     */
    String getText();
    
    /**
     * Set the element's text
     * 
     * @param text The text to set
     */
    default void setText(String text) {
        // Default implementation is a no-op
    }
    
    /**
     * Get the child elements of this element
     * 
     * @return Array of child elements
     */
    default UIElementInterface[] getChildren() {
        return new UIElementInterface[0];
    }
    
    /**
     * Set the child elements of this element
     * 
     * @param children The children to set
     */
    default void setChildren(UIElementInterface[] children) {
        // Default implementation is a no-op
    }
    
    /**
     * Check if the element is clickable
     * 
     * @return True if clickable, false otherwise
     */
    boolean isClickable();
    
    /**
     * Set whether the element is clickable
     * 
     * @param clickable True if clickable, false otherwise
     */
    default void setClickable(boolean clickable) {
        // Default implementation is a no-op
    }
    
    /**
     * Check if the element is visible
     * 
     * @return True if visible, false otherwise
     */
    boolean isVisible();
    
    /**
     * Check if the element is enabled
     * 
     * @return True if enabled, false otherwise
     */
    default boolean isEnabled() {
        return true;
    }
    
    /**
     * Set whether the element is enabled
     * 
     * @param enabled True if enabled, false otherwise
     */
    default void setEnabled(boolean enabled) {
        // Default implementation is a no-op
    }
    
    /**
     * Check if the element is selectable
     * 
     * @return True if selectable, false otherwise
     */
    default boolean isSelectable() {
        return false;
    }
    
    /**
     * Set whether the element is selectable
     * 
     * @param selectable True if selectable, false otherwise
     */
    default void setSelectable(boolean selectable) {
        // Default implementation is a no-op
    }
    
    /**
     * Check if the element is selected
     * 
     * @return True if selected, false otherwise
     */
    default boolean isSelected() {
        return false;
    }
    
    /**
     * Set whether the element is selected
     * 
     * @param selected True if selected, false otherwise
     */
    default void setSelected(boolean selected) {
        // Default implementation is a no-op
    }
    
    /**
     * Check if the element is scrollable
     * 
     * @return True if scrollable, false otherwise
     */
    default boolean isScrollable() {
        return false;
    }
    
    /**
     * Set whether the element is scrollable
     * 
     * @param scrollable True if scrollable, false otherwise
     */
    default void setScrollable(boolean scrollable) {
        // Default implementation is a no-op
    }
    
    /**
     * Check if the element is editable
     * 
     * @return True if editable, false otherwise
     */
    default boolean isEditable() {
        return false;
    }
    
    /**
     * Set whether the element is editable
     * 
     * @param editable True if editable, false otherwise
     */
    default void setEditable(boolean editable) {
        // Default implementation is a no-op
    }
    
    /**
     * Check if the element is focusable
     * 
     * @return True if focusable, false otherwise
     */
    default boolean isFocusable() {
        return false;
    }
    
    /**
     * Set whether the element is focusable
     * 
     * @param focusable True if focusable, false otherwise
     */
    default void setFocusable(boolean focusable) {
        // Default implementation is a no-op
    }
    
    /**
     * Check if the element is focused
     * 
     * @return True if focused, false otherwise
     */
    default boolean isFocused() {
        return false;
    }
    
    /**
     * Set whether the element is visible
     * 
     * @param visible True if visible, false otherwise
     */
    default void setVisible(boolean visible) {
        // Default implementation is a no-op
    }
    
    /**
     * Set whether the element is focused
     * 
     * @param focused True if focused, false otherwise
     */
    default void setFocused(boolean focused) {
        // Default implementation is a no-op
    }
    
    /**
     * Get the description of this element
     *
     * @return The description or null if not set
     */
    default String getDescription() {
        return (String)getAttribute("description");
    }
    
    /**
     * Set the description of this element
     *
     * @param description The description to set
     */
    default void setDescription(String description) {
        setAttribute("description", description);
    }
    
    /**
     * Get the parent ID of this element
     *
     * @return The parent ID or null if not set
     */
    default String getParentId() {
        return (String)getAttribute("parentId");
    }
    
    /**
     * Set the parent ID of this element
     *
     * @param parentId The parent ID to set
     */
    default void setParentId(String parentId) {
        setAttribute("parentId", parentId);
    }
    
    /**
     * Check if a point is contained within this element
     * 
     * @param x X coordinate
     * @param y Y coordinate
     * @return True if the point is contained, false otherwise
     */
    boolean contains(int x, int y);
    
    /**
     * Check if this element intersects with another element
     * 
     * @param element The other element
     * @return True if they intersect, false otherwise
     */
    boolean intersects(UIElementInterface element);
    
    /**
     * Set the element type
     * 
     * @param type The type to set
     */
    void setType(String type);
    
    /**
     * Set the element type using an object
     * 
     * @param type The type to set
     */
    void setType(Object type);
    
    /**
     * Get a property value
     * 
     * @param key The property key
     * @return The property value or null if not found
     */
    default Object getProperty(String key) {
        return null;
    }
    
    /**
     * Set a property value
     * 
     * @param key The property key
     * @param value The property value
     */
    default void setProperty(String key, Object value) {
        // Default implementation is a no-op
    }
    
    /**
     * Get all properties
     * 
     * @return Map of property keys to values
     */
    default Map<String, Object> getProperties() {
        return new java.util.HashMap<>();
    }
    
    /**
     * Set all properties
     * 
     * @param properties Map of property keys to values
     */
    default void setProperties(Map<String, Object> properties) {
        // Default implementation is a no-op
    }
    
    /**
     * Convert this UIElementInterface to a utils.UIElement
     * 
     * @return A utils.UIElement representing this element
     */
    default utils.UIElement toUIElement() {
        return new utils.UIElementAdapter(this);
    }
}