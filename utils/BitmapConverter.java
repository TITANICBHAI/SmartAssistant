package utils;

import java.awt.image.BufferedImage;
import java.lang.reflect.Method;
import java.nio.ByteBuffer;
import java.nio.IntBuffer;

/**
 * Utility class for converting between different bitmap implementations and pixel formats
 */
public class BitmapConverter {
    
    /**
     * Convert a byte array of pixel data to an int array
     * @param bytes The byte array
     * @return The corresponding int array
     */
    public static int[] bytesToInts(byte[] bytes) {
        if (bytes == null) {
            return new int[0];
        }
        
        // Assuming ARGB_8888 format where each pixel is 4 bytes
        int[] result = new int[bytes.length / 4];
        ByteBuffer byteBuffer = ByteBuffer.wrap(bytes);
        IntBuffer intBuffer = byteBuffer.asIntBuffer();
        intBuffer.get(result);
        return result;
    }
    
    /**
     * Convert an int array of pixel data to a byte array
     * @param ints The int array
     * @return The corresponding byte array
     */
    public static byte[] intsToBytes(int[] ints) {
        if (ints == null) {
            return new byte[0];
        }
        
        // Assuming ARGB_8888 format where each pixel is 4 bytes
        ByteBuffer byteBuffer = ByteBuffer.allocate(ints.length * 4);
        IntBuffer intBuffer = byteBuffer.asIntBuffer();
        intBuffer.put(ints);
        return byteBuffer.array();
    }
    
    /**
     * Convert from utils.Bitmap to android.graphics.Bitmap
     * @param utilsBitmap The utils.Bitmap to convert
     * @return An equivalent android.graphics.Bitmap
     */
    public static android.graphics.Bitmap toAndroidBitmap(utils.Bitmap utilsBitmap) {
        if (utilsBitmap == null) {
            return null;
        }
        
        int width = utilsBitmap.getWidth();
        int height = utilsBitmap.getHeight();
        byte[] bytes = utilsBitmap.getPixels();
        int[] pixels = bytesToInts(bytes);
        
        android.graphics.Bitmap.Config config = 
            BitmapFactory.toAndroidConfig(utilsBitmap.getConfig());
            
        return android.graphics.Bitmap.createBitmap(
            pixels, 0, width, width, height, config);
    }
    
    /**
     * Convert from android.graphics.Bitmap to utils.Bitmap
     * @param androidBitmap The android.graphics.Bitmap to convert
     * @return An equivalent utils.Bitmap
     */
    public static utils.Bitmap toUtilsBitmap(android.graphics.Bitmap androidBitmap) {
        if (androidBitmap == null) {
            return null;
        }
        
        int width = androidBitmap.getWidth();
        int height = androidBitmap.getHeight();
        int[] pixels = new int[width * height];
        
        // Extract pixels from Android bitmap
        androidBitmap.getPixels(pixels, 0, width, 0, 0, width, height);
        
        // Convert to our format
        byte[] bytes = intsToBytes(pixels);
        utils.Bitmap.Config config = BitmapFactory.fromAndroidConfig(androidBitmap.getConfig());
        
        return new utils.Bitmap(width, height, config, bytes);
    }
    
    /**
     * Alias for toUtilsBitmap to match the method name in GameStateHelper
     * @param screenshot The Android bitmap to convert
     * @return An equivalent utils.Bitmap
     */
    public static utils.Bitmap convertToUtilsBitmap(android.graphics.Bitmap screenshot) {
        return toUtilsBitmap(screenshot);
    }
    
