package android.util;

/**
 * Container for a dynamically typed data value. The value may be
 * retrieved in a variety of ways based on its type.
 * 
 * This is a simple mock implementation for the purposes of simulating
 * the Android SDK TypedValue class.
 */
public class TypedValue {
    /**
     * The value contains no data.
     */
    public static final int TYPE_NULL = 0x00;
    
    /**
     * The value contains a reference to another resource.
     */
    public static final int TYPE_REFERENCE = 0x01;
    
    /**
     * The value contains a string.
     */
    public static final int TYPE_STRING = 0x03;
    
    /**
     * The value contains a floating point number.
     */
    public static final int TYPE_FLOAT = 0x04;
    
    /**
     * The value contains a dimension.
     */
    public static final int TYPE_DIMENSION = 0x05;
    
    /**
     * The value contains a fraction.
     */
    public static final int TYPE_FRACTION = 0x06;
    
    /**
     * The value contains a dynamic reference to another resource.
     */
    public static final int TYPE_DYNAMIC_REFERENCE = 0x07;
    
    /**
     * The value contains a dynamic attribute resource.
     */
    public static final int TYPE_ATTRIBUTE = 0x08;
    
    /**
     * The value contains an integer.
     */
    public static final int TYPE_INT_DEC = 0x10;
    
    /**
     * The value contains a hexadecimal integer.
     */
    public static final int TYPE_INT_HEX = 0x11;
    
    /**
     * The value contains a boolean integer.
     */
    public static final int TYPE_INT_BOOLEAN = 0x12;
    
    /**
     * The value contains a color integer.
     */
    public static final int TYPE_INT_COLOR_ARGB8 = 0x1c;
    
    /**
     * The value contains a color integer (similar to TYPE_INT_COLOR_ARGB8
     * but different endian).
     */
    public static final int TYPE_INT_COLOR_RGB8 = 0x1d;
    
    /**
     * The value contains a color integer (similar to TYPE_INT_COLOR_ARGB8
     * but different endian).
     */
    public static final int TYPE_INT_COLOR_ARGB4 = 0x1e;
    
    /**
     * The value contains a color integer (similar to TYPE_INT_COLOR_ARGB8
     * but different endian).
     */
    public static final int TYPE_INT_COLOR_RGB4 = 0x1f;
    
    /**
     * The complex data for the dimension units.
     */
    public static final int COMPLEX_UNIT_MASK = 0xf;
    
    /**
     * The complex unit for dimension values.
     */
    public static final int COMPLEX_UNIT_PX = 0;
    
    /**
     * The complex unit for dimension values.
     */
    public static final int COMPLEX_UNIT_DIP = 1;
    
    /**
     * The complex unit for dimension values.
     */
    public static final int COMPLEX_UNIT_SP = 2;
    
    /**
     * The complex unit for dimension values.
     */
    public static final int COMPLEX_UNIT_PT = 3;
    
    /**
     * The complex unit for dimension values.
     */
    public static final int COMPLEX_UNIT_IN = 4;
    
    /**
     * The complex unit for dimension values.
     */
    public static final int COMPLEX_UNIT_MM = 5;
    
    /**
     * The type held by this value, as defined by the constants here.
     */
    public int type;
    
    /**
     * If the value holds a string, this is it.
     */
    public CharSequence string;
    
    /**
     * Basic data in the value, interpreted according to type.
     */
    public int data;
    
    /**
     * Additional information about the value.
     */
    public int assetCookie;
    
    /**
     * If the type is TYPE_STRING, this holds the string resource id.
     */
    public int resourceId;
    
    /**
     * If the type is TYPE_DIMENSION, the value contains the raw dimension data.
     */
    public float density;
    
    /**
     * If the type is TYPE_FLOAT, this holds the float value.
     */
    public float getFloat() {
        return Float.intBitsToFloat(data);
    }
    
    /**
     * If the type is TYPE_DIMENSION, this returns the float value.
     */
    public float getDimension(DisplayMetrics metrics) {
        return 0.0f;
    }
    
    /**
     * If the type is TYPE_FRACTION, this returns the float value.
     */
    public float getFraction(float base, float pbase) {
        return 0.0f;
    }
    
    /**
     * Retrieve the base value from a complex data integer.
     */
    public static float complexToFloat(int complex) {
        return 0.0f;
    }
    
    /**
     * Retrieve the raw integer data from a complex data integer.
     */
    public static int complexToDimensionPixelOffset(int data, DisplayMetrics metrics) {
        return 0;
    }
    
    /**
     * Retrieve the raw integer data from a complex data integer.
     */
    public static int complexToDimensionPixelSize(int data, DisplayMetrics metrics) {
        return 0;
    }
    
    /**
     * Converts a complex data value holding a dimension to its floating
     * point pixel equivalent.
     */
    public static float complexToDimension(int data, DisplayMetrics metrics) {
        return 0.0f;
    }
    
    /**
     * Converts an unpacked complex data value holding a dimension to its
     * floating point pixel equivalent.
     */
    public static float applyDimension(int unit, float value, DisplayMetrics metrics) {
        switch (unit) {
            case COMPLEX_UNIT_PX:
                return value;
            case COMPLEX_UNIT_DIP:
                return value * metrics.density;
            case COMPLEX_UNIT_SP:
                return value * metrics.scaledDensity;
            case COMPLEX_UNIT_PT:
                return value * metrics.xdpi * (1.0f/72);
            case COMPLEX_UNIT_IN:
                return value * metrics.xdpi;
            case COMPLEX_UNIT_MM:
                return value * metrics.xdpi * (1.0f/25.4f);
        }
        return 0;
    }
}