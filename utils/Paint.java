package utils;

/**
 * The Paint class holds the style and color information about how to draw geometries, text, and bitmaps.
 */
public class Paint {
    /**
     * Paint style: fill, stroke, or both
     */
    public enum Style {
        /**
         * Fill the shape
         */
        FILL,
        
        /**
         * Outline the shape
         */
        STROKE,
        
        /**
         * Fill and outline the shape
         */
        FILL_AND_STROKE
    }
    
    /**
     * Cap style: how to draw the endpoints of a stroked line
     */
    public enum Cap {
        /**
         * Square line end, extends beyond the endpoint by half stroke width
         */
        SQUARE,
        
        /**
         * Rounded line end, adds a semicircle at the endpoint
         */
        ROUND,
        
        /**
         * No extension beyond the endpoint
         */
        BUTT
    }
    
    /**
     * Join style: how to draw joins between line segments
     */
    public enum Join {
        /**
         * Join segments with a sharp corner
         */
        MITER,
        
        /**
         * Join segments with a rounded corner
         */
        ROUND,
        
        /**
         * Join segments with a beveled corner
         */
        BEVEL
    }
    
    /**
     * Text alignment: horizontal alignment of text
     */
    public enum Align {
        /**
         * Align text to the left
         */
        LEFT,
        
        /**
         * Align text to the center
         */
        CENTER,
        
        /**
         * Align text to the right
         */
        RIGHT
    }
    
    // Paint state
    private int mColor = Color.BLACK;
    private Style mStyle = Style.FILL;
    private Cap mCap = Cap.BUTT;
    private Join mJoin = Join.MITER;
    private float mStrokeWidth = 0;
    private float mTextSize = 12;
    private Align mAlign = Align.LEFT;
    private int mAlpha = 255;
    private boolean mAntiAlias = false;
    private boolean mDither = false;
    private boolean mFakeBoldText = false;
    private boolean mLinearText = false;
    private boolean mSubpixelText = false;
    private boolean mUnderlineText = false;
    private boolean mStrikeThruText = false;
    
    /**
     * Create a new paint with default settings
     */
    public Paint() {
    }
    
    /**
     * Create a new paint, initialized with the settings from the specified paint
     * @param paint The paint to copy settings from
     */
    public Paint(Paint paint) {
        if (paint != null) {
            mColor = paint.mColor;
            mStyle = paint.mStyle;
            mCap = paint.mCap;
            mJoin = paint.mJoin;
            mStrokeWidth = paint.mStrokeWidth;
            mTextSize = paint.mTextSize;
            mAlign = paint.mAlign;
            mAlpha = paint.mAlpha;
            mAntiAlias = paint.mAntiAlias;
            mDither = paint.mDither;
            mFakeBoldText = paint.mFakeBoldText;
            mLinearText = paint.mLinearText;
            mSubpixelText = paint.mSubpixelText;
            mUnderlineText = paint.mUnderlineText;
            mStrikeThruText = paint.mStrikeThruText;
        }
    }
    
    /**
     * Set the paint's color
     * @param color The new color
     */
    public void setColor(int color) {
        mColor = color;
    }
    
    /**
     * Get the paint's color
     * @return The paint's color
     */
    public int getColor() {
        return mColor;
    }
    
    /**
     * Set the paint's style
     * @param style The new style
     */
    public void setStyle(Style style) {
        mStyle = style;
    }
    
    /**
     * Get the paint's style
     * @return The paint's style
     */
    public Style getStyle() {
        return mStyle;
    }
    
    /**
     * Set the paint's stroke cap
     * @param cap The new cap
     */
    public void setStrokeCap(Cap cap) {
        mCap = cap;
    }
    
    /**
     * Get the paint's stroke cap
     * @return The paint's stroke cap
     */
    public Cap getStrokeCap() {
        return mCap;
    }
    
    /**
     * Set the paint's stroke join
     * @param join The new join
     */
    public void setStrokeJoin(Join join) {
        mJoin = join;
    }
    
    /**
     * Get the paint's stroke join
     * @return The paint's stroke join
     */
    public Join getStrokeJoin() {
        return mJoin;
    }
    
    /**
     * Set the paint's stroke width
     * @param width The new width
     */
    public void setStrokeWidth(float width) {
        mStrokeWidth = width;
    }
    
    /**
     * Get the paint's stroke width
     * @return The paint's stroke width
     */
    public float getStrokeWidth() {
        return mStrokeWidth;
    }
    
    /**
     * Set the paint's text size
     * @param textSize The new text size
     */
    public void setTextSize(float textSize) {
        mTextSize = textSize;
    }
    
