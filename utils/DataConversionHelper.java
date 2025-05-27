package utils;

import java.util.Map;
import java.util.HashMap;
import java.util.List;
import java.util.ArrayList;
import java.util.Set;
import java.util.HashSet;

/**
 * Helper class providing data conversion methods between different formats.
 */
public class DataConversionHelper {
    // Cache of feature names for consistent mapping
    private static final List<String> FEATURE_NAMES = new ArrayList<>();
    private static final Set<String> FEATURE_SET = new HashSet<>();
    
    // Initialize with common feature names to ensure consistent ordering
    static {
        addFeature("posX");
        addFeature("posY");
        addFeature("velocity");
        addFeature("acceleration");
        addFeature("health");
        addFeature("energy");
        addFeature("score");
        addFeature("level");
        addFeature("enemies");
        addFeature("time");
        addFeature("buttons");
        addFeature("lives");
        addFeature("ammo");
        addFeature("powerups");
        addFeature("obstacles");
        addFeature("ui_elements_count");
        addFeature("buttons_count");
    }
    
    /**
     * Add a feature to the tracking list
     * 
     * @param featureName Name of feature to track
     */
    private static void addFeature(String featureName) {
        if (!FEATURE_SET.contains(featureName)) {
            FEATURE_NAMES.add(featureName);
            FEATURE_SET.add(featureName);
        }
    }
    
    /**
     * Convert a Map<String, Object> to a float array
     * This is useful for neural network models that expect float[] inputs
     * 
     * @param stateData Map containing state data
     * @return float array representation
     */
    public static float[] mapToFloatArray(Map<String, Object> stateData) {
        if (stateData == null) {
            return new float[0];
        }
        
        // Add any new keys to our feature tracking
        for (String key : stateData.keySet()) {
            if (!FEATURE_SET.contains(key)) {
                addFeature(key);
            }
        }
        
        // Create float array with consistent ordering based on feature names
        float[] result = new float[FEATURE_NAMES.size()];
        
        // Fill the array with values from the map
        for (int i = 0; i < FEATURE_NAMES.size(); i++) {
            String featureName = FEATURE_NAMES.get(i);
            Object value = stateData.get(featureName);
            result[i] = convertToFloat(value);
        }
        
        return result;
    }
    
    /**
     * Convert a float array back to a Map<String, Object>
     * This is useful when neural network models return float[] outputs
     * 
     * @param floatArray Float array data
     * @return Map representation
     */
    public static Map<String, Object> floatArrayToMap(float[] floatArray) {
        if (floatArray == null) {
            return new HashMap<>();
        }
        
        Map<String, Object> result = new HashMap<>();
        
        // Fill the map with values from the array
        for (int i = 0; i < Math.min(floatArray.length, FEATURE_NAMES.size()); i++) {
            result.put(FEATURE_NAMES.get(i), floatArray[i]);
        }
        
        // Add any extra values with generic names
        for (int i = FEATURE_NAMES.size(); i < floatArray.length; i++) {
            result.put("feature_" + i, floatArray[i]);
        }
        
        return result;
    }
    
    /**
     * Convert a float array back to a Map<String, Object> with custom keys
     * This is useful when neural network models return float[] outputs
     * 
     * @param floatArray Float array data
     * @param keys Custom keys to use for the map
     * @return Map representation
     */
    public static Map<String, Object> floatArrayToMap(float[] floatArray, String[] keys) {
        if (floatArray == null) {
            return new HashMap<>();
        }
        
        Map<String, Object> result = new HashMap<>();
        
        // Fill the map with values from the array using custom keys
        for (int i = 0; i < Math.min(floatArray.length, keys.length); i++) {
            result.put(keys[i], floatArray[i]);
        }
        
        // Add any extra values with generic names
        for (int i = keys.length; i < floatArray.length; i++) {
            result.put("feature_" + i, floatArray[i]);
        }
        
        return result;
    }
    
