package utils;

import android.content.Context;

/**
 * Utility class for converting between different types of contexts.
 */
public class ContextConverter {
    
    /**
     * Convert a generic context to a DummyAndroidContext.
     * 
     * @param context The context to convert
     * @return A DummyAndroidContext
     */
    public static DummyAndroidContext toDummyContext(Context context) {
        if (context == null) {
            return new DummyAndroidContext();
        }
        
        if (context instanceof DummyAndroidContext) {
            return (DummyAndroidContext) context;
        }
        
        DummyAndroidContext dummyContext = new DummyAndroidContext(context.getPackageName());
        
        // Copy any system services or other state as needed
        
        return dummyContext;
    }
    
    /**
     * Convert a custom context to an Android context.
     * 
     * @param context The custom context to convert
     * @return The Android context
     */
    public static Context toAndroidContext(Object context) {
        if (context == null) {
            return new DummyAndroidContext();
        }
        
        if (context instanceof Context) {
            return (Context) context;
        }
        
        // For now, just return a default context
        return new DummyAndroidContext();
    }
    
    /**
     * Convert an Android context to a custom context object.
     * 
     * @param context The Android context
     * @return The custom context
     */
    public static utils.Context toUtilsContext(Context context) {
        // Return a DummyAndroidContext wrapped in a utils.Context
        DummyAndroidContext dummyContext = toDummyContext(context);
        return new utils.BasicContext(dummyContext);
    }
    
    /**
     * Create a generic context for use with the application.
     * 
     * @return A Context
     */
    public static Context createContext() {
        return new DummyAndroidContext();
    }
    
    /**
     * Create a context with a specific package name.
     * 
     * @param packageName The package name
     * @return A Context
     */
    public static Context createContext(String packageName) {
        return new DummyAndroidContext(packageName);
    }
    
    /**
     * Get a system service from a context.
     * 
     * @param androidContext The context
     * @param name The name of the service
     * @return The system service
     */
    public static Object getSystemService(Context androidContext, String name) {
        if (androidContext == null) {
            return null;
        }
        
        return androidContext.getSystemService(name);
    }
}