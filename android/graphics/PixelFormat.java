package android.graphics;

/**
 * The PixelFormat class specifies different types of pixel formats.
 * 
 * This is a simple mock implementation for the purposes of simulating
 * the Android SDK PixelFormat class.
 */
public class PixelFormat {
    /**
     * Unknown pixel format.
     */
    public static final int UNKNOWN = 0;
    
    /**
     * Translucent, with 8 bits of alpha.
     */
    public static final int TRANSLUCENT = -3;
    
    /**
     * No alpha, with 8 bits each of red, green, blue.
     */
    public static final int OPAQUE = -1;
    
    /**
     * Transparent, with alpha = 0. All RGB values are ignored.
     */
    public static final int TRANSPARENT = -2;
    
    /**
     * RGB 8:8:8:8 format, with alpha.
     */
    public static final int RGBA_8888 = 1;
    
    /**
     * RGB 8:8:8 format.
     */
    public static final int RGBX_8888 = 2;
    
    /**
     * RGB 5:6:5 format.
     */
    public static final int RGB_565 = 4;
    
    /**
     * a pre-multiplied RGBA_8888 format.
     */
    public static final int RGBA_F16 = 0x16;
    
    /**
     * JPEG format.
     */
    public static final int JPEG = 0x100;
    
    /**
     * Return the pixel format's name.
     * 
     * @param format The format identifier.
     * @return The name of the format.
     */
    public static String formatToString(int format) {
        switch (format) {
            case UNKNOWN:
                return "UNKNOWN";
            case TRANSLUCENT:
                return "TRANSLUCENT";
            case TRANSPARENT:
                return "TRANSPARENT";
            case OPAQUE:
                return "OPAQUE";
            case RGBA_8888:
                return "RGBA_8888";
            case RGBX_8888:
                return "RGBX_8888";
            case RGB_565:
                return "RGB_565";
            case RGBA_F16:
                return "RGBA_F16";
            case JPEG:
                return "JPEG";
            default:
                return String.format("UNKNOWN_FORMAT_(#%d)", format);
        }
    }
    
    /**
     * Return the bits per pixel for the specified format.
     * 
     * @param format The format identifier.
     * @return The bits per pixel or -1 if unknown.
     */
    public static int getBitsPerPixel(int format) {
        switch (format) {
            case RGBA_8888:
                return 32;
            case RGBX_8888:
                return 32;
            case RGB_565:
                return 16;
            case RGBA_F16:
                return 64;
            default:
                return -1;
        }
    }
    
    /**
     * Return whether the specified format has an alpha channel.
     * 
     * @param format The format identifier.
     * @return True if the format has an alpha channel.
     */
    public static boolean hasAlpha(int format) {
        switch (format) {
            case TRANSLUCENT:
            case TRANSPARENT:
            case RGBA_8888:
            case RGBA_F16:
                return true;
        }
        return false;
    }
}