package android.content.res;

import androidx.annotation.NonNull;

import java.io.IOException;
import java.io.InputStream;

/**
 * Mock implementation of Android AssetManager class for development outside of Android.
 * Provides access to an application's raw asset files.
 */
public final class AssetManager {
    /**
     * Create a new AssetManager.
     */
    /*package*/ AssetManager() {
        // Created only from the Resources class
    }
    
    /**
     * Open an asset using ACCESS_STREAMING mode.
     * 
     * @param fileName The name of the asset to open.
     * @return An InputStream for reading the asset.
     * @throws IOException If the asset could not be opened.
     */
    @NonNull
    public InputStream open(@NonNull String fileName) throws IOException {
        throw new IOException("Mock implementation does not provide assets");
    }
    
    /**
     * Open an asset with the specified mode.
     * 
     * @param fileName The name of the asset to open.
     * @param accessMode The mode to use.
     * @return An InputStream for reading the asset.
     * @throws IOException If the asset could not be opened.
     */
    @NonNull
    public InputStream open(@NonNull String fileName, int accessMode) throws IOException {
        throw new IOException("Mock implementation does not provide assets");
    }
    
    /**
     * Return a String array of all the assets at the given path.
     * 
     * @param path A relative path within the assets, i.e., "docs/home.html".
     * @return A String array of all the assets that the path contains.
     * @throws IOException If the path is invalid.
     */
    @NonNull
    public String[] list(@NonNull String path) throws IOException {
        return new String[0]; // Mock implementation, always returns an empty array
    }
    
    /**
     * Close this asset manager.
     */
    public void close() {
        // Mock implementation, does nothing
    }
    
    /**
     * Modes used to open assets with open(String, int).
     */
    public static final int ACCESS_UNKNOWN = 0;
    public static final int ACCESS_RANDOM = 1;
    public static final int ACCESS_STREAMING = 2;
    public static final int ACCESS_BUFFER = 3;
}