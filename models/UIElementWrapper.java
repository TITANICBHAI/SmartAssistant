package models;

import utils.Rect;
import java.util.Map;

/**
 * A wrapper that converts utils.UIElementInterface to models.UIElementInterface
 * This allows compatibility between the two element systems
 */
public class UIElementWrapper implements UIElementInterface {
    private utils.UIElementInterface wrappedElement;
    
    // Fields to use when there's no wrapped element
    private String id;
    private String type;
    private String text;
    private Rect bounds;
    private double confidence;
    private boolean clickable;
    private boolean visible = true;
    private boolean enabled = true;
    private Map<String, Object> attributes = new java.util.HashMap<>();
    private Map<String, Object> properties = new java.util.HashMap<>();
    private UIElementInterface[] children = new UIElementInterface[0];
    
    /**
     * Create a new empty wrapper
     */
    public UIElementWrapper() {
        this.wrappedElement = null;
    }
    
    /**
     * Create a new wrapper around a utils.UIElementInterface
     * 
     * @param element The element to wrap
     */
    public UIElementWrapper(utils.UIElementInterface element) {
        this.wrappedElement = element;
    }
    
    /**
     * Get the wrapped element
     * 
     * @return The wrapped element
     */
    @Override
    public utils.UIElementInterface getWrappedElement() {
        return wrappedElement;
    }
    
    @Override
    public String getType() {
        if (wrappedElement != null) {
            return wrappedElement.getType();
        }
        return type;
    }
    
    @Override
    public ElementType getElementType() {
        String typeStr = getType();
        if (typeStr == null) {
            return ElementType.UNKNOWN;
        }
        try {
            return ElementType.valueOf(typeStr.toUpperCase());
        } catch (IllegalArgumentException e) {
            return ElementType.UNKNOWN;
        }
    }
    
    @Override
    public Object getAttribute(String name) {
        if (wrappedElement != null) {
            return wrappedElement.getAttribute(name);
        }
        return attributes.get(name);
    }
    
    @Override
    public Map<String, Object> getAttributes() {
        if (wrappedElement != null) {
            return wrappedElement.getAttributes();
        }
        return attributes;
    }
    
    @Override
    public void setAttribute(String name, Object value) {
        if (wrappedElement != null) {
            wrappedElement.setAttribute(name, value);
        } else {
            attributes.put(name, value);
        }
    }
    
    @Override
    public void setAttributes(Map<String, Object> attributes) {
        if (wrappedElement != null) {
            wrappedElement.setAttributes(attributes);
        } else if (attributes != null) {
            this.attributes.clear();
            this.attributes.putAll(attributes);
        }
    }
    
    @Override
    public Rect getBounds() {
        if (wrappedElement != null) {
            return wrappedElement.getRectBounds();
        }
        return bounds;
    }
    
    @Override
    public Rect getRectBounds() {
        return getBounds();
    }
    
    @Override
    public int[] getBoundsArray() {
        if (wrappedElement != null) {
            return wrappedElement.getBoundsArray();
        }
        Rect rect = getBounds();
        if (rect == null) {
            return new int[]{0, 0, 0, 0};
        }
        return new int[]{rect.left, rect.top, rect.width(), rect.height()};
    }
    
    @Override
    public void setBounds(Rect bounds) {
        if (wrappedElement != null) {
            wrappedElement.setBounds(bounds);
        } else {
            this.bounds = bounds;
        }
    }
    
    @Override
    public void setBounds(int[] bounds) {
        if (wrappedElement != null) {
            wrappedElement.setBounds(bounds);
        } else if (bounds != null && bounds.length >= 4) {
            this.bounds = new Rect(
                bounds[0], 
                bounds[1], 
                bounds[0] + bounds[2], 
                bounds[1] + bounds[3]
            );
        }
    }
    
    @Override
    public double getConfidence() {
        if (wrappedElement != null) {
            return wrappedElement.getConfidence();
        }
        return confidence;
    }
    
    @Override
    public float getConfidenceFloat() {
        return (float)getConfidence();
    }
    
