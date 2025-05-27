package android.content;

import androidx.annotation.NonNull;
import java.io.IOException;
import java.io.InputStream;

/**
 * Mock implementation of AssetManager for testing.
 * This provides access to an application's raw asset files.
 */
public class AssetManager {
    /**
     * Create a new AssetManager
     */
    protected AssetManager() {
        // Protected constructor
    }
    
    /**
     * Returns a string array of all the assets at the given path.
     *
     * @param path A relative path within the assets, i.e., "docs/home.html".
     * @return String[] Array of strings, one for each asset. These file
     *         names are relative to 'path'.
     */
    @NonNull
    public String[] list(@NonNull String path) throws IOException {
        return new String[0]; // Mock implementation
    }
    
    /**
     * Open an asset using ACCESS_STREAMING mode.
     *
     * @param fileName The name of the asset to open. This name can be
     *                 hierarchical.
     * @return InputStream An InputStream to read the asset data.
     */
    @NonNull
    public InputStream open(@NonNull String fileName) throws IOException {
        throw new IOException("Asset not found: " + fileName);
    }
    
    /**
     * Open an asset using an explicit access mode.
     *
     * @param fileName The name of the asset to open. This name can be
     *                 hierarchical.
     * @param accessMode Desired access mode.
     * @return InputStream An InputStream to read the asset data.
     */
    @NonNull
    public InputStream open(@NonNull String fileName, int accessMode) throws IOException {
        return open(fileName);
    }
    
    /**
     * Close this asset manager.
     */
    public final void close() {
        // Nothing to do in this mock
    }
}