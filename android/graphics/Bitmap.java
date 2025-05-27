package android.graphics;

import java.io.OutputStream;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

/**
 * Mock implementation of Android Bitmap class for development outside of Android.
 * The Bitmap class represents a bitmap image for drawing on a Canvas.
 */
public class Bitmap {
    
    /**
     * Indicates that the bitmap was created for an unknown pixel density.
     */
    public static final int DENSITY_NONE = 0;
    
    /**
     * Indicates that the bitmap was created for the default pixel density of the display.
     */
    public static final int DENSITY_DEFAULT = 160;
    
    /**
     * Possible bitmap compression formats
     */
    public enum CompressFormat {
        /**
         * Compressed to a PNG format
         */
        PNG(0),
        /**
         * Compressed to a JPEG format
         */
        JPEG(1),
        /**
         * Compressed to a WEBP format
         */
        WEBP(2),
        /**
         * Compressed to a WEBP lossless format
         */
        WEBP_LOSSLESS(3);
        
        CompressFormat(int nativeInt) {
            this.nativeInt = nativeInt;
        }
        
        final int nativeInt;
    }
    
    /**
     * Possible bitmap configs
     */
    public enum Config {
        /**
         * Each pixel is stored as a single translucency (alpha) channel.
         * This is very useful to efficiently store masks for instance.
         * No color information is stored.
         * With this configuration, each pixel requires 1 byte of memory.
         */
        ALPHA_8(1),
        
        /**
         * Each pixel is stored on 2 bytes and only the RGB channels are encoded:
         * red is stored with 5 bits of precision (32 possible values),
         * green is stored with 6 bits of precision (64 possible values) and
         * blue is stored with 5 bits of precision (32 possible values).
         * This configuration can produce slight visual artifacts depending on the
         * configuration of the source. For instance, without dithering, the
         * result might show a greenish tint. To get better results dithering
         * should be applied.
         * This configuration may be useful when using opaque bitmaps that do
         * not require high color fidelity.
         */
        RGB_565(3),
        
        /**
         * Each pixel is stored on 2 bytes. The three RGB color channels
         * and the alpha channel (translucency) are stored with a 4 bits
         * precision (16 possible values.)
         * This configuration is mostly useful if the application needs
         * to store translucency information but also needs to save memory.
         * It is recommended to use ARGB_8888 instead of this configuration.
         */
        ARGB_4444(4),
        
        /**
         * Each pixel is stored on 4 bytes. Each channel (RGB and alpha
         * for translucency) is stored with 8 bits of precision (256
         * possible values.)
         * This configuration is very flexible and offers the best
         * quality. It should be used whenever possible.
         */
        ARGB_8888(5),
        
        /**
         * Each pixel is stored on 8 bytes. Each channel (RGB and alpha
         * for translucency) is stored as a 64bit floating point value.
         * This configuration is particularly suited for wide-gamut and
         * HDR content.
         */
        RGBA_F16(6),
        
        /**
         * Special configuration, when bitmap is stored only in graphic memory.
         * This configuration can be used only with getBitmap() method.
         */
        HARDWARE(7);
        
        Config(int nativeInt) {
            this.nativeInt = nativeInt;
        }
        
        final int nativeInt;
    }
    
    private final int width;
    private final int height;
    private final Config config;
    private boolean mutable;
    private boolean recycled;
    private int mDensity = DENSITY_DEFAULT;
    
    private Bitmap(int width, int height, Config config) {
        this.width = width;
        this.height = height;
        this.config = config;
        this.mutable = true;
        this.recycled = false;
    }
    
    /**
     * Returns a new bitmap with the specified width and height.
     * 
     * @param width The width of the bitmap
     * @param height The height of the bitmap
     * @param config The bitmap config to create
     * @return The created bitmap
     */
    @NonNull
    public static Bitmap createBitmap(int width, int height, @NonNull Config config) {
        if (width <= 0 || height <= 0) {
            throw new IllegalArgumentException("width and height must be > 0");
        }
        return new Bitmap(width, height, config);
    }
    
