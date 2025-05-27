package utils;

import android.util.Log;
import java.lang.reflect.Method;

/**
 * Helper for StandardizedUIElementType
 * This class provides methods for converting between different UI element type implementations
 */
public class StandardizedUIElementTypeHelper {
    private static final String TAG = "StandardizedUIElementTypeHelper";
    
    /**
     * Convert a UI element type to a standardized type
     * @param elementType The UI element type
     * @return The standardized type
     */
    public static Object toStandardizedType(Object elementType) {
        if (elementType == null) {
            return null;
        }
        
        try {
            // Check if this is already a StandardizedUIElementType
            Class<?> standardizedClass = Class.forName("com.aiassistant.detection.StandardizedUIElementType");
            if (standardizedClass.isInstance(elementType)) {
                return elementType;
            }
            
            // Try to use the fromUIElementType method if this is a UIElementType
            if (elementType.getClass().getName().contains("UIElementType")) {
                try {
                    Method fromUIElementTypeMethod = standardizedClass.getMethod("fromUIElementType", elementType.getClass());
                    return fromUIElementTypeMethod.invoke(null, elementType);
                } catch (Exception e) {
                    // Method doesn't exist or error occurred
                }
            }
            
            // Try to use the fromElementType method if this is an ElementType
            if (elementType.getClass().getName().contains("ElementType")) {
                try {
                    Method fromElementTypeMethod = standardizedClass.getMethod("fromElementType", elementType.getClass());
                    return fromElementTypeMethod.invoke(null, elementType);
                } catch (Exception e) {
                    // Method doesn't exist or error occurred
                }
            }
            
            // Try to use the fromString method
            try {
                Method fromStringMethod = standardizedClass.getMethod("fromString", String.class);
                return fromStringMethod.invoke(null, elementType.toString());
            } catch (Exception e) {
                // Method doesn't exist or error occurred
            }
            
            // Try to use the valueOf method
            try {
                Method valueOfMethod = standardizedClass.getMethod("valueOf", String.class);
                return valueOfMethod.invoke(null, elementType.toString());
            } catch (Exception e) {
                // Method doesn't exist or error occurred
            }
            
            // Use reflection to find the UNKNOWN value
            for (Object constant : standardizedClass.getEnumConstants()) {
                if (constant.toString().equals("UNKNOWN")) {
                    return constant;
                }
            }
            
            // Return null if all else fails
            Log.e(TAG, "Failed to convert to standardized UI element type: " + elementType);
            return null;
            
        } catch (Exception e) {
            Log.e(TAG, "Error converting to standardized UI element type: " + e.getMessage());
            return null;
        }
    }
    
    /**
     * Convert a UI element type from a UIElementType
     * @param uiElementType The UIElementType
     * @return The standardized type
     */
    public static Object fromUIElementType(Object uiElementType) {
        return toStandardizedType(uiElementType);
    }
    
    /**
     * Convert a UI element type from an ElementType
     * @param elementType The ElementType
     * @return The standardized type
     */
    public static Object fromElementType(Object elementType) {
        return toStandardizedType(elementType);
    }
    
    /**
     * Get the unknown type
     * @return The unknown type
     */
    public static Object getUnknown() {
        try {
            Class<?> standardizedClass = Class.forName("com.aiassistant.detection.StandardizedUIElementType");
            
            // Use reflection to find the UNKNOWN value
            for (Object constant : standardizedClass.getEnumConstants()) {
                if (constant.toString().equals("UNKNOWN")) {
                    return constant;
                }
            }
            
            // Return null if not found
            return null;
            
        } catch (Exception e) {
            Log.e(TAG, "Error getting unknown UI element type: " + e.getMessage());
            return null;
        }
    }
    
    /**
     * Convert a string to a standardized type
     * @param typeString The type string
     * @return The standardized type
     */
    public static Object fromString(String typeString) {
        if (typeString == null || typeString.isEmpty()) {
            return getUnknown();
        }
        
        try {
            Class<?> standardizedClass = Class.forName("com.aiassistant.detection.StandardizedUIElementType");
            
            // Try to use the fromString method
            try {
                Method fromStringMethod = standardizedClass.getMethod("fromString", String.class);
                return fromStringMethod.invoke(null, typeString);
            } catch (Exception e) {
                // Method doesn't exist or error occurred
            }
            
            // Try to use the valueOf method
            try {
                Method valueOfMethod = standardizedClass.getMethod("valueOf", String.class);
                return valueOfMethod.invoke(null, typeString.toUpperCase());
            } catch (Exception e) {
                // Method doesn't exist or error occurred
            }
            
            // Try to match by name
            for (Object constant : standardizedClass.getEnumConstants()) {
                if (constant.toString().equalsIgnoreCase(typeString)) {
                    return constant;
                }
            }
            
            // Return unknown if not found
            return getUnknown();
            
        } catch (Exception e) {
            Log.e(TAG, "Error converting string to standardized UI element type: " + e.getMessage());
            return getUnknown();
        }
    }
    
