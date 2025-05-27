package utils;

import java.util.HashMap;
import java.util.Map;

/**
 * Basic implementation of UIElementInterface in the utils package
 */
public class UIElement implements UIElementInterface {
    /**
     * Element types for UI elements.
     */
    public enum ElementType {
        BUTTON,
        TEXT,
        IMAGE,
        CHECKBOX,
        RADIO_BUTTON,
        TOGGLE,
        INPUT,
        DROPDOWN,
        LIST,
        SCROLLVIEW,
        CONTAINER,
        NAVIGATION,
        CARD,
        SLIDER,
        PROGRESS,
        ICON,
        MENU,
        DIALOG,
        TOOLBAR,
        HEADER,
        FOOTER,
        GAME_CONTROL,
        UNKNOWN;
        
        /**
         * Parse a string to an ElementType
         * 
         * @param value String value to parse
         * @return Matching ElementType or UNKNOWN if no match
         */
        public static ElementType fromString(String value) {
            if (value == null || value.isEmpty()) {
                return UNKNOWN;
            }
            
            try {
                return valueOf(value.toUpperCase());
            } catch (IllegalArgumentException e) {
                return UNKNOWN;
            }
        }
    }
    private String id;
    private String text;
    private String type;
    private Map<String, Object> attributes;
    private Map<String, Object> properties;
    private Rect bounds;
    private float confidence;
    private boolean visible;
    private boolean clickable;
    private boolean enabled = true;
    private boolean selectable;
    private boolean selected;
    private boolean scrollable;
    private boolean editable;
    private boolean focusable;
    private boolean focused;
    private UIElementInterface[] children;
    
    /**
     * Default constructor
     */
    public UIElement() {
        this.id = "";
        this.text = "";
        this.type = "UNKNOWN";
        this.attributes = new HashMap<>();
        this.properties = new HashMap<>();
        this.bounds = new Rect(0, 0, 0, 0);
        this.confidence = 1.0f;
        this.visible = true;
        this.clickable = false;
        this.children = new UIElementInterface[0];
    }
    
    /**
     * Constructor with ID and type
     * 
     * @param id Element ID
     * @param type Element type
     */
    public UIElement(String id, String type) {
        this();
        this.id = id;
        this.type = type;
    }
    
    /**
     * Constructor with ID, type, and text
     * 
     * @param id Element ID
     * @param type Element type
     * @param text Element text
     */
    public UIElement(String id, String type, String text) {
        this(id, type);
        this.text = text;
    }
    
    /**
     * Constructor with all basic properties
     * 
     * @param id Element ID
     * @param type Element type
     * @param text Element text
     * @param bounds Element bounds
     * @param clickable Whether the element is clickable
     */
    public UIElement(String id, String type, String text, Rect bounds, boolean clickable) {
        this(id, type, text);
        this.bounds = bounds;
        this.clickable = clickable;
    }
    
    @Override
    public String getType() {
        return type;
    }
    
    @Override
    public void setType(String type) {
        this.type = type;
    }
    
    @Override
    public void setType(Object type) {
        if (type instanceof String) {
            this.type = (String) type;
        } else if (type instanceof ElementType) {
            this.type = ((ElementType) type).name();
        } else if (type != null) {
            this.type = type.toString();
        }
    }
    
    /**
     * Get the element type as an ElementType enum
     * 
     * @return The element type
     */
    public ElementType getElementType() {
        return ElementType.fromString(type);
    }
    
    /**
     * Get the element type as a utils.ElementType enum
     * 
     * @return The element type
     */
    public utils.ElementType getUtilsElementType() {
        return utils.ElementType.fromString(type);
    }
    
    @Override
    public Object getAttribute(String name) {
        return attributes.get(name);
    }
    
    @Override
    public void setAttribute(String name, Object value) {
        attributes.put(name, value);
    }
    
    @Override
    public Map<String, Object> getAttributes() {
        return attributes;
    }
    
    @Override
    public void setAttributes(Map<String, Object> attributes) {
        this.attributes = attributes != null ? attributes : new HashMap<>();
    }
    
    /**
     * Get the bounds of the element (for backwards compatibility)
     * 
     * @return The bounds
     */
    public Rect getBounds() {
        return bounds;
    }
    
    @Override
    public Rect getRectBounds() {
        return bounds;
    }
    
    @Override
    public int[] getBoundsArray() {
        if (bounds == null) {
            return new int[] {0, 0, 0, 0};
        }
        return new int[] {bounds.left, bounds.top, bounds.right, bounds.bottom};
    }
    
    /**
     * Get the x-coordinate of the element
     * 
     * @return The x-coordinate
     */
    public int getX() {
        return bounds != null ? bounds.left : 0;
    }
    
    /**
     * Get the y-coordinate of the element
     * 
     * @return The y-coordinate
     */
    public int getY() {
        return bounds != null ? bounds.top : 0;
    }
    
