package android.support.v4.content;

import android.content.Context;

/**
 * Mock implementation of Android's ContextCompat class
 * This provides compatibility methods for working with resources and permissions
 */
public class ContextCompat {
    
    /**
     * Get a color from resources
     * 
     * @param context The Android context
     * @param colorResId Resource ID of the color
     * @return The color value as an int
     */
    public static int getColor(Context context, int colorResId) {
        // In a real Android app, this would access the resource system
        // For our mock implementation, return a default value for each resource ID
        
        // If this is called through ResourcesHelper.color class
        if (colorResId == utils.ResourcesHelper.color.status_active) {
            return 0xFF4CAF50; // Green
        } else if (colorResId == utils.ResourcesHelper.color.status_scheduled) {
            return 0xFF2196F3; // Blue
        } else if (colorResId == utils.ResourcesHelper.color.status_completed) {
            return 0xFF9E9E9E; // Gray
        } else if (colorResId == utils.ResourcesHelper.color.status_cancelled) {
            return 0xFFE53935; // Red
        } else if (colorResId == utils.ResourcesHelper.color.priority_high) {
            return 0xFFE53935; // Red
        } else if (colorResId == utils.ResourcesHelper.color.priority_medium) {
            return 0xFFFF9800; // Orange
        } else if (colorResId == utils.ResourcesHelper.color.priority_low) {
            return 0xFF4CAF50; // Green
        }
        
        // Default color
        return 0xFF000000; // Black
    }
    
    /**
     * Get a drawable from resources
     * 
     * @param context The Android context
     * @param drawableResId Resource ID of the drawable
     * @return The drawable object
     */
    public static Object getDrawable(Context context, int drawableResId) {
        // This would normally return a Drawable object
        // For our mock implementation, return a placeholder object
        return new Object();
    }
    
    /**
     * Check if a permission has been granted
     * 
     * @param context The Android context
     * @param permission The permission to check
     * @return True if the permission is granted
     */
    public static boolean checkSelfPermission(Context context, String permission) {
        // Always return true in our mock implementation
        return true;
    }
}