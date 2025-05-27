package utils;

import android.graphics.Rect;
import android.graphics.RectF;

/**
 * Helper methods for working with Rect objects.
 */
public class RectHelper {
    
    /**
     * Get the width of a rectangle.
     * 
     * @param rect The rectangle
     * @return The width
     */
    public static int width(Rect rect) {
        if (rect == null) {
            return 0;
        }
        
        return rect.right - rect.left;
    }
    
    /**
     * Get the height of a rectangle.
     * 
     * @param rect The rectangle
     * @return The height
     */
    public static int height(Rect rect) {
        if (rect == null) {
            return 0;
        }
        
        return rect.bottom - rect.top;
    }
    
    /**
     * Get the center X coordinate of a rectangle.
     * 
     * @param rect The rectangle
     * @return The center X coordinate
     */
    public static float centerX(Rect rect) {
        if (rect == null) {
            return 0;
        }
        
        return rect.left + width(rect) / 2.0f;
    }
    
    /**
     * Get the center Y coordinate of a rectangle.
     * 
     * @param rect The rectangle
     * @return The center Y coordinate
     */
    public static float centerY(Rect rect) {
        if (rect == null) {
            return 0;
        }
        
        return rect.top + height(rect) / 2.0f;
    }
    
    /**
     * Check if two rectangles intersect.
     * 
     * @param a The first rectangle
     * @param b The second rectangle
     * @return True if the rectangles intersect
     */
    public static boolean intersects(Rect a, Rect b) {
        if (a == null || b == null) {
            return false;
        }
        
        return a.intersects(b);
    }
    
    /**
     * Check if two rectangles intersect, handling different rectangle types.
     * 
     * @param a The first rectangle (can be utils.Rect or android.graphics.Rect)
     * @param b The second rectangle (can be utils.Rect or android.graphics.Rect)
     * @return True if the rectangles intersect
     */
    public static boolean intersects(Object a, Object b) {
        if (a == null || b == null) {
            return false;
        }
        
        // Convert to the same type for comparison
        if (a instanceof utils.Rect && b instanceof utils.Rect) {
            return intersects((utils.Rect)a, (utils.Rect)b);
        }
        
        if (a instanceof android.graphics.Rect && b instanceof android.graphics.Rect) {
            android.graphics.Rect rectA = (android.graphics.Rect)a;
            android.graphics.Rect rectB = (android.graphics.Rect)b;
            return rectA.intersect(rectB);
        }
        
        // Handle mixed types
        if (a instanceof utils.Rect && b instanceof android.graphics.Rect) {
            utils.Rect rectA = (utils.Rect)a;
            android.graphics.Rect rectB = (android.graphics.Rect)b;
            
            // Convert to same type
            android.graphics.Rect convertedA = toAndroidRect(rectA);
            return convertedA.intersect(rectB);
        }
        
        if (a instanceof android.graphics.Rect && b instanceof utils.Rect) {
            android.graphics.Rect rectA = (android.graphics.Rect)a;
            utils.Rect rectB = (utils.Rect)b;
            
            // Convert to same type
            android.graphics.Rect convertedB = toAndroidRect(rectB);
            return rectA.intersect(convertedB);
        }
        
        // Can't determine the type, return false
        return false;
    }
    
    /**
     * Calculate the distance between the centers of two rectangles.
     * 
     * @param a The first rectangle
     * @param b The second rectangle
     * @return The distance
     */
    public static float distanceBetweenCenters(Rect a, Rect b) {
        if (a == null || b == null) {
            return 0;
        }
        
        float dx = centerX(a) - centerX(b);
        float dy = centerY(a) - centerY(b);
        
        return (float) Math.sqrt(dx * dx + dy * dy);
    }
    
    /**
     * Calculate the intersection area of two rectangles.
     * 
     * @param a The first rectangle
     * @param b The second rectangle
     * @return The intersection area, or 0 if they don't intersect
     */
    public static int intersectionArea(Rect a, Rect b) {
        if (a == null || b == null || !intersects(a, b)) {
            return 0;
        }
        
        int left = Math.max(a.left, b.left);
        int right = Math.min(a.right, b.right);
        int top = Math.max(a.top, b.top);
        int bottom = Math.min(a.bottom, b.bottom);
        
        return (right - left) * (bottom - top);
    }
    
    /**
     * Create a new Rect that represents the union of two rectangles.
     * 
     * @param a The first rectangle
     * @param b The second rectangle
     * @return A new Rect that contains both input rectangles
     */
    public static Rect union(Rect a, Rect b) {
        if (a == null) {
            return b != null ? new Rect(b) : null;
        }
        
        if (b == null) {
            return new Rect(a);
        }
        
        int left = Math.min(a.left, b.left);
        int top = Math.min(a.top, b.top);
        int right = Math.max(a.right, b.right);
        int bottom = Math.max(a.bottom, b.bottom);
        
        return new Rect(left, top, right, bottom);
    }
    
