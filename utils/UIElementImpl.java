package utils;

import java.util.HashMap;
import java.util.Map;
import java.util.ArrayList;
import java.util.List;
import java.util.Arrays;
import java.lang.reflect.Method;
import utils.ElementTypeConverter;

/**
 * Implementation of UIElementInterface, with methods for UIElement compatibility.
 * Using composition instead of multiple inheritance to avoid method conflicts.
 */
public class UIElementImpl extends UIElement implements UIElementInterface {
    private String id;
    private String typeStr;
    private utils.ElementType elementType = utils.ElementType.UNKNOWN;
    private utils.Rect bounds;
    private String text;
    private boolean clickable;
    private Map<String, Object> attributes;
    private double confidence = 1.0;
    private boolean scrollable = false;
    private boolean editable = false;
    private boolean focusable = false;
    private boolean focused = false;
    private boolean visible = true;
    private boolean enabled = true;
    private boolean selectable = false;
    private boolean selected = false;
    private int depth = 0;
    private UIElement parent = null;
    private List<UIElement> children = new ArrayList<>();
    private Map<String, Object> properties = new HashMap<>();
    
    /**
     * Default constructor.
     */
    public UIElementImpl() {
        this.attributes = new HashMap<>();
        this.properties = new HashMap<>();
    }
    
    /**
     * Constructor with ID and type.
     * 
     * @param id The element's ID
     * @param type The element's type
     */
    public UIElementImpl(String id, String type) {
        this();
        this.id = id;
        this.typeStr = type;
        try {
            this.elementType = utils.ElementType.valueOf(type.toUpperCase());
        } catch (Exception e) {
            this.elementType = utils.ElementType.UNKNOWN;
        }
    }
    
    /**
     * Constructor with full parameters.
     * 
     * @param id The element's ID
     * @param type The element's type
     * @param bounds The element's bounds
     * @param text The element's text
     * @param clickable Whether the element is clickable
     */
    public UIElementImpl(String id, String type, utils.Rect bounds, String text, boolean clickable) {
        this();
        this.id = id;
        this.typeStr = type;
        try {
            this.elementType = utils.ElementType.valueOf(type.toUpperCase());
        } catch (Exception e) {
            this.elementType = utils.ElementType.UNKNOWN;
        }
        this.bounds = bounds;
        this.text = text;
        this.clickable = clickable;
    }
    
    @Override
    public String getId() {
        return id;
    }
    
    /**
     * Set the element's ID.
     * 
     * @param id The ID to set
     */
    public void setId(String id) {
        this.id = id;
    }
    
    /**
     * Get the element type as a string for UIElementInterface
     * 
     * @return The element type as a string
     */
    /**
     * Get the type string for UIElementInterface
     * This method satisfies the UIElementInterface requirement
     * 
     * @return The element type as a string
     */
    @Override
    public String getType() {
        return elementType != null ? elementType.name() : "UNKNOWN";
    }
    
    /**
     * Get the element type as an enum for UIElement
     * This method satisfies the UIElement interface requirement
     * 
     * @return The element type as an enum
     */
    @Override
    public UIElement.ElementType getElementType() {
        // Convert from utils.ElementType to UIElement.ElementType
        if (elementType == null) {
            return UIElement.ElementType.UNKNOWN;
        }
        try {
            return UIElement.ElementType.valueOf(elementType.name());
        } catch (IllegalArgumentException e) {
            return UIElement.ElementType.UNKNOWN;
        }
    }
    
    @Override
    public void setType(String typeStr) {
        this.typeStr = typeStr;
        // Try to update enum type too
        try {
            this.elementType = utils.ElementType.valueOf(typeStr.toUpperCase());
        } catch (Exception e) {
            this.elementType = utils.ElementType.UNKNOWN;
        }
    }
    
    @Override
    public void setType(Object type) {
        if (type != null) {
            setType(type.toString());
        }
    }
    
    public void setType(utils.ElementType type) {
        this.elementType = type;
        this.typeStr = type != null ? type.name() : "UNKNOWN";
    }
    
    /**
     * Set the element type using the ElementType enum
     * This method is needed for compatibility with UIElementDetector
     * 
     * @param type The element type
     */
    public void setElementType(utils.ElementType type) {
        setType(type);
    }
    
    /**
     * Set the element type using the models.ElementType enum
     * This method is needed for compatibility with code using models.ElementType
     * 
     * @param type The element type from models package
     */
    public void setElementType(models.ElementType type) {
        if (type != null) {
            this.elementType = ElementTypeConverter.toUtilsElementType(type);
            this.typeStr = this.elementType.name();
        }
    }
    
    /**
     * Get the bounds for UIElementInterface - returns a Rect
     * This implements UIElementInterface.getRectBounds() but with a different name
     * for backward compatibility with other code expecting getBounds()
     * 
     * @return The bounds as a Rect
     */
    public utils.Rect getBounds() {
        return bounds;
    }
    