    @Override
    public void setConfidence(double confidence) {
        if (wrappedElement != null) {
            wrappedElement.setConfidence(confidence);
        } else {
            this.confidence = confidence;
        }
    }
    
    @Override
    public void setConfidence(float confidence) {
        setConfidence((double)confidence);
    }
    
    @Override
    public String getId() {
        if (wrappedElement != null) {
            return wrappedElement.getId();
        }
        return id;
    }
    
    @Override
    public void setId(String id) {
        if (wrappedElement != null) {
            wrappedElement.setId(id);
        } else {
            this.id = id;
        }
    }
    
    @Override
    public String getText() {
        if (wrappedElement != null) {
            return wrappedElement.getText();
        }
        return text;
    }
    
    @Override
    public void setText(String text) {
        if (wrappedElement != null) {
            wrappedElement.setText(text);
        } else {
            this.text = text;
        }
    }
    
    @Override
    public UIElementInterface[] getChildren() {
        if (wrappedElement != null) {
            utils.UIElementInterface[] utilsChildren = wrappedElement.getChildren();
            if (utilsChildren == null) {
                return new UIElementInterface[0];
            }
            
            UIElementInterface[] result = new UIElementInterface[utilsChildren.length];
            for (int i = 0; i < utilsChildren.length; i++) {
                result[i] = new UIElementWrapper(utilsChildren[i]);
            }
            return result;
        }
        return children;
    }
    
    @Override
    public void setChildren(UIElementInterface[] children) {
        if (wrappedElement != null) {
            if (children == null) {
                wrappedElement.setChildren(new utils.UIElementInterface[0]);
                return;
            }
            
            utils.UIElementInterface[] result = new utils.UIElementInterface[children.length];
            for (int i = 0; i < children.length; i++) {
                utils.UIElementInterface wrapped = children[i].getWrappedElement();
                result[i] = wrapped != null ? wrapped : new utils.UIElementAdapter(children[i]);
            }
            wrappedElement.setChildren(result);
        } else {
            this.children = children != null ? children : new UIElementInterface[0];
        }
    }
    
    @Override
    public boolean isClickable() {
        if (wrappedElement != null) {
            return wrappedElement.isClickable();
        }
        return clickable;
    }
    
    @Override
    public void setClickable(boolean clickable) {
        if (wrappedElement != null) {
            wrappedElement.setClickable(clickable);
        } else {
            this.clickable = clickable;
        }
    }
    
    @Override
    public boolean isVisible() {
        if (wrappedElement != null) {
            return wrappedElement.isVisible();
        }
        return visible;
    }
    
    @Override
    public boolean isEnabled() {
        if (wrappedElement != null) {
            return wrappedElement.isEnabled();
        }
        return enabled;
    }
    
    @Override
    public void setEnabled(boolean enabled) {
        if (wrappedElement != null) {
            wrappedElement.setEnabled(enabled);
        } else {
            this.enabled = enabled;
        }
    }
    
    // Flags for standalone element
    private boolean selectable = false;
    private boolean selected = false;
    private boolean scrollable = false;
    private boolean editable = false;
    private boolean focusable = false;
    private boolean focused = false;
    
    @Override
    public boolean isSelectable() {
        if (wrappedElement != null) {
            return wrappedElement.isSelectable();
        }
        return selectable;
    }
    
    @Override
    public void setSelectable(boolean selectable) {
        if (wrappedElement != null) {
            wrappedElement.setSelectable(selectable);
        } else {
            this.selectable = selectable;
        }
    }
    
    @Override
    public boolean isSelected() {
        if (wrappedElement != null) {
            return wrappedElement.isSelected();
        }
        return selected;
    }
    
    @Override
    public void setSelected(boolean selected) {
        if (wrappedElement != null) {
            wrappedElement.setSelected(selected);
        } else {
            this.selected = selected;
        }
    }
    
    @Override
    public boolean isScrollable() {
        if (wrappedElement != null) {
            return wrappedElement.isScrollable();
        }
        return scrollable;
    }
    
