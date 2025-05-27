package utils;

import android.graphics.Bitmap;

/**
 * Helper class for bitmap operations
 */
public class BitmapHelper {
    
    /**
     * Convert a utils.Bitmap.Config to android.graphics.Bitmap.Config
     * @param config The utils config
     * @return The Android config
     */
    public static android.graphics.Bitmap.Config toAndroidConfig(utils.Bitmap.Config config) {
        if (config == null) {
            return android.graphics.Bitmap.Config.ARGB_8888;
        }
        
        switch (config) {
            case ALPHA_8:
                return android.graphics.Bitmap.Config.ALPHA_8;
            case RGB_565:
                return android.graphics.Bitmap.Config.RGB_565;
            case ARGB_4444:
                return android.graphics.Bitmap.Config.ARGB_4444;
            case ARGB_8888:
            default:
                return android.graphics.Bitmap.Config.ARGB_8888;
        }
    }
    
    /**
     * Convert an android.graphics.Bitmap.Config to utils.Bitmap.Config
     * @param config The Android config
     * @return The utils config
     */
    public static utils.Bitmap.Config fromAndroidConfig(android.graphics.Bitmap.Config config) {
        if (config == null) {
            return utils.Bitmap.Config.ARGB_8888;
        }
        
        if (config.equals(android.graphics.Bitmap.Config.ALPHA_8)) {
            return utils.Bitmap.Config.ALPHA_8;
        } else if (config.equals(android.graphics.Bitmap.Config.RGB_565)) {
            return utils.Bitmap.Config.RGB_565;
        } else if (config.equals(android.graphics.Bitmap.Config.ARGB_4444)) {
            return utils.Bitmap.Config.ARGB_4444;
        } else if (config.equals(android.graphics.Bitmap.Config.HARDWARE)) {
            return utils.Bitmap.Config.ARGB_8888; // Best match for HARDWARE
        } else if (android.os.Build.VERSION.SDK_INT >= 26 && 
                config.toString().equals("RGBA_F16")) {
            return utils.Bitmap.Config.ARGB_8888; // Best match for RGBA_F16
        } else {
            return utils.Bitmap.Config.ARGB_8888;
        }
    }
    
    /**
     * Get the bitmap configuration from a bitmap
     * @param bitmap The bitmap
     * @return The configuration
     */
    public static utils.Bitmap.Config getConfig(Bitmap bitmap) {
        if (bitmap == null) {
            return utils.Bitmap.Config.ARGB_8888;
        }
        
        return fromAndroidConfig(bitmap.getConfig());
    }
    
    /**
     * Copy a bitmap
     * @param bitmap The bitmap to copy
     * @param isMutable Whether the copy should be mutable
     * @return The copied bitmap
     */
    public static Bitmap copy(Bitmap bitmap, boolean isMutable) {
        if (bitmap == null) {
            return null;
        }
        
        Bitmap copy = bitmap.copy(bitmap.getConfig(), isMutable);
        return copy;
    }
    
    /**
     * Copy a bitmap with a specific config
     * @param bitmap The bitmap to copy
     * @param config The configuration for the copy
     * @param isMutable Whether the copy should be mutable
     * @return The copied bitmap
     */
    public static Bitmap copy(Bitmap bitmap, android.graphics.Bitmap.Config config, boolean isMutable) {
        if (bitmap == null) {
            return null;
        }
        
        Bitmap copy = bitmap.copy(config != null ? config : bitmap.getConfig(), isMutable);
        return copy;
    }
    
    /**
     * Create a bitmap from a utils.Bitmap
     * @param utilsBitmap The utils bitmap
     * @return The Android bitmap
     */
    public static Bitmap createBitmap(utils.Bitmap utilsBitmap) {
        if (utilsBitmap == null) {
            return null;
        }
        
        // Use the converter to handle the conversion
        return BitmapConverter.toAndroidBitmap(utilsBitmap);
    }
    
