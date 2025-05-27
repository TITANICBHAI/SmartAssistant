package utils;

/**
 * A mock implementation of the Android Bitmap class.
 */
public class Bitmap {

    /**
     * The bitmap configuration.
     */
    public enum Config {
        ALPHA_8,
        ARGB_4444,
        ARGB_8888,
        RGB_565;

        @Override
        public String toString() {
            return name();
        }
    }

    private int width;
    private int height;
    private Config config;
    private boolean immutable;
    private boolean recycled;
    private byte[] pixels;
    
    /**
     * Create a new bitmap with the specified width, height, and config.
     * 
     * @param width The width
     * @param height The height
     * @param config The config
     */
    public Bitmap(int width, int height, Config config) {
        this.width = width;
        this.height = height;
        this.config = config;
        this.immutable = false;
        this.recycled = false;
        this.pixels = new byte[getByteCount()];
    }
    
    /**
     * Create a bitmap from pixel data.
     * 
     * @param width The width
     * @param height The height
     * @param config The config
     * @param pixels The pixel data
     */
    public Bitmap(int width, int height, Config config, byte[] pixels) {
        this.width = width;
        this.height = height;
        this.config = config;
        this.immutable = false;
        this.recycled = false;
        this.pixels = pixels != null ? pixels.clone() : new byte[getByteCount()];
    }
    
    /**
     * Create a bitmap by copying an existing bitmap.
     * 
     * @param source The source bitmap
     */
    private Bitmap(Bitmap source) {
        this.width = source.width;
        this.height = source.height;
        this.config = source.config;
        this.immutable = false;
        this.recycled = false;
        this.pixels = source.pixels.clone();
    }
    
    /**
     * Get the width of the bitmap.
     * 
     * @return The width
     */
    public int getWidth() {
        return width;
    }
    
    /**
     * Get the height of the bitmap.
     * 
     * @return The height
     */
    public int getHeight() {
        return height;
    }
    
    /**
     * Get the config of the bitmap.
     * 
     * @return The config
     */
    public Config getConfig() {
        return config;
    }
    
    /**
     * Check if the bitmap is mutable.
     * 
     * @return True if mutable
     */
    public boolean isMutable() {
        return !immutable;
    }
    
    /**
     * Set the bitmap to be immutable.
     */
    public void setImmutable() {
        immutable = true;
    }
    
    /**
     * Check if the bitmap is recycled.
     * 
     * @return True if recycled
     */
    public boolean isRecycled() {
        return recycled;
    }
    
    /**
     * Recycle the bitmap.
     */
    public void recycle() {
        recycled = true;
        pixels = null;
    }
    
    /**
     * Create a copy of the bitmap.
     * 
     * @param config The config for the new bitmap
     * @param isMutable Whether the new bitmap should be mutable
     * @return A new bitmap
     */
    public Bitmap copy(Config config, boolean isMutable) {
        Bitmap copy = new Bitmap(this);
        copy.config = config != null ? config : this.config;
        copy.immutable = !isMutable;
        return copy;
    }
    
    /**
     * Get the byte count of the bitmap.
     * 
     * @return The byte count
     */
    public int getByteCount() {
        int bytesPerPixel;
        switch (config) {
            case ALPHA_8:
                bytesPerPixel = 1;
                break;
            case RGB_565:
            case ARGB_4444:
                bytesPerPixel = 2;
                break;
            case ARGB_8888:
            default:
                bytesPerPixel = 4;
                break;
        }
        return width * height * bytesPerPixel;
    }
    
    /**
     * Get a pixel at the specified coordinates.
     * 
     * @param x The x coordinate
     * @param y The y coordinate
     * @return The pixel value
     */
    public int getPixel(int x, int y) {
        if (recycled) {
            throw new IllegalStateException("Cannot call getPixel() on a recycled bitmap");
        }
        if (x < 0 || x >= width || y < 0 || y >= height) {
            throw new IllegalArgumentException("Coordinates outside of bitmap bounds");
        }
        
        // Mock implementation
        return 0;
    }
    
    /**
     * Set a pixel at the specified coordinates.
     * 
     * @param x The x coordinate
     * @param y The y coordinate
     * @param color The color value
     */
    public void setPixel(int x, int y, int color) {
        if (recycled) {
            throw new IllegalStateException("Cannot call setPixel() on a recycled bitmap");
        }
        if (immutable) {
            throw new IllegalStateException("Cannot call setPixel() on an immutable bitmap");
        }
        if (x < 0 || x >= width || y < 0 || y >= height) {
            throw new IllegalArgumentException("Coordinates outside of bitmap bounds");
        }
        
        // Mock implementation
    }
    