    @Override
    public void setScrollable(boolean scrollable) {
        if (wrappedElement != null) {
            wrappedElement.setScrollable(scrollable);
        } else {
            this.scrollable = scrollable;
        }
    }
    
    @Override
    public boolean isEditable() {
        if (wrappedElement != null) {
            return wrappedElement.isEditable();
        }
        return editable;
    }
    
    @Override
    public void setEditable(boolean editable) {
        if (wrappedElement != null) {
            wrappedElement.setEditable(editable);
        } else {
            this.editable = editable;
        }
    }
    
    @Override
    public boolean isFocusable() {
        if (wrappedElement != null) {
            return wrappedElement.isFocusable();
        }
        return focusable;
    }
    
    @Override
    public void setFocusable(boolean focusable) {
        if (wrappedElement != null) {
            wrappedElement.setFocusable(focusable);
        } else {
            this.focusable = focusable;
        }
    }
    
    @Override
    public boolean isFocused() {
        if (wrappedElement != null) {
            return wrappedElement.isFocused();
        }
        return focused;
    }
    
    @Override
    public void setVisible(boolean visible) {
        if (wrappedElement != null) {
            wrappedElement.setVisible(visible);
        } else {
            this.visible = visible;
        }
    }
    
    @Override
    public void setFocused(boolean focused) {
        if (wrappedElement != null) {
            wrappedElement.setFocused(focused);
        } else {
            this.focused = focused;
        }
    }
    
    @Override
    public boolean contains(int x, int y) {
        if (wrappedElement != null) {
            return wrappedElement.contains(x, y);
        }
        
        // Check if the point is within this element's bounds
        Rect rect = getBounds();
        if (rect == null) {
            return false;
        }
        return x >= rect.left && x < rect.right && y >= rect.top && y < rect.bottom;
    }
    
    @Override
    public boolean intersects(UIElementInterface element) {
        if (element == null) {
            return false;
        }
        
        if (wrappedElement != null) {
            utils.UIElementInterface wrapped = element.getWrappedElement();
            if (wrapped != null) {
                return wrappedElement.intersects(wrapped);
            }
            return wrappedElement.intersects(new utils.UIElementAdapter(element));
        }
        
        // Check if the bounds rectangles intersect
        Rect thisBounds = getBounds();
        Rect otherBounds = element.getBounds();
        if (thisBounds == null || otherBounds == null) {
            return false;
        }
        
        return thisBounds.intersects(otherBounds);
    }
    
    @Override
    public void setType(String type) {
        if (wrappedElement != null) {
            wrappedElement.setType(type);
        } else {
            this.type = type;
        }
    }
    
    @Override
    public void setType(Object type) {
        if (wrappedElement != null) {
            wrappedElement.setType(type);
        } else if (type != null) {
            this.type = type.toString();
        }
    }
    
    @Override
    public Object getProperty(String key) {
        if (wrappedElement != null) {
            return wrappedElement.getProperty(key);
        }
        return properties.get(key);
    }
    
    @Override
    public void setProperty(String key, Object value) {
        if (wrappedElement != null) {
            wrappedElement.setProperty(key, value);
        } else if (key != null) {
            properties.put(key, value);
        }
    }
    
    @Override
    public Map<String, Object> getProperties() {
        if (wrappedElement != null) {
            return wrappedElement.getProperties();
        }
        return properties;
    }
    
    @Override
    public void setProperties(Map<String, Object> properties) {
        if (wrappedElement != null) {
            wrappedElement.setProperties(properties);
        } else if (properties != null) {
            this.properties.clear();
            this.properties.putAll(properties);
        }
    }
    
    @Override
    public utils.UIElement toUIElement() {
        if (wrappedElement instanceof utils.UIElement) {
            return (utils.UIElement)wrappedElement;
        }
        return new utils.UIElementAdapter(this);
    }
    
    @Override
    public String toString() {
        return "UIElementWrapper{" +
                "id='" + getId() + '\'' +
                ", type='" + getType() + '\'' +
                ", text='" + getText() + '\'' +
                ", bounds=" + getBounds() +
                ", clickable=" + isClickable() +
                ", visible=" + isVisible() +
                '}';
    }
}