    /**
     * Create a new bitmap with the specified width, height, and config
     * @param width The width of the bitmap
     * @param height The height of the bitmap 
     * @param config The bitmap config to use
     * @return The created bitmap
     */
    public static Bitmap createBitmap(int width, int height, Bitmap.Config config) {
        if (width <= 0 || height <= 0) {
            return null;
        }
        
        return Bitmap.createBitmap(width, height, config != null ? config : Bitmap.Config.ARGB_8888);
    }
    
    /**
     * Create a utils.Bitmap from an Android Bitmap
     * @param bitmap The Android bitmap
     * @return The utils bitmap
     */
    public static utils.Bitmap createUtilsBitmap(Bitmap bitmap) {
        if (bitmap == null) {
            return null;
        }
        
        int width = bitmap.getWidth();
        int height = bitmap.getHeight();
        int[] pixels = new int[width * height];
        
        bitmap.getPixels(pixels, 0, width, 0, 0, width, height);
        
        // Convert int[] to byte[] with the new converter
        byte[] bytes = BitmapConverter.intsToBytes(pixels);
        
        return new utils.Bitmap(width, height, utils.Bitmap.Config.ARGB_8888, bytes);
    }
    
    /**
     * Convert a utils.Bitmap to android.graphics.Bitmap
     * @param utilsBitmap The utils bitmap
     * @return The Android bitmap
     */
    public static android.graphics.Bitmap toAndroidBitmap(utils.Bitmap utilsBitmap) {
        return createBitmap(utilsBitmap);
    }
    
    /**
     * Convert an android.graphics.Bitmap to utils.Bitmap
     * @param androidBitmap The Android bitmap
     * @return The utils bitmap
     */
    public static utils.Bitmap fromAndroidBitmap(android.graphics.Bitmap androidBitmap) {
        return createUtilsBitmap(androidBitmap);
    }
    
    /**
     * Convert an Android Bitmap to a utils Bitmap using reflection
     * This method is for use when direct casting is not possible
     * 
     * @param androidBitmapObj The Android bitmap as a generic Object
     * @return The utils bitmap or null if conversion fails
     */
    public static utils.Bitmap fromAndroidBitmapUsingReflection(Object androidBitmapObj) {
        if (androidBitmapObj == null) {
            return null;
        }
        
        try {
            // Get width and height using reflection
            int width = (Integer)androidBitmapObj.getClass().getMethod("getWidth").invoke(androidBitmapObj);
            int height = (Integer)androidBitmapObj.getClass().getMethod("getHeight").invoke(androidBitmapObj);
            
            // Create pixel array
            int[] pixels = new int[width * height];
            
            // Get pixels using reflection
            Class<?>[] paramTypes = {int[].class, int.class, int.class, int.class, int.class, int.class, int.class};
            Object[] params = {pixels, 0, width, 0, 0, width, height};
            androidBitmapObj.getClass().getMethod("getPixels", paramTypes).invoke(androidBitmapObj, params);
            
            // Convert int[] to byte[] with the new converter
            byte[] bytes = BitmapConverter.intsToBytes(pixels);
            
            // Create utils.Bitmap
            return new utils.Bitmap(width, height, utils.Bitmap.Config.ARGB_8888, bytes);
        } catch (Exception e) {
            System.err.println("Error converting Android Bitmap using reflection: " + e.getMessage());
            e.printStackTrace();
            
            // Create an empty bitmap as fallback
            return new utils.Bitmap(1, 1, utils.Bitmap.Config.ARGB_8888, new byte[4]);
        }
    }
    
    /**
     * Create a scaled bitmap from the source bitmap
     * @param src The source bitmap
     * @param dstWidth The destination width
     * @param dstHeight The destination height
     * @param filter Whether to apply a filter when scaling
     * @return The scaled bitmap
     */
    public static android.graphics.Bitmap createScaledBitmap(android.graphics.Bitmap src, 
                                                          int dstWidth, int dstHeight, boolean filter) {
        if (src == null) {
            return null;
        }
        
        return android.graphics.Bitmap.createScaledBitmap(src, dstWidth, dstHeight, filter);
    }
    
