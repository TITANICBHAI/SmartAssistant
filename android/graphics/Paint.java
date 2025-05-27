package android.graphics;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

/**
 * Mock implementation of Android Paint class for development outside of Android.
 * The Paint class holds the style and color information about how to draw geometries, text and bitmaps.
 */
public class Paint {
    /**
     * Paint flag that enables antialiasing when drawing.
     */
    public static final int ANTI_ALIAS_FLAG = 0x01;
    
    /**
     * Paint flag that enables bilinear sampling on scaled bitmaps.
     */
    public static final int FILTER_BITMAP_FLAG = 0x02;
    
    /**
     * Paint flag that enables dithering.
     */
    public static final int DITHER_FLAG = 0x04;
    
    /**
     * Paint flag that enables underline text.
     */
    public static final int UNDERLINE_TEXT_FLAG = 0x08;
    
    /**
     * Paint flag that enables strike-through text.
     */
    public static final int STRIKE_THRU_TEXT_FLAG = 0x10;
    
    /**
     * Paint flag that enables fake bold text.
     */
    public static final int FAKE_BOLD_TEXT_FLAG = 0x20;
    
    /**
     * Paint flag that enables subpixel positioning of text.
     */
    public static final int SUBPIXEL_TEXT_FLAG = 0x80;
    
    /**
     * Paint flag that enables device kerning when drawing text.
     */
    public static final int LINEAR_TEXT_FLAG = 0x40;
    
    /**
     * Paint flag that enables EmbossMask.
     */
    public static final int DEV_KERN_TEXT_FLAG = 0x100;
    
    /**
     * Align text at the left of the position.
     */
    public enum Align {
        LEFT, CENTER, RIGHT
    }
    
    /**
     * Enum of drawing style.
     */
    public enum Style {
        /**
         * Draw with only fill.
         */
        FILL,
        /**
         * Draw with only stroke.
         */
        STROKE,
        /**
         * Draw with fill and stroke.
         */
        FILL_AND_STROKE
    }
    
    /**
     * Enum of stroke cap.
     */
    public enum Cap {
        /**
         * The stroke has a flat cap.
         */
        BUTT,
        /**
         * The stroke has a circular cap.
         */
        ROUND,
        /**
         * The stroke has a square cap.
         */
        SQUARE
    }
    
    /**
     * Enum of stroke join.
     */
    public enum Join {
        /**
         * The stroke has a miter join.
         */
        MITER,
        /**
         * The stroke has a round join.
         */
        ROUND,
        /**
         * The stroke has a bevel join.
         */
        BEVEL
    }
    
    private int flags;
    private Style style = Style.FILL;
    private Cap strokeCap = Cap.BUTT;
    private Join strokeJoin = Join.MITER;
    private float strokeWidth = 0;
    private float strokeMiter = 4;
    private int color = 0xFF000000; // Black
    private float textSize = 12;
    private Align textAlign = Align.LEFT;
    private ColorFilter colorFilter;
    private Shader shader;
    private MaskFilter maskFilter;
    private boolean filterBitmap;
    private boolean antialias;
    private int alpha = 255;
    
    /**
     * Create a new paint with default settings.
     */
    public Paint() {
        this(0);
    }
    
    /**
     * Create a new paint with the specified flags.
     * 
     * @param flags Initial flag bits
     */
    public Paint(int flags) {
        this.flags = flags;
        antialias = (flags & ANTI_ALIAS_FLAG) != 0;
        filterBitmap = (flags & FILTER_BITMAP_FLAG) != 0;
    }
    
    /**
     * Create a new paint, initialized with the attributes in the specified paint parameter.
     * 
     * @param paint The paint to copy style and attributes from
     */
    public Paint(@NonNull Paint paint) {
        flags = paint.flags;
        style = paint.style;
        strokeCap = paint.strokeCap;
        strokeJoin = paint.strokeJoin;
        strokeWidth = paint.strokeWidth;
        strokeMiter = paint.strokeMiter;
        color = paint.color;
        textSize = paint.textSize;
        textAlign = paint.textAlign;
        colorFilter = paint.colorFilter;
        shader = paint.shader;
        maskFilter = paint.maskFilter;
        filterBitmap = paint.filterBitmap;
        antialias = paint.antialias;
        alpha = paint.alpha;
    }
    
    /**
     * Set the paint's flags
     * 
     * @param flags The new flag bits
     */
    public void setFlags(int flags) {
        this.flags = flags;
        antialias = (flags & ANTI_ALIAS_FLAG) != 0;
        filterBitmap = (flags & FILTER_BITMAP_FLAG) != 0;
    }
    
    /**
     * Return the paint's flags
     * 
     * @return The flag bits
     */
    public int getFlags() {
        return flags;
    }
    
