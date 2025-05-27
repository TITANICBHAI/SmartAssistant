package utils;

/**
 * Abstract class for managing application packages.
 * This is a compatibility class for Android's PackageManager.
 */
public abstract class PackageManager {
    
    /**
     * Get application information for a package
     * @param packageName The package name
     * @return The ApplicationInfo
     * @throws NameNotFoundException If the package was not found
     */
    public ApplicationInfo getApplicationInfo(String packageName) throws NameNotFoundException {
        return getApplicationInfo(packageName, 0);
    }
    
    /**
     * Get application information for a package with flags
     * @param packageName The package name
     * @param flags The flags
     * @return The ApplicationInfo
     * @throws NameNotFoundException If the package was not found
     */
    public abstract ApplicationInfo getApplicationInfo(String packageName, int flags) throws NameNotFoundException;
    
    /**
     * Check if a package is installed
     * @param packageName The package name
     * @return True if the package is installed, false otherwise
     */
    public boolean isPackageInstalled(String packageName) {
        try {
            getApplicationInfo(packageName);
            return true;
        } catch (NameNotFoundException e) {
            return false;
        }
    }
    
    /**
     * Get the version name of a package
     * @param packageName The package name
     * @return The version name
     * @throws NameNotFoundException If the package was not found
     */
    public String getPackageVersion(String packageName) throws NameNotFoundException {
        // Default implementation returns a placeholder version
        return "1.0.0";
    }
    
    /**
     * Get a list of installed packages
     * @return An array of package names
     */
    public String[] getInstalledPackages() {
        // Default implementation returns an empty array
        return new String[0];
    }
    
    /**
     * Exception thrown when a package is not found
     */
    public static class NameNotFoundException extends Exception {
        /**
         * Create a new NameNotFoundException
         */
        public NameNotFoundException() {
            super();
        }
        
        /**
         * Create a new NameNotFoundException with a message
         * @param message The message
         */
        public NameNotFoundException(String message) {
            super(message);
        }
        
        /**
         * Create a new NameNotFoundException with a message and cause
         * @param message The message
         * @param cause The cause
         */
        public NameNotFoundException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}