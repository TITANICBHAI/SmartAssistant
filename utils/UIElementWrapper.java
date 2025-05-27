package utils;

/**
 * Wrapper class for UIElementInterface objects.
 * This is a utility class to provide common functionality across all UIElementInterface implementations.
 */
public class UIElementWrapper {
    /**
     * The wrapped element.
     */
    protected final UIElementInterface element;
    
    /**
     * Constructor for UIElementWrapper.
     * 
     * @param element The element to wrap
     */
    public UIElementWrapper(UIElementInterface element) {
        this.element = element;
    }
    
    /**
     * Get the wrapped element.
     * 
     * @return The wrapped element
     */
    public UIElementInterface getWrappedElement() {
        return element;
    }
    
    /**
     * Get the wrapped element if it's a StandardizedUIElement.
     * 
     * @return The wrapped StandardizedUIElement or null if not a StandardizedUIElement
     */
    public StandardizedUIElement getWrappedStandardizedElement() {
        if (element instanceof StandardizedUIElement) {
            return (StandardizedUIElement) element;
        }
        return null;
    }
    
    /**
     * Convert the wrapped element to a StandardizedUIElement.
     * 
     * @return A StandardizedUIElement representation of the wrapped element
     */
    public StandardizedUIElement toStandardizedElement() {
        return StandardizedUIElementHelper.toStandardizedElement(element);
    }
    
    /**
     * Get the element ID.
     * 
     * @return The element ID
     */
    public String getId() {
        return element.getId();
    }
    
    /**
     * Get the element text.
     * 
     * @return The element text
     */
    public String getText() {
        return element.getText();
    }
    
    /**
     * Get the element type.
     * 
     * @return The element type
     */
    public String getType() {
        return element.getType();
    }
    
    /**
     * Get the element bounds.
     * 
     * @return The element bounds
     */
    public Rect getBounds() {
        return element.getRectBounds();
    }
    
    /**
     * Check if the element is clickable.
     * 
     * @return True if clickable
     */
    public boolean isClickable() {
        return element.isClickable();
    }
    
    /**
     * Check if the element is visible.
     * 
     * @return True if visible
     */
    public boolean isVisible() {
        return element.isVisible();
    }
    
    /**
     * Check if the element is enabled.
     * 
     * @return True if enabled
     */
    public boolean isEnabled() {
        return element.isEnabled();
    }
    
    /**
     * Get a description of the element.
     * 
     * @return A description string
     */
    public String getDescription() {
        return StandardizedUIElementHelper.getElementDescription(element);
    }
    
    @Override
    public String toString() {
        return getDescription();
    }
    
    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null || getClass() != obj.getClass()) {
            return false;
        }
        
        UIElementWrapper other = (UIElementWrapper) obj;
        return element.equals(other.element);
    }
    
    @Override
    public int hashCode() {
        return element.hashCode();
    }
}