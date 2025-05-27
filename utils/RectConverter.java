package utils;

/**
 * Utility class to convert between different Rect types
 * Used to fix compatibility issues between utils.Rect and android.graphics.Rect
 */
public class RectConverter {
    
    /**
     * Convert utils.Rect to android.graphics.Rect
     * 
     * @param utilsRect The utils.Rect to convert
     * @return The android.graphics.Rect (or null if input is null)
     */
    public static android.graphics.Rect toAndroidRect(utils.Rect utilsRect) {
        if (utilsRect == null) {
            return null;
        }
        return new android.graphics.Rect(
            utilsRect.left,
            utilsRect.top,
            utilsRect.right,
            utilsRect.bottom
        );
    }
    
    /**
     * Convert android.graphics.Rect to utils.Rect
     * 
     * @param androidRect The android.graphics.Rect to convert
     * @return The utils.Rect (or null if input is null)
     */
    public static utils.Rect toUtilsRect(android.graphics.Rect androidRect) {
        if (androidRect == null) {
            return null;
        }
        return new utils.Rect(
            androidRect.left,
            androidRect.top,
            androidRect.right,
            androidRect.bottom
        );
    }
    
    /**
     * Convert utils.Rect to android.graphics.RectF
     * 
     * @param utilsRect The utils.Rect to convert
     * @return The android.graphics.RectF (or null if input is null)
     */
    public static android.graphics.RectF toAndroidRectF(utils.Rect utilsRect) {
        if (utilsRect == null) {
            return null;
        }
        return new android.graphics.RectF(
            utilsRect.left,
            utilsRect.top,
            utilsRect.right,
            utilsRect.bottom
        );
    }
    
    /**
     * Convert android.graphics.RectF to utils.Rect
     * 
     * @param rectF The android.graphics.RectF to convert
     * @return The utils.Rect (or null if input is null)
     */
    public static utils.Rect fromRectF(android.graphics.RectF rectF) {
        if (rectF == null) {
            return null;
        }
        return new utils.Rect(
            Math.round(rectF.left),
            Math.round(rectF.top),
            Math.round(rectF.right),
            Math.round(rectF.bottom)
        );
    }
}