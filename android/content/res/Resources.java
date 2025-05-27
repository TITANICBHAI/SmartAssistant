package android.content.res;

import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.util.DisplayMetrics;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

/**
 * Mock implementation of Android Resources class for development outside of Android.
 * Class for accessing an application's resources.
 */
public class Resources {
    private final AssetManager mAssets;
    private final DisplayMetrics mDisplayMetrics;
    private final Configuration mConfiguration;
    
    /**
     * Create a Resources object with the given asset manager, display metrics, and configuration.
     * 
     * @param assets The asset manager for this resources object.
     * @param metrics The display metrics for this resources object.
     * @param config The configuration for this resources object.
     */
    public Resources(@NonNull AssetManager assets, @NonNull DisplayMetrics metrics, @NonNull Configuration config) {
        mAssets = assets;
        mDisplayMetrics = metrics;
        mConfiguration = config;
    }
    
    /**
     * Create a Resources object with a mock asset manager, display metrics, and configuration.
     */
    public Resources() {
        mAssets = new AssetManager();
        mDisplayMetrics = new DisplayMetrics();
        mConfiguration = new Configuration();
    }
    
    /**
     * Return the current configuration that is in effect.
     * 
     * @return The configuration object.
     */
    @NonNull
    public Configuration getConfiguration() {
        return mConfiguration;
    }
    
    /**
     * Return the current display metrics that are in effect.
     * 
     * @return The display metrics object.
     */
    @NonNull
    public DisplayMetrics getDisplayMetrics() {
        return mDisplayMetrics;
    }
    
    /**
     * Return a resource identifier for the given resource name.
     * 
     * @param name The name of the desired resource.
     * @param defType The resource type (e.g., "drawable", "string", etc.).
     * @param defPackage The package name to use if the resource name does not include a package.
     * @return The associated resource identifier.
     */
    public int getIdentifier(@NonNull String name, @Nullable String defType, @Nullable String defPackage) {
        // Mock implementation, always returns 0
        return 0;
    }
    
    /**
     * Return a string for the given resource identifier.
     * 
     * @param id The resource identifier.
     * @return The associated string object.
     */
    @NonNull
    public String getString(int id) {
        // Mock implementation, always returns an empty string
        return "";
    }
    
    /**
     * Return a string for the given resource identifier, substituting the format arguments.
     * 
     * @param id The resource identifier.
     * @param formatArgs The format arguments that will be used for substitution.
     * @return The associated string object.
     */
    @NonNull
    public String getString(int id, Object... formatArgs) {
        // Mock implementation, always returns an empty string
        return "";
    }
    
    /**
     * Return a drawable for the given resource identifier.
     * 
     * @param id The resource identifier.
     * @return The associated drawable object.
     */
    @Nullable
    public Drawable getDrawable(int id) {
        // Mock implementation, always returns null
        return null;
    }
    
    /**
     * Return a drawable for the given resource identifier, using the specified theme.
     * 
     * @param id The resource identifier.
     * @param theme The theme used to style the drawable attributes.
     * @return The associated drawable object.
     */
    @Nullable
    public Drawable getDrawable(int id, @Nullable Resources.Theme theme) {
        // Mock implementation, always returns null
        return null;
    }
    
    /**
     * Return the dimension value for the given resource identifier.
     * 
     * @param id The resource identifier.
     * @return The dimension value.
     */
    public float getDimension(int id) {
        // Mock implementation, always returns 0
        return 0;
    }
    
    /**
     * Return the pixel size for the given resource identifier.
     * 
     * @param id The resource identifier.
     * @return The pixel size.
     */
    public int getDimensionPixelSize(int id) {
        // Mock implementation, always returns 0
        return 0;
    }
    
    /**
     * Return the integer value for the given resource identifier.
     * 
     * @param id The resource identifier.
     * @return The integer value.
     */
    public int getInteger(int id) {
        // Mock implementation, always returns 0
        return 0;
    }
    
    /**
     * Return the boolean value for the given resource identifier.
     * 
     * @param id The resource identifier.
     * @return The boolean value.
     */
    public boolean getBoolean(int id) {
        // Mock implementation, always returns false
        return false;
    }
    
    /**
     * Return the color value for the given resource identifier.
     * 
     * @param id The resource identifier.
     * @return The color value.
     */
    public int getColor(int id) {
        // Mock implementation, always returns 0 (transparent black)
        return 0;
    }
    
    /**
     * Return the color value for the given resource identifier, using the specified theme.
     * 
     * @param id The resource identifier.
     * @param theme The theme used to style the color attributes.
     * @return The color value.
     */
    public int getColor(int id, @Nullable Resources.Theme theme) {
        // Mock implementation, always returns 0 (transparent black)
        return 0;
    }
    
    /**
     * Return a typed array for the given resource identifier.
     * 
     * @param id The resource identifier.
     * @return The typed array.
     */
    @NonNull
    public TypedArray obtainTypedArray(int id) {
        // Mock implementation, always returns an empty typed array
        return new TypedArray();
    }
    
    /**
     * Return a typed array of strings for the given resource identifier.
     * 
     * @param id The resource identifier.
     * @return The string array.
     */
    @NonNull
    public String[] getStringArray(int id) {
        // Mock implementation, always returns an empty array
        return new String[0];
    }
    
    /**
     * Return a typed array of integers for the given resource identifier.
     * 
     * @param id The resource identifier.
     * @return The integer array.
     */
    @NonNull
    public int[] getIntArray(int id) {
        // Mock implementation, always returns an empty array
        return new int[0];
    }
    
    /**
     * Returns the AssetManager associated with these Resources.
     * 
     * @return The AssetManager.
     */
    @NonNull
    public AssetManager getAssets() {
        return mAssets;
    }
    
    /**
     * Return a layout resource for the given resource identifier.
     * 
     * @param id The resource identifier.
     * @return The layout resource.
     */
    @NonNull
    public XmlResourceParser getLayout(int id) {
        // Mock implementation, always returns an empty parser
        return new XmlResourceParser();
    }
    
    /**
     * Mock implementation of TypedArray.
     */
    public static class TypedArray {
        // Mock implementation
    }
    
    /**
     * Mock implementation of Theme.
     */
    public static final class Theme {
        // Mock implementation
    }
    
    /**
     * Mock implementation of XmlResourceParser.
     */
    public static class XmlResourceParser {
        // Mock implementation
    }
}