    @Override
    public utils.Rect getRectBounds() {
        return bounds;
    }
    
    /**
     * Get the bounds as an array [left, top, width, height]
     * This implements both UIElementInterface.getBoundsArray() and UIElement.getBoundsArray() methods
     * 
     * @return The bounds as an array of 4 integers [left, top, width, height]
     */
    @Override
    public int[] getBoundsArray() {
        if (bounds == null) {
            return new int[]{0, 0, 0, 0};
        }
        return new int[]{bounds.left, bounds.top, bounds.width(), bounds.height()};
    }
    
    /**
     * Legacy method to get bounds for UIElement - returns an array
     * Note: This is now handled by default method in UIElement interface
     * 
     * @return The bounds as an array [x, y, width, height]
     */
    public int[] getBoundsAsArray() {
        if (bounds == null) {
            return new int[]{0, 0, 0, 0};
        }
        return new int[]{bounds.left, bounds.top, bounds.right - bounds.left, bounds.bottom - bounds.top};
    }
    
    @Override
    public void setBounds(utils.Rect bounds) {
        this.bounds = bounds;
    }
    
    @Override
    public void setRectBounds(utils.Rect bounds) {
        this.bounds = bounds;
    }
    
    @Override
    public void setBounds(int[] bounds) {
        if (bounds != null && bounds.length >= 4) {
            this.bounds = new utils.Rect(bounds[0], bounds[1], bounds[0] + bounds[2], bounds[1] + bounds[3]);
        }
    }
    
    /**
     * Sets the bounds from an Android graphics Rect
     * This method is added for compatibility with Android's Rect class
     * 
     * @param androidRect The Android graphics.Rect to set bounds from
     */
    public void setBounds(android.graphics.Rect androidRect) {
        if (androidRect == null) {
            this.bounds = new utils.Rect();
            return;
        }
        
        this.bounds = new utils.Rect(
            androidRect.left,
            androidRect.top,
            androidRect.right,
            androidRect.bottom
        );
    }
    
    @Override
    public String getText() {
        return text;
    }
    
    /**
     * Set the element's text.
     * 
     * @param text The text to set
     */
    @Override
    public void setText(String text) {
        this.text = text;
    }
    
    @Override
    public boolean isClickable() {
        return clickable;
    }
    
    /**
     * Set whether the element is clickable.
     * 
     * @param clickable Whether the element is clickable
     */
    @Override
    public void setClickable(boolean clickable) {
        this.clickable = clickable;
    }
    
    public int getX() {
        return bounds != null ? bounds.left : 0;
    }
    
    public int getY() {
        return bounds != null ? bounds.top : 0;
    }
    
    public int getWidth() {
        return bounds != null ? bounds.width() : 0;
    }
    
    public int getHeight() {
        return bounds != null ? bounds.height() : 0;
    }
    
    @Override
    public Map<String, Object> getAttributes() {
        return attributes;
    }
    
    /**
     * Set all attributes.
     * 
     * @param attributes The attributes to set
     */
    @Override
    public void setAttributes(Map<String, Object> attributes) {
        if (attributes != null) {
            this.attributes = attributes;
        } else {
            this.attributes = new HashMap<>();
        }
    }
    
    @Override
    public Object getAttribute(String name) {
        return attributes.get(name);
    }
    
    /**
     * Get an attribute with a default value
     * 
     * @param name The attribute name
     * @param defaultValue The default value if not found
     * @return The attribute value or the default value
     */
    public Object getAttribute(String name, Object defaultValue) {
        Object value = attributes.get(name);
        return value != null ? value : defaultValue;
    }
    
    /**
     * Set an attribute.
     * 
     * @param name The attribute name
     * @param value The attribute value
     */
    @Override
    public void setAttribute(String name, Object value) {
        if (name != null) {
            attributes.put(name, value);
        }
    }
    
    @Override
    public boolean contains(int x, int y) {
        return bounds != null && bounds.contains(x, y);
    }
    
    @Override
    public boolean intersects(UIElementInterface element) {
        if (bounds == null || element == null || element.getRectBounds() == null) {
            return false;
        }
        utils.Rect b1 = bounds;
        utils.Rect b2 = element.getRectBounds();
        // Check for intersection manually since Rect.intersects might not be available
        return !(b1.left > b2.right || b1.right < b2.left || b1.top > b2.bottom || b1.bottom < b2.top);
    }
    
