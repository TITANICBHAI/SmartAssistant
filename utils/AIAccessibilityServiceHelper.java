package utils;

import android.util.Log;
import java.lang.reflect.Method;

/**
 * Helper for AIAccessibilityService
 * This class provides methods for working with AIAccessibilityService
 */
public class AIAccessibilityServiceHelper {
    private static final String TAG = "AIAccessibilityServiceHelper";
    
    /**
     * Execute an action using the accessibility service
     * @param service The accessibility service
     * @param action The action
     * @return True if successful, false otherwise
     */
    public static boolean executeAction(Object service, Object action) {
        if (service == null || action == null) {
            return false;
        }
        
        try {
            // First try the executeAction method
            try {
                Method executeActionMethod = service.getClass().getMethod("executeAction", action.getClass());
                Object result = executeActionMethod.invoke(service, action);
                if (result instanceof Boolean) {
                    return (Boolean) result;
                }
                return true; // Assume success if not a boolean result
            } catch (Exception e) {
                // Method doesn't exist or error occurred
            }
            
            // Try the performAction method
            try {
                Method performActionMethod = service.getClass().getMethod("performAction", action.getClass());
                Object result = performActionMethod.invoke(service, action);
                if (result instanceof Boolean) {
                    return (Boolean) result;
                }
                return true; // Assume success if not a boolean result
            } catch (Exception e) {
                // Method doesn't exist or error occurred
            }
            
            // Try the runAction method
            try {
                Method runActionMethod = service.getClass().getMethod("runAction", action.getClass());
                Object result = runActionMethod.invoke(service, action);
                if (result instanceof Boolean) {
                    return (Boolean) result;
                }
                return true; // Assume success if not a boolean result
            } catch (Exception e) {
                // Method doesn't exist or error occurred
            }
            
            // Try the handleAction method
            try {
                Method handleActionMethod = service.getClass().getMethod("handleAction", action.getClass());
                Object result = handleActionMethod.invoke(service, action);
                if (result instanceof Boolean) {
                    return (Boolean) result;
                }
                return true; // Assume success if not a boolean result
            } catch (Exception e) {
                // Method doesn't exist or error occurred
            }
            
            // Try the processAction method
            try {
                Method processActionMethod = service.getClass().getMethod("processAction", action.getClass());
                Object result = processActionMethod.invoke(service, action);
                if (result instanceof Boolean) {
                    return (Boolean) result;
                }
                return true; // Assume success if not a boolean result
            } catch (Exception e) {
                // Method doesn't exist or error occurred
            }
            
            // If we've reached here, none of the methods worked
            Log.e(TAG, "Failed to execute action");
            return false;
            
        } catch (Exception e) {
            Log.e(TAG, "Error executing action: " + e.getMessage());
            return false;
        }
    }
    
    /**
     * Get the accessibility service instance
     * @return The accessibility service instance
     */
    public static Object getAccessibilityService() {
        try {
            Class<?> serviceClass = Class.forName("com.aiassistant.services.AIAccessibilityService");
            
            // Try to use the getInstance method
            try {
                Method getInstanceMethod = serviceClass.getMethod("getInstance");
                return getInstanceMethod.invoke(null);
            } catch (Exception e) {
                // Method doesn't exist or error occurred
            }
            
            // Try to get the instance field
            try {
                java.lang.reflect.Field instanceField = serviceClass.getDeclaredField("instance");
                instanceField.setAccessible(true);
                return instanceField.get(null);
            } catch (Exception e) {
                // Field doesn't exist or error occurred
            }
            
            // Try to use the getService method
            try {
                Method getServiceMethod = serviceClass.getMethod("getService");
                return getServiceMethod.invoke(null);
            } catch (Exception e) {
                // Method doesn't exist or error occurred
            }
            
            Log.e(TAG, "Failed to get accessibility service instance");
            return null;
            
        } catch (Exception e) {
            Log.e(TAG, "Error getting accessibility service instance: " + e.getMessage());
            return null;
        }
    }
    
    /**
     * Perform an action by action type
     * @param service The accessibility service
     * @param actionType The action type
     * @param params The parameters
     * @return True if successful, false otherwise
     */
    public static boolean performActionByType(Object service, String actionType, Object... params) {
        if (service == null || actionType == null) {
            return false;
        }
        
        try {
            // Try to find a method matching the action type
            String methodName = "perform" + capitalize(actionType) + "Action";
            
            // Try with different parameter counts
            Method[] methods = service.getClass().getMethods();
            for (Method method : methods) {
                if (method.getName().equals(methodName)) {
                    try {
                        if (method.getParameterCount() == params.length) {
                            Object result = method.invoke(service, params);
                            if (result instanceof Boolean) {
                                return (Boolean) result;
                            }
                            return true; // Assume success if not a boolean result
                        }
                    } catch (Exception e) {
                        // Method invocation failed
                    }
                }
            }
            
            // Try generic action methods
            String[] methodNames = {
                "performAction",
                "executeAction",
                "runAction",
                "handleAction",
                "processAction"
            };
            
            for (String name : methodNames) {
                try {
                    Method method = service.getClass().getMethod(name, String.class);
                    Object result = method.invoke(service, actionType);
                    if (result instanceof Boolean) {
                        return (Boolean) result;
                    }
                    return true; // Assume success if not a boolean result
                } catch (Exception e) {
                    // Method doesn't exist or error occurred
                }
            }
            
            Log.e(TAG, "Failed to perform action by type: " + actionType);
            return false;
            
        } catch (Exception e) {
            Log.e(TAG, "Error performing action by type: " + e.getMessage());
            return false;
        }
    }
    
    /**
     * Capitalize a string
     * @param str The string
     * @return The capitalized string
     */
    private static String capitalize(String str) {
        if (str == null || str.isEmpty()) {
            return str;
        }
        return Character.toUpperCase(str.charAt(0)) + str.substring(1);
    }
}