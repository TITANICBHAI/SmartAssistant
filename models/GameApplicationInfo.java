package models;

import java.util.Map;

/**
 * Interface for application information
 * This was moved from GameContext.ApplicationInfo to fix class naming issues
 */
public interface GameApplicationInfo {
    /**
     * Get the package name
     * 
     * @return The package name
     */
    String getPackageName();
    
    /**
     * Get the application name
     * 
     * @return The application name
     */
    String getAppName();
    
    /**
     * Get the version name
     * 
     * @return The version name
     */
    String getVersionName();
    
    /**
     * Get the version code
     * 
     * @return The version code
     */
    int getVersionCode();
    
    /**
     * Get metadata
     * 
     * @return The metadata map or null if none
     */
    Map<String, Object> getMetadata();
    
    /**
     * Default implementation of GameApplicationInfo
     */
    public static class DefaultApplicationInfo implements GameApplicationInfo {
        private final String packageName;
        private final String appName;
        private final String versionName;
        private final int versionCode;
        private final Map<String, Object> metadata;
        
        /**
         * Create a new ApplicationInfo
         * 
         * @param packageName The package name
         * @param appName The application name
         * @param versionName The version name
         * @param versionCode The version code
         * @param metadata Additional metadata
         */
        public DefaultApplicationInfo(String packageName, String appName, String versionName, 
                                   int versionCode, Map<String, Object> metadata) {
            this.packageName = packageName;
            this.appName = appName;
            this.versionName = versionName;
            this.versionCode = versionCode;
            this.metadata = metadata;
        }
        
        /**
         * Create a new ApplicationInfo with default version code and no metadata
         * 
         * @param packageName The package name
         * @param appName The application name
         * @param versionName The version name
         */
        public DefaultApplicationInfo(String packageName, String appName, String versionName) {
            this(packageName, appName, versionName, 1, null);
        }
        
        @Override
        public String getPackageName() {
            return packageName;
        }
        
        @Override
        public String getAppName() {
            return appName;
        }
        
        @Override
        public String getVersionName() {
            return versionName;
        }
        
        @Override
        public int getVersionCode() {
            return versionCode;
        }
        
        @Override
        public Map<String, Object> getMetadata() {
            return metadata;
        }
    }
}