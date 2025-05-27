package utils;

import android.util.Log;
import java.lang.reflect.Method;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;

/**
 * Utility class for reflection operations
 * This class provides utilities for reflection operations
 */
public class ReflectionUtils {
    private static final String TAG = "ReflectionUtils";
    
    /**
     * Get a method from a class
     * @param clazz The class
     * @param methodName The method name
     * @param parameterTypes The parameter types
     * @return The method, or null if not found
     */
    public static Method getMethod(Class<?> clazz, String methodName, Class<?>... parameterTypes) {
        if (clazz == null) {
            return null;
        }
        
        try {
            return clazz.getMethod(methodName, parameterTypes);
        } catch (Exception e) {
            Log.e(TAG, "Error getting method " + methodName + ": " + e.getMessage());
            return null;
        }
    }
    
    /**
     * Invoke a method on an object
     * @param obj The object
     * @param methodName The method name
     * @param args The arguments
     * @return The result, or null if the method could not be invoked
     */
    public static Object invokeMethod(Object obj, String methodName, Object... args) {
        if (obj == null) {
            return null;
        }
        
        try {
            // Prepare parameter types
            Class<?>[] parameterTypes = new Class<?>[args.length];
            for (int i = 0; i < args.length; i++) {
                parameterTypes[i] = args[i] != null ? args[i].getClass() : null;
            }
            
            // Find the method
            Method method = null;
            try {
                method = obj.getClass().getMethod(methodName, parameterTypes);
            } catch (Exception e) {
                // Try to find a compatible method
                for (Method m : obj.getClass().getMethods()) {
                    if (m.getName().equals(methodName) && m.getParameterCount() == args.length) {
                        boolean matches = true;
                        Class<?>[] methodParameterTypes = m.getParameterTypes();
                        
                        for (int i = 0; i < args.length; i++) {
                            if (args[i] != null && !methodParameterTypes[i].isInstance(args[i])) {
                                matches = false;
                                break;
                            }
                        }
                        
                        if (matches) {
                            method = m;
                            break;
                        }
                    }
                }
            }
            
            if (method == null) {
                Log.e(TAG, "Method " + methodName + " not found");
                return null;
            }
            
            // Invoke the method
            return method.invoke(obj, args);
            
        } catch (Exception e) {
            Log.e(TAG, "Error invoking method " + methodName + ": " + e.getMessage());
            return null;
        }
    }
    
    /**
     * Get a field from a class
     * @param clazz The class
     * @param fieldName The field name
     * @return The field, or null if not found
     */
    public static Field getField(Class<?> clazz, String fieldName) {
        if (clazz == null) {
            return null;
        }
        
        try {
            return clazz.getField(fieldName);
        } catch (Exception e) {
            try {
                // Try to get the field as a declared field
                Field field = clazz.getDeclaredField(fieldName);
                field.setAccessible(true);
                return field;
            } catch (Exception ex) {
                Log.e(TAG, "Error getting field " + fieldName + ": " + ex.getMessage());
                return null;
            }
        }
    }
    
    /**
     * Get the value of a field on an object
     * @param obj The object
     * @param fieldName The field name
     * @return The value, or null if the field could not be accessed
     */
    public static Object getFieldValue(Object obj, String fieldName) {
        if (obj == null) {
            return null;
        }
        
        try {
            Field field = getField(obj.getClass(), fieldName);
            if (field == null) {
                return null;
            }
            
            return field.get(obj);
            
        } catch (Exception e) {
            Log.e(TAG, "Error getting field value " + fieldName + ": " + e.getMessage());
            return null;
        }
    }
    
    /**
     * Set the value of a field on an object
     * @param obj The object
     * @param fieldName The field name
     * @param value The value
     * @return true if successful, false otherwise
     */
    public static boolean setFieldValue(Object obj, String fieldName, Object value) {
        if (obj == null) {
            return false;
        }
        
        try {
            Field field = getField(obj.getClass(), fieldName);
            if (field == null) {
                return false;
            }
            
            field.set(obj, value);
            return true;
            
        } catch (Exception e) {
            Log.e(TAG, "Error setting field value " + fieldName + ": " + e.getMessage());
            return false;
        }
    }
    
