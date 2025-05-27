package utils;

import android.util.Log;
import android.content.Context;
import java.lang.reflect.Method;

/**
 * Wrapper for NLPIntegrationHelper
 * This class handles parameter count mismatch in processCommand method
 */
public class NLPIntegrationHelperWrapper {
    private static final String TAG = "NLPIntegrationHelperWrapper";
    
    /**
     * Process a command
     * @param nlpHelper The NLP helper
     * @param command The command
     * @param context The context
     * @param controller The AI controller
     * @param additionalArgs Additional arguments
     * @return The result
     */
    public static Object processCommand(Object nlpHelper, String command, Context context, Object controller, Object... additionalArgs) {
        if (nlpHelper == null || command == null) {
            return null;
        }
        
        try {
            // Try to find the right method signature
            Class<?> nlpHelperClass = nlpHelper.getClass();
            
            // Try with just the command
            try {
                Method processCommandMethod = nlpHelperClass.getMethod("processCommand", String.class);
                return processCommandMethod.invoke(nlpHelper, command);
            } catch (Exception e) {
                // Method doesn't exist or error occurred
            }
            
            // Try with command and context
            try {
                Method processCommandMethod = nlpHelperClass.getMethod("processCommand", String.class, Context.class);
                return processCommandMethod.invoke(nlpHelper, command, context);
            } catch (Exception e) {
                // Method doesn't exist or error occurred
            }
            
            // Try with command, context, and controller
            try {
                Method processCommandMethod = nlpHelperClass.getMethod("processCommand", String.class, Context.class, controller.getClass());
                return processCommandMethod.invoke(nlpHelper, command, context, controller);
            } catch (Exception e) {
                // Method doesn't exist or error occurred
            }
            
            // Try with variable arguments - create an array with all objects
            Object[] allArgs = new Object[additionalArgs.length + 3];
            allArgs[0] = command;
            allArgs[1] = context;
            allArgs[2] = controller;
            System.arraycopy(additionalArgs, 0, allArgs, 3, additionalArgs.length);
            
            // Get all methods and find one that might work
            Method[] methods = nlpHelperClass.getMethods();
            for (Method method : methods) {
                if (method.getName().equals("processCommand")) {
                    try {
                        // Try to invoke with available arguments
                        return method.invoke(nlpHelper, getMatchingArgs(method, allArgs));
                    } catch (Exception e) {
                        // Method invocation failed, try next method
                    }
                }
            }
            
            Log.e(TAG, "Failed to find suitable processCommand method");
            return null;
            
        } catch (Exception e) {
            Log.e(TAG, "Error processing command: " + e.getMessage());
            return null;
        }
    }
    
    /**
     * Get matching arguments for a method
     * @param method The method
     * @param allArgs All available arguments
     * @return The matching arguments
     */
    private static Object[] getMatchingArgs(Method method, Object[] allArgs) {
        Class<?>[] paramTypes = method.getParameterTypes();
        
        // If exact match, use all args
        if (paramTypes.length == allArgs.length) {
            return allArgs;
        }
        
        // If fewer params than args, use first N args
        if (paramTypes.length < allArgs.length) {
            Object[] matchingArgs = new Object[paramTypes.length];
            System.arraycopy(allArgs, 0, matchingArgs, 0, paramTypes.length);
            return matchingArgs;
        }
        
        // If more params than args, return null for extras
        Object[] matchingArgs = new Object[paramTypes.length];
        System.arraycopy(allArgs, 0, matchingArgs, 0, allArgs.length);
        for (int i = allArgs.length; i < paramTypes.length; i++) {
            matchingArgs[i] = null;
        }
        return matchingArgs;
    }
    
    /**
     * Process a text input
     * @param nlpHelper The NLP helper
     * @param text The text
     * @return The result
     */
    public static Object processText(Object nlpHelper, String text) {
        if (nlpHelper == null || text == null) {
            return null;
        }
        
        try {
            Method processTextMethod = nlpHelper.getClass().getMethod("processText", String.class);
            return processTextMethod.invoke(nlpHelper, text);
        } catch (Exception e) {
            try {
                Method processCommandMethod = nlpHelper.getClass().getMethod("processCommand", String.class);
                return processCommandMethod.invoke(nlpHelper, text);
            } catch (Exception ex) {
                Log.e(TAG, "Error processing text: " + ex.getMessage());
                return null;
            }
        }
    }
    
    /**
     * Get the NLP integration helper instance
     * @param context The context
     * @return The NLP integration helper
     */
    public static Object getInstance(Context context) {
        try {
            Class<?> nlpHelperClass = Class.forName("com.aiassistant.nlp.NLPIntegrationHelper");
            
            // Try to use the getInstance method
            try {
                Method getInstanceMethod = nlpHelperClass.getMethod("getInstance", Context.class);
                return getInstanceMethod.invoke(null, context);
            } catch (Exception e) {
                // Method doesn't exist or error occurred
            }
            
            // Try to create a new instance
            try {
                return nlpHelperClass.getDeclaredConstructor(Context.class).newInstance(context);
            } catch (Exception e) {
                // Constructor doesn't exist or error occurred
            }
            
            // Try no-arg constructor
            try {
                Object helper = nlpHelperClass.getDeclaredConstructor().newInstance();
                
                // Try to initialize with context
                try {
                    Method initMethod = nlpHelperClass.getMethod("init", Context.class);
                    initMethod.invoke(helper, context);
                } catch (Exception e) {
                    // Method doesn't exist or error occurred
                }
                
                try {
                    Method initializeMethod = nlpHelperClass.getMethod("initialize", Context.class);
                    initializeMethod.invoke(helper, context);
                } catch (Exception e) {
                    // Method doesn't exist or error occurred
                }
                
                return helper;
            } catch (Exception e) {
                // Constructor doesn't exist or error occurred
            }
            
            Log.e(TAG, "Failed to get NLPIntegrationHelper instance");
            return null;
            
        } catch (Exception e) {
            Log.e(TAG, "Error getting NLPIntegrationHelper instance: " + e.getMessage());
            return null;
        }
    }
}