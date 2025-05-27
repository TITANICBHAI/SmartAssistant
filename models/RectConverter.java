package models;

/**
 * Utility class for converting between different Rect implementations
 * and providing common operations.
 */
public class RectConverter {
    
    /**
     * Convert an Android Rect to a utils.Rect
     * 
     * @param androidRect The Android Rect to convert
     * @return The equivalent utils.Rect
     */
    public static utils.Rect toUtilsRect(android.graphics.Rect androidRect) {
        if (androidRect == null) {
            return null;
        }
        return new utils.Rect(androidRect.left, androidRect.top, androidRect.right, androidRect.bottom);
    }
    
    /**
     * Convert a utils.Rect to an Android Rect
     * 
     * @param utilsRect The utils.Rect to convert
     * @return The equivalent Android Rect
     */
    public static android.graphics.Rect toAndroidRect(utils.Rect utilsRect) {
        if (utilsRect == null) {
            return null;
        }
        return new android.graphics.Rect(utilsRect.left, utilsRect.top, utilsRect.right, utilsRect.bottom);
    }
    
    /**
     * Check if two rectangles intersect
     * 
     * @param rect1 The first rectangle
     * @param rect2 The second rectangle
     * @return True if the rectangles intersect, false otherwise
     */
    public static boolean intersects(android.graphics.Rect rect1, android.graphics.Rect rect2) {
        if (rect1 == null || rect2 == null) {
            return false;
        }
        return rect1.intersect(rect2);
    }
    
    /**
     * Check if two utils.Rect instances intersect
     * 
     * @param rect1 The first rectangle
     * @param rect2 The second rectangle
     * @return True if the rectangles intersect, false otherwise
     */
    public static boolean intersects(utils.Rect rect1, utils.Rect rect2) {
        if (rect1 == null || rect2 == null) {
            return false;
        }
        // Convert to standard rectangle representation for intersection test
        return !(rect1.left > rect2.right || rect1.right < rect2.left || 
                 rect1.top > rect2.bottom || rect1.bottom < rect2.top);
    }
    
    /**
     * Check if a utils.Rect and an android.graphics.Rect intersect
     * 
     * @param utilsRect The utils.Rect
     * @param androidRect The android.graphics.Rect
     * @return True if the rectangles intersect, false otherwise
     */
    public static boolean intersects(utils.Rect utilsRect, android.graphics.Rect androidRect) {
        if (utilsRect == null || androidRect == null) {
            return false;
        }
        // Convert utilsRect to androidRect and check intersection
        android.graphics.Rect converted = toAndroidRect(utilsRect);
        return converted.intersect(androidRect);
    }
    
    /**
     * Check if an android.graphics.Rect and a utils.Rect intersect
     * 
     * @param androidRect The android.graphics.Rect
     * @param utilsRect The utils.Rect
     * @return True if the rectangles intersect, false otherwise
     */
    public static boolean intersects(android.graphics.Rect androidRect, utils.Rect utilsRect) {
        return intersects(utilsRect, androidRect);
    }
    
    /**
     * Convert any object to an Android Rect if possible
     * 
     * @param rectObj The object to convert
     * @return The equivalent Android Rect or null if conversion is not possible
     */
    public static android.graphics.Rect toAndroidRect(Object rectObj) {
        if (rectObj == null) {
            return null;
        }
        
        if (rectObj instanceof android.graphics.Rect) {
            return (android.graphics.Rect) rectObj;
        }
        
        if (rectObj instanceof utils.Rect) {
            return toAndroidRect((utils.Rect) rectObj);
        }
        
        // Try to extract properties using reflection
        try {
            java.lang.reflect.Method getLeftMethod = rectObj.getClass().getMethod("getLeft");
            java.lang.reflect.Method getTopMethod = rectObj.getClass().getMethod("getTop");
            java.lang.reflect.Method getRightMethod = rectObj.getClass().getMethod("getRight");
            java.lang.reflect.Method getBottomMethod = rectObj.getClass().getMethod("getBottom");
            
            int left = ((Number) getLeftMethod.invoke(rectObj)).intValue();
            int top = ((Number) getTopMethod.invoke(rectObj)).intValue();
            int right = ((Number) getRightMethod.invoke(rectObj)).intValue();
            int bottom = ((Number) getBottomMethod.invoke(rectObj)).intValue();
            
            return new android.graphics.Rect(left, top, right, bottom);
        } catch (Exception e) {
            // Try direct field access if methods don't work
            try {
                java.lang.reflect.Field leftField = rectObj.getClass().getField("left");
                java.lang.reflect.Field topField = rectObj.getClass().getField("top");
                java.lang.reflect.Field rightField = rectObj.getClass().getField("right");
                java.lang.reflect.Field bottomField = rectObj.getClass().getField("bottom");
                
                int left = leftField.getInt(rectObj);
                int top = topField.getInt(rectObj);
                int right = rightField.getInt(rectObj);
                int bottom = bottomField.getInt(rectObj);
                
                return new android.graphics.Rect(left, top, right, bottom);
            } catch (Exception ex) {
                // Conversion failed
                return null;
            }
        }
    }
}