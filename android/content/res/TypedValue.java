package android.content.res;

import android.util.DisplayMetrics;

/**
 * Mock implementation of Android's TypedValue class.
 */
public class TypedValue {
    public static final int TYPE_NULL = 0;
    public static final int TYPE_REFERENCE = 1;
    public static final int TYPE_ATTRIBUTE = 2;
    public static final int TYPE_STRING = 3;
    public static final int TYPE_FLOAT = 4;
    public static final int TYPE_DIMENSION = 5;
    public static final int TYPE_FRACTION = 6;
    public static final int TYPE_INT_DEC = 16;
    public static final int TYPE_INT_HEX = 17;
    public static final int TYPE_INT_BOOLEAN = 18;
    public static final int TYPE_FIRST_COLOR_INT = 28;
    public static final int TYPE_LAST_COLOR_INT = 31;
    public static final int TYPE_FIRST_INT = 16;
    public static final int TYPE_LAST_INT = 31;
    
    /**
     * The value contains a dynamic reference, like @id/whatever. The resourceId
     * field is filled in with the resource identifier. The value field holds
     * the raw string value in the resources.
     */
    public static final int COMPLEX_MANTISSA_MASK = 0x00FFFFFF;
    
    /**
     * Shift value for the exponent part of a complex value.
     */
    public static final int COMPLEX_EXPONENT_SHIFT = 24;
    
    public static final int DENSITY_DEFAULT = 0;
    public static final int DENSITY_NONE = 0xffff;
    
    /**
     * The type held by this value, as defined by the constants here.
     * This tells you how to interpret the other fields in the object.
     */
    public int type;
    
    /**
     * If the value holds a reference, this is the resource identifier
     * that the reference is pointing to.
     */
    public int resourceId;
    
    /**
     * Basic data in the value.
     */
    public int data;
    
    /**
     * Additional information about where this value came from; currently
     * this is only used for describing the type of resource references:
     * {@link #RESOURCE_ID_TYPE}.
     */
    public int assetCookie;
    
    /**
     * If value holds a string, this is it.
     */
    public String string;
    
    /**
     * This holds the raw TypedValue that android uses.
     */
    public float density;
    
    /**
     * Convert the value to a string representation.
     */
    public String coerceToString() {
        if (type == TYPE_STRING) {
            return string != null ? string : "";
        } else if (type == TYPE_REFERENCE) {
            return "@" + resourceId;
        } else if (type == TYPE_ATTRIBUTE) {
            return "?" + resourceId;
        } else if (type == TYPE_FLOAT) {
            return Float.toString(Float.intBitsToFloat(data));
        } else if (type == TYPE_DIMENSION) {
            return Float.toString(Float.intBitsToFloat(data)) + "px";
        } else if (type == TYPE_INT_DEC || type == TYPE_INT_HEX || type == TYPE_INT_BOOLEAN) {
            return Integer.toString(data);
        } else if (type >= TYPE_FIRST_COLOR_INT && type <= TYPE_LAST_COLOR_INT) {
            return String.format("#%08x", data);
        }
        return "";
    }
    
    /**
     * Convert a complex data value holding a dimension to its final floating
     * point value. The given <var>data</var> must be structured as a
     * {@link #TYPE_DIMENSION}.
     * 
     * @param data The complex data value.
     * @param metrics Current display metrics to use in the conversion.
     * 
     * @return The complex floating point value multiplied by the appropriate 
     * metrics depending on the dimension type.
     */
    public static float complexToDimension(int data, DisplayMetrics metrics) {
        return applyDimension(
                (data >> COMPLEX_EXPONENT_SHIFT) & 0xff,
                data & COMPLEX_MANTISSA_MASK,
                metrics);
    }
    
    /**
     * Converts an unpacked complex data value holding a dimension to its final floating
     * point value. The two parameters <var>unit</var> and <var>value</var>
     * are as in {@link #TYPE_DIMENSION}.
     * 
     * @param unit The unit to convert from.
     * @param value The value to apply the unit to.
     * @param metrics Current display metrics to use in the conversion.
     * 
     * @return The complex floating point value multiplied by the appropriate 
     * metrics depending on the unit.
     */
    public static float applyDimension(int unit, float value, DisplayMetrics metrics) {
        switch (unit) {
            case 0: // TypedValue.COMPLEX_UNIT_PX
                return value;
            case 1: // TypedValue.COMPLEX_UNIT_DIP
                return value * metrics.density;
            case 2: // TypedValue.COMPLEX_UNIT_SP
                return value * metrics.scaledDensity;
            case 3: // TypedValue.COMPLEX_UNIT_PT
                return value * metrics.xdpi * (1.0f/72);
            case 4: // TypedValue.COMPLEX_UNIT_IN
                return value * metrics.xdpi;
            case 5: // TypedValue.COMPLEX_UNIT_MM
                return value * metrics.xdpi * (1.0f/25.4f);
        }
        return 0;
    }
    
    /**
     * Return the data for this value as a float.  Only use for values
     * whose type is {@link #TYPE_FLOAT}.
     */
    public float getFloat() {
        return Float.intBitsToFloat(data);
    }
}