    /**
     * Get the width of the element
     * 
     * @return The width
     */
    public int getWidth() {
        return bounds != null ? bounds.width() : 0;
    }
    
    /**
     * Get the height of the element
     * 
     * @return The height
     */
    public int getHeight() {
        return bounds != null ? bounds.height() : 0;
    }
    
    /**
     * Get the content description of the element
     * 
     * @return The content description
     */
    public String getContentDescription() {
        Object desc = getAttribute("contentDescription");
        return desc != null ? desc.toString() : "";
    }
    
    /**
     * Set the content description of the element
     * 
     * @param description The content description
     */
    public void setContentDescription(String description) {
        setAttribute("contentDescription", description);
    }
    
    @Override
    public void setRectBounds(Rect bounds) {
        this.bounds = bounds != null ? bounds : new Rect(0, 0, 0, 0);
    }
    
    @Override
    public double getConfidence() {
        return confidence;
    }
    
    @Override
    public void setConfidence(double confidence) {
        this.confidence = (float)confidence;
    }
    
    @Override
    public void setConfidence(float confidence) {
        this.confidence = confidence;
    }
    
    @Override
    public float getConfidenceFloat() {
        return confidence;
    }
    
    @Override
    public String getId() {
        return id;
    }
    
    @Override
    public void setId(String id) {
        this.id = id != null ? id : "";
    }
    
    @Override
    public String getText() {
        return text;
    }
    
    @Override
    public void setText(String text) {
        this.text = text != null ? text : "";
    }
    
    @Override
    public UIElementInterface[] getChildren() {
        return children;
    }
    
    @Override
    public void setChildren(UIElementInterface[] children) {
        this.children = children != null ? children : new UIElementInterface[0];
    }
    
    @Override
    public boolean isClickable() {
        return clickable;
    }
    
    @Override
    public void setClickable(boolean clickable) {
        this.clickable = clickable;
    }
    
    @Override
    public boolean isVisible() {
        return visible;
    }
    
    @Override
    public void setVisible(boolean visible) {
        this.visible = visible;
    }
    
    @Override
    public boolean isEnabled() {
        return enabled;
    }
    
    @Override
    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }
    
    @Override
    public boolean isSelectable() {
        return selectable;
    }
    
    @Override
    public void setSelectable(boolean selectable) {
        this.selectable = selectable;
    }
    
    @Override
    public boolean isSelected() {
        return selected;
    }
    
    @Override
    public void setSelected(boolean selected) {
        this.selected = selected;
    }
    
    @Override
    public boolean isScrollable() {
        return scrollable;
    }
    
    @Override
    public void setScrollable(boolean scrollable) {
        this.scrollable = scrollable;
    }
    
    @Override
    public boolean isEditable() {
        return editable;
    }
    
    @Override
    public void setEditable(boolean editable) {
        this.editable = editable;
    }
    
    @Override
    public boolean isFocusable() {
        return focusable;
    }
    
    @Override
    public void setFocusable(boolean focusable) {
        this.focusable = focusable;
    }
    
    @Override
    public boolean isFocused() {
        return focused;
    }
    
    @Override
    public void setFocused(boolean focused) {
        this.focused = focused;
    }
    
    @Override
    public boolean contains(int x, int y) {
        return bounds != null && bounds.contains(x, y);
    }
    
    @Override
    public boolean intersects(UIElementInterface element) {
        if (element == null || element.getRectBounds() == null || bounds == null) {
            return false;
        }
        // Use the intersect instance method instead of the static method
        return bounds.intersect(element.getRectBounds());
    }
    
    @Override
    public Object getProperty(String key) {
        return properties.get(key);
    }
    
    @Override
    public void setProperty(String key, Object value) {
        properties.put(key, value);
    }
    
    @Override
    public Map<String, Object> getProperties() {
        return properties;
    }
    
    @Override
    public void setProperties(Map<String, Object> properties) {
        this.properties = properties != null ? properties : new HashMap<>();
    }
    
    @Override
    public String toString() {
        return "UIElement{" +
                "id='" + id + '\'' +
                ", type='" + type + '\'' +
                ", text='" + text + '\'' +
                ", bounds=" + bounds +
                ", clickable=" + clickable +
                ", visible=" + visible +
                '}';
    }
    
    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null || getClass() != obj.getClass()) {
            return false;
        }
        
        UIElement other = (UIElement) obj;
        return id.equals(other.id) &&
                type.equals(other.type) &&
                (bounds == null ? other.bounds == null : bounds.equals(other.bounds));
    }
    
    @Override
    public int hashCode() {
        int result = id.hashCode();
        result = 31 * result + type.hashCode();
        result = 31 * result + (bounds != null ? bounds.hashCode() : 0);
        return result;
    }
}