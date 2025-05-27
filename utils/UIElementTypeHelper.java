package utils;

import android.util.Log;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

/**
 * Helper for UI element types
 * This class provides methods for converting between different UI element type enums
 */
public class UIElementTypeHelper {
    private static final String TAG = "UIElementTypeHelper";
    
    // Cache for standard element type conversions
    private static final Map<String, Object> standardTypeCache = new HashMap<>();
    
    /**
     * Convert from UIElementType to StandardizedUIElementType
     * @param elementType The UIElementType
     * @return The StandardizedUIElementType
     */
    public static Object fromUIElementType(Object elementType) {
        if (elementType == null) {
            return null;
        }
        
        try {
            String typeName = elementType.toString();
            
            // Check cache first
            if (standardTypeCache.containsKey(typeName)) {
                return standardTypeCache.get(typeName);
            }
            
            // Try to get the StandardizedUIElementType class
            Class<?> standardizedTypeClass = Class.forName("com.aiassistant.detection.StandardizedUIElementType");
            
            // First try to use the fromUIElementType method directly
            try {
                Class<?> uiElementTypeClass = Class.forName("com.aiassistant.models.UIElementType");
                Method fromUIElementTypeMethod = standardizedTypeClass.getMethod("fromUIElementType", uiElementTypeClass);
                Object result = fromUIElementTypeMethod.invoke(null, elementType);
                standardTypeCache.put(typeName, result);
                return result;
            } catch (Exception e) {
                // Method doesn't exist with this parameter type
            }
            
            // Try to use valueOf
            try {
                Method valueOfMethod = standardizedTypeClass.getMethod("valueOf", String.class);
                Object result = valueOfMethod.invoke(null, typeName);
                standardTypeCache.put(typeName, result);
                return result;
            } catch (Exception e) {
                // Enum constant doesn't exist with this exact name
            }
            
            // Try to map common names
            Object result = mapToStandardizedType(standardizedTypeClass, typeName);
            if (result != null) {
                standardTypeCache.put(typeName, result);
                return result;
            }
            
            // Get the UNKNOWN constant as a fallback
            try {
                java.lang.reflect.Field unknownField = standardizedTypeClass.getField("UNKNOWN");
                Object unknownResult = unknownField.get(null);
                standardTypeCache.put(typeName, unknownResult);
                return unknownResult;
            } catch (Exception e) {
                // Field doesn't exist
            }
            
            Log.e(TAG, "Could not convert UIElementType to StandardizedUIElementType: " + typeName);
            return null;
            
        } catch (Exception e) {
            Log.e(TAG, "Error converting UIElementType: " + e.getMessage());
            return null;
        }
    }
    
    /**
     * Convert from ElementType to StandardizedUIElementType
     * @param elementType The ElementType
     * @return The StandardizedUIElementType
     */
    public static Object fromElementType(Object elementType) {
        if (elementType == null) {
            return null;
        }
        
        try {
            String typeName = elementType.toString();
            
            // Check cache first
            if (standardTypeCache.containsKey(typeName)) {
                return standardTypeCache.get(typeName);
            }
            
            // Try to get the StandardizedUIElementType class
            Class<?> standardizedTypeClass = Class.forName("com.aiassistant.detection.StandardizedUIElementType");
            
            // First try to use the fromElementType method directly
            try {
                Class<?> gameAppElementDetectorClass = Class.forName("com.aiassistant.detection.GameAppElementDetector");
                Class<?> elementTypeClass = Class.forName("com.aiassistant.detection.GameAppElementDetector$ElementType");
                Method fromElementTypeMethod = standardizedTypeClass.getMethod("fromElementType", elementTypeClass);
                Object result = fromElementTypeMethod.invoke(null, elementType);
                standardTypeCache.put(typeName, result);
                return result;
            } catch (Exception e) {
                // Method doesn't exist with this parameter type
            }
            
            // Try to use valueOf
            try {
                Method valueOfMethod = standardizedTypeClass.getMethod("valueOf", String.class);
                Object result = valueOfMethod.invoke(null, typeName);
                standardTypeCache.put(typeName, result);
                return result;
            } catch (Exception e) {
                // Enum constant doesn't exist with this exact name
            }
            
            // Try to map common names
            Object result = mapToStandardizedType(standardizedTypeClass, typeName);
            if (result != null) {
                standardTypeCache.put(typeName, result);
                return result;
            }
            
            // Get the UNKNOWN constant as a fallback
            try {
                java.lang.reflect.Field unknownField = standardizedTypeClass.getField("UNKNOWN");
                Object unknownResult = unknownField.get(null);
                standardTypeCache.put(typeName, unknownResult);
                return unknownResult;
            } catch (Exception e) {
                // Field doesn't exist
            }
            
            Log.e(TAG, "Could not convert ElementType to StandardizedUIElementType: " + typeName);
            return null;
            
        } catch (Exception e) {
            Log.e(TAG, "Error converting ElementType: " + e.getMessage());
            return null;
        }
    }
    