    /**
     * Convert an int array to a float array
     * 
     * @param intArray Int array to convert
     * @return Equivalent float array
     */
    public static float[] intArrayToFloatArray(int[] intArray) {
        if (intArray == null) {
            return new float[0];
        }
        
        float[] floatArray = new float[intArray.length];
        for (int i = 0; i < intArray.length; i++) {
            floatArray[i] = (float) intArray[i];
        }
        
        return floatArray;
    }
    
    /**
     * Convert an object to a float value
     * 
     * @param obj Object to convert
     * @return Float value
     */
    public static float convertToFloat(Object obj) {
        if (obj == null) {
            return 0.0f;
        }
        
        if (obj instanceof Number) {
            return ((Number) obj).floatValue();
        }
        
        if (obj instanceof Boolean) {
            return ((Boolean) obj) ? 1.0f : 0.0f;
        }
        
        if (obj instanceof String) {
            try {
                return Float.parseFloat((String) obj);
            } catch (NumberFormatException e) {
                // If the string can't be parsed, use its hash code
                return (float) (((String) obj).hashCode() % 100) / 100.0f;
            }
        }
        
        // For other types, use hash code
        return (float) (obj.hashCode() % 100) / 100.0f;
    }
    
    /**
     * Convert a List to a Map
     * 
     * @param list List to convert
     * @return Map with indices as keys
     */
    public static Map<String, Object> listToMap(List<?> list) {
        if (list == null) {
            return new HashMap<>();
        }
        
        Map<String, Object> result = new HashMap<>();
        for (int i = 0; i < list.size(); i++) {
            result.put("item_" + i, list.get(i));
        }
        
        result.put("size", list.size());
        return result;
    }
    
    /**
     * Merge two maps together
     * 
     * @param map1 First map
     * @param map2 Second map (values override first map if keys conflict)
     * @return Merged map
     */
    public static Map<String, Object> mergeMaps(Map<String, Object> map1, Map<String, Object> map2) {
        if (map1 == null && map2 == null) {
            return new HashMap<>();
        }
        
        if (map1 == null) {
            return new HashMap<>(map2);
        }
        
        if (map2 == null) {
            return new HashMap<>(map1);
        }
        
        Map<String, Object> result = new HashMap<>(map1);
        result.putAll(map2);
        return result;
    }
    
    /**
     * Get a nested value from a map using a dot-separated path
     * 
     * @param map Map to get value from
     * @param path Dot-separated path (e.g., "player.stats.health")
     * @param defaultValue Default value if path not found
     * @return Value at path or default if not found
     */
    @SuppressWarnings("unchecked")
    public static <T> T getNestedValue(Map<String, Object> map, String path, T defaultValue) {
        if (map == null || path == null || path.isEmpty()) {
            return defaultValue;
        }
        
        String[] parts = path.split("\\.");
        Object current = map;
        
        for (String part : parts) {
            if (current instanceof Map) {
                current = ((Map<?, ?>) current).get(part);
                if (current == null) {
                    return defaultValue;
                }
            } else {
                return defaultValue;
            }
        }
        
        try {
            return (T) current;
        } catch (ClassCastException e) {
            return defaultValue;
        }
    }
    
    /**
     * Set a nested value in a map using a dot-separated path
     * 
     * @param map Map to set value in
     * @param path Dot-separated path (e.g., "player.stats.health")
     * @param value Value to set
     */
    @SuppressWarnings("unchecked")
    public static void setNestedValue(Map<String, Object> map, String path, Object value) {
        if (map == null || path == null || path.isEmpty()) {
            return;
        }
        
        String[] parts = path.split("\\.");
        Map<String, Object> current = map;
        
        for (int i = 0; i < parts.length - 1; i++) {
            String part = parts[i];
            Object next = current.get(part);
            
            if (next instanceof Map) {
                current = (Map<String, Object>) next;
            } else {
                Map<String, Object> newMap = new HashMap<>();
                current.put(part, newMap);
                current = newMap;
            }
        }
        
        current.put(parts[parts.length - 1], value);
    }
}