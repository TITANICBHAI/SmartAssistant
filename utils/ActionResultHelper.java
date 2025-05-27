package utils;

import java.util.HashMap;
import java.util.Map;

/**
 * Helper class for ActionResult compatibility.
 * This class provides methods to create ActionResult objects with appropriate success and error methods
 * to maintain compatibility between different versions of the codebase.
 */
public class ActionResultHelper {
    
    /**
     * Create a success result with only a message.
     * This method handles compatibility with ActionResult classes that require multiple parameters.
     * 
     * @param message The success message
     * @return An ActionResult object representing success
     */
    public static Object success(String message) {
        return success(message, new HashMap<>(), System.currentTimeMillis());
    }
    
    /**
     * Create a success result with message and data.
     * 
     * @param message The success message
     * @param data Additional data for the result
     * @return An ActionResult object representing success
     */
    public static Object success(String message, Map<String, Object> data) {
        return success(message, data, System.currentTimeMillis());
    }
    
    /**
     * Create a full success result.
     * This matches the signature of existing ActionResult.success method.
     * 
     * @param message The success message
     * @param data Additional data for the result
     * @param timestamp The timestamp for when this result was created
     * @return An ActionResult object representing success
     */
    public static Object success(String message, Map<String, Object> data, long timestamp) {
        try {
            // Try to call the original ActionResult.success method if it exists
            Class<?> actionResultClass = Class.forName("com.aiassistant.models.ActionResult");
            return actionResultClass.getMethod("success", String.class, Map.class, long.class)
                .invoke(null, message, data, timestamp);
        } catch (Exception e) {
            // Fallback implementation
            return new HashMap<String, Object>() {{
                put("success", true);
                put("message", message);
                put("data", data);
                put("timestamp", timestamp);
            }};
        }
    }
    
    /**
     * Create an error result with only a message.
     * 
     * @param message The error message
     * @return An ActionResult object representing an error
     */
    public static Object error(String message) {
        return error(message, new HashMap<>(), System.currentTimeMillis());
    }
    
    /**
     * Create an error result with message and data.
     * 
     * @param message The error message
     * @param data Additional data for the result
     * @return An ActionResult object representing an error
     */
    public static Object error(String message, Map<String, Object> data) {
        return error(message, data, System.currentTimeMillis());
    }
    
    /**
     * Create a full error result.
     * 
     * @param message The error message
     * @param data Additional data for the result
     * @param timestamp The timestamp for when this result was created
     * @return An ActionResult object representing an error
     */
    public static Object error(String message, Map<String, Object> data, long timestamp) {
        try {
            // Try to call the original ActionResult.error method if it exists
            Class<?> actionResultClass = Class.forName("com.aiassistant.models.ActionResult");
            return actionResultClass.getMethod("error", String.class, Map.class, long.class)
                .invoke(null, message, data, timestamp);
        } catch (Exception e) {
            // Fallback implementation
            return new HashMap<String, Object>() {{
                put("success", false);
                put("message", message);
                put("data", data);
                put("timestamp", timestamp);
            }};
        }
    }
    
    /**
     * Determine if the given ActionResult represents a success.
     * 
     * @param result The ActionResult object to check
     * @return true if the result represents a success, false otherwise
     */
    public static boolean isSuccess(Object result) {
        if (result instanceof Map) {
            return Boolean.TRUE.equals(((Map<?, ?>)result).get("success"));
        }
        
        try {
            // Try to call the isSuccess method if it exists
            return (Boolean)result.getClass().getMethod("isSuccess").invoke(result);
        } catch (Exception e) {
            return false;
        }
    }
    
    /**
     * Get the message from an ActionResult object.
     * 
     * @param result The ActionResult object
     * @return The message string or null if not available
     */
    public static String getMessage(Object result) {
        if (result instanceof Map) {
            Object message = ((Map<?, ?>)result).get("message");
            return message != null ? message.toString() : null;
        }
        
        try {
            // Try to call the getMessage method if it exists
            return (String)result.getClass().getMethod("getMessage").invoke(result);
        } catch (Exception e) {
            return null;
        }
    }
    
    /**
     * Get the data map from an ActionResult object.
     * 
     * @param result The ActionResult object
     * @return The data map or an empty map if not available
     */
    @SuppressWarnings("unchecked")
    public static Map<String, Object> getData(Object result) {
        if (result instanceof Map) {
            Object data = ((Map<?, ?>)result).get("data");
            if (data instanceof Map) {
                return (Map<String, Object>)data;
            }
            return new HashMap<>();
        }
        
        try {
            // Try to call the getData method if it exists
            return (Map<String, Object>)result.getClass().getMethod("getData").invoke(result);
        } catch (Exception e) {
            return new HashMap<>();
        }
    }
}