    /**
     * Convert a standardized type to a UIElementType
     * @param standardizedType The standardized type
     * @return The UIElementType
     */
    public static Object toUIElementType(Object standardizedType) {
        if (standardizedType == null) {
            return null;
        }
        
        try {
            // Try to use the toUIElementType method
            try {
                Method toUIElementTypeMethod = standardizedType.getClass().getMethod("toUIElementType");
                return toUIElementTypeMethod.invoke(standardizedType);
            } catch (Exception e) {
                // Method doesn't exist or error occurred
            }
            
            // Try to get the UIElementType by name
            try {
                Class<?> uiElementTypeClass = Class.forName("com.aiassistant.models.UIElementType");
                String typeName = standardizedType.toString();
                
                // Try valueOf
                try {
                    Method valueOfMethod = uiElementTypeClass.getMethod("valueOf", String.class);
                    return valueOfMethod.invoke(null, typeName);
                } catch (Exception e) {
                    // Method doesn't exist or enum constant doesn't exist
                }
                
                // Try fromString
                try {
                    Method fromStringMethod = uiElementTypeClass.getMethod("fromString", String.class);
                    return fromStringMethod.invoke(null, typeName);
                } catch (Exception e) {
                    // Method doesn't exist or error occurred
                }
                
                // Try to match by name
                for (Object constant : uiElementTypeClass.getEnumConstants()) {
                    if (constant.toString().equalsIgnoreCase(typeName)) {
                        return constant;
                    }
                }
                
                // Get the unknown value
                try {
                    java.lang.reflect.Field unknownField = uiElementTypeClass.getField("UNKNOWN");
                    return unknownField.get(null);
                } catch (Exception e) {
                    // Field doesn't exist
                }
                
                // Return the first value as a fallback
                Object[] constants = uiElementTypeClass.getEnumConstants();
                if (constants != null && constants.length > 0) {
                    return constants[0];
                }
            } catch (Exception e) {
                // Class not found or error occurred
            }
            
            return null;
            
        } catch (Exception e) {
            Log.e(TAG, "Error converting to UIElementType: " + e.getMessage());
            return null;
        }
    }
    
    /**
     * Convert a standardized type to an ElementType
     * @param standardizedType The standardized type
     * @return The ElementType
     */
    public static Object toElementType(Object standardizedType) {
        if (standardizedType == null) {
            return null;
        }
        
        try {
            // Try to use the toElementType method
            try {
                Method toElementTypeMethod = standardizedType.getClass().getMethod("toElementType");
                return toElementTypeMethod.invoke(standardizedType);
            } catch (Exception e) {
                // Method doesn't exist or error occurred
            }
            
            // Try to get the ElementType by name
            try {
                Class<?> elementTypeClass = Class.forName("com.aiassistant.detection.GameAppElementDetector$ElementType");
                String typeName = standardizedType.toString();
                
                // Try valueOf
                try {
                    Method valueOfMethod = elementTypeClass.getMethod("valueOf", String.class);
                    return valueOfMethod.invoke(null, typeName);
                } catch (Exception e) {
                    // Method doesn't exist or enum constant doesn't exist
                }
                
                // Try fromString
                try {
                    Method fromStringMethod = elementTypeClass.getMethod("fromString", String.class);
                    return fromStringMethod.invoke(null, typeName);
                } catch (Exception e) {
                    // Method doesn't exist or error occurred
                }
                
                // Try to match by name
                for (Object constant : elementTypeClass.getEnumConstants()) {
                    if (constant.toString().equalsIgnoreCase(typeName)) {
                        return constant;
                    }
                }
                
                // Return the first value as a fallback
                Object[] constants = elementTypeClass.getEnumConstants();
                if (constants != null && constants.length > 0) {
                    return constants[0];
                }
            } catch (Exception e) {
                // Class not found or error occurred
            }
            
            return null;
            
        } catch (Exception e) {
            Log.e(TAG, "Error converting to ElementType: " + e.getMessage());
            return null;
        }
    }
}