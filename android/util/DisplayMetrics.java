package android.util;

/**
 * Mock implementation of Android DisplayMetrics class for development outside of Android.
 * A structure describing general information about a display, such as its size, density, and font scaling.
 */
public class DisplayMetrics {
    /**
     * The absolute width of the display in pixels.
     */
    public int widthPixels;
    
    /**
     * The absolute height of the display in pixels.
     */
    public int heightPixels;
    
    /**
     * The logical density of the display.
     */
    public float density;
    
    /**
     * The screen density expressed as dots-per-inch.
     */
    public int densityDpi;
    
    /**
     * The exact physical pixels per inch of the screen in the X dimension.
     */
    public float xdpi;
    
    /**
     * The exact physical pixels per inch of the screen in the Y dimension.
     */
    public float ydpi;
    
    /**
     * A scaling factor for fonts displayed on the display.
     */
    public float scaledDensity;
    
    /**
     * Width of the display in reference to the landscape orientation.
     */
    public int noncompatWidthPixels;
    
    /**
     * Height of the display in reference to the landscape orientation.
     */
    public int noncompatHeightPixels;
    
    /**
     * Construct default DisplayMetrics.
     */
    public DisplayMetrics() {
        // Default values for a typical phone display
        widthPixels = 1080;
        heightPixels = 1920;
        density = 2.0f;
        densityDpi = 320;
        xdpi = 320.0f;
        ydpi = 320.0f;
        scaledDensity = 2.0f;
        noncompatWidthPixels = 1080;
        noncompatHeightPixels = 1920;
    }
    
    /**
     * Copy the values from another DisplayMetrics.
     * 
     * @param metrics The DisplayMetrics to copy from.
     */
    public void setTo(DisplayMetrics metrics) {
        if (metrics == null) {
            return;
        }
        
        widthPixels = metrics.widthPixels;
        heightPixels = metrics.heightPixels;
        density = metrics.density;
        densityDpi = metrics.densityDpi;
        xdpi = metrics.xdpi;
        ydpi = metrics.ydpi;
        scaledDensity = metrics.scaledDensity;
        noncompatWidthPixels = metrics.noncompatWidthPixels;
        noncompatHeightPixels = metrics.noncompatHeightPixels;
    }
    
    /**
     * Return a nice readable representation of these metrics.
     */
    @Override
    public String toString() {
        return "DisplayMetrics{density=" + density + ", width=" + widthPixels +
               ", height=" + heightPixels + ", scaledDensity=" + scaledDensity +
               ", xdpi=" + xdpi + ", ydpi=" + ydpi + "}";
    }
    
    /**
     * Constant for density bucket: not specified.
     */
    public static final int DENSITY_DEFAULT = 0;
    
    /**
     * Constant for density bucket: low density screen.
     */
    public static final int DENSITY_LOW = 120;
    
    /**
     * Constant for density bucket: medium density screen.
     */
    public static final int DENSITY_MEDIUM = 160;
    
    /**
     * Constant for density bucket: high density screen.
     */
    public static final int DENSITY_HIGH = 240;
    
    /**
     * Constant for density bucket: extra high density screen.
     */
    public static final int DENSITY_XHIGH = 320;
    
    /**
     * Constant for density bucket: extra extra high density screen.
     */
    public static final int DENSITY_XXHIGH = 480;
    
    /**
     * Constant for density bucket: extra extra extra high density screen.
     */
    public static final int DENSITY_XXXHIGH = 640;
}