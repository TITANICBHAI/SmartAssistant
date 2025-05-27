package models;

import java.util.HashMap;
import java.util.Map;
import utils.Rect;

/**
 * A standardized UI element for cross-platform compatibility.
 */
public class StandardizedUIElement implements UIElementInterface {
    /**
     * Get the type as a standardized string value
     * 
     * @return The type as a string
     */
    public String getTypeString() {
        return getType() != null ? getType().toUpperCase() : "";
    }
    private String id;
    private String text;
    private StandardizedUIElementType type;
    private int[] bounds;
    private boolean enabled;
    private boolean selectable;
    private boolean selected;
    private boolean clickable;
    private Map<String, Object> properties;
    private int depth;
    
    /**
     * Default constructor.
     */
    public StandardizedUIElement() {
        this.id = "";
        this.text = "";
        this.type = StandardizedUIElementType.UNKNOWN;
        this.bounds = new int[]{0, 0, 0, 0};
        this.enabled = true;
        this.selectable = false;
        this.selected = false;
        this.clickable = false;
        this.properties = new HashMap<>();
    }
    
    /**
     * Constructor with ID and text.
     * 
     * @param id Element ID
     * @param text Element text
     */
    public StandardizedUIElement(String id, String text) {
        this();
        this.id = id != null ? id : "";
        this.text = text != null ? text : "";
    }
    
    /**
     * Constructor with ID, text, and type.
     * 
     * @param id Element ID
     * @param text Element text
     * @param type Element type
     */
    public StandardizedUIElement(String id, String text, StandardizedUIElementType type) {
        this(id, text);
        this.type = type != null ? type : StandardizedUIElementType.UNKNOWN;
    }
    
    /**
     * Constructor with all basic properties.
     * 
     * @param id Element ID
     * @param text Element text
     * @param type Element type
     * @param bounds Element bounds
     * @param enabled Whether the element is enabled
     * @param selectable Whether the element is selectable
     * @param selected Whether the element is selected
     * @param clickable Whether the element is clickable
     */
    public StandardizedUIElement(String id, String text, StandardizedUIElementType type, int[] bounds,
                               boolean enabled, boolean selectable, boolean selected, boolean clickable) {
        this.id = id != null ? id : "";
        this.text = text != null ? text : "";
        this.type = type != null ? type : StandardizedUIElementType.UNKNOWN;
        this.bounds = bounds != null ? bounds : new int[]{0, 0, 0, 0};
        this.enabled = enabled;
        this.selectable = selectable;
        this.selected = selected;
        this.clickable = clickable;
        this.properties = new HashMap<>();
    }
    
    /**
     * Get the element ID.
     * 
     * @return Element ID
     */
    public String getId() {
        return id;
    }
    
    /**
     * Set the element ID.
     * 
     * @param id Element ID
     */
    public void setId(String id) {
        this.id = id != null ? id : "";
    }
    
    /**
     * Get the element text.
     * 
     * @return Element text
     */
    public String getText() {
        return text;
    }
    
    /**
     * Set the element text.
     * 
     * @param text Element text
     */
    public void setText(String text) {
        this.text = text != null ? text : "";
    }
    
    /**
     * Get the element type.
     * 
     * @return Element type
     */
    public StandardizedUIElementType getElementTypeObject() {
        return type;
    }
    
    /**
     * Get the element type as a string.
     * This implements the UIElementInterface method.
     * 
     * @return Element type as a string
     */
    @Override
    public String getType() {
        return type != null ? type.name() : "UNKNOWN";
    }
    
    /**
     * Set the element type.
     * 
     * @param type Element type
     */
    public void setType(StandardizedUIElementType type) {
        this.type = type != null ? type : StandardizedUIElementType.UNKNOWN;
    }
    
    /**
     * Set the element type using a string.
     * Required by UIElementInterface
     * 
     * @param type The type as string
     */
    @Override
    public void setType(String type) {
        this.type = type != null ? StandardizedUIElementType.fromString(type) : StandardizedUIElementType.UNKNOWN;
    }
    
    /**
     * Get the element bounds.
     * 
     * @return Bounds as [left, top, right, bottom]
     */
    public int[] getBoundsArray() {
        return bounds;
    }
    
    /**
     * Set the element bounds.
     * 
     * @param bounds Bounds as [left, top, right, bottom]
     */
    public void setBounds(int[] bounds) {
        this.bounds = bounds != null ? bounds : new int[]{0, 0, 0, 0};
    }
    
    /**
     * Check if the element is enabled.
     * 
     * @return True if enabled
     */
    public boolean isEnabled() {
        return enabled;
    }
    
