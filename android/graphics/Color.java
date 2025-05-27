package android.graphics;

/**
 * Mock implementation of Android Color class for development outside of Android.
 * The Color class defines methods for creating and converting color values.
 */
public class Color {
    /**
     * Return the alpha component of a color int. This is the same as saying
     * color >>> 24
     */
    public static int alpha(int color) {
        return (color >> 24) & 0xFF;
    }
    
    /**
     * Return the red component of a color int. This is the same as saying
     * (color >> 16) & 0xFF
     */
    public static int red(int color) {
        return (color >> 16) & 0xFF;
    }
    
    /**
     * Return the green component of a color int. This is the same as saying
     * (color >> 8) & 0xFF
     */
    public static int green(int color) {
        return (color >> 8) & 0xFF;
    }
    
    /**
     * Return the blue component of a color int. This is the same as saying
     * color & 0xFF
     */
    public static int blue(int color) {
        return color & 0xFF;
    }
    
    /**
     * Return a color-int from alpha, red, green, blue components.
     * These component values should be [0..255], but there is no
     * range check performed, so if they are out of range, the
     * returned color is undefined.
     */
    public static int argb(int alpha, int red, int green, int blue) {
        return (alpha << 24) | (red << 16) | (green << 8) | blue;
    }
    
    /**
     * Return a color-int from red, green, blue components.
     * The alpha component is implicitly 255 (fully opaque).
     * These component values should be [0..255], but there is no
     * range check performed, so if they are out of range, the
     * returned color is undefined.
     */
    public static int rgb(int red, int green, int blue) {
        return (0xFF << 24) | (red << 16) | (green << 8) | blue;
    }
    
    /**
     * Convert RGB components to HSV.
     *   hsv[0] is Hue [0 .. 360)
     *   hsv[1] is Saturation [0...1]
     *   hsv[2] is Value [0...1]
     */
    public static void RGBToHSV(int red, int green, int blue, float[] hsv) {
        float rf = red / 255f;
        float gf = green / 255f;
        float bf = blue / 255f;
        
        float max = Math.max(rf, Math.max(gf, bf));
        float min = Math.min(rf, Math.min(gf, bf));
        float range = max - min;
        
        hsv[2] = max;
        hsv[1] = max == 0 ? 0 : range / max;
        
        if (hsv[1] == 0) {
            hsv[0] = 0;
        } else {
            float r = (max - rf) / range;
            float g = (max - gf) / range;
            float b = (max - bf) / range;
            
            float h;
            if (rf == max) {
                h = b - g;
            } else if (gf == max) {
                h = 2 + r - b;
            } else {
                h = 4 + g - r;
            }
            
            h *= 60;
            if (h < 0) {
                h += 360;
            }
            hsv[0] = h;
        }
    }
    
    /**
     * Convert the ARGB color to its HSV components.
     *   hsv[0] is Hue [0 .. 360)
     *   hsv[1] is Saturation [0...1]
     *   hsv[2] is Value [0...1]
     */
    public static void colorToHSV(int color, float[] hsv) {
        RGBToHSV((color >> 16) & 0xFF, (color >> 8) & 0xFF, color & 0xFF, hsv);
    }
    
    /**
     * Convert HSV components to an ARGB color. Alpha is set to 0xFF.
     *   hsv[0] is Hue [0 .. 360)
     *   hsv[1] is Saturation [0...1]
     *   hsv[2] is Value [0...1]
     */
    public static int HSVToColor(float[] hsv) {
        return HSVToColor(0xFF, hsv);
    }
    
    /**
     * Convert HSV components to an ARGB color. The alpha component is passed
     * through unchanged.
     *   hsv[0] is Hue [0 .. 360)
     *   hsv[1] is Saturation [0...1]
     *   hsv[2] is Value [0...1]
     */
    public static int HSVToColor(int alpha, float[] hsv) {
        float h = hsv[0];
        float s = hsv[1];
        float v = hsv[2];
        
        float c = v * s;
        float m = v - c;
        float x = c * (1 - Math.abs((h / 60f) % 2 - 1));
        
        float rf, gf, bf;
        
        if (h < 60) {
            rf = c;
            gf = x;
            bf = 0;
        } else if (h < 120) {
            rf = x;
            gf = c;
            bf = 0;
        } else if (h < 180) {
            rf = 0;
            gf = c;
            bf = x;
        } else if (h < 240) {
            rf = 0;
            gf = x;
            bf = c;
        } else if (h < 300) {
            rf = x;
            gf = 0;
            bf = c;
        } else {
            rf = c;
            gf = 0;
            bf = x;
        }
        
        int r = Math.round((rf + m) * 255);
        int g = Math.round((gf + m) * 255);
        int b = Math.round((bf + m) * 255);
        
        return (alpha << 24) | (r << 16) | (g << 8) | b;
    }
    
    // Common colors
    public static final int BLACK       = 0xFF000000;
    public static final int DKGRAY      = 0xFF444444;
    public static final int GRAY        = 0xFF888888;
    public static final int LTGRAY      = 0xFFCCCCCC;
    public static final int WHITE       = 0xFFFFFFFF;
    public static final int RED         = 0xFFFF0000;
    public static final int GREEN       = 0xFF00FF00;
    public static final int BLUE        = 0xFF0000FF;
    public static final int YELLOW      = 0xFFFFFF00;
    public static final int CYAN        = 0xFF00FFFF;
    public static final int MAGENTA     = 0xFFFF00FF;
    public static final int TRANSPARENT = 0;
}