    /**
     * Returns a mutable bitmap with the specified width and height.
     * 
     * @param width The width of the bitmap
     * @param height The height of the bitmap
     * @return The created bitmap
     */
    @NonNull
    public static Bitmap createBitmap(int width, int height) {
        return createBitmap(width, height, Config.ARGB_8888);
    }
    
    /**
     * Returns a immutable bitmap from the source bitmap. The new bitmap may
     * be the same object as source, or a copy may have been made.
     * 
     * @param src The bitmap to copy from
     * @return The immutable bitmap
     */
    @NonNull
    public static Bitmap createBitmap(@NonNull Bitmap src) {
        Bitmap bitmap = new Bitmap(src.getWidth(), src.getHeight(), src.getConfig());
        bitmap.mutable = false;
        return bitmap;
    }
    
    /**
     * Returns an immutable bitmap from the specified subset of the source
     * bitmap. The new bitmap may be the same object as source, or a copy may
     * have been made.
     * 
     * @param source The bitmap to subsample
     * @param x The x coordinate of the first pixel in source
     * @param y The y coordinate of the first pixel in source
     * @param width The number of pixels in each row
     * @param height The number of rows
     * @return The immutable bitmap
     */
    @NonNull
    public static Bitmap createBitmap(@NonNull Bitmap source, int x, int y, int width, int height) {
        if (x + width > source.getWidth()) {
            throw new IllegalArgumentException("x + width must be <= bitmap.width()");
        }
        if (y + height > source.getHeight()) {
            throw new IllegalArgumentException("y + height must be <= bitmap.height()");
        }
        
        Bitmap bitmap = new Bitmap(width, height, source.getConfig());
        bitmap.mutable = false;
        return bitmap;
    }
    
    /**
     * Returns an immutable bitmap from subset of the source bitmap,
     * transformed by the optional matrix.
     * 
     * @param source The bitmap to subsample
     * @param x The x coordinate of the first pixel in source
     * @param y The y coordinate of the first pixel in source
     * @param width The number of pixels in each row
     * @param height The number of rows
     * @param m Optional matrix to be applied to the pixels
     * @param filter true if the source should be filtered.
     * @return The immutable bitmap
     */
    @NonNull
    public static Bitmap createBitmap(@NonNull Bitmap source, int x, int y, int width, int height,
            @Nullable Matrix m, boolean filter) {
        if (x + width > source.getWidth()) {
            throw new IllegalArgumentException("x + width must be <= bitmap.width()");
        }
        if (y + height > source.getHeight()) {
            throw new IllegalArgumentException("y + height must be <= bitmap.height()");
        }
        
        Bitmap bitmap = new Bitmap(width, height, source.getConfig());
        bitmap.mutable = false;
        return bitmap;
    }
    
    /**
     * Creates a bitmap from the given width, height, and color.
     * 
     * @param width The width of the bitmap
     * @param height The height of the bitmap
     * @param color The color to fill the bitmap
     * @return The new bitmap
     */
    @NonNull
    public static Bitmap createBitmap(int width, int height, int color) {
        Bitmap bitmap = createBitmap(width, height, Config.ARGB_8888);
        bitmap.eraseColor(color);
        return bitmap;
    }
    
    // Method removed to avoid duplication
    // The createBitmap(int, int, Config) method is already defined earlier in the class
    
    /**
     * Creates a mutable bitmap from the source bitmap.
     *
     * @param source The source bitmap
     * @param destWidth The new bitmap's desired width
     * @param destHeight The new bitmap's desired height
     * @param filter true if the source should be filtered
     * @return The new scaled bitmap
     */
    @NonNull
    public static Bitmap createScaledBitmap(@NonNull Bitmap source,
            int destWidth, int destHeight, boolean filter) {
        Bitmap bitmap = createBitmap(destWidth, destHeight, source.getConfig());
        return bitmap;
    }
    
