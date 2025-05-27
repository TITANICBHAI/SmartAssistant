package utils;

import java.io.InputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.File;

/**
 * BitmapFactory for creating Bitmap instances from various sources
 */
public class BitmapFactory {
    /**
     * Options for decoding bitmaps
     */
    public static class Options {
        public boolean inJustDecodeBounds;
        public int outWidth;
        public int outHeight;
        public String outMimeType;
        public android.graphics.Bitmap.Config inPreferredConfig = android.graphics.Bitmap.Config.ARGB_8888;
        public boolean inMutable;
        public int inSampleSize = 1;
    }
    
    /**
     * Decode a file into a bitmap
     * @param path The path to the file
     * @return The decoded bitmap, or null if decoding failed
     */
    public static utils.Bitmap decodeFile(String path) {
        return decodeFile(path, null);
    }
    
    /**
     * Decode a file into a bitmap with options
     * @param path The path to the file
     * @param opts The options for decoding
     * @return The decoded bitmap, or null if decoding failed
     */
    public static utils.Bitmap decodeFile(String path, Options opts) {
        try (FileInputStream stream = new FileInputStream(path)) {
            return decodeStream(stream, null, opts);
        } catch (IOException e) {
            return null;
        }
    }
    
    /**
     * Decode a stream into a bitmap
     * @param is The input stream
     * @return The decoded bitmap, or null if decoding failed
     */
    public static utils.Bitmap decodeStream(InputStream is) {
        return decodeStream(is, null, null);
    }
    
    /**
     * Decode a stream into a bitmap with options
     * @param is The input stream
     * @param outPadding The padding rect to receive the internal padding for the image
     * @param opts The options for decoding
     * @return The decoded bitmap, or null if decoding failed
     */
    public static utils.Bitmap decodeStream(InputStream is, android.graphics.Rect outPadding, Options opts) {
        try {
            // Mock implementation - create a simple bitmap
            int width = 100;
            int height = 100;
            
            if (opts != null && opts.inJustDecodeBounds) {
                if (opts != null) {
                    opts.outWidth = width;
                    opts.outHeight = height;
                    opts.outMimeType = "image/png";
                }
                return null;
            }
            
            if (opts != null && opts.inSampleSize > 1) {
                width = width / opts.inSampleSize;
                height = height / opts.inSampleSize;
            }
            
            utils.Bitmap bitmap = new utils.Bitmap(width, height, utils.Bitmap.Config.ARGB_8888);
            
            // Fill with a simple pattern
            for (int y = 0; y < height; y++) {
                for (int x = 0; x < width; x++) {
                    int color = ((x + y) % 2 == 0) ? 0xFFFFFFFF : 0xFF000000;
                    bitmap.setPixel(x, y, color);
                }
            }
            
            return bitmap;
        } catch (Exception e) {
            return null;
        }
    }
    
    /**
     * Convert utils.Bitmap.Config to android.graphics.Bitmap.Config
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
     * Convert android.graphics.Bitmap.Config to utils.Bitmap.Config
     * @param config The Android config
     * @return The utils config
     */
    public static utils.Bitmap.Config fromAndroidConfig(android.graphics.Bitmap.Config config) {
        if (config == null) {
            return utils.Bitmap.Config.ARGB_8888;
        }
        switch (config) {
            case ALPHA_8:
                return utils.Bitmap.Config.ALPHA_8;
            case RGB_565:
                return utils.Bitmap.Config.RGB_565;
            case ARGB_4444:
                return utils.Bitmap.Config.ARGB_4444;
            case ARGB_8888:
            default:
                return utils.Bitmap.Config.ARGB_8888;
        }
    }
}