    /**
     * Create a scaled bitmap from the source bitmap
     * @param src The source bitmap
     * @param dstWidth The destination width
     * @param dstHeight The destination height
     * @return The scaled bitmap
     */
    public static utils.Bitmap createScaledBitmap(utils.Bitmap src, int dstWidth, int dstHeight) {
        if (src == null) {
            return null;
        }
        
        // Convert to Android bitmap, scale it, then convert back
        android.graphics.Bitmap androidBitmap = toAndroidBitmap(src);
        android.graphics.Bitmap scaledBitmap = createScaledBitmap(androidBitmap, dstWidth, dstHeight, true);
        return createUtilsBitmap(scaledBitmap);
    }
    
    /**
     * Convert an java.awt.image.BufferedImage to android.graphics.Bitmap
     * @param bufferedImage The BufferedImage to convert
     * @return The Android bitmap
     */
    public static android.graphics.Bitmap fromBufferedImage(java.awt.image.BufferedImage bufferedImage) {
        if (bufferedImage == null) {
            return null;
        }
        
        int width = bufferedImage.getWidth();
        int height = bufferedImage.getHeight();
        int[] pixels = new int[width * height];
        
        bufferedImage.getRGB(0, 0, width, height, pixels, 0, width);
        
        // Convert ARGB to RGBA
        for (int i = 0; i < pixels.length; i++) {
            int pixel = pixels[i];
            int alpha = (pixel >> 24) & 0xFF;
            int red = (pixel >> 16) & 0xFF;
            int green = (pixel >> 8) & 0xFF;
            int blue = pixel & 0xFF;
            pixels[i] = (alpha << 24) | (red << 16) | (green << 8) | blue;
        }
        
        return android.graphics.Bitmap.createBitmap(pixels, width, height, android.graphics.Bitmap.Config.ARGB_8888);
    }
    
    /**
     * Convert an android.graphics.Bitmap to java.awt.image.BufferedImage
     * @param bitmap The Android bitmap to convert
     * @return The BufferedImage
     */
    public static java.awt.image.BufferedImage toBufferedImage(android.graphics.Bitmap bitmap) {
        if (bitmap == null) {
            return null;
        }
        
        int width = bitmap.getWidth();
        int height = bitmap.getHeight();
        int[] pixels = new int[width * height];
        
        bitmap.getPixels(pixels, 0, width, 0, 0, width, height);
        
        // Create a BufferedImage
        java.awt.image.BufferedImage bufferedImage = new java.awt.image.BufferedImage(
            width, height, java.awt.image.BufferedImage.TYPE_INT_ARGB);
        
        bufferedImage.setRGB(0, 0, width, height, pixels, 0, width);
        
        return bufferedImage;
    }
    
    /**
     * Convert a utils.Bitmap to java.awt.image.BufferedImage
     * @param bitmap The utils bitmap to convert
     * @return The BufferedImage
     */
    public static java.awt.image.BufferedImage toBufferedImage(utils.Bitmap bitmap) {
        if (bitmap == null) {
            return null;
        }
        
        // Convert to Android bitmap first, then to BufferedImage
        android.graphics.Bitmap androidBitmap = toAndroidBitmap(bitmap);
        return toBufferedImage(androidBitmap);
    }
    
    /**
     * Convert a java.awt.image.BufferedImage to utils.Bitmap
     * @param bufferedImage The BufferedImage to convert
     * @return The utils bitmap
     */
    public static utils.Bitmap bufferedImageToUtilsBitmap(java.awt.image.BufferedImage bufferedImage) {
        if (bufferedImage == null) {
            return null;
        }
        
        // Convert to Android bitmap first, then to utils.Bitmap
        android.graphics.Bitmap androidBitmap = fromBufferedImage(bufferedImage);
        return fromAndroidBitmap(androidBitmap);
    }
}