    /**
     * Creates a bitmap from the specified subset of the source
     * pixels. The new bitmap may be the same object as source, or a copy may
     * have been made. 
     *
     * @param colors The colors to write to the bitmap
     * @param offset The index of the first color to read from colors[]
     * @param stride The number of colors in pixels[] to skip between rows (must be >= width or <= -width)
     * @param width The width of the bitmap
     * @param height The height of the bitmap
     * @param config The bitmap config to create
     * @return The new bitmap
     */
    @NonNull
    public static Bitmap createBitmap(@NonNull int[] colors, int offset, int stride,
            int width, int height, Config config) {
        if (width <= 0) {
            throw new IllegalArgumentException("width must be > 0");
        }
        if (height <= 0) {
            throw new IllegalArgumentException("height must be > 0");
        }
        if (Math.abs(stride) < width) {
            throw new IllegalArgumentException("abs(stride) must be >= width");
        }
        
        Bitmap bitmap = new Bitmap(width, height, config);
        return bitmap;
    }
    
    /**
     * Creates a bitmap from an array of colors. The new bitmap will be immutable.
     *
     * @param colors The colors to create the bitmap from
     * @param width The width of the bitmap
     * @param height The height of the bitmap
     * @param config The bitmap config to create
     * @return The new bitmap
     * @throws IllegalArgumentException if the width or height is <= 0
     */
    @NonNull
    public static Bitmap createBitmap(@NonNull int[] colors, int width, int height, Config config) {
        if (width <= 0) {
            throw new IllegalArgumentException("width must be > 0");
        }
        if (height <= 0) {
            throw new IllegalArgumentException("height must be > 0");
        }
        if (colors.length < width * height) {
            throw new IllegalArgumentException("array length must be >= width * height");
        }
        
        return createBitmap(colors, 0, width, width, height, config);
    }
    
    /**
     * Returns the width of the bitmap.
     * 
     * @return The width of the bitmap
     */
    public int getWidth() {
        return width;
    }
    
    /**
     * Returns the height of the bitmap.
     * 
     * @return The height of the bitmap
     */
    public int getHeight() {
        return height;
    }
    
    /**
     * Return the bitmap's config. If the bitmap was created with
     * a mutable config, the same config will be returned. Otherwise,
     * null may be returned.
     * 
     * @return The bitmap's config, or null if it uses a system-dependent
     * default.
     */
    @Nullable
    public Config getConfig() {
        return config;
    }
    
    /**
     * Returns true if the bitmap's config supports per-pixel alpha,
     * and if the pixels may contain non-opaque alpha values.
     * 
     * @return True if the bitmap's pixels may contain non-opaque alpha values.
     */
    public boolean hasAlpha() {
        return config == Config.ARGB_8888 || config == Config.ARGB_4444 || config == Config.RGBA_F16;
    }
    
    /**
     * Sets the bitmap's mutable property. This is used internally by the
     * various create methods.
     * 
     * @param mutable The new mutable state of the bitmap
     */
    public void setHasAlpha(boolean hasAlpha) {
        // Not implemented in this mock
    }
    
    /**
     * Returns the color at the specified pixel.
     * 
     * @param x The x coordinate (0...width-1) of the pixel to return
     * @param y The y coordinate (0...height-1) of the pixel to return
     * @return The ARGB color at the specified coordinate
     */
    public int getPixel(int x, int y) {
        checkRecycled();
        checkPixelAccess(x, y);
        return 0; // Mock implementation
    }
    