    /**
     * Check if this element intersects with a UIElement.
     * This method is not directly from UIElementInterface but added for compatibility with UIElement.
     * 
     * @param element The element to check
     * @return True if they intersect, false otherwise
     */
    public boolean intersects(UIElement element) {
        if (bounds == null || element == null) {
            return false;
        }
        
        utils.Rect otherRect = null;
        
        // Try to get the rectangle directly if possible
        if (element instanceof UIElementInterface) {
            otherRect = ((UIElementInterface) element).getRectBounds();
        }
        
        // Fall back to using the array method
        if (otherRect == null) {
            int[] elemBounds = element.getBoundsArray();
            if (elemBounds == null || elemBounds.length < 4) {
                return false;
            }
            
            otherRect = new utils.Rect(
                elemBounds[0], 
                elemBounds[1], 
                elemBounds[0] + elemBounds[2], 
                elemBounds[1] + elemBounds[3]
            );
        }
        
        // Use the built-in intersects method
        return bounds.intersects(otherRect);
    }
    
    @Override
    public double getConfidence() {
        return confidence;
    }
    
    @Override
    public float getConfidenceFloat() {
        return (float)confidence;
    }
    
    /**
     * Override to handle float return type for models.UIElementInterface compatibility
     * This is needed to avoid conflicting method signatures
     * Note: This is handled at compile time based on the interface being implemented
     */
    
    @Override
    public void setConfidence(double confidence) {
        this.confidence = confidence;
    }
    
    @Override
    public void setConfidence(float confidence) {
        this.confidence = confidence;
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
    public Map<String, Object> getProperties() {
        return properties;
    }
    
    @Override
    public void setProperties(Map<String, Object> properties) {
        this.properties = properties != null ? properties : new HashMap<>();
    }
    
    @Override
    public Object getProperty(String key) {
        return properties.get(key);
    }
    
    @Override
    public void setProperty(String key, Object value) {
        if (key != null) {
            properties.put(key, value);
        }
    }
    
    // These methods are not in UIElementInterface, so no @Override annotation
    public UIElement getParent() {
        return parent;
    }
    
    public void setParent(UIElement parent) {
        this.parent = parent;
    }
    
    // Implementation for UIElementInterface
    @Override
    public UIElementInterface[] getChildren() {
        return children.toArray(new UIElement[0]);
    }
    
    // Implementation for UIElement-specific functionality
    public UIElement[] getChildrenAsElements() {
        return children.toArray(new UIElement[0]);
    }
    
    @Override
    public void setChildren(UIElementInterface[] children) {
        this.children.clear();
        if (children != null) {
            for (UIElementInterface child : children) {
                if (child instanceof UIElement) {
                    addChild((UIElement)child);
                }
            }
        }
    }
    
    // Implementation for UIElement-specific functionality
    public void setElementChildren(UIElement[] children) {
        this.children.clear();
        if (children != null) {
            for (UIElement child : children) {
                addChild(child);
            }
        }
    }
    
    public void addChild(UIElement child) {
        if (child != null) {
            children.add(child);
            
            // Safely set parent reference using runtime type check
            // Try different approaches to set the parent relationship based on actual type
            if (child.getClass() == UIElementImpl.class) {
                // Direct cast is safe if we're exactly the same class (not a subclass)
                UIElementImpl childImpl = (UIElementImpl)child;
                childImpl.setParent(this);
            } else {
                // For other implementations or adapters, try to use reflection 
                // to avoid direct dependencies and casting issues
                try {
                    // Look for a setParent method on the child
                    Method setParentMethod = child.getClass().getMethod("setParent", UIElement.class);
                    setParentMethod.invoke(child, this);
                } catch (Exception e) {
                    // Silently ignore if setParent doesn't exist or can't be called
                    // This is OK, not all implementations will support parent relationships
                }
            }
        }
    }
    
    public boolean removeChild(UIElement child) {
        if (child != null) {
            return children.remove(child);
        }
        return false;
    }
    
    /**
     * Get the depth of this element in the hierarchy
     * @return The depth
     */
    public int getDepth() {
        return depth;
    }
    
    /**
     * Set the depth of this element in the hierarchy
     * @param depth The depth
     */
    public void setDepth(int depth) {
        this.depth = depth;
    }
    
    @Override
    public String toString() {
        return "UIElementImpl{" +
                "id='" + id + '\'' +
                ", type='" + typeStr + '\'' +
                ", text='" + text + '\'' +
                ", bounds=" + (bounds != null ? bounds.toString() : "null") +
                ", clickable=" + clickable +
                ", confidence=" + confidence +
                '}';
    }
    
    /**
     * Add an attribute to this element
     * @param name Attribute name
     * @param value Attribute value
     */
    public void addAttribute(String name, Object value) {
        setAttribute(name, value);
    }
    
    /**
     * Get the content description of this element
     * @return Content description or null if not set
     */
    public String getContentDescription() {
        Object desc = getAttribute("contentDescription");
        return desc != null ? desc.toString() : null;
    }
    
    /**
     * Set the content description of this element
     * @param description Content description to set
     */
    public void setContentDescription(String description) {
        setAttribute("contentDescription", description);
    }
}