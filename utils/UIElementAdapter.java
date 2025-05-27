package utils;

import models.UIElementInterface;
import java.util.Map;
import java.util.HashMap;

/**
 * Adapter that converts models.UIElementInterface to utils.UIElement
 * This allows compatibility between the two element systems
 */
public class UIElementAdapter extends UIElement {
    // The wrapped element
    private final models.UIElementInterface wrappedElement;
    
    /**
     * Create a new adapter from a models.UIElementInterface
     * 
     * @param element The element to wrap
     */
    public UIElementAdapter(models.UIElementInterface element) {
        super();
        this.wrappedElement = element;
        
        // Copy basic properties
        setId(element.getId());
        setType(element.getType());
        setText(element.getText());
        setClickable(element.isClickable());
        setVisible(element.isVisible());
        setEnabled(element.isEnabled());
        
        // Copy bounds
        Rect rect = element.getRectBounds();
        if (rect != null) {
            setBounds(rect);
        } else {
            int[] bounds = element.getBoundsArray();
            if (bounds != null && bounds.length >= 4) {
                setBounds(bounds);
            }
        }
        
        // Copy attributes
        Map<String, Object> attrs = element.getAttributes();
        if (attrs != null) {
            setAttributes(new HashMap<>(attrs));
        }
        
        // Copy properties
        Map<String, Object> props = element.getProperties();
        if (props != null) {
            setProperties(new HashMap<>(props));
        }
        
        // Copy confidence
        setConfidence((float)element.getConfidence());
        
        // Copy children
        models.UIElementInterface[] children = element.getChildren();
        if (children != null && children.length > 0) {
            UIElement[] uiChildren = new UIElement[children.length];
            for (int i = 0; i < children.length; i++) {
                uiChildren[i] = new UIElementAdapter(children[i]);
            }
            setChildren(uiChildren);
        }
    }
    
    /**
     * Create a new adapter from a DefaultUIElement or any UIElementInterface implementation
     * 
     * @param element The element to adapt
     */
    public UIElementAdapter(utils.UIElementInterface element) {
        super();
        
        // Create a wrapper to bridge between the two interfaces
        this.wrappedElement = new models.UIElementWrapper(element);
        
        // Copy basic properties directly from the source element
        setId(element.getId());
        setType(element.getType());
        setText(element.getText());
        setClickable(element.isClickable());
        setVisible(element.isVisible());
        setEnabled(element.isEnabled());
        
        // Copy bounds
        Rect rect = element.getRectBounds();
        if (rect != null) {
            setBounds(rect);
        } else {
            int[] bounds = element.getBoundsArray();
            if (bounds != null && bounds.length >= 4) {
                setBounds(bounds);
            }
        }
        
        // Copy attributes
        Map<String, Object> attrs = element.getAttributes();
        if (attrs != null) {
            setAttributes(new HashMap<>(attrs));
        }
        
        // Copy properties
        Map<String, Object> props = element.getProperties();
        if (props != null) {
            setProperties(new HashMap<>(props));
        }
        
        // Copy confidence
        setConfidence(element.getConfidence());
        
        // Copy children - this cast needs to be fixed
        // The problem is that element.getChildren() returns utils.UIElementInterface[] 
        // but we're trying to assign it to models.UIElementInterface[]
        // We need to convert each child element manually
        utils.UIElementInterface[] utilsChildren = element.getChildren();
        if (utilsChildren != null && utilsChildren.length > 0) {
            UIElement[] uiChildren = new UIElement[utilsChildren.length];
            for (int i = 0; i < utilsChildren.length; i++) {
                if (utilsChildren[i] instanceof utils.UIElement) {
                    uiChildren[i] = (utils.UIElement)utilsChildren[i];
                } else {
                    // Create a new adapter for each non-UIElement child
                    uiChildren[i] = new UIElementAdapter(utilsChildren[i]);
                }
            }
            setChildren(uiChildren);
        }
    }
    
    /**
     * Get the wrapped element
     * 
     * @return The wrapped element
     */
    public models.UIElementInterface getWrappedElement() {
        return wrappedElement;
    }
    
    @Override
    public void setType(String type) {
        super.setType(type);
        if (wrappedElement != null) {
            wrappedElement.setType(type);
        }
    }
    
    @Override
    public void setText(String text) {
        super.setText(text);
        if (wrappedElement != null) {
            wrappedElement.setText(text);
        }
    }
    
    @Override
    public void setClickable(boolean clickable) {
        super.setClickable(clickable);
        if (wrappedElement != null) {
            wrappedElement.setClickable(clickable);
        }
    }
    
    @Override
    public void setVisible(boolean visible) {
        super.setVisible(visible);
        if (wrappedElement != null) {
            wrappedElement.setVisible(visible);
        }
    }
    
    @Override
    public void setBounds(Rect bounds) {
        super.setBounds(bounds);
        if (wrappedElement != null) {
            wrappedElement.setBounds(bounds);
        }
    }
    
    @Override
    public void setBounds(int[] bounds) {
        super.setBounds(bounds);
        if (wrappedElement != null) {
            wrappedElement.setBounds(bounds);
        }
    }
    
    @Override
    public void setAttribute(String name, Object value) {
        super.setAttribute(name, value);
        if (wrappedElement != null) {
            wrappedElement.setAttribute(name, value);
        }
    }
    
    @Override
    public void setProperty(String key, Object value) {
        super.setProperty(key, value);
        if (wrappedElement != null) {
            wrappedElement.setProperty(key, value);
        }
    }
}