package utils;

/**
 * A simplified context interface that mimics key functionality from Android Context.
 * This allows non-Android code to work with context-like objects.
 */
public interface Context {
    
    /**
     * Return the package name of this context
     * 
     * @return The package name
     */
    String getPackageName();
    
    /**
     * Return a system service by name
     * 
     * @param name The name of the service
     * @return The service or null if not available
     */
    Object getSystemService(String name);
}