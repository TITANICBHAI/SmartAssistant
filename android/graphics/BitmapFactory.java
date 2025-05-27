package android.graphics;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import android.content.res.Resources;

/**
 * Mock implementation of BitmapFactory class that creates Bitmap objects
 * from various sources like files, streams, and resource IDs.
 */
public class BitmapFactory {
    
    /**
     * Options for decoding bitmaps
     */
    public static class Options {
        /**
         * If set to true, the decoder will return null, but the dimensions
         * will still be set in outWidth and outHeight
         */
        public boolean inJustDecodeBounds;
        
        /**
         * If this is non-null, the decoder will try to decode into this bitmap
         */
        public Bitmap inBitmap;
        
        /**
         * If set to true, the decoder will try to decode a mutable bitmap
         */
        public boolean inMutable;
        
        /**
         * If set, the decoder will decode into this config
         */
        public Bitmap.Config inPreferredConfig = Bitmap.Config.ARGB_8888;
        
        /**
         * Set to true to enable bitmap scaling
         */
        public boolean inScaled;
        
        /**
         * Target density of resulting bitmap
         */
        public int inTargetDensity;
        
        /**
         * The density of the original source
         */
        public int inDensity;
        
        /**
         * Scaling factor (deprecated)
         */
        public int inSampleSize = 1;
        
        /**
         * Output width
         */
        public int outWidth;
        
        /**
         * Output height
         */
        public int outHeight;
        
        /**
         * Output MIME type
         */
        public String outMimeType;
        
        /**
         * Flag for MIME type JPEG
         */
        public static final int DECODE_BUFFER_SIZE = 16 * 1024;
        
        /**
         * Constructor
         */
        public Options() {
            inJustDecodeBounds = false;
            inMutable = false;
            inScaled = true;
            inSampleSize = 1;
        }
    }
    
    /**
     * Decode a file into a bitmap
     */
    public static Bitmap decodeFile(String pathName, Options opts) {
        if (pathName == null) {
            return null;
        }
        
        boolean decodeAsBounds = false;
        if (opts != null) {
            decodeAsBounds = opts.inJustDecodeBounds;
        }
        
        // Mock implementation - return a minimal bitmap or null if just decoding bounds
        if (decodeAsBounds) {
            if (opts != null) {
                opts.outWidth = 100;
                opts.outHeight = 100;
                opts.outMimeType = "image/jpeg";
            }
            return null;
        } else {
            // Create a mock bitmap
            return createMockBitmap(opts);
        }
    }
    
    /**
     * Decode a file into a bitmap
     */
    public static Bitmap decodeFile(String pathName) {
        return decodeFile(pathName, null);
    }
    
    /**
     * Decode a file into a bitmap
     */
    public static Bitmap decodeFileDescriptor(java.io.FileDescriptor fd, Rect outPadding, Options opts) {
        if (fd == null) {
            return null;
        }
        
        boolean decodeAsBounds = false;
        if (opts != null) {
            decodeAsBounds = opts.inJustDecodeBounds;
        }
        
        // Mock implementation - return a minimal bitmap or null if just decoding bounds
        if (decodeAsBounds) {
            if (opts != null) {
                opts.outWidth = 100;
                opts.outHeight = 100;
                opts.outMimeType = "image/png";
            }
            return null;
        } else {
            // Create a mock bitmap
            return createMockBitmap(opts);
        }
    }
    
    /**
     * Decode a file into a bitmap
     */
    public static Bitmap decodeFileDescriptor(java.io.FileDescriptor fd) {
        return decodeFileDescriptor(fd, null, null);
    }
    
    /**
     * Decode a resource into a bitmap
     */
    public static Bitmap decodeResource(Resources res, int id, Options opts) {
        if (res == null) {
            return null;
        }
        
        boolean decodeAsBounds = false;
        if (opts != null) {
            decodeAsBounds = opts.inJustDecodeBounds;
        }
        
        // Mock implementation - return a minimal bitmap or null if just decoding bounds
        if (decodeAsBounds) {
            if (opts != null) {
                opts.outWidth = 100;
                opts.outHeight = 100;
                opts.outMimeType = "image/png";
            }
            return null;
        } else {
            // Create a mock bitmap
            return createMockBitmap(opts);
        }
    }
    
    /**
     * Decode a resource into a bitmap
     */
    public static Bitmap decodeResource(Resources res, int id) {
        return decodeResource(res, id, null);
    }
    
    /**
     * Decode a stream into a bitmap
     */
    public static Bitmap decodeStream(InputStream is, Rect outPadding, Options opts) {
        if (is == null) {
            return null;
        }
        
        boolean decodeAsBounds = false;
        if (opts != null) {
            decodeAsBounds = opts.inJustDecodeBounds;
        }
        
        // Mock implementation - return a minimal bitmap or null if just decoding bounds
        if (decodeAsBounds) {
            if (opts != null) {
                opts.outWidth = 100;
                opts.outHeight = 100;
                opts.outMimeType = "image/jpeg";
            }
            return null;
        } else {
            // Create a mock bitmap
            return createMockBitmap(opts);
        }
    }
    
    /**
     * Decode a stream into a bitmap
     */
    public static Bitmap decodeStream(InputStream is) {
        return decodeStream(is, null, null);
    }
    
    /**
     * Decode a byte array into a bitmap
     */
    public static Bitmap decodeByteArray(byte[] data, int offset, int length, Options opts) {
        if (data == null || length <= 0 || offset < 0 || (offset + length) > data.length) {
            return null;
        }
        
        boolean decodeAsBounds = false;
        if (opts != null) {
            decodeAsBounds = opts.inJustDecodeBounds;
        }
        
        // Mock implementation - return a minimal bitmap or null if just decoding bounds
        if (decodeAsBounds) {
            if (opts != null) {
                opts.outWidth = 100;
                opts.outHeight = 100;
                opts.outMimeType = "image/jpeg";
            }
            return null;
        } else {
            // Create a mock bitmap
            return createMockBitmap(opts);
        }
    }
    
    /**
     * Decode a byte array into a bitmap
     */
    public static Bitmap decodeByteArray(byte[] data, int offset, int length) {
        return decodeByteArray(data, offset, length, null);
    }
    
    /**
     * Helper method to create a mock bitmap
     */
    private static Bitmap createMockBitmap(Options opts) {
        Bitmap.Config config = Bitmap.Config.ARGB_8888;
        boolean isMutable = false;
        
        if (opts != null) {
            config = opts.inPreferredConfig;
            isMutable = opts.inMutable;
        }
        
        // Create a standard mock bitmap
        Bitmap bitmap = Bitmap.createBitmap(100, 100, config);
        // In a real implementation, the bitmap would be filled with image data here
        return bitmap;
    }
}