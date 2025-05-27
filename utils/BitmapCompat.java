package utils;

import android.graphics.Bitmap;

/**
 * Helper for accessing features in Bitmap in a backward compatible fashion.
 */
public class BitmapCompat {
    /**
     * Get the hardware buffer size in bytes of the bitmap.
     *
     * @param bitmap The bitmap to query for its allocation size.
     * @return Size of the buffer in bytes. 0 if not applicable.
     */
    public static int getAllocationByteCount(Bitmap bitmap) {
        if (bitmap == null) {
            return 0;
        }
        
        // In our simplified implementation, we'll estimate the size based on the bitmap's dimensions and config
        int bytesPerPixel = getBytesPerPixel(bitmap.getConfig());
        return bitmap.getWidth() * bitmap.getHeight() * bytesPerPixel;
    }
    
    /**
     * Return the byte usage per pixel of a bitmap based on its configuration.
     *
     * @param config The bitmap configuration to query.
     * @return Byte usage per pixel.
     */
    private static int getBytesPerPixel(Bitmap.Config config) {
        if (config == null) {
            config = Bitmap.Config.ARGB_8888;  // Default
        }
        
        switch (config) {
            case ALPHA_8:
                return 1;
            case RGB_565:
            case ARGB_4444:
                return 2;
            case ARGB_8888:
                return 4;
            case RGBA_F16:
                return 8;
            default:
                return 4;
        }
    }
    
    /**
     * Returns true if the bitmap's hardware config is supported.
     *
     * @param bitmap The bitmap to check.
     * @return True if the bitmap uses a hardware config, false otherwise.
     */
    public static boolean hasAlpha(Bitmap bitmap) {
        if (bitmap == null) {
            return false;
        }
        
        Bitmap.Config config = bitmap.getConfig();
        return config == Bitmap.Config.ARGB_8888 || config == Bitmap.Config.ARGB_4444 || config == Bitmap.Config.RGBA_F16;
    }
    
    /**
     * Set the bitmap's mutable state.
     *
     * @param bitmap The bitmap to modify.
     * @param isMutable True if the bitmap should be mutable, false otherwise.
     */
    public static void setMutable(Bitmap bitmap, boolean isMutable) {
        // In our simplified implementation, this is a no-op
        // In the actual Android implementation, this would set the bitmap's mutable state
        // For our purposes, we'll assume all bitmaps are created mutable
    }
    
    /**
     * Get the density of the bitmap.
     *
     * @param bitmap The bitmap to query for its density.
     * @return The density of the bitmap.
     */
    public static int getDensity(Bitmap bitmap) {
        if (bitmap == null) {
            return Bitmap.DENSITY_NONE;
        }
        
        return bitmap.getDensity();
    }
    
    /**
     * Get the allocation byte count of the bitmap.
     *
     * @param bitmap The bitmap to query for its allocation byte count.
     * @return The allocation byte count of the bitmap.
     */
    public static int getByteCount(Bitmap bitmap) {
        if (bitmap == null) {
            return 0;
        }
        
        return bitmap.getWidth() * bitmap.getHeight() * getBytesPerPixel(bitmap.getConfig());
    }
}