    /**
     * Check if two rectangles are equal.
     * 
     * @param a The first rectangle
     * @param b The second rectangle
     * @return True if the rectangles are equal
     */
    public static boolean equals(Rect a, Rect b) {
        if (a == b) {
            return true;
        }
        
        if (a == null || b == null) {
            return false;
        }
        
        return a.left == b.left && a.top == b.top && a.right == b.right && a.bottom == b.bottom;
    }
    
    /**
     * Convert a rectangle to a readable string.
     * 
     * @param rect The rectangle
     * @return The string representation
     */
    public static String toString(Rect rect) {
        if (rect == null) {
            return "null";
        }
        
        return "Rect(" + rect.left + ", " + rect.top + ", " + rect.right + ", " + rect.bottom + ")";
    }
    
    /**
     * Convert a rectangle to a short readable string.
     * 
     * @param rect The rectangle
     * @return The short string representation
     */
    public static String toShortString(Rect rect) {
        if (rect == null) {
            return "null";
        }
        
        return "[" + rect.left + "," + rect.top + "," + rect.right + "," + rect.bottom + "]";
    }
    
    /**
     * Scale a rectangle by a factor.
     * 
     * @param rect The rectangle to scale
     * @param scale The scale factor
     * @return A new scaled rectangle
     */
    public static Rect scale(Rect rect, float scale) {
        if (rect == null) {
            return null;
        }
        
        int centerX = (int) centerX(rect);
        int centerY = (int) centerY(rect);
        int newWidth = (int) (width(rect) * scale);
        int newHeight = (int) (height(rect) * scale);
        
        return new Rect(
            centerX - newWidth / 2,
            centerY - newHeight / 2,
            centerX + newWidth / 2,
            centerY + newHeight / 2
        );
    }
    
    /**
     * Offset a rectangle.
     * 
     * @param rect The rectangle to offset
     * @param dx The x offset
     * @param dy The y offset
     * @return A new offset rectangle
     */
    public static Rect offset(Rect rect, int dx, int dy) {
        if (rect == null) {
            return null;
        }
        
        return new Rect(
            rect.left + dx,
            rect.top + dy,
            rect.right + dx,
            rect.bottom + dy
        );
    }
    
    /**
     * Inset a rectangle.
     * 
     * @param rect The rectangle to inset
     * @param dx The x inset
     * @param dy The y inset
     * @return A new inset rectangle
     */
    public static Rect inset(Rect rect, int dx, int dy) {
        if (rect == null) {
            return null;
        }
        
        return new Rect(
            rect.left + dx,
            rect.top + dy,
            rect.right - dx,
            rect.bottom - dy
        );
    }
    
    /**
     * Create a rectangle from an Android graphics rectangle.
     * 
     * @param androidRect The Android graphics rectangle
     * @return A new utils.Rect
     */
    public static Rect fromAndroidRect(android.graphics.Rect androidRect) {
        if (androidRect == null) {
            return null;
        }
        
        return new Rect(
            androidRect.left,
            androidRect.top,
            androidRect.right,
            androidRect.bottom
        );
    }
    
    /**
     * Convert a utils.Rect to an Android graphics rectangle.
     * 
     * @param rect The utils.Rect
     * @return A new Android graphics rectangle
     */
    public static android.graphics.Rect toAndroidRect(utils.Rect rect) {
        if (rect == null) {
            return null;
        }
        
        return new android.graphics.Rect(
            rect.left,
            rect.top,
            rect.right,
            rect.bottom
        );
    }
    
    /**
     * Convert a utils.Rect to an Android graphics RectF.
     * 
     * @param rect The utils.Rect
     * @return A new Android graphics RectF
     */
    public static android.graphics.RectF toRectF(utils.Rect rect) {
        if (rect == null) {
            return null;
        }
        
        return new android.graphics.RectF(
            rect.left,
            rect.top,
            rect.right,
            rect.bottom
        );
    }
    
    /**
     * Convert an Android graphics Rect to an Android graphics RectF.
     * 
     * @param rect The Android graphics Rect
     * @return A new Android graphics RectF
     */
    public static android.graphics.RectF toRectF(android.graphics.Rect rect) {
        if (rect == null) {
            return null;
        }
        
        return new android.graphics.RectF(
            rect.left,
            rect.top,
            rect.right,
            rect.bottom
        );
    }
    
    /**
     * Convert an object to an Android graphics RectF
     * This handles different types of rectangle objects by checking their type
     * 
     * @param rectObj An object representing a rectangle
     * @return A new Android graphics RectF or null if conversion fails
     */
    public static android.graphics.RectF toRectF(Object rectObj) {
        if (rectObj == null) {
            return null;
        }
        
        if (rectObj instanceof android.graphics.Rect) {
            return toRectF((android.graphics.Rect)rectObj);
        }
        
        if (rectObj instanceof android.graphics.RectF) {
            return (android.graphics.RectF)rectObj;
        }
        
        if (rectObj instanceof utils.Rect) {
            utils.Rect rect = (utils.Rect)rectObj;
            return new android.graphics.RectF(
                rect.left,
                rect.top,
                rect.right,
                rect.bottom
            );
        }
        
        // Add more conversions as needed for different rectangle types
        
        return null; // Could not convert
    }
}