package utils;

import java.util.HashMap;
import java.util.Map;

/**
 * Default implementation of UIElementInterface.
 */
public class DefaultUIElement implements UIElementInterface {
    private String id;
    private String text;
    private String type;
    private Rect bounds;
    private boolean clickable;
    private boolean visible;
    private boolean enabled;
    private boolean selectable;
    private boolean selected;
    private boolean scrollable;
    private boolean editable;
    private boolean focusable;
    private boolean focused;
    private double confidence;
    private Map<String, Object> properties;
    private Map<String, Object> attributes;
    private UIElementInterface[] children;
    
    /**
     * Default constructor.
     */
    public DefaultUIElement() {
        this.id = "";
        this.text = "";
        this.type = "";
        this.bounds = new Rect(0, 0, 0, 0);
        this.clickable = true;
        this.visible = true;
        this.enabled = true;
        this.properties = new HashMap<>();
        this.attributes = new HashMap<>();
        this.confidence = 1.0;
    }
    
    /**
     * Constructor with ID, text, and bounds.
     * 
     * @param id The ID
     * @param text The text
     * @param bounds The bounds
     */
    public DefaultUIElement(String id, String text, Rect bounds) {
        this();
        this.id = id;
        this.text = text;
        this.bounds = bounds;
    }
    
    /**
     * Constructor with ID, text, and bounds array.
     * 
     * @param id The ID
     * @param text The text
     * @param boundsArray The bounds array (left, top, right, bottom)
     */
    public DefaultUIElement(String id, String text, int[] boundsArray) {
        this();
        this.id = id;
        this.text = text;
        if (boundsArray != null && boundsArray.length >= 4) {
            this.bounds = new Rect(boundsArray[0], boundsArray[1], boundsArray[2], boundsArray[3]);
        }
    }
    
    /**
     * Constructor with all properties.
     * 
     * @param id The ID
     * @param text The text
     * @param type The type
     * @param bounds The bounds
     * @param clickable Whether it's clickable
     * @param visible Whether it's visible
     * @param enabled Whether it's enabled
     */
    public DefaultUIElement(String id, String text, String type, Rect bounds, boolean clickable, boolean visible, boolean enabled) {
        this(id, text, bounds);
        this.type = type;
        this.clickable = clickable;
        this.visible = visible;
        this.enabled = enabled;
    }
    
    /**
     * Set the ID.
     * 
     * @param id The ID
     */
    public void setId(String id) {
        this.id = id;
    }
    
    /**
     * Set the text.
     * 
     * @param text The text
     */
    public void setText(String text) {
        this.text = text;
    }
    
    /**
     * Set the type.
     * 
     * @param type The type
     */
    public void setType(String type) {
        this.type = type;
    }
    
    /**
     * Set the bounds.
     * 
     * @param bounds The bounds
     */
    public void setBounds(Rect bounds) {
        this.bounds = bounds;
    }
    
    /**
     * Set the bounds.
     * 
     * @param boundsArray The bounds array (left, top, right, bottom)
     */
    public void setBounds(int[] boundsArray) {
        if (boundsArray != null && boundsArray.length >= 4) {
            this.bounds = new Rect(boundsArray[0], boundsArray[1], boundsArray[2], boundsArray[3]);
        }
    }
    
    /**
     * Set whether it's clickable.
     * 
     * @param clickable Whether it's clickable
     */
    public void setClickable(boolean clickable) {
        this.clickable = clickable;
    }
    
    /**
     * Set whether it's visible.
     * 
     * @param visible Whether it's visible
     */
    public void setVisible(boolean visible) {
        this.visible = visible;
    }
    
