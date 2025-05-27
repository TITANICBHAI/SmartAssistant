package utils;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;
import android.util.Log;

/**
 * Helper for managing package compatibility issues between different environments
 * This helps bridge the gap between the Android app package structure and the local implementation
 */
public class PackageCompatHelper {
    
    private static final String TAG = "PackageCompatHelper";
    
    // Map of Android package names to local package names
    private static final Map<String, String> PACKAGE_MAPPING = new HashMap<>();
    
    static {
        // Initialize the package mapping
        PACKAGE_MAPPING.put("com.aiassistant.models", "models");
        PACKAGE_MAPPING.put("com.aiassistant.utils", "utils");
        PACKAGE_MAPPING.put("com.aiassistant.ml", "ml");
        PACKAGE_MAPPING.put("com.aiassistant.core", "core");
        PACKAGE_MAPPING.put("com.aiassistant.detection", "detection");
        PACKAGE_MAPPING.put("com.aiassistant.learning", "learning");
        PACKAGE_MAPPING.put("com.aiassistant.learning.video", "learning.video");
        PACKAGE_MAPPING.put("com.aiassistant.scheduler", "scheduler");
    }
    
    /**
     * Convert an Android package name to a local package name
     * 
     * @param androidPackage Android package name
     * @return Corresponding local package name
     */
    public static String toLocalPackage(String androidPackage) {
        if (androidPackage == null) {
            return null;
        }
        
        // Check if we have a direct mapping
        if (PACKAGE_MAPPING.containsKey(androidPackage)) {
            return PACKAGE_MAPPING.get(androidPackage);
        }
        
        // Try to find a parent package that maps
        for (Map.Entry<String, String> entry : PACKAGE_MAPPING.entrySet()) {
            String androidPrefix = entry.getKey();
            if (androidPackage.startsWith(androidPrefix + ".")) {
                String suffix = androidPackage.substring(androidPrefix.length());
                return entry.getValue() + suffix;
            }
        }
        
        // No mapping found, return the original
        return androidPackage;
    }
    
    /**
     * Convert a local package name to an Android package name
     * 
     * @param localPackage Local package name
     * @return Corresponding Android package name
     */
    public static String toAndroidPackage(String localPackage) {
        if (localPackage == null) {
            return null;
        }
        
        // Check if we have a reverse mapping
        for (Map.Entry<String, String> entry : PACKAGE_MAPPING.entrySet()) {
            if (entry.getValue().equals(localPackage)) {
                return entry.getKey();
            }
        }
        
        // Try to find a parent package that maps
        for (Map.Entry<String, String> entry : PACKAGE_MAPPING.entrySet()) {
            String localPrefix = entry.getValue();
            if (localPackage.startsWith(localPrefix + ".")) {
                String suffix = localPackage.substring(localPrefix.length());
                return entry.getKey() + suffix;
            }
        }
        
        // No mapping found, assume it's an Android package
        if (!localPackage.startsWith("com.")) {
            return "com.aiassistant." + localPackage;
        }
        
        return localPackage;
    }
    
    /**
     * Load a class by name, trying both Android and local package conventions
     * 
     * @param className Class name (may be in either Android or local format)
     * @return Loaded class or null if not found
     */
    public static Class<?> loadClass(String className) {
        if (className == null) {
            return null;
        }
        
        try {
            // Try loading directly first
            return Class.forName(className);
        } catch (ClassNotFoundException e) {
            // Try with Android package
            int lastDot = className.lastIndexOf('.');
            if (lastDot > 0) {
                String packageName = className.substring(0, lastDot);
                String simpleClassName = className.substring(lastDot + 1);
                
                String androidPackage = toAndroidPackage(packageName);
                try {
                    return Class.forName(androidPackage + "." + simpleClassName);
                } catch (ClassNotFoundException e2) {
                    // Try with local package
                    String localPackage = toLocalPackage(packageName);
                    try {
                        return Class.forName(localPackage + "." + simpleClassName);
                    } catch (ClassNotFoundException e3) {
                        LogHelper.e(TAG, "Failed to load class: " + className, e3);
                        return null;
                    }
                }
            }
        }
        
        return null;
    }
    
    /**
     * Find a method in a class or its helper class
     * 
     * @param clazz The class to search in
     * @param methodName The method name
     * @param parameterTypes The method parameter types
     * @return The method object or null if not found
     */
    public static Method findMethod(Class<?> clazz, String methodName, Class<?>... parameterTypes) {
        if (clazz == null || methodName == null) {
            return null;
        }
        
        try {
            // Try to find the method directly
            return clazz.getMethod(methodName, parameterTypes);
        } catch (NoSuchMethodException e) {
            // Try to find a helper class
            String helperClassName = clazz.getName() + "Helper";
            try {
                Class<?> helperClass = Class.forName(helperClassName);
                try {
                    // Check for a static method in the helper
                    Method helperMethod = helperClass.getMethod(methodName, parameterTypes);
                    if (java.lang.reflect.Modifier.isStatic(helperMethod.getModifiers())) {
                        return helperMethod;
                    }
                } catch (NoSuchMethodException e2) {
                    // No such method in helper
                }
            } catch (ClassNotFoundException e2) {
                // No helper class
            }
            
            // Try with a parameter type that includes the original class
            if (parameterTypes.length > 0) {
                Class<?>[] newTypes = new Class<?>[parameterTypes.length + 1];
                newTypes[0] = clazz;
                System.arraycopy(parameterTypes, 0, newTypes, 1, parameterTypes.length);
                
                try {
                    Class<?> helperClass = Class.forName(clazz.getName() + "Helper");
                    return helperClass.getMethod(methodName, newTypes);
                } catch (Exception e2) {
                    // Failed to find helper method
                }
            }
        }
        
        return null;
    }
    
    /**
     * Create a local implementation of an Android class
     * 
     * @param androidClassName Android class name
     * @return Instance of a compatible local implementation or null if not available
     */
    public static Object createLocalImplementation(String androidClassName) {
        if (androidClassName == null) {
            return null;
        }
        
        try {
            // Try to convert the package name
            int lastDot = androidClassName.lastIndexOf('.');
            if (lastDot > 0) {
                String packageName = androidClassName.substring(0, lastDot);
                String simpleClassName = androidClassName.substring(lastDot + 1);
                
                String localPackage = toLocalPackage(packageName);
                String localImplName = localPackage + "." + simpleClassName + "Impl";
                
                try {
                    Class<?> implClass = Class.forName(localImplName);
                    return implClass.newInstance();
                } catch (ClassNotFoundException e) {
                    // No implementation class found
                    LogHelper.e(TAG, "No implementation found for: " + androidClassName, e);
                }
            }
        } catch (Exception e) {
            LogHelper.e(TAG, "Failed to create implementation for: " + androidClassName, e);
        }
        
        return null;
    }
}