    /**
     * Map a type name to a StandardizedUIElementType
     * @param standardizedTypeClass The StandardizedUIElementType class
     * @param typeName The type name
     * @return The StandardizedUIElementType
     */
    private static Object mapToStandardizedType(Class<?> standardizedTypeClass, String typeName) {
        // Common mappings
        Map<String, String> mappings = new HashMap<>();
        mappings.put("BUTTON", "BUTTON");
        mappings.put("TEXT", "TEXT");
        mappings.put("TEXTVIEW", "TEXT");
        mappings.put("IMAGE", "IMAGE");
        mappings.put("IMAGEVIEW", "IMAGE");
        mappings.put("INPUT_FIELD", "INPUT");
        mappings.put("EDITTEXT", "INPUT");
        mappings.put("INPUT", "INPUT");
        mappings.put("CONTAINER", "CONTAINER");
        mappings.put("LAYOUT", "CONTAINER");
        mappings.put("SCROLL_VIEW", "SCROLLVIEW");
        mappings.put("SCROLLVIEW", "SCROLLVIEW");
        mappings.put("LIST_ITEM", "LIST_ITEM");
        mappings.put("LISTITEM", "LIST_ITEM");
        mappings.put("CHECKBOX", "CHECKBOX");
        mappings.put("RADIO_BUTTON", "RADIO");
        mappings.put("RADIOBUTTON", "RADIO");
        mappings.put("RADIO", "RADIO");
        mappings.put("TOGGLE", "TOGGLE");
        mappings.put("SWITCH", "TOGGLE");
        mappings.put("SLIDER", "SLIDER");
        mappings.put("SEEKBAR", "SLIDER");
        mappings.put("PROGRESS_BAR", "PROGRESS");
        mappings.put("PROGRESSBAR", "PROGRESS");
        mappings.put("PROGRESS", "PROGRESS");
        mappings.put("ICON", "ICON");
        mappings.put("UNKNOWN", "UNKNOWN");
        
        try {
            String standardName = mappings.get(typeName.toUpperCase());
            if (standardName != null) {
                Method valueOfMethod = standardizedTypeClass.getMethod("valueOf", String.class);
                return valueOfMethod.invoke(null, standardName);
            }
            
            return null;
            
        } catch (Exception e) {
            return null;
        }
    }
    
    /**
     * Get a UIElementType from StandardizedUIElementType
     * @param standardizedType The StandardizedUIElementType
     * @return The UIElementType
     */
    public static Object toUIElementType(Object standardizedType) {
        if (standardizedType == null) {
            return null;
        }
        
        try {
            String typeName = standardizedType.toString();
            
            // Try to get the UIElementType class
            Class<?> uiElementTypeClass = Class.forName("com.aiassistant.models.UIElementType");
            
            // Try to use valueOf
            try {
                Method valueOfMethod = uiElementTypeClass.getMethod("valueOf", String.class);
                return valueOfMethod.invoke(null, typeName);
            } catch (Exception e) {
                // Enum constant doesn't exist with this exact name
            }
            
            // Try to use fromString if available
            try {
                Method fromStringMethod = uiElementTypeClass.getMethod("fromString", String.class);
                return fromStringMethod.invoke(null, typeName.toLowerCase());
            } catch (Exception e) {
                // Method doesn't exist
            }
            
            // Try to map common names
            Map<String, String> mappings = new HashMap<>();
            mappings.put("BUTTON", "BUTTON");
            mappings.put("TEXT", "TEXT");
            mappings.put("IMAGE", "IMAGE");
            mappings.put("INPUT", "INPUT_FIELD");
            mappings.put("CONTAINER", "CONTAINER");
            mappings.put("SCROLLVIEW", "SCROLL_VIEW");
            mappings.put("LIST_ITEM", "LIST_ITEM");
            mappings.put("CHECKBOX", "CHECKBOX");
            mappings.put("RADIO", "RADIO_BUTTON");
            mappings.put("TOGGLE", "TOGGLE");
            mappings.put("SLIDER", "SLIDER");
            mappings.put("PROGRESS", "PROGRESS_BAR");
            mappings.put("ICON", "ICON");
            mappings.put("UNKNOWN", "UNKNOWN");
            
            String mappedName = mappings.get(typeName.toUpperCase());
            if (mappedName != null) {
                try {
                    Method valueOfMethod = uiElementTypeClass.getMethod("valueOf", String.class);
                    return valueOfMethod.invoke(null, mappedName);
                } catch (Exception e) {
                    // Enum constant doesn't exist
                }
            }
            
            // Get the UNKNOWN constant as a fallback
            try {
                java.lang.reflect.Field unknownField = uiElementTypeClass.getField("UNKNOWN");
                return unknownField.get(null);
            } catch (Exception e) {
                // Field doesn't exist
            }
            
            Log.e(TAG, "Could not convert StandardizedUIElementType to UIElementType: " + typeName);
            return null;
            
        } catch (Exception e) {
            Log.e(TAG, "Error converting StandardizedUIElementType: " + e.getMessage());
            return null;
        }
    }
    
    /**
     * Convert an element type to its string representation
     * @param elementType The element type
     * @return The string representation
     */
    public static String toString(Object elementType) {
        if (elementType == null) {
            return "UNKNOWN";
        }
        
        return elementType.toString();
    }
    
    /**
     * Check if an element type is of a specific type
     * @param elementType The element type
     * @param typeName The type name to check for
     * @return True if the element type matches, false otherwise
     */
    public static boolean isType(Object elementType, String typeName) {
        if (elementType == null || typeName == null) {
            return false;
        }
        
        return elementType.toString().equalsIgnoreCase(typeName);
    }
}