    /**
     * Get the paint's text size
     * @return The paint's text size
     */
    public float getTextSize() {
        return mTextSize;
    }
    
    /**
     * Set the paint's text alignment
     * @param align The new alignment
     */
    public void setTextAlign(Align align) {
        mAlign = align;
    }
    
    /**
     * Get the paint's text alignment
     * @return The paint's text alignment
     */
    public Align getTextAlign() {
        return mAlign;
    }
    
    /**
     * Set the paint's alpha
     * @param alpha The new alpha [0..255]
     */
    public void setAlpha(int alpha) {
        mAlpha = alpha;
    }
    
    /**
     * Get the paint's alpha
     * @return The paint's alpha [0..255]
     */
    public int getAlpha() {
        return mAlpha;
    }
    
    /**
     * Set the paint's anti-alias setting
     * @param aa True to enable anti-aliasing
     */
    public void setAntiAlias(boolean aa) {
        mAntiAlias = aa;
    }
    
    /**
     * Get the paint's anti-alias setting
     * @return True if anti-aliasing is enabled
     */
    public boolean isAntiAlias() {
        return mAntiAlias;
    }
    
    /**
     * Set the paint's dithering setting
     * @param dither True to enable dithering
     */
    public void setDither(boolean dither) {
        mDither = dither;
    }
    
    /**
     * Get the paint's dithering setting
     * @return True if dithering is enabled
     */
    public boolean isDither() {
        return mDither;
    }
    
    /**
     * Set the paint's fake bold text setting
     * @param fakeBoldText True to enable fake bold text
     */
    public void setFakeBoldText(boolean fakeBoldText) {
        mFakeBoldText = fakeBoldText;
    }
    
    /**
     * Get the paint's fake bold text setting
     * @return True if fake bold text is enabled
     */
    public boolean isFakeBoldText() {
        return mFakeBoldText;
    }
    
    /**
     * Set the paint's linear text setting
     * @param linearText True to enable linear text
     */
    public void setLinearText(boolean linearText) {
        mLinearText = linearText;
    }
    
    /**
     * Get the paint's linear text setting
     * @return True if linear text is enabled
     */
    public boolean isLinearText() {
        return mLinearText;
    }
    
    /**
     * Set the paint's subpixel text setting
     * @param subpixelText True to enable subpixel text
     */
    public void setSubpixelText(boolean subpixelText) {
        mSubpixelText = subpixelText;
    }
    
    /**
     * Get the paint's subpixel text setting
     * @return True if subpixel text is enabled
     */
    public boolean isSubpixelText() {
        return mSubpixelText;
    }
    
    /**
     * Set the paint's underline text setting
     * @param underlineText True to enable underline text
     */
    public void setUnderlineText(boolean underlineText) {
        mUnderlineText = underlineText;
    }
    
    /**
     * Get the paint's underline text setting
     * @return True if underline text is enabled
     */
    public boolean isUnderlineText() {
        return mUnderlineText;
    }
    
    /**
     * Set the paint's strike-through text setting
     * @param strikeThruText True to enable strike-through text
     */
    public void setStrikeThruText(boolean strikeThruText) {
        mStrikeThruText = strikeThruText;
    }
    
    /**
     * Get the paint's strike-through text setting
     * @return True if strike-through text is enabled
     */
    public boolean isStrikeThruText() {
        return mStrikeThruText;
    }
    
    /**
     * Set the paint's shader
     * @param shader The new shader (currently not implemented in this mock)
     */
    public void setShader(Object shader) {
        // Not implemented in this mock
    }
    
    /**
     * Set the paint's color filter
     * @param filter The new color filter (currently not implemented in this mock)
     */
    public void setColorFilter(Object filter) {
        // Not implemented in this mock
    }
    
    /**
     * Set the paint's xfermode
     * @param xfermode The new xfermode (currently not implemented in this mock)
     */
    public void setXfermode(Object xfermode) {
        // Not implemented in this mock
    }
    
    /**
     * Set the paint's path effect
     * @param effect The new path effect (currently not implemented in this mock)
     */
    public void setPathEffect(Object effect) {
        // Not implemented in this mock
    }
    
    /**
     * Set the paint's mask filter
     * @param filter The new mask filter (currently not implemented in this mock)
     */
    public void setMaskFilter(Object filter) {
        // Not implemented in this mock
    }
    
    /**
     * Set the paint's typeface
     * @param typeface The new typeface (currently not implemented in this mock)
     */
    public void setTypeface(Object typeface) {
        // Not implemented in this mock
    }
    
    /**
     * Return a copy of this paint
     * @return A new paint with the same settings as this one
     */
    public Paint copy() {
        return new Paint(this);
    }
}