    /**
     * Helper to set the flags indicating antialiasing
     * 
     * @param aa True if antialiasing should be enabled
     */
    public void setAntiAlias(boolean aa) {
        antialias = aa;
        flags = (flags & ~ANTI_ALIAS_FLAG) | (aa ? ANTI_ALIAS_FLAG : 0);
    }
    
    /**
     * Return the paint's antialiasing flag
     * 
     * @return True if antialiasing is enabled
     */
    public boolean isAntiAlias() {
        return antialias;
    }
    
    /**
     * Helper to set the flags indicating filtering bitmap scaling/drawing
     * 
     * @param filter True if filtering should be enabled
     */
    public void setFilterBitmap(boolean filter) {
        filterBitmap = filter;
        flags = (flags & ~FILTER_BITMAP_FLAG) | (filter ? FILTER_BITMAP_FLAG : 0);
    }
    
    /**
     * Return the paint's filter flag
     * 
     * @return True if filtering is enabled
     */
    public boolean isFilterBitmap() {
        return filterBitmap;
    }
    
    /**
     * Set the paint's color
     * 
     * @param color The new color (including alpha)
     */
    public void setColor(int color) {
        this.color = color;
    }
    
    /**
     * Return the paint's color
     * 
     * @return The color (including alpha)
     */
    public int getColor() {
        return color;
    }
    
    /**
     * Set the paint's alpha. This affects the alpha bits of the color.
     * 
     * @param alpha The new alpha, between 0 (transparent) and 255 (opaque)
     */
    public void setAlpha(int alpha) {
        this.alpha = alpha;
        color = (color & 0x00FFFFFF) | (alpha << 24);
    }
    
    /**
     * Return the paint's alpha
     * 
     * @return The alpha component of the paint's color, between 0 (transparent) and 255 (opaque)
     */
    public int getAlpha() {
        return alpha;
    }
    
    /**
     * Set the paint's style
     * 
     * @param style The new style
     */
    public void setStyle(Style style) {
        this.style = style;
    }
    
    /**
     * Return the paint's style
     * 
     * @return The style
     */
    public Style getStyle() {
        return style;
    }
    
    /**
     * Set the paint's stroke width
     * 
     * @param width The new stroke width
     */
    public void setStrokeWidth(float width) {
        strokeWidth = width;
    }
    
    /**
     * Return the paint's stroke width
     * 
     * @return The stroke width
     */
    public float getStrokeWidth() {
        return strokeWidth;
    }
    
    /**
     * Set the paint's stroke cap
     * 
     * @param cap The new stroke cap
     */
    public void setStrokeCap(Cap cap) {
        strokeCap = cap;
    }
    
    /**
     * Return the paint's stroke cap
     * 
     * @return The stroke cap
     */
    public Cap getStrokeCap() {
        return strokeCap;
    }
    
    /**
     * Set the paint's stroke join
     * 
     * @param join The new stroke join
     */
    public void setStrokeJoin(Join join) {
        strokeJoin = join;
    }
    
    /**
     * Return the paint's stroke join
     * 
     * @return The stroke join
     */
    public Join getStrokeJoin() {
        return strokeJoin;
    }
    
    /**
     * Set the paint's stroke miter value
     * 
     * @param miter The new stroke miter value
     */
    public void setStrokeMiter(float miter) {
        strokeMiter = miter;
    }
    
    /**
     * Return the paint's stroke miter value
     * 
     * @return The stroke miter value
     */
    public float getStrokeMiter() {
        return strokeMiter;
    }
    
    /**
     * Set the paint's text size
     * 
     * @param textSize The new text size
     */
    public void setTextSize(float textSize) {
        this.textSize = textSize;
    }
    
    /**
     * Return the paint's text size
     * 
     * @return The text size
     */
    public float getTextSize() {
        return textSize;
    }
    
    /**
     * Set the paint's text alignment
     * 
     * @param align The new text alignment
     */
    public void setTextAlign(Align align) {
        textAlign = align;
    }
    
    /**
     * Return the paint's text alignment
     * 
     * @return The text alignment
     */
    public Align getTextAlign() {
        return textAlign;
    }
    
    /**
     * Set the paint's color filter
     * 
     * @param filter The new color filter (null to clear)
     */
    public void setColorFilter(@Nullable ColorFilter filter) {
        colorFilter = filter;
    }
    
    /**
     * Return the paint's color filter
     * 
     * @return The color filter
     */
    @Nullable
    public ColorFilter getColorFilter() {
        return colorFilter;
    }
    
    /**
     * Set the paint's shader
     * 
     * @param shader The new shader (null to clear)
     */
    public void setShader(@Nullable Shader shader) {
        this.shader = shader;
    }
    
