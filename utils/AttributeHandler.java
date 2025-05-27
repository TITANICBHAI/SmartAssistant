package utils;

import java.util.HashMap;
import java.util.Map;

/**
 * Helper class for handling attribute operations for UI elements.
 */
public class AttributeHandler {
    private Map<String, Object> attributes;
    
    /**
     * Creates a new AttributeHandler with an empty attribute map.
     */
    public AttributeHandler() {
        this.attributes = new HashMap<>();
    }
    
    /**
     * Creates a new AttributeHandler with the provided attribute map.
     * 
     * @param attributes Initial attribute map
     */
    public AttributeHandler(Map<String, Object> attributes) {
        this.attributes = attributes != null ? 
            new HashMap<>(attributes) : new HashMap<>();
    }
    
    /**
     * Gets an attribute by name.
     * 
     * @param name Attribute name
     * @return Attribute value or null if not found
     */
    public Object getAttribute(String name) {
        return attributes.get(name);
    }
    
    /**
     * Gets an attribute by name with a default value if the attribute is not found.
     * 
     * @param name Attribute name
     * @param defaultValue Default value to return if attribute is not found
     * @return Attribute value or default value
     */
    public Object getAttribute(String name, Object defaultValue) {
        Object value = getAttribute(name);
        return value != null ? value : defaultValue;
    }
    
    /**
     * Sets an attribute.
     * 
     * @param name Attribute name
     * @param value Attribute value
     */
    public void setAttribute(String name, Object value) {
        attributes.put(name, value);
    }
    
    /**
     * Gets a boolean attribute with a default value.
     * 
     * @param name Attribute name
     * @param defaultValue Default value
     * @return Boolean value or default
     */
    public boolean getBooleanAttribute(String name, boolean defaultValue) {
        Object value = getAttribute(name);
        if (value instanceof Boolean) {
            return (Boolean) value;
        }
        return defaultValue;
    }
    
    /**
     * Gets an integer attribute with a default value.
     * 
     * @param name Attribute name
     * @param defaultValue Default value
     * @return Integer value or default
     */
    public int getIntAttribute(String name, int defaultValue) {
        Object value = getAttribute(name);
        if (value instanceof Integer) {
            return (Integer) value;
        }
        if (value instanceof Number) {
            return ((Number) value).intValue();
        }
        return defaultValue;
    }
    
    /**
     * Gets a float attribute with a default value.
     * 
     * @param name Attribute name
     * @param defaultValue Default value
     * @return Float value or default
     */
    public float getFloatAttribute(String name, float defaultValue) {
        Object value = getAttribute(name);
        if (value instanceof Float) {
            return (Float) value;
        }
        if (value instanceof Number) {
            return ((Number) value).floatValue();
        }
        return defaultValue;
    }
    
    /**
     * Gets a string attribute with a default value.
     * 
     * @param name Attribute name
     * @param defaultValue Default value
     * @return String value or default
     */
    public String getStringAttribute(String name, String defaultValue) {
        Object value = getAttribute(name);
        if (value instanceof String) {
            return (String) value;
        }
        return value != null ? value.toString() : defaultValue;
    }
    
    /**
     * Gets all attributes.
     * 
     * @return Map of all attributes
     */
    public Map<String, Object> getAttributes() {
        return new HashMap<>(attributes);
    }
    
    /**
     * Sets all attributes from the provided map.
     * 
     * @param attributes Attribute map
     */
    public void setAttributes(Map<String, Object> attributes) {
        this.attributes.clear();
        if (attributes != null) {
            this.attributes.putAll(attributes);
        }
    }
    
    /**
     * Clears all attributes.
     */
    public void clearAttributes() {
        attributes.clear();
    }
    
    /**
     * Removes an attribute.
     * 
     * @param name Attribute name
     * @return Previous value or null
     */
    public Object removeAttribute(String name) {
        return attributes.remove(name);
    }
    
    /**
     * Checks if an attribute exists.
     * 
     * @param name Attribute name
     * @return True if attribute exists
     */
    public boolean hasAttribute(String name) {
        return attributes.containsKey(name);
    }
    
    /**
     * Gets the number of attributes.
     * 
     * @return Number of attributes
     */
    public int getAttributeCount() {
        return attributes.size();
    }
    
    /**
     * Merges attributes from another AttributeHandler.
     * 
     * @param other Other AttributeHandler to merge from
     * @param overwrite True to overwrite existing attributes
     */
    public void mergeAttributes(AttributeHandler other, boolean overwrite) {
        if (other == null) {
            return;
        }
        
        Map<String, Object> otherAttributes = other.getAttributes();
        for (Map.Entry<String, Object> entry : otherAttributes.entrySet()) {
            if (overwrite || !attributes.containsKey(entry.getKey())) {
                attributes.put(entry.getKey(), entry.getValue());
            }
        }
    }
}