    /**
     * Set whether it's enabled.
     * 
     * @param enabled Whether it's enabled
     */
    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }
    
    /**
     * Set whether it's selectable.
     * 
     * @param selectable Whether it's selectable
     */
    public void setSelectable(boolean selectable) {
        this.selectable = selectable;
    }
    
    /**
     * Set whether it's selected.
     * 
     * @param selected Whether it's selected
     */
    public void setSelected(boolean selected) {
        this.selected = selected;
    }
    
    /**
     * Set whether it's scrollable.
     * 
     * @param scrollable Whether it's scrollable
     */
    public void setScrollable(boolean scrollable) {
        this.scrollable = scrollable;
    }
    
    /**
     * Set whether it's editable.
     * 
     * @param editable Whether it's editable
     */
    public void setEditable(boolean editable) {
        this.editable = editable;
    }
    
    /**
     * Set whether it's focusable.
     * 
     * @param focusable Whether it's focusable
     */
    public void setFocusable(boolean focusable) {
        this.focusable = focusable;
    }
    
    /**
     * Set whether it's focused.
     * 
     * @param focused Whether it's focused
     */
    public void setFocused(boolean focused) {
        this.focused = focused;
    }
    
    /**
     * Set the confidence.
     * 
     * @param confidence The confidence
     */
    public void setConfidence(double confidence) {
        this.confidence = confidence;
    }
    
    /**
     * Set a property.
     * 
     * @param key The property key
     * @param value The property value
     */
    public void setProperty(String key, Object value) {
        properties.put(key, value);
    }
    
    /**
     * Set the properties.
     * 
     * @param properties The properties
     */
    public void setProperties(Map<String, Object> properties) {
        this.properties = new HashMap<>(properties);
    }
    
    /**
     * Set an attribute.
     * 
     * @param key The attribute key
     * @param value The attribute value
     */
    public void setAttribute(String key, Object value) {
        attributes.put(key, value);
    }
    
    /**
     * Set the attributes.
     * 
     * @param attributes The attributes
     */
    public void setAttributes(Map<String, Object> attributes) {
        this.attributes = new HashMap<>(attributes);
    }
    
    /**
     * Set the children.
     * 
     * @param children The children
     */
    public void setChildren(UIElementInterface[] children) {
        this.children = children;
    }
    
    @Override
    public String getId() {
        return id;
    }
    
    @Override
    public String getText() {
        return text;
    }
    
    @Override
    public String getType() {
        return type;
    }
    
    @Override
    public Rect getRectBounds() {
        return bounds;
    }
    
    @Override
    public int[] getBoundsArray() {
        if (bounds == null) {
            return new int[]{0, 0, 0, 0};
        }
        return new int[]{bounds.left, bounds.top, bounds.right, bounds.bottom};
    }
    
    @Override
    public boolean isClickable() {
        return clickable;
    }
    
    @Override
    public boolean isVisible() {
        return visible;
    }
    
    @Override
    public boolean isEnabled() {
        return enabled;
    }
    
    @Override
    public boolean isSelectable() {
        return selectable;
    }
    
    @Override
    public boolean isSelected() {
        return selected;
    }
    
    @Override
    public boolean isScrollable() {
        return scrollable;
    }
    
    @Override
    public boolean isEditable() {
        return editable;
    }
    
    @Override
    public boolean isFocusable() {
        return focusable;
    }
    
    @Override
    public boolean isFocused() {
        return focused;
    }
    
    @Override
    public double getConfidence() {
        return confidence;
    }
    
    @Override
    public Map<String, Object> getProperties() {
        return new HashMap<>(properties);
    }
    
    @Override
    public Map<String, Object> getAttributes() {
        return new HashMap<>(attributes);
    }
    
    @Override
    public UIElementInterface[] getChildren() {
        return children;
    }
    
    /**
     * Convert this DefaultUIElement to a UIElement
     * 
     * @return A new UIElement with the same properties
     */
    public UIElement convertToUIElement() {
        UIElement element = new UIElement(id, type, text);
        element.setRectBounds(bounds);
        element.setClickable(clickable);
        element.setVisible(visible);
        element.setEnabled(enabled);
        element.setSelectable(selectable);
        element.setSelected(selected);
        element.setScrollable(scrollable);
        element.setEditable(editable);
        element.setFocusable(focusable);
        element.setFocused(focused);
        element.setConfidence((float)confidence);
        
        // Copy attributes
        if (attributes != null) {
            for (Map.Entry<String, Object> entry : attributes.entrySet()) {
                element.setAttribute(entry.getKey(), entry.getValue());
            }
        }
        
        // Copy properties
        if (properties != null) {
            for (Map.Entry<String, Object> entry : properties.entrySet()) {
                element.setProperty(entry.getKey(), entry.getValue());
            }
        }
        
        // Copy children if any
        if (children != null && children.length > 0) {
            element.setChildren(children);
        }
        
        return element;
    }
}