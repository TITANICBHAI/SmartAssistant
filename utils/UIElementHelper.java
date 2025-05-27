package utils;

import java.util.Map;
import java.util.HashMap;

/**
 * Helper class for working with UI elements
 */
public class UIElementHelper {
    /**
     * Convert a UIElement to a map representation
     *
     * @param element The UI element to convert
     * @return A map representing the element
     */
    public static Map<String, Object> toMap(UIElement element) {
        if (element == null) {
            return new HashMap<>();
        }
        
        Map<String, Object> map = new HashMap<>();
        map.put("id", element.getId());
        map.put("text", element.getText());
        map.put("x", element.getX());
        map.put("y", element.getY());
        map.put("width", element.getWidth());
        map.put("height", element.getHeight());
        map.put("clickable", element.isClickable());
        
        String contentDescription = element.getContentDescription();
        if (contentDescription != null && !contentDescription.isEmpty()) {
            map.put("contentDescription", contentDescription);
        }
        
        ElementType type = element.getUtilsElementType();
        if (type != null && type != ElementType.UNKNOWN) {
            map.put("type", type.name());
        }
        
        // Add bounds as a separate object
        Map<String, Integer> bounds = new HashMap<>();
        bounds.put("left", element.getX());
        bounds.put("top", element.getY());
        bounds.put("right", element.getX() + element.getWidth());
        bounds.put("bottom", element.getY() + element.getHeight());
        map.put("bounds", bounds);
        
        // Add additional properties
        addIfNotEmpty(map, "focused", element.isFocused());
        addIfNotEmpty(map, "enabled", element.isEnabled());
        addIfNotEmpty(map, "selected", element.isSelected());
        addIfNotEmpty(map, "visible", element.isVisible());
        
        // Add children
        UIElementInterface[] children = element.getChildren();
        if (children != null && children.length > 0) {
            Map<String, Object>[] childMaps = new Map[children.length];
            for (int i = 0; i < children.length; i++) {
                if (children[i] instanceof UIElement) {
                    childMaps[i] = toMap((UIElement) children[i]);
                } else {
                    // Create a simple representation for non-UIElement children
                    Map<String, Object> childMap = new HashMap<>();
                    childMap.put("id", children[i].getId());
                    childMap.put("text", children[i].getText());
                    childMaps[i] = childMap;
                }
            }
            map.put("children", childMaps);
        }
        
        return map;
    }
    
    /**
     * Convert a map representation to a UIElement
     *
     * @param map The map to convert
     * @return A UIElement created from the map
     */
    public static UIElement fromMap(Map<String, Object> map) {
        if (map == null || map.isEmpty()) {
            return null;
        }
        
        UIElement element = new UIElement();
        
        // Set basic properties
        if (map.containsKey("id")) {
            Object id = map.get("id");
            if (id instanceof Number) {
                element.setId(String.valueOf(((Number) id).intValue()));
            } else if (id instanceof String) {
                element.setId((String) id);
            } else if (id != null) {
                element.setId(id.toString());
            }
        }
        
        if (map.containsKey("text")) {
            Object text = map.get("text");
            if (text != null) {
                element.setText(text.toString());
            }
        }
        
        // Set position and size
        int x = getIntValue(map, "x", 0);
        int y = getIntValue(map, "y", 0);
        int width = getIntValue(map, "width", 0);
        int height = getIntValue(map, "height", 0);
        setBounds(element, x, y, width, height);
        
        // Set other properties
        element.setClickable(getBooleanValue(map, "clickable", false));
        
        if (map.containsKey("contentDescription")) {
            Object desc = map.get("contentDescription");
            if (desc != null) {
                element.setContentDescription(desc.toString());
            }
        }
        
        if (map.containsKey("type")) {
            Object type = map.get("type");
            if (type != null) {
                element.setType(type.toString());
            }
        }
        
        element.setFocused(getBooleanValue(map, "focused", false));
        element.setEnabled(getBooleanValue(map, "enabled", true));
        element.setSelected(getBooleanValue(map, "selected", false));
        element.setVisible(getBooleanValue(map, "visible", true));
        
        // Process children
        if (map.containsKey("children") && map.get("children") instanceof Object[]) {
            Object[] childMaps = (Object[]) map.get("children");
            UIElementInterface[] children = new UIElementInterface[childMaps.length];
            
            for (int i = 0; i < childMaps.length; i++) {
                if (childMaps[i] instanceof Map) {
                    children[i] = fromMap((Map<String, Object>) childMaps[i]);
                }
            }
            
            element.setChildren(children);
        }
        
        return element;
    }
    
    /**
     * Add a value to a map if it's not empty or the default value
     *
     * @param map The map to add to
     * @param key The key to use
     * @param value The value to add
     */
    private static void addIfNotEmpty(Map<String, Object> map, String key, Object value) {
        if (value != null) {
            if (value instanceof Boolean) {
                // Only add boolean values if they're not the default
                // Default for focused, selected is false; default for enabled, visible is true
                Boolean boolValue = (Boolean) value;
                if (("focused".equals(key) || "selected".equals(key)) && boolValue) {
                    map.put(key, value);
                } else if (("enabled".equals(key) || "visible".equals(key)) && !boolValue) {
                    map.put(key, value);
                }
            } else if (value instanceof String) {
                if (((String) value).length() > 0) {
                    map.put(key, value);
                }
            } else if (value instanceof Number) {
                if (((Number) value).doubleValue() != 0) {
                    map.put(key, value);
                }
            } else {
                map.put(key, value);
            }
        }
    }
    
    /**
     * Get an integer value from a map
     *
     * @param map The map to get from
     * @param key The key to use
     * @param defaultValue The default value if the key is not found
     * @return The integer value
     */
    private static int getIntValue(Map<String, Object> map, String key, int defaultValue) {
        if (map.containsKey(key)) {
            Object value = map.get(key);
            if (value instanceof Number) {
                return ((Number) value).intValue();
            } else if (value instanceof String) {
                try {
                    return Integer.parseInt((String) value);
                } catch (NumberFormatException e) {
                    // Ignore and return default
                }
            }
        }
        return defaultValue;
    }
    
    /**
     * Get a boolean value from a map
     *
     * @param map The map to get from
     * @param key The key to use
     * @param defaultValue The default value if the key is not found
     * @return The boolean value
     */
    private static boolean getBooleanValue(Map<String, Object> map, String key, boolean defaultValue) {
        if (map.containsKey(key)) {
            Object value = map.get(key);
            if (value instanceof Boolean) {
                return (Boolean) value;
            } else if (value instanceof String) {
                return Boolean.parseBoolean((String) value);
            } else if (value instanceof Number) {
                return ((Number) value).intValue() != 0;
            }
        }
        return defaultValue;
    }
    
    /**
     * Set bounds for a UI element using x, y, width, height parameters
     *
     * @param element The element to set bounds for
     * @param x The x position
     * @param y The y position
     * @param width The width
     * @param height The height
     */
    public static void setBounds(UIElementInterface element, int x, int y, int width, int height) {
        if (element == null) return;
        
        // If it's a UIElement, we can use the specialized method
        if (element instanceof UIElement) {
            Rect rect = new Rect(x, y, x + width, y + height);
            ((UIElement)element).setBounds(rect);
        } else {
            // For other implementations, try to use the existing method with an array
            int[] bounds = new int[]{x, y, width, height};
            element.setBounds(bounds);
        }
    }
}