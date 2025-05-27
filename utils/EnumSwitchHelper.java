package utils;

import android.util.Log;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

/**
 * Helper for handling enum switches
 * This class provides methods for handling enum switch statements
 */
public class EnumSwitchHelper {
    private static final String TAG = "EnumSwitchHelper";
    
    /**
     * Get the corresponding enum value from a partial or fully qualified name
     * @param enumClass The enum class
     * @param name The name (possibly fully qualified)
     * @return The enum value, or null if not found
     */
    public static Object getEnumValue(Class<?> enumClass, String name) {
        if (enumClass == null || name == null || name.isEmpty()) {
            return null;
        }
        
        try {
            // Check if the name is fully qualified
            int lastDotIndex = name.lastIndexOf('.');
            String unqualifiedName = lastDotIndex >= 0 ? name.substring(lastDotIndex + 1) : name;
            
            // Try to use valueOf
            try {
                Method valueOfMethod = enumClass.getMethod("valueOf", String.class);
                return valueOfMethod.invoke(null, unqualifiedName);
            } catch (Exception e) {
                // Enum constant doesn't exist with this exact name
            }
            
            // Try to match by toString
            for (Object enumValue : enumClass.getEnumConstants()) {
                if (enumValue.toString().equals(unqualifiedName)) {
                    return enumValue;
                }
            }
            
            Log.e(TAG, "Could not find enum value " + name + " in " + enumClass.getName());
            return null;
            
        } catch (Exception e) {
            Log.e(TAG, "Error getting enum value: " + e.getMessage());
            return null;
        }
    }
    
    /**
     * Check if an enum value matches a name
     * @param enumValue The enum value
     * @param name The name to check against
     * @return True if the enum value matches the name, false otherwise
     */
    public static boolean isEnumValue(Object enumValue, String name) {
        if (enumValue == null || name == null || name.isEmpty()) {
            return false;
        }
        
        try {
            // Check if the name is fully qualified
            int lastDotIndex = name.lastIndexOf('.');
            String unqualifiedName = lastDotIndex >= 0 ? name.substring(lastDotIndex + 1) : name;
            
            // Compare the unqualified name with the enum value's name
            return enumValue.toString().equals(unqualifiedName);
            
        } catch (Exception e) {
            Log.e(TAG, "Error checking enum value: " + e.getMessage());
            return false;
        }
    }
    
    /**
     * Process a switch statement for an enum value
     * @param enumValue The enum value
     * @param handlers Map of enum value names to handlers
     * @param defaultHandler The default handler
     * @return The result of the handler, or null if no handler was found
     */
    public static Object processSwitchStatement(Object enumValue, Map<String, SwitchCaseHandler> handlers, SwitchCaseHandler defaultHandler) {
        if (enumValue == null) {
            return defaultHandler != null ? defaultHandler.handle() : null;
        }
        
        try {
            String enumName = enumValue.toString();
            SwitchCaseHandler handler = handlers.get(enumName);
            
            if (handler != null) {
                return handler.handle();
            }
            
            // Try with class name prefix
            String fullName = enumValue.getClass().getSimpleName() + "." + enumName;
            handler = handlers.get(fullName);
            
            if (handler != null) {
                return handler.handle();
            }
            
            // Try with fully qualified name
            fullName = enumValue.getClass().getName() + "." + enumName;
            handler = handlers.get(fullName);
            
            if (handler != null) {
                return handler.handle();
            }
            
            return defaultHandler != null ? defaultHandler.handle() : null;
            
        } catch (Exception e) {
            Log.e(TAG, "Error processing switch statement: " + e.getMessage());
            return defaultHandler != null ? defaultHandler.handle() : null;
        }
    }
    
    /**
     * Interface for switch case handlers
     */
    public interface SwitchCaseHandler {
        /**
         * Handle the switch case
         * @return The result
         */
        Object handle();
    }
    
    /**
     * Get all enum values of a class
     * @param enumClass The enum class
     * @return The enum values
     */
    public static Object[] getEnumValues(Class<?> enumClass) {
        if (enumClass == null || !enumClass.isEnum()) {
            return new Object[0];
        }
        
        return enumClass.getEnumConstants();
    }
    
    /**
     * Get an enum value by ordinal
     * @param enumClass The enum class
     * @param ordinal The ordinal
     * @return The enum value
     */
    public static Object getEnumValueByOrdinal(Class<?> enumClass, int ordinal) {
        if (enumClass == null || !enumClass.isEnum()) {
            return null;
        }
        
        Object[] values = enumClass.getEnumConstants();
        if (ordinal >= 0 && ordinal < values.length) {
            return values[ordinal];
        }
        
        return null;
    }
    
    /**
     * Create a handler map for a switch statement
     * @return A new handler map
     */
    public static Map<String, SwitchCaseHandler> createHandlerMap() {
        return new HashMap<>();
    }
    
    /**
     * Add a handler to a handler map
     * @param handlers The handler map
     * @param enumValue The enum value
     * @param handler The handler
     */
    public static void addHandler(Map<String, SwitchCaseHandler> handlers, Object enumValue, SwitchCaseHandler handler) {
        if (handlers == null || enumValue == null || handler == null) {
            return;
        }
        
        handlers.put(enumValue.toString(), handler);
    }
    
    /**
     * Add a handler to a handler map by name
     * @param handlers The handler map
     * @param enumName The enum name
     * @param handler The handler
     */
    public static void addHandlerByName(Map<String, SwitchCaseHandler> handlers, String enumName, SwitchCaseHandler handler) {
        if (handlers == null || enumName == null || enumName.isEmpty() || handler == null) {
            return;
        }
        
        handlers.put(enumName, handler);
    }
}