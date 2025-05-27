package android.os;

/**
 * Mock Android PackageManager class for compilation without Android SDK
 * This is a simplified version of Android's PackageManager class with only the minimum needed methods.
 * 
 * Note: For better emulation, we're using utils.PackageManager for some functionality
 */
public class PackageManager {
    private utils.PackageManager internalManager = new utils.PackageManager();
    
    /**
     * Get package information
     * @param packageName The package name
     * @param flags The flags for the request
     * @return The package information
     */
    public android.content.pm.PackageInfo getPackageInfo(String packageName, int flags) throws NameNotFoundException {
        try {
            // First try to get from our utils version
            utils.ApplicationInfo appInfo = internalManager.getApplicationInfo(packageName, 0);
            
            // Then convert to Android's version
            android.content.pm.PackageInfo info = new android.content.pm.PackageInfo();
            info.packageName = packageName;
            info.versionName = "1.0.0";
            
            android.content.pm.ApplicationInfo androidAppInfo = new android.content.pm.ApplicationInfo();
            androidAppInfo.packageName = packageName;
            androidAppInfo.nonLocalizedLabel = appInfo.getLabel();
            
            info.applicationInfo = androidAppInfo;
            
            return info;
        } catch (utils.PackageManager.NameNotFoundException e) {
            throw new NameNotFoundException(e.getMessage());
        }
    }
    
    /**
     * Exception thrown when a package is not found
     */
    public static class NameNotFoundException extends Exception {
        public NameNotFoundException() {
            super("Package not found");
        }
        
        public NameNotFoundException(String message) {
            super(message);
        }
    }
}