    /**
     * Convert a Java AWT BufferedImage to utils.Bitmap
     * @param bufferedImage The BufferedImage to convert
     * @return An equivalent utils.Bitmap
     */
    public static utils.Bitmap fromBufferedImage(BufferedImage bufferedImage) {
        if (bufferedImage == null) {
            return null;
        }
        
        int width = bufferedImage.getWidth();
        int height = bufferedImage.getHeight();
        int[] pixels = new int[width * height];
        
        // Get the RGB data from the BufferedImage
        bufferedImage.getRGB(0, 0, width, height, pixels, 0, width);
        
        // Convert to our format
        byte[] bytes = intsToBytes(pixels);
        utils.Bitmap.Config config = utils.Bitmap.Config.ARGB_8888;
        
        return new utils.Bitmap(width, height, config, bytes);
    }
    
    /**
     * Convert an Android Rect to a utils Rect
     * @param rect The Android rect to convert
     * @return An equivalent utils.Rect
     */
    public static utils.Rect toUtilsRect(android.graphics.Rect rect) {
        if (rect == null) {
            return null;
        }
        
        return new utils.Rect(rect.left, rect.top, rect.right, rect.bottom);
    }
    
    /**
     * Convert any bitmap type to utils.Bitmap
     * Supports android.graphics.Bitmap, utils.Bitmap, or any other bitmap-like object
     * that has width, height, and getPixels methods.
     * 
     * @param bitmap The bitmap object to convert
     * @return An equivalent utils.Bitmap
     */
    public static utils.Bitmap fromAny(Object bitmap) {
        if (bitmap == null) {
            return null;
        }
        
        // If it's already a utils.Bitmap, just return it
        if (bitmap instanceof utils.Bitmap) {
            return (utils.Bitmap) bitmap;
        }
        
        // If it's an Android bitmap, use the specific converter
        if (bitmap instanceof android.graphics.Bitmap) {
            return toUtilsBitmap((android.graphics.Bitmap) bitmap);
        }
        
        // If it's a Java BufferedImage, use the specific converter
        if (bitmap instanceof BufferedImage) {
            return fromBufferedImage((BufferedImage) bitmap);
        }
        
        // For any other type, try to use reflection to extract the necessary information
        try {
            Class<?> bitmapClass = bitmap.getClass();
            
            // Get width and height
            Method getWidthMethod = bitmapClass.getMethod("getWidth");
            Method getHeightMethod = bitmapClass.getMethod("getHeight");
            
            int width = (Integer) getWidthMethod.invoke(bitmap);
            int height = (Integer) getHeightMethod.invoke(bitmap);
            
            // Try to get pixels
            try {
                // First try getPixels method that returns int[]
                Method getPixelsMethod = bitmapClass.getMethod("getPixels", int[].class, int.class, int.class, int.class, int.class, int.class, int.class);
                int[] pixels = new int[width * height];
                getPixelsMethod.invoke(bitmap, pixels, 0, width, 0, 0, width, height);
                byte[] bytes = intsToBytes(pixels);
                
                // Use default config
                utils.Bitmap.Config config = utils.Bitmap.Config.ARGB_8888;
                
                return new utils.Bitmap(width, height, config, bytes);
            } catch (NoSuchMethodException e) {
                // If getPixels doesn't exist, try other methods
                try {
                    // Try getPixels method that returns byte[]
                    Method getPixelsMethod = bitmapClass.getMethod("getPixels");
                    byte[] bytes = (byte[]) getPixelsMethod.invoke(bitmap);
                    
                    // Use default config
                    utils.Bitmap.Config config = utils.Bitmap.Config.ARGB_8888;
                    
                    return new utils.Bitmap(width, height, config, bytes);
                } catch (NoSuchMethodException e2) {
                    // If all else fails, create an empty bitmap with the right dimensions
                    utils.Bitmap.Config config = utils.Bitmap.Config.ARGB_8888;
                    byte[] emptyBytes = new byte[width * height * 4]; // 4 bytes per pixel for ARGB_8888
                    return new utils.Bitmap(width, height, config, emptyBytes);
                }
            }
        } catch (Exception e) {
            // If we encounter any error, return null
            System.err.println("Error converting bitmap: " + e.getMessage());
            return null;
        }
    }
}