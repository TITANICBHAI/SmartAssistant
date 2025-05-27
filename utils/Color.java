package utils;

/**
 * The Color class defines methods for creating and converting color integers.
 */
public class Color {
    // Common color constants
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
    public static final int TRANSPARENT = 0x00000000;
    
    /**
     * Create a color value with the specified alpha, red, green, and blue components.
     * These component values should be [0..255], but there is no range check.
     * 
     * @param alpha Alpha component [0..255]
     * @param red Red component [0..255]
     * @param green Green component [0..255]
     * @param blue Blue component [0..255]
     * @return A color int
     */
    public static int argb(int alpha, int red, int green, int blue) {
        return (alpha << 24) | (red << 16) | (green << 8) | blue;
    }
    
    /**
     * Create a color value with the specified red, green, and blue components
     * and an implied alpha value of 255.
     * These component values should be [0..255], but there is no range check.
     * 
     * @param red Red component [0..255]
     * @param green Green component [0..255]
     * @param blue Blue component [0..255]
     * @return A color int
     */
    public static int rgb(int red, int green, int blue) {
        return (0xFF << 24) | (red << 16) | (green << 8) | blue;
    }
    
    /**
     * Return the alpha component of a color int.
     * 
     * @param color The color int
     * @return The alpha component [0..255]
     */
    public static int alpha(int color) {
        return (color >> 24) & 0xFF;
    }
    
    /**
     * Return the red component of a color int.
     * 
     * @param color The color int
     * @return The red component [0..255]
     */
    public static int red(int color) {
        return (color >> 16) & 0xFF;
    }
    
    /**
     * Return the green component of a color int.
     * 
     * @param color The color int
     * @return The green component [0..255]
     */
    public static int green(int color) {
        return (color >> 8) & 0xFF;
    }
    
    /**
     * Return the blue component of a color int.
     * 
     * @param color The color int
     * @return The blue component [0..255]
     */
    public static int blue(int color) {
        return color & 0xFF;
    }
    
    /**
     * Creates a color from a hue, saturation, and value.
     * 
     * @param hue Hue component [0..360)
     * @param saturation Saturation component [0...1]
     * @param value Value component [0...1]
     * @return A color int
     */
    public static int HSVToColor(float hue, float saturation, float value) {
        float[] hsv = {hue, saturation, value};
        return HSVToColor(hsv);
    }
    
    /**
     * Creates a color from a hue, saturation, and value array.
     * 
     * @param hsv HSV components: [0] = hue [0..360), [1] = saturation [0...1], [2] = value [0...1]
     * @return A color int
     */
    public static int HSVToColor(float[] hsv) {
        return HSVToColor(255, hsv);
    }
    
    /**
     * Creates a color from alpha, hue, saturation, and value components.
     * 
     * @param alpha The alpha component [0..255]
     * @param hsv HSV components: [0] = hue [0..360), [1] = saturation [0...1], [2] = value [0...1]
     * @return A color int
     */
    public static int HSVToColor(int alpha, float[] hsv) {
        float h = hsv[0];
        float s = hsv[1];
        float v = hsv[2];
        
        // Handle edge cases
        if (s <= 0.0f) {
            int grey = (int) (v * 255.0f + 0.5f);
            return argb(alpha, grey, grey, grey);
        }
        
        if (h >= 360.0f) {
            h = 0.0f;
        } else {
            h /= 60.0f;
        }
        
        int i = (int) h;
        float f = h - i;
        float p = v * (1.0f - s);
        float q = v * (1.0f - s * f);
        float t = v * (1.0f - s * (1.0f - f));
        
        float r, g, b;
        switch (i) {
            case 0:
                r = v;
                g = t;
                b = p;
                break;
            case 1:
                r = q;
                g = v;
                b = p;
                break;
            case 2:
                r = p;
                g = v;
                b = t;
                break;
            case 3:
                r = p;
                g = q;
                b = v;
                break;
            case 4:
                r = t;
                g = p;
                b = v;
                break;
            case 5:
            default:
                r = v;
                g = p;
                b = q;
                break;
        }
        
        return argb(alpha, 
                   (int) (r * 255.0f + 0.5f), 
                   (int) (g * 255.0f + 0.5f),
                   (int) (b * 255.0f + 0.5f));
    }
    
    /**
     * Converts a color int to HSV components.
     * 
     * @param color The color int
     * @param hsv 3-element array to receive the HSV components
     */
    public static void colorToHSV(int color, float[] hsv) {
        int r = red(color);
        int g = green(color);
        int b = blue(color);
        
        int min = Math.min(r, Math.min(g, b));
        int max = Math.max(r, Math.max(g, b));
        int delta = max - min;
        
        float value = max / 255.0f;
        float saturation;
        float hue;
        
        if (max == 0) {
            hsv[0] = 0.0f;
            hsv[1] = 0.0f;
            hsv[2] = 0.0f;
            return;
        }
        
        saturation = delta / (float) max;
        
        if (delta == 0) {
            hue = 0.0f;
        } else {
            float hh;
            if (r == max) {
                hh = (float) (g - b) / delta;
                if (hh < 0) hh += 6;
            } else if (g == max) {
                hh = 2.0f + (float) (b - r) / delta;
            } else {
                hh = 4.0f + (float) (r - g) / delta;
            }
            hue = hh * 60.0f;
        }
        
        hsv[0] = hue;
        hsv[1] = saturation;
        hsv[2] = value;
    }
    
    /**
     * Creates a color with the specified color components and the given alpha value.
     * 
     * @param alpha The alpha component [0..255]
     * @param color The color int without alpha
     * @return A color int with the specified alpha
     */
    public static int setAlpha(int alpha, int color) {
        return (alpha << 24) | (color & 0x00FFFFFF);
    }
    
    /**
     * Parse the color string, and return the corresponding color-int.
     * Supported formats are:
     * #RRGGBB
     * #AARRGGBB
     * 
     * @param colorString The color string to parse
     * @return A color int
     * @throws IllegalArgumentException if the color string is invalid
     */
    public static int parseColor(String colorString) {
        if (colorString.charAt(0) == '#') {
            // Use a long to avoid rollovers on #ffXXXXXX
            long color = Long.parseLong(colorString.substring(1), 16);
            if (colorString.length() == 7) {
                // Set alpha component to 0xFF
                color |= 0x00000000FF000000L;
            } else if (colorString.length() != 9) {
                throw new IllegalArgumentException("Unknown color");
            }
            return (int) color;
        }
        throw new IllegalArgumentException("Unknown color");
    }
}