    /**
     * Returns in pixels[] a copy of the data in the bitmap. Each value is
     * a packed int representing a Color.
     * 
     * @param pixels The array to receive the bitmap's colors
     * @param offset The first index to write into pixels[]
     * @param stride The number of entries in pixels[] to skip between
     *               rows (must be >= width or <= -width).
     * @param x The x coordinate of the first pixel to read from the bitmap
     * @param y The y coordinate of the first pixel to read from the bitmap
     * @param width The number of pixels to read from each row
     * @param height The number of rows to read
     */
    public void getPixels(int[] pixels, int offset, int stride, int x, int y, int width, int height) {
        checkRecycled();
        
        if (width <= 0 || height <= 0) {
            return;
        }
        
        if (x < 0 || y < 0) {
            throw new IllegalArgumentException("x and y must be >= 0");
        }
        if (x + width > this.width) {
            throw new IllegalArgumentException("x + width must be <= bitmap.width()");
        }
        if (y + height > this.height) {
            throw new IllegalArgumentException("y + height must be <= bitmap.height()");
        }
        if (Math.abs(stride) < width) {
            throw new IllegalArgumentException("abs(stride) must be >= width");
        }
        
        // Just fill with transparent black
        for (int i = 0; i < height; i++) {
            int rowOffset = offset + i * stride;
            for (int j = 0; j < width; j++) {
                pixels[rowOffset + j] = 0;
            }
        }
    }
    
    /**
     * Write a single pixel into the bitmap at the specified coordinates.
     * The color must be in the same format as the bitmap's config, e.g. ARGB_8888.
     * 
     * @param x The horizontal coordinate of the pixel to set
     * @param y The vertical coordinate of the pixel to set
     * @param color The color to set in the pixel
     */
    public void setPixel(int x, int y, int color) {
        checkRecycled();
        
        if (!mutable) {
            throw new IllegalStateException("Bitmap is immutable");
        }
        
        checkPixelAccess(x, y);
        
        // Not actually doing anything in this mock
    }
    
    /**
     * Replace pixels in the bitmap with the colors in the array.
     * Each element in the array is a packed int representing a Color.
     * 
     * @param pixels The colors to write to the bitmap
     * @param offset The index of the first color to read from pixels[]
     * @param stride The number of colors in pixels[] to skip between rows.
     * @param x The x coordinate of the first pixel to write
     * @param y The y coordinate of the first pixel to write
     * @param width The number of pixels to write to each row
     * @param height The number of rows to write
     */
    public void setPixels(int[] pixels, int offset, int stride, int x, int y, int width, int height) {
        checkRecycled();
        
        if (!mutable) {
            throw new IllegalStateException("Bitmap is immutable");
        }
        
        if (width <= 0 || height <= 0) {
            return;
        }
        
        if (x < 0 || y < 0) {
            throw new IllegalArgumentException("x and y must be >= 0");
        }
        if (x + width > this.width) {
            throw new IllegalArgumentException("x + width must be <= bitmap.width()");
        }
        if (y + height > this.height) {
            throw new IllegalArgumentException("y + height must be <= bitmap.height()");
        }
        if (Math.abs(stride) < width) {
            throw new IllegalArgumentException("abs(stride) must be >= width");
        }
        
        // Not actually doing anything in this mock
    }
    
    /**
     * Fill the bitmap with the specified color.
     * 
     * @param color The color to fill the bitmap with
     */
    public void eraseColor(int color) {
        checkRecycled();
        
        if (!mutable) {
            throw new IllegalStateException("Bitmap is immutable");
        }
        
        // Not actually doing anything in this mock
    }
    
    /**
     * Returns a boolean indicating whether the bitmap has been recycled.
     * 
     * @return True if the bitmap has been recycled
     */
    public boolean isRecycled() {
        return recycled;
    }
    
    /**
     * Free the native object associated with this bitmap, and clear the
     * reference to the pixel data. This will not free the pixel data synchronously;
     * it simply allows it to be garbage collected if there are no other references.
     * The bitmap is marked as "dead", meaning it will throw an exception if
     * getPixels() or setPixels() is called, and will draw nothing.
     */
    public void recycle() {
        if (!recycled) {
            recycled = true;
        }
    }
    
    /**
     * Returns true if this bitmap was created for a mutable pixel buffer.
     * 
     * @return True if the bitmap is mutable
     */
    public boolean isMutable() {
        return mutable;
    }
    
    /**
     * Returns a mutable bitmap with the specified alpha.
     * 
     * @param config New bitmap configuration
     * @param isMutable True if the returned bitmap should be mutable
     * @return A mutable copy of the bitmap
     */
    public Bitmap copy(Config config, boolean isMutable) {
        checkRecycled();
        
        Bitmap bitmap = new Bitmap(width, height, config);
        bitmap.mutable = isMutable;
        return bitmap;
    }
    