    /**
     * Return the paint's shader
     * 
     * @return The shader
     */
    @Nullable
    public Shader getShader() {
        return shader;
    }
    
    /**
     * Set the paint's mask filter
     * 
     * @param filter The new mask filter (null to clear)
     */
    public void setMaskFilter(@Nullable MaskFilter filter) {
        maskFilter = filter;
    }
    
    /**
     * Return the paint's mask filter
     * 
     * @return The mask filter
     */
    @Nullable
    public MaskFilter getMaskFilter() {
        return maskFilter;
    }
    
    /**
     * Reset the paint to its default settings
     */
    public void reset() {
        flags = 0;
        style = Style.FILL;
        strokeCap = Cap.BUTT;
        strokeJoin = Join.MITER;
        strokeWidth = 0;
        strokeMiter = 4;
        color = 0xFF000000; // Black
        textSize = 12;
        textAlign = Align.LEFT;
        colorFilter = null;
        shader = null;
        maskFilter = null;
        filterBitmap = false;
        antialias = false;
        alpha = 255;
    }
    
    /**
     * Returns the width of the text
     * 
     * @param text The text
     * @return The width
     */
    public float measureText(String text) {
        // Simple implementation assuming monospace and ignoring size
        return text.length() * textSize * 0.6f;
    }
    
    /**
     * Returns the width of the text
     * 
     * @param text The text
     * @param index The start index
     * @param count The number of characters
     * @return The width
     */
    public float measureText(String text, int index, int count) {
        return measureText(text.substring(index, index + count));
    }
    
    /**
     * Returns the width of the text
     * 
     * @param text The text
     * @param index The start index
     * @param count The number of characters
     * @return The width
     */
    public float measureText(char[] text, int index, int count) {
        return measureText(new String(text, index, count));
    }
    
    /**
     * A specialized version of Shader that returns a shader that draws a linear gradient.
     */
    public static class LinearGradient extends Shader {
        private final float x0, y0, x1, y1;
        private final int color0, color1;
        private final TileMode tileMode;
        
        /**
         * Create a shader that draws a linear gradient.
         * 
         * @param x0 The x-coordinate for the start point
         * @param y0 The y-coordinate for the start point
         * @param x1 The x-coordinate for the end point
         * @param y1 The y-coordinate for the end point
         * @param color0 The color at the start point
         * @param color1 The color at the end point
         * @param tileMode The tile mode
         */
        public LinearGradient(float x0, float y0, float x1, float y1, 
                int color0, int color1, TileMode tileMode) {
            this.x0 = x0;
            this.y0 = y0;
            this.x1 = x1;
            this.y1 = y1;
            this.color0 = color0;
            this.color1 = color1;
            this.tileMode = tileMode;
        }
        
        /**
         * Create a shader that draws a linear gradient.
         * 
         * @param x0 The x-coordinate for the start point
         * @param y0 The y-coordinate for the start point
         * @param x1 The x-coordinate for the end point
         * @param y1 The y-coordinate for the end point
         * @param colors The array of colors
         * @param positions The array of positions
         * @param tileMode The tile mode
         */
        public LinearGradient(float x0, float y0, float x1, float y1, 
                int[] colors, float[] positions, TileMode tileMode) {
            this.x0 = x0;
            this.y0 = y0;
            this.x1 = x1;
            this.y1 = y1;
            this.color0 = colors[0];
            this.color1 = colors[colors.length - 1];
            this.tileMode = tileMode;
        }
    }
    
    /**
     * Base class for Shader implementations
     */
    public static class Shader {
        /**
         * Enum of tiling modes
         */
        public enum TileMode {
            /**
             * Replicate the edge color if the shader draws outside of its bounds
             */
            CLAMP,
            /**
             * Repeat the shader's image horizontally and vertically
             */
            REPEAT,
            /**
             * Mirror the shader's image horizontally and vertically
             */
            MIRROR
        }
    }
    
    /**
     * Base class for MaskFilter implementations
     */
    public static class MaskFilter {
    }
    
    /**
     * A MaskFilter that creates a blur effect
     */
    public static class BlurMaskFilter extends MaskFilter {
        /**
         * Enum of blur styles
         */
        public enum Blur {
            /**
             * Normal blur
             */
            NORMAL,
            /**
             * Solid inner blur
             */
            SOLID,
            /**
             * Outer blur
             */
            OUTER,
            /**
             * Inner blur
             */
            INNER
        }
        
        private final float radius;
        private final Blur style;
        
        /**
         * Create a blur mask filter
         * 
         * @param radius The radius of the blur
         * @param style The blur style
         */
        public BlurMaskFilter(float radius, Blur style) {
            this.radius = radius;
            this.style = style;
        }
    }
}