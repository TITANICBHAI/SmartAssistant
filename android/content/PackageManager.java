package android.content;

import java.util.List;

/**
 * Stub implementation of Android PackageManager class for compatibility
 */
public abstract class PackageManager {
    /**
     * Exception thrown when a given package, application, or component name cannot be found
     */
    public static class NameNotFoundException extends Exception {
        /**
         * Constructor
         */
        public NameNotFoundException() {
            super();
        }
        
        /**
         * Constructor with a message
         * 
         * @param name The message
         */
        public NameNotFoundException(String name) {
            super(name);
        }
    }
    
    /**
     * Get a list of all installed packages on the device
     * 
     * @param flags Additional flags to filter the packages
     * @return A List of PackageInfo objects, one for each installed package
     */
    public abstract List<android.content.pm.PackageInfo> getInstalledPackages(int flags);
    
    /**
     * Retrieve information about a particular package
     * 
     * @param packageName The name of the package to find
     * @param flags Additional flags to filter the packages
     * @return A PackageInfo object containing information about the package
     * @throws NameNotFoundException if the package cannot be found
     */
    public abstract android.content.pm.PackageInfo getPackageInfo(String packageName, int flags) 
            throws NameNotFoundException;
    
    /**
     * Determine the best action to perform for a given Intent
     * 
     * @param intent The Intent to resolve
     * @return ResolveInfo containing the final activity intent that was determined to be the best action
     */
    public abstract android.content.pm.ResolveInfo resolveActivity(Intent intent, int flags);
    
    /**
     * Retrieve all activities that can be performed for the given intent
     * 
     * @param intent The Intent to find activities for
     * @param flags Additional options to modify the data returned
     * @return A List of ResolveInfo objects containing one entry for each matching activity
     */
    public abstract List<android.content.pm.ResolveInfo> queryIntentActivities(Intent intent, int flags);
}