    /**
     * Compresses this bitmap using the specified format (e.g. PNG or JPEG).
     * 
     * @param format The format to compress to
     * @param quality Hint to the compressor, 0-100. 0 meaning compress for
     *                small size, 100 meaning compress for max quality. Some
     *                formats, like PNG which is lossless, will ignore the
     *                quality setting
     * @param stream The outputstream to write the compressed data.
     * @return true if successfully compressed to the specified stream.
     */
    public boolean compress(CompressFormat format, int quality, @NonNull OutputStream stream) {
        checkRecycled();
        
        if (quality < 0 || quality > 100) {
            throw new IllegalArgumentException("quality must be 0..100");
        }
        
        // Not actually doing anything in this mock
        return true;
    }
    
    /**
     * Copy the bitmap's pixels into the specified buffer (allocated by the
     * caller). An exception is thrown if the buffer is not large enough to
     * hold all of the pixels (taking into account the number of bytes per
     * pixel) or if the Buffer subclass is not one of the supported types
     * (ByteBuffer, ShortBuffer, IntBuffer).
     * 
     * @param dst The buffer to receive the bitmap's pixels
     */
    public void copyPixelsToBuffer(@NonNull java.nio.Buffer dst) {
        checkRecycled();
        
        // Not actually doing anything in this mock
    }
    
    /**
     * Copy the pixels from the buffer, beginning at the specified offset,
     * overwriting the bitmap's pixels. The data in the buffer is not changed
     * in any way (unlike setPixels(), which converts from unpremultipled 32bit
     * to whatever the bitmap's native format is).
     * 
     * @param src The buffer containing the pixels to copy
     */
    public void copyPixelsFromBuffer(@NonNull java.nio.Buffer src) {
        checkRecycled();
        
        if (!mutable) {
            throw new IllegalStateException("Bitmap is immutable");
        }
        
        // Not actually doing anything in this mock
    }
    
    /**
     * Returns a non-null copy of the bitmap's config. If the bitmap was created with
     * a null config, this returns ARGB_8888.
     * 
     * @return The bitmap's config value, which is meaningful even if the bitmap was created with a
     *         null config.
     */
    @NonNull
    public Config getConfigOrDefault() {
        return config != null ? config : Config.ARGB_8888;
    }
    
    /**
     * Returns the bitmap's density.
     *
     * @return The bitmap's density value, or {@link #DENSITY_NONE} if the
     *         bitmap has no density.
     */
    public int getDensity() {
        return mDensity;
    }
    
    /**
     * Sets the bitmap's density. The density is used to determine the scaling
     * factor when the bitmap is drawn to a Canvas or when it is set as the
     * density of another bitmap.
     *
     * @param density The new density, or {@link #DENSITY_NONE} to remove
     *        the density from the bitmap.
     */
    public void setDensity(int density) {
        mDensity = density;
    }
    
    /**
     * Helper function to check if a bitmap has been recycled.
     * 
     * @throws IllegalStateException if the bitmap has been recycled.
     */
    private void checkRecycled() {
        if (recycled) {
            throw new IllegalStateException("Can't call on a recycled bitmap");
        }
    }
    
    /**
     * Helper function to check if a pixel coordinate is valid.
     * 
     * @param x The x coordinate to check
     * @param y The y coordinate to check
     * @throws IllegalArgumentException if the coordinate is outside the bitmap's bounds.
     */
    private void checkPixelAccess(int x, int y) {
        if (x < 0) {
            throw new IllegalArgumentException("x must be >= 0");
        }
        if (y < 0) {
            throw new IllegalArgumentException("y must be >= 0");
        }
        if (x >= width) {
            throw new IllegalArgumentException("x must be < bitmap.width()");
        }
        if (y >= height) {
            throw new IllegalArgumentException("y must be < bitmap.height()");
        }
    }
}