    /**
     * Find classes that implement or extend a specific class or interface
     * @param basePackage The base package to search in
     * @param baseClass The base class or interface
     * @return A list of matching classes
     */
    public static List<Class<?>> findImplementations(String basePackage, Class<?> baseClass) {
        List<Class<?>> implementations = new ArrayList<>();
        
        try {
            // This is just a stub implementation that needs a proper ClassPath scanner
            // which is not available in Android without additional libraries
            
            // For now, just try some common implementation names
            String baseName = baseClass.getSimpleName();
            String[] commonSuffixes = {"Impl", "Implementation", "Default", "Standard", "Basic", "Simple", "Base"};
            
            for (String suffix : commonSuffixes) {
                try {
                    Class<?> impl = Class.forName(basePackage + "." + baseName + suffix);
                    if (baseClass.isAssignableFrom(impl)) {
                        implementations.add(impl);
                    }
                } catch (Exception e) {
                    // Class not found, ignore
                }
            }
            
        } catch (Exception e) {
            Log.e(TAG, "Error finding implementations: " + e.getMessage());
        }
        
        return implementations;
    }
    
    /**
     * Create an instance of a class
     * @param className The class name
     * @param constructorArgs The constructor arguments
     * @return The instance, or null if it could not be created
     */
    public static Object createInstance(String className, Object... constructorArgs) {
        try {
            Class<?> clazz = Class.forName(className);
            return createInstance(clazz, constructorArgs);
        } catch (Exception e) {
            Log.e(TAG, "Error creating instance of " + className + ": " + e.getMessage());
            return null;
        }
    }
    
    /**
     * Create an instance of a class
     * @param clazz The class
     * @param constructorArgs The constructor arguments
     * @return The instance, or null if it could not be created
     */
    public static Object createInstance(Class<?> clazz, Object... constructorArgs) {
        if (clazz == null) {
            return null;
        }
        
        try {
            // Prepare parameter types
            Class<?>[] parameterTypes = new Class<?>[constructorArgs.length];
            for (int i = 0; i < constructorArgs.length; i++) {
                parameterTypes[i] = constructorArgs[i] != null ? constructorArgs[i].getClass() : null;
            }
            
            // Find the constructor
            try {
                return clazz.getConstructor(parameterTypes).newInstance(constructorArgs);
            } catch (Exception e) {
                // Try to find a compatible constructor
                for (java.lang.reflect.Constructor<?> constructor : clazz.getConstructors()) {
                    if (constructor.getParameterCount() == constructorArgs.length) {
                        try {
                            return constructor.newInstance(constructorArgs);
                        } catch (Exception ex) {
                            // Try next constructor
                        }
                    }
                }
                
                // If no constructor with matching parameter count, try a no-arg constructor
                if (constructorArgs.length == 0) {
                    Log.e(TAG, "No suitable constructor found for " + clazz.getName());
                    return null;
                } else {
                    return clazz.newInstance();
                }
            }
            
        } catch (Exception e) {
            Log.e(TAG, "Error creating instance of " + clazz.getName() + ": " + e.getMessage());
            return null;
        }
    }
    
    /**
     * Import a class from a different package
     * @param className The class name
     * @param importPackages The packages to import from
     * @return The class, or null if not found
     */
    public static Class<?> importClass(String className, String... importPackages) {
        for (String packageName : importPackages) {
            try {
                return Class.forName(packageName + "." + className);
            } catch (Exception e) {
                // Class not found in this package, try the next one
            }
        }
        
        try {
            // Try to load the class directly
            return Class.forName(className);
        } catch (Exception e) {
            Log.e(TAG, "Error importing class " + className + ": " + e.getMessage());
            return null;
        }
    }
}