    /**
     * Set the enabled state.
     * 
     * @param enabled Enabled state
     */
    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }
    
    /**
     * Check if the element is selectable.
     * 
     * @return True if selectable
     */
    public boolean isSelectable() {
        return selectable;
    }
    
    /**
     * Set the selectable state.
     * 
     * @param selectable Selectable state
     */
    public void setSelectable(boolean selectable) {
        this.selectable = selectable;
    }
    
    /**
     * Check if the element is selected.
     * 
     * @return True if selected
     */
    public boolean isSelected() {
        return selected;
    }
    
    /**
     * Set the selected state.
     * 
     * @param selected Selected state
     */
    public void setSelected(boolean selected) {
        this.selected = selected;
    }
    
    /**
     * Check if the element is clickable.
     * 
     * @return True if clickable
     */
    public boolean isClickable() {
        return clickable;
    }
    
    /**
     * Set the clickable state.
     * 
     * @param clickable Clickable state
     */
    public void setClickable(boolean clickable) {
        this.clickable = clickable;
    }
    
    /**
     * Get all element properties.
     * 
     * @return Map of property name to value
     */
    public Map<String, Object> getProperties() {
        return new HashMap<>(properties);
    }
    
    /**
     * Set a property.
     * 
     * @param name Property name
     * @param value Property value
     */
    public void setProperty(String name, Object value) {
        if (name != null && !name.isEmpty()) {
            properties.put(name, value);
        }
    }
    
    /**
     * Get a property.
     * 
     * @param name Property name
     * @return Property value or null
     */
    public Object getProperty(String name) {
        if (name != null && !name.isEmpty() && properties.containsKey(name)) {
            return properties.get(name);
        }
        return null;
    }
    
    /**
     * Convert to a UI element.
     * 
     * @return UIElement representation
     */
    @Override
    public utils.UIElement toUIElement() {
        return toUtilsUIElement();
    }
    
    /**
     * Convert to a models.UIElement.
     * 
     * @return models.UIElement representation
     */
    public UIElement toModelsUIElement() {
        UIElement element = new UIElement();
        element.setId(id);
        element.setText(text);
        element.setType(type.toString());
        element.setBounds(bounds);
        element.setEnabled(enabled);
        element.setSelectable(selectable);
        element.setSelected(selected);
        element.setClickable(clickable);
        
        // Copy properties
        for (Map.Entry<String, Object> entry : properties.entrySet()) {
            element.setProperty(entry.getKey(), entry.getValue());
        }
        
        return element;
    }
    
    /**
     * Set element's bounds from Android Rect object
     * 
     * @param rect The Android Rect object
     */
    public void setBounds(android.graphics.Rect rect) {
        if (rect == null) {
            this.bounds = new int[]{0, 0, 0, 0};
            return;
        }
        this.bounds = new int[]{rect.left, rect.top, rect.right, rect.bottom};
    }
    
    /**
     * Set element's bounds from Android RectF object
     * 
     * @param rect The Android RectF object
     */
    public void setBounds(android.graphics.RectF rect) {
        if (rect == null) {
            this.bounds = new int[]{0, 0, 0, 0};
            return;
        }
        this.bounds = new int[]{(int)rect.left, (int)rect.top, (int)rect.right, (int)rect.bottom};
    }
    
    /**
     * Constructor with ID, bounds and type.
     * This is a compatibility constructor for certain cases.
     * 
     * @param id Element ID
     * @param bounds Element bounds as utils.Rect
     * @param type Element type from utils.UIElement.ElementType
     */
    public StandardizedUIElement(String id, utils.Rect bounds, utils.UIElement.ElementType type) {
        this();
        this.id = id != null ? id : "";
        if (bounds != null) {
            this.bounds = new int[]{bounds.left, bounds.top, bounds.right, bounds.bottom};
        }
        // Convert the type from utils.UIElement.ElementType to StandardizedUIElementType
        if (type != null) {
            String typeStr = type.toString();
            this.type = StandardizedUIElementType.fromString(typeStr);
        }
    }
    
    /**
     * Set the content description
     * 
     * @param contentDescription The content description
     */
    public void setContentDescription(String contentDescription) {
        setProperty("contentDescription", contentDescription);
    }
    
    /**
     * Get the content description
     * 
     * @return The content description
     */
    public String getContentDescription() {
        Object desc = getProperty("contentDescription");
        return desc instanceof String ? (String)desc : "";
    }
    
    /**
     * Set the visibility of this element
     * 
     * @param visible True if element is visible
     */
    public void setVisible(boolean visible) {
        setProperty("visible", visible);
    }
    
    /**
     * Check if the element is visible
     * 
     * @return True if visible
     */
    public boolean isVisible() {
        Object visible = getProperty("visible");
        return visible instanceof Boolean ? (Boolean)visible : true;
    }
    
    /**
     * Set the confidence score of this element
     * 
     * @param confidence Confidence value between 0 and 1
     */
    public void setConfidence(float confidence) {
        setProperty("confidence", confidence);
    }
    
    /**
     * Get the confidence score of this element
     * 
     * @return Confidence value between 0 and 1
     */
    @Override
    public double getConfidence() {
        Object confidence = getProperty("confidence");
        return confidence instanceof Number ? ((Number)confidence).doubleValue() : 1.0;
    }
    
    /**
     * Set multiple attributes at once
     * 
     * @param attributes Map of attribute names and values
     */
    public void setAttributes(Map<String, Object> attributes) {
        if (attributes != null) {
            this.properties.putAll(attributes);
        }
    }
    
    /**
     * Get all element attributes
     * 
     * @return Map of attribute names and values
     */
    public Map<String, Object> getAttributes() {
        return new HashMap<>(properties);
    }
    
    /**
     * Set whether this element is scrollable
     * 
     * @param scrollable True if element is scrollable
     */
    public void setScrollable(boolean scrollable) {
        setProperty("scrollable", scrollable);
    }
    
    /**
     * Check if this element is scrollable
     * 
     * @return True if scrollable
     */
    public boolean isScrollable() {
        Object scrollable = getProperty("scrollable");
        return scrollable instanceof Boolean ? (Boolean)scrollable : false;
    }
    
    /**
     * Set whether this element is editable
     * 
     * @param editable True if element is editable
     */
    public void setEditable(boolean editable) {
        setProperty("editable", editable);
    }
    
    /**
     * Check if this element is editable
     * 
     * @return True if editable
     */
    public boolean isEditable() {
        Object editable = getProperty("editable");
        return editable instanceof Boolean ? (Boolean)editable : false;
    }
    
    /**
     * Set whether this element is focusable
     * 
     * @param focusable True if element is focusable
     */
    public void setFocusable(boolean focusable) {
        setProperty("focusable", focusable);
    }
    
    /**
     * Check if this element is focusable
     * 
     * @return True if focusable
     */
    public boolean isFocusable() {
        Object focusable = getProperty("focusable");
        return focusable instanceof Boolean ? (Boolean)focusable : false;
    }
    
    /**
     * Set whether this element is focused
     * 
     * @param focused True if element is focused
     */
    public void setFocused(boolean focused) {
        setProperty("focused", focused);
    }
    
    /**
     * Check if this element is focused
     * 
     * @return True if focused
     */
    public boolean isFocused() {
        Object focused = getProperty("focused");
        return focused instanceof Boolean ? (Boolean)focused : false;
    }
    
    /**
     * Set the depth of this element in the UI hierarchy
     * 
     * @param depth The depth value
     */
    public void setDepth(int depth) {
        this.depth = depth;
        setProperty("depth", depth);
    }
    
    /**
     * Get the depth of this element in the UI hierarchy
     * 
     * @return The depth value
     */
    public int getDepth() {
        Object depthObj = getProperty("depth");
        if (depthObj instanceof Number) {
            return ((Number)depthObj).intValue();
        }
        return this.depth;
    }
    
    /**
     * Set all properties at once
     * 
     * @param properties Map of property names and values
     */
    public void setProperties(Map<String, Object> properties) {
        if (properties != null) {
            this.properties.clear();
            this.properties.putAll(properties);
        }
    }
    
    /**
     * Convert to a map representation.
     * 
     * @return Map representation
     */
    public Map<String, Object> toMap() {
        Map<String, Object> map = new HashMap<>();
        map.put("id", id);
        map.put("text", text);
        map.put("type", type.toString());
        map.put("bounds", bounds);
        map.put("enabled", enabled);
        map.put("selectable", selectable);
        map.put("selected", selected);
        map.put("clickable", clickable);
        map.put("properties", properties);
        return map;
    }
    
    /**
     * Create a StandardizedUIElement from a map representation.
     * 
     * @param map Map representation
     * @return StandardizedUIElement instance
     */
    public static StandardizedUIElement fromMap(Map<String, Object> map) {
        if (map == null) {
            return new StandardizedUIElement();
        }
        
        StandardizedUIElement element = new StandardizedUIElement();
        
        if (map.containsKey("id")) {
            element.setId((String) map.get("id"));
        }
        
        if (map.containsKey("text")) {
            element.setText((String) map.get("text"));
        }
        
        if (map.containsKey("type")) {
            String typeStr = (String) map.get("type");
            element.setType(StandardizedUIElementType.fromString(typeStr));
        }
        
        if (map.containsKey("bounds") && map.get("bounds") instanceof int[]) {
            element.setBounds((int[]) map.get("bounds"));
        }
        
        if (map.containsKey("enabled") && map.get("enabled") instanceof Boolean) {
            element.setEnabled((Boolean) map.get("enabled"));
        }
        
        if (map.containsKey("selectable") && map.get("selectable") instanceof Boolean) {
            element.setSelectable((Boolean) map.get("selectable"));
        }
        
        if (map.containsKey("selected") && map.get("selected") instanceof Boolean) {
            element.setSelected((Boolean) map.get("selected"));
        }
        
        if (map.containsKey("clickable") && map.get("clickable") instanceof Boolean) {
            element.setClickable((Boolean) map.get("clickable"));
        }
        
        if (map.containsKey("properties") && map.get("properties") instanceof Map) {
            @SuppressWarnings("unchecked")
            Map<String, Object> props = (Map<String, Object>) map.get("properties");
            for (Map.Entry<String, Object> entry : props.entrySet()) {
                element.setProperty(entry.getKey(), entry.getValue());
            }
        }
        
        return element;
    }
    
    /**
     * Get an attribute value
     * Required by UIElementInterface
     * 
     * @param name Attribute name
     * @return Attribute value
     */
    @Override
    public Object getAttribute(String name) {
        return getProperty(name);
    }
    
    /**
     * Get an attribute value with a default value
     * Required by UIElementInterface
     * 
     * @param name Attribute name
     * @param defaultValue Default value to return if attribute is not found
     * @return Attribute value or defaultValue
     */
    @Override
    public Object getAttribute(String name, Object defaultValue) {
        Object value = getAttribute(name);
        return value != null ? value : defaultValue;
    }

    /**
     * Get the element bounds as a Rect
     * Required by UIElementInterface
     * 
     * @return Bounds rectangle
     */
    @Override
    public Rect getBounds() {
        if (bounds == null || bounds.length < 4) {
            return new Rect(0, 0, 0, 0);
        }
        return new Rect(bounds[0], bounds[1], bounds[2], bounds[3]);
    }
    
    /**
     * Set the element type using an object
     * Required by UIElementInterface
     * 
     * @param type The type object
     */
    @Override
    public void setType(Object type) {
        if (type instanceof String) {
            this.type = StandardizedUIElementType.fromString((String)type);
        } else if (type instanceof StandardizedUIElementType) {
            this.type = (StandardizedUIElementType)type;
        } else if (type instanceof ElementType) {
            String typeName = ((ElementType)type).name();
            this.type = StandardizedUIElementType.fromString(typeName);
        } else if (type != null) {
            this.type = StandardizedUIElementType.fromString(type.toString());
        } else {
            this.type = StandardizedUIElementType.UNKNOWN;
        }
    }
    
    /**
     * Check if a point is contained within this element
     * 
     * @param x X coordinate
     * @param y Y coordinate
     * @return True if the point is contained, false otherwise
     */
    @Override
    public boolean contains(int x, int y) {
        Rect rect = getBounds();
        return x >= rect.left && x <= rect.right && y >= rect.top && y <= rect.bottom;
    }

    /**
     * Check if this element intersects with another element
     * 
     * @param element The other element
     * @return True if they intersect, false otherwise
     */
    @Override
    public boolean intersects(UIElementInterface element) {
        if (element == null) {
            return false;
        }
        
        Rect rect1 = getBounds();
        Rect rect2 = element.getBounds();
        
        return !(rect1.left > rect2.right || 
                rect1.right < rect2.left || 
                rect1.top > rect2.bottom || 
                rect1.bottom < rect2.top);
    }
    
    /**
     * Convert this StandardizedUIElement to a utils.UIElement
     * 
     * @return a utils.UIElement representation of this element
     */
    public utils.UIElement toUtilsUIElement() {
        utils.UIElement element = new utils.UIElement();
        element.setId(this.getId());
        element.setText(this.getText());
        
        // Convert element type
        if (this.getElementTypeObject() != null) {
            utils.ElementType utilsType = 
                utils.ElementTypeConverter.toUtilsElementType(this.getElementTypeObject().toElementType());
            element.setType(utilsType);
        }
        
        // Convert bounds
        if (this.getBoundsArray() != null && this.getBoundsArray().length == 4) {
            utils.Rect rect = new utils.Rect(
                this.getBoundsArray()[0],
                this.getBoundsArray()[1],
                this.getBoundsArray()[2],
                this.getBoundsArray()[3]
            );
            element.setBounds(rect);
        }
        
        // Set basic properties
        element.setEnabled(this.isEnabled());
        element.setClickable(this.isClickable());
        element.setSelected(this.isSelected());
        element.setScrollable(this.isScrollable());
        element.setFocusable(this.isFocusable());
        element.setEditable(this.isEditable());
        
        // Copy additional properties
        for (Map.Entry<String, Object> entry : this.getProperties().entrySet()) {
            element.setProperty(entry.getKey(), entry.getValue());
        }
        
        return element;
    }
}