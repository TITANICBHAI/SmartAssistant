package utils;

import java.util.Map;

/**
 * Helper class for converting between UIElementInterface implementations
 */
public class UIElementInterfaceHelper {
    
    /**
     * Convert a models.UIElementInterface to a utils.UIElementInterface
     * 
     * @param element The models.UIElementInterface to convert
     * @return A utils.UIElementInterface representing the provided element
     */
    public static UIElementInterface fromInterface(models.UIElementInterface element) {
        if (element == null) {
            return null;
        }
        
        // If element is already a utils.UIElement, just return it
        if (element instanceof UIElement) {
            return (UIElement) element;
        }
        
        // Create a new DefaultUIElement
        DefaultUIElement result = new DefaultUIElement();
        
        // Copy basic properties
        result.setId(element.getId());
        result.setText(element.getText());
        result.setType(element.getType());
        
        // Get bounds from the element
        result.setBounds(element.getBoundsArray());
        
        // Copy state properties
        result.setClickable(element.isClickable());
        result.setVisible(element.isVisible());
        result.setEnabled(element.isEnabled());
        result.setSelectable(element.isSelectable());
        result.setSelected(element.isSelected());
        result.setScrollable(element.isScrollable());
        result.setEditable(element.isEditable());
        result.setFocusable(element.isFocusable());
        result.setFocused(element.isFocused());
        
        // Copy confidence score
        result.setConfidence(element.getConfidence());
        
        // Copy attributes
        Map<String, Object> attributes = element.getAttributes();
        if (attributes != null) {
            for (Map.Entry<String, Object> entry : attributes.entrySet()) {
                result.setAttribute(entry.getKey(), entry.getValue());
            }
        }
        
        // Copy properties
        Map<String, Object> properties = element.getProperties();
        if (properties != null) {
            for (Map.Entry<String, Object> entry : properties.entrySet()) {
                result.setProperty(entry.getKey(), entry.getValue());
            }
        }
        
        return result;
    }
    
    /**
     * Convert a utils.UIElement to a models.UIElement
     * 
     * @param element The utils.UIElement to convert
     * @return A models.UIElement representing the provided element
     */
    public static models.UIElement toModelsElement(UIElement element) {
        if (element == null) {
            return null;
        }
        
        // If element is already a models.UIElementInterface, we need to adapt it
        if (element instanceof models.UIElementInterface) {
            // Get the element as a models.UIElement
            models.UIElementInterface modelsInterface = (models.UIElementInterface)element;
            // Use the special UIElement implementation in models package (not the wrapper)
            if (modelsInterface instanceof models.UIElement) {
                return (models.UIElement)modelsInterface;
            } else {
                // Create a wrapper only if needed for other interface implementations
                models.UIElement newElement = new models.UIElement();
                newElement.setId(modelsInterface.getId());
                newElement.setText(modelsInterface.getText());
                newElement.setType(modelsInterface.getType());
                newElement.setBounds(modelsInterface.getBoundsArray());
                newElement.setClickable(modelsInterface.isClickable());
                newElement.setVisible(modelsInterface.isVisible());
                newElement.setEnabled(modelsInterface.isEnabled());
                newElement.setConfidence(modelsInterface.getConfidence());
                return newElement;
            }
        }
        
        // Create a new models.UIElement
        models.UIElement result = new models.UIElement();
        
        // Copy basic properties
        result.setId(element.getId());
        result.setText(element.getText());
        result.setType(element.getType());
        
        // Get bounds from the element
        result.setBounds(element.getBoundsArray());
        
        // Copy state properties
        result.setClickable(element.isClickable());
        result.setVisible(element.isVisible());
        result.setEnabled(element.isEnabled());
        result.setSelectable(element.isSelectable());
        result.setSelected(element.isSelected());
        result.setScrollable(element.isScrollable());
        result.setEditable(element.isEditable());
        result.setFocusable(element.isFocusable());
        result.setFocused(element.isFocused());
        
        // Copy confidence score
        result.setConfidence(element.getConfidence());
        
        // Copy attributes
        Map<String, Object> attributes = element.getAttributes();
        if (attributes != null) {
            for (Map.Entry<String, Object> entry : attributes.entrySet()) {
                result.setAttribute(entry.getKey(), entry.getValue());
            }
        }
        
        // Copy properties
        Map<String, Object> properties = element.getProperties();
        if (properties != null) {
            for (Map.Entry<String, Object> entry : properties.entrySet()) {
                result.setProperty(entry.getKey(), entry.getValue());
            }
        }
        
        return result;
    }
    
    /**
     * Wrap a utils.UIElement in a models.UIElementWrapper
     * 
     * @param element The utils.UIElement to wrap
     * @return A models.UIElementWrapper wrapping the provided element
     */
    public static models.UIElementInterface wrapElement(UIElement element) {
        if (element == null) {
            return null;
        }
        
        try {
            // Use reflection to create the wrapper to avoid direct dependency
            Class<?> wrapperClass = Class.forName("models.UIElementWrapper");
            java.lang.reflect.Constructor<?> constructor = wrapperClass.getConstructor(UIElementInterface.class);
            return (models.UIElementInterface) constructor.newInstance(element);
        } catch (Exception e) {
            System.err.println("Error creating wrapper: " + e.getMessage());
            return null;
        }
    }
}