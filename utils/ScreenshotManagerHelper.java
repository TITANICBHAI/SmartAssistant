package utils;

import android.graphics.Bitmap;
import android.util.Log;

/**
 * Helper class for converting between different ScreenshotManager implementations.
 */
public class ScreenshotManagerHelper {
    private static final String TAG = "ScreenshotManagerHelper";
    
    /**
     * Get an instance of ScreenshotManager
     * @param context The context
     * @return The ScreenshotManager instance
     */
    public static Object getInstance(Object context) {
        try {
            Class<?> managerClass = Class.forName("com.aiassistant.utils.ScreenshotManager");
            java.lang.reflect.Method method = managerClass.getMethod("getInstance", Object.class);
            return method.invoke(null, context);
        } catch (Exception e) {
            Log.e(TAG, "Error getting ScreenshotManager instance: " + e.getMessage());
            return null;
        }
    }
    
    /**
     * Capture a screenshot
     * @param manager The ScreenshotManager
     * @return The captured screenshot
     */
    public static Bitmap captureScreenshot(Object manager) {
        if (manager == null) {
            return null;
        }
        
        try {
            java.lang.reflect.Method method = manager.getClass().getMethod("captureScreenshot");
            return (Bitmap) method.invoke(manager);
        } catch (Exception e) {
            Log.e(TAG, "Error capturing screenshot: " + e.getMessage());
            return null;
        }
    }
    
    /**
     * Get last screenshot width
     * @param manager The ScreenshotManager
     * @return The width
     */
    public static int getLastScreenshotWidth(Object manager) {
        if (manager == null) {
            return 0;
        }
        
        try {
            java.lang.reflect.Method method = manager.getClass().getMethod("getLastScreenshotWidth");
            return (int) method.invoke(manager);
        } catch (Exception e) {
            Log.e(TAG, "Error getting last screenshot width: " + e.getMessage());
            return 0;
        }
    }
    
    /**
     * Get last screenshot height
     * @param manager The ScreenshotManager
     * @return The height
     */
    public static int getLastScreenshotHeight(Object manager) {
        if (manager == null) {
            return 0;
        }
        
        try {
            java.lang.reflect.Method method = manager.getClass().getMethod("getLastScreenshotHeight");
            return (int) method.invoke(manager);
        } catch (Exception e) {
            Log.e(TAG, "Error getting last screenshot height: " + e.getMessage());
            return 0;
        }
    }
}
