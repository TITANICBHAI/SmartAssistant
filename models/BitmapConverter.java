package models;

import android.graphics.Bitmap;

/**
 * Utility class for converting between different Bitmap types
 */
public class BitmapConverter {
    
    /**
     * Convert from android.graphics.Bitmap to utils.Bitmap
     * 
     * @param androidBitmap The Android Bitmap to convert
     * @return Equivalent utils.Bitmap
     */
    public static utils.Bitmap toUtilsBitmap(android.graphics.Bitmap androidBitmap) {
        if (androidBitmap == null) {
            return null;
        }
        
        // Create a new utils.Bitmap with the same dimensions and data
        utils.Bitmap utilsBitmap = utils.Bitmap.createBitmap(
            androidBitmap.getWidth(), 
            androidBitmap.getHeight(), 
            utils.Bitmap.Config.ARGB_8888
        );
        
        // Copy pixel data if needed
        try {
            // Convert int[] pixels to byte[] for utils.Bitmap
            int[] intPixels = new int[androidBitmap.getWidth() * androidBitmap.getHeight()];
            androidBitmap.getPixels(intPixels, 0, androidBitmap.getWidth(), 0, 0, 
                                   androidBitmap.getWidth(), androidBitmap.getHeight());
            
            // Convert int[] to byte[]
            byte[] bytePixels = new byte[intPixels.length * 4]; // 4 bytes per int for ARGB_8888
            for (int i = 0; i < intPixels.length; i++) {
                int pixel = intPixels[i];
                int byteIndex = i * 4;
                bytePixels[byteIndex] = (byte)((pixel >> 24) & 0xFF); // Alpha
                bytePixels[byteIndex + 1] = (byte)((pixel >> 16) & 0xFF); // Red
                bytePixels[byteIndex + 2] = (byte)((pixel >> 8) & 0xFF); // Green
                bytePixels[byteIndex + 3] = (byte)(pixel & 0xFF); // Blue
            }
            
            utilsBitmap.setPixels(bytePixels);
        } catch (Exception e) {
            System.err.println("Error converting android.graphics.Bitmap to utils.Bitmap: " + e.getMessage());
            e.printStackTrace();
        }
        
        return utilsBitmap;
    }
    
    /**
     * Convert from utils.Bitmap to android.graphics.Bitmap
     * 
     * @param utilsBitmap The utils.Bitmap to convert
     * @return Equivalent android.graphics.Bitmap
     */
    public static android.graphics.Bitmap toAndroidBitmap(utils.Bitmap utilsBitmap) {
        if (utilsBitmap == null) {
            return null;
        }
        
        // Create a new Android Bitmap with the same dimensions
        android.graphics.Bitmap androidBitmap = null;
        try {
            androidBitmap = android.graphics.Bitmap.createBitmap(
                utilsBitmap.getWidth(), utilsBitmap.getHeight(), android.graphics.Bitmap.Config.ARGB_8888);
            
            // Copy pixel data - need to convert from byte[] to int[]
            byte[] bytePixels = utilsBitmap.getPixels();
            int[] intPixels = new int[utilsBitmap.getWidth() * utilsBitmap.getHeight()];
            
            // Convert byte[] to int[] - 4 bytes per int for ARGB_8888
            for (int i = 0; i < intPixels.length; i++) {
                int byteIndex = i * 4;
                if (byteIndex + 3 < bytePixels.length) {
                    intPixels[i] = ((bytePixels[byteIndex] & 0xFF) << 24) | // Alpha
                                   ((bytePixels[byteIndex + 1] & 0xFF) << 16) | // Red
                                   ((bytePixels[byteIndex + 2] & 0xFF) << 8) | // Green
                                    (bytePixels[byteIndex + 3] & 0xFF); // Blue
                }
            }
            
            androidBitmap.setPixels(intPixels, 0, utilsBitmap.getWidth(), 0, 0, 
                                   utilsBitmap.getWidth(), utilsBitmap.getHeight());
        } catch (Exception e) {
            System.err.println("Error converting utils.Bitmap to android.graphics.Bitmap: " + e.getMessage());
            e.printStackTrace();
        }
        
        return androidBitmap;
    }
    
    /**
     * Create an empty utils.Bitmap compatible with the converter
     * 
     * @param width The width of the bitmap
     * @param height The height of the bitmap
     * @return A new utils.Bitmap
     */
    public static utils.Bitmap createUtilsBitmap(int width, int height) {
        return utils.Bitmap.createBitmap(width, height, utils.Bitmap.Config.ARGB_8888);
    }
    
    /**
     * Create an empty android.graphics.Bitmap compatible with the converter
     * 
     * @param width The width of the bitmap
     * @param height The height of the bitmap
     * @return A new android.graphics.Bitmap
     */
    public static android.graphics.Bitmap createAndroidBitmap(int width, int height) {
        return android.graphics.Bitmap.createBitmap(width, height, android.graphics.Bitmap.Config.ARGB_8888);
    }
}