    /**
     * Get the pixels of the bitmap.
     * 
     * @return The pixels
     */
    public byte[] getPixels() {
        if (recycled) {
            throw new IllegalStateException("Cannot call getPixels() on a recycled bitmap");
        }
        return pixels.clone();
    }
    
    /**
     * Set the pixels of the bitmap.
     * 
     * @param pixels The pixels
     */
    public void setPixels(byte[] pixels) {
        if (recycled) {
            throw new IllegalStateException("Cannot call setPixels() on a recycled bitmap");
        }
        if (immutable) {
            throw new IllegalStateException("Cannot call setPixels() on an immutable bitmap");
        }
        if (pixels == null) {
            throw new IllegalArgumentException("Pixels array cannot be null");
        }
        if (pixels.length != this.pixels.length) {
            throw new IllegalArgumentException("Pixels array must be the same size as the bitmap");
        }
        
        this.pixels = pixels.clone();
    }
    
    /**
     * Compress the bitmap to a byte array.
     * 
     * @param format The format to compress to
     * @param quality The quality of the compression (0-100)
     * @param stream The stream to write to
     * @return True if successful
     */
    public boolean compress(String format, int quality, byte[] stream) {
        if (recycled) {
            throw new IllegalStateException("Cannot call compress() on a recycled bitmap");
        }
        
        // Mock implementation
        return true;
    }
    
    /**
     * Create a new bitmap with the specified dimensions.
     * 
     * @param width The width
     * @param height The height
     * @param config The config
     * @return A new bitmap
     */
    public static Bitmap createBitmap(int width, int height, Config config) {
        if (width <= 0 || height <= 0) {
            throw new IllegalArgumentException("Width and height must be > 0");
        }
        return new Bitmap(width, height, config);
    }
    
    /**
     * Create a scaled version of a bitmap.
     * 
     * @param src The source bitmap
     * @param dstWidth The destination width
     * @param dstHeight The destination height
     * @param filter Whether to filter
     * @return A new bitmap
     */
    public static Bitmap createScaledBitmap(Bitmap src, int dstWidth, int dstHeight, boolean filter) {
        if (src == null) {
            throw new IllegalArgumentException("Source bitmap cannot be null");
        }
        if (src.isRecycled()) {
            throw new IllegalArgumentException("Source bitmap is recycled");
        }
        if (dstWidth <= 0 || dstHeight <= 0) {
            throw new IllegalArgumentException("Width and height must be > 0");
        }
        
        Bitmap result = new Bitmap(dstWidth, dstHeight, src.getConfig());
        
        // Simple scaling for mock implementation
        return result;
    }
    
    /**
     * Create a bitmap from a subset of another bitmap.
     * 
     * @param source The source bitmap
     * @param x The x coordinate of the subset
     * @param y The y coordinate of the subset
     * @param width The width of the subset
     * @param height The height of the subset
     * @return A new bitmap
     */
    public static Bitmap createBitmap(Bitmap source, int x, int y, int width, int height) {
        if (source == null) {
            throw new IllegalArgumentException("Source bitmap cannot be null");
        }
        if (source.isRecycled()) {
            throw new IllegalArgumentException("Source bitmap is recycled");
        }
        if (x < 0 || y < 0 || width <= 0 || height <= 0 || 
            x + width > source.getWidth() || y + height > source.getHeight()) {
            throw new IllegalArgumentException("Invalid crop coordinates");
        }
        
        Bitmap result = new Bitmap(width, height, source.getConfig());
        
        // Simple copying for mock implementation
        return result;
    }
    
    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null || getClass() != obj.getClass()) {
            return false;
        }
        
        Bitmap other = (Bitmap) obj;
        if (width != other.width || height != other.height || 
            config != other.config || immutable != other.immutable || 
            recycled != other.recycled) {
            return false;
        }
        
        if (pixels == null) {
            return other.pixels == null;
        } else if (other.pixels == null) {
            return false;
        } else if (pixels.length != other.pixels.length) {
            return false;
        }
        
        for (int i = 0; i < pixels.length; i++) {
            if (pixels[i] != other.pixels[i]) {
                return false;
            }
        }
        
        return true;
    }
    
    @Override
    public int hashCode() {
        int result = width;
        result = 31 * result + height;
        result = 31 * result + (config != null ? config.hashCode() : 0);
        result = 31 * result + (immutable ? 1 : 0);
        result = 31 * result + (recycled ? 1 : 0);
        if (pixels != null) {
            for (int i = 0; i < Math.min(pixels.length, 100); i++) {
                result = 31 * result + pixels[i];
            }
        }
        return result;
    }
    
    @Override
    public String toString() {
        return "Bitmap(" + width + "x" + height + ", " + config + ")";
    }
}