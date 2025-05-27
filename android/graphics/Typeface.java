package android.graphics;

/**
 * Mock implementation of Android's Typeface class
 */
public class Typeface {
    /**
     * The normal typeface style
     */
    public static final int NORMAL = 0;
    
    /**
     * The bold typeface style
     */
    public static final int BOLD = 1;
    
    /**
     * The italic typeface style
     */
    public static final int ITALIC = 2;
    
    /**
     * The bold italic typeface style
     */
    public static final int BOLD_ITALIC = 3;
    
    /**
     * The default NORMAL typeface
     */
    public static final Typeface DEFAULT = new Typeface(NORMAL);
    
    /**
     * The default BOLD typeface
     */
    public static final Typeface DEFAULT_BOLD = new Typeface(BOLD);
    
    /**
     * The Monospace typeface
     */
    public static final Typeface MONOSPACE = new Typeface(NORMAL);
    
    /**
     * The Serif typeface
     */
    public static final Typeface SERIF = new Typeface(NORMAL);
    
    /**
     * The Sans Serif typeface
     */
    public static final Typeface SANS_SERIF = new Typeface(NORMAL);
    
    private final int style;
    
    /**
     * Create a new typeface with the specified style
     * @param style The style (NORMAL, BOLD, ITALIC, BOLD_ITALIC)
     */
    private Typeface(int style) {
        this.style = style;
    }
    
    /**
     * Create a typeface from an existing typeface but a new style
     * @param family The family typeface
     * @param style The style (NORMAL, BOLD, ITALIC, BOLD_ITALIC)
     * @return The new typeface
     */
    public static Typeface create(Typeface family, int style) {
        return new Typeface(style);
    }
    
    /**
     * Create a typeface from a font family and style
     * @param familyName The font family name
     * @param style The style (NORMAL, BOLD, ITALIC, BOLD_ITALIC)
     * @return The new typeface
     */
    public static Typeface create(String familyName, int style) {
        return new Typeface(style);
    }
    
    /**
     * Create a typeface from a file
     * @param path The file path
     * @return The new typeface
     */
    public static Typeface createFromFile(String path) {
        return new Typeface(NORMAL);
    }
    
    /**
     * Create a typeface from an asset
     * @param mgr The asset manager
     * @param path The asset path
     * @return The new typeface
     */
    public static Typeface createFromAsset(android.content.res.AssetManager mgr, String path) {
        return new Typeface(NORMAL);
    }
    
    /**
     * Get the style of this typeface
     * @return The style
     */
    public int getStyle() {
        return style;
    }
    
    /**
     * Check if the typeface is bold
     * @return True if bold
     */
    public boolean isBold() {
        return (style & BOLD) != 0;
    }
    
    /**
     * Check if the typeface is italic
     * @return True if italic
     */
    public boolean isItalic() {
        return (style & ITALIC) != 0;
    }
}