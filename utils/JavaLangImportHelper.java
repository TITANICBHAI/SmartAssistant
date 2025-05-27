package utils;

import android.util.Log;
import java.lang.reflect.Method;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Proxy;

/**
 * Helper class for Java language imports and reflection utilities
 * Provides methods to handle reflection operations safely
 */
public class JavaLangImportHelper {
    private static final String TAG = "JavaLangImportHelper";
    
    /**
     * Create a placeholder Method object for cases where the actual method cannot be found
     * This prevents null pointer exceptions when dealing with reflection
     * 
     * @param methodName The name of the method to create a placeholder for
     * @return A placeholder Method object
     */
    public static Method createMethodPlaceholder(final String methodName) {
        try {
            // Create a dynamic proxy that implements Method interface
            InvocationHandler handler = (proxy, method, args) -> {
                if (method.getName().equals("getName")) {
                    return methodName;
                } else if (method.getName().equals("toString")) {
                    return "Method placeholder for " + methodName;
                } else if (method.getName().equals("invoke")) {
                    // Return default value based on return type
                    Class<?> returnType = method.getReturnType();
                    if (returnType.isPrimitive()) {
                        if (returnType == boolean.class) return false;
                        if (returnType == int.class) return 0;
                        if (returnType == long.class) return 0L;
                        if (returnType == float.class) return 0.0f;
                        if (returnType == double.class) return 0.0d;
                        if (returnType == byte.class) return (byte)0;
                        if (returnType == short.class) return (short)0;
                        if (returnType == char.class) return (char)0;
                        return null;
                    } else {
                        return null;
                    }
                }
                // Default behavior for other methods
                return null;
            };
            
            return (Method) Proxy.newProxyInstance(
                Method.class.getClassLoader(),
                new Class<?>[] { Method.class },
                handler
            );
        } catch (Exception e) {
            Log.e(TAG, "Error creating method placeholder: " + e.getMessage());
            return null;
        }
    }
    
    /**
     * Safely get the Method object for a class
     * 
     * @param clazz The Class to get the Method from
     * @param methodName The name of the method
     * @param parameterTypes The parameter types of the method
     * @return The Method object, or a placeholder if it couldn't be found
     */
    public static Method safeGetMethod(Class<?> clazz, String methodName, Class<?>... parameterTypes) {
        if (clazz == null) {
            return createMethodPlaceholder(methodName);
        }
        
        try {
            return clazz.getMethod(methodName, parameterTypes);
        } catch (NoSuchMethodException e) {
            Log.e(TAG, "Method " + methodName + " not found in class " + clazz.getName());
            return createMethodPlaceholder(methodName);
        } catch (SecurityException e) {
            Log.e(TAG, "Security exception getting method " + methodName + ": " + e.getMessage());
            return createMethodPlaceholder(methodName);
        }
    }
    
    /**
     * Safely invoke a method
     * 
     * @param method The Method to invoke
     * @param obj The object to invoke the method on (or null for static methods)
     * @param args The arguments to pass to the method
     * @return The result of the method invocation, or null if it failed
     */
    public static Object safeInvoke(Method method, Object obj, Object... args) {
        if (method == null) {
            return null;
        }
        
        try {
            return method.invoke(obj, args);
        } catch (Exception e) {
            Log.e(TAG, "Error invoking method " + method.getName() + ": " + e.getMessage());
            return null;
        }
    }
}