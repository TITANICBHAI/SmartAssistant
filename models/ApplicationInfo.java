package models;

import java.util.Map;
import java.util.HashMap;

/**
 * Class representing application information.
 * This is used to provide metadata about installed applications.
 */
public interface ApplicationInfo {
    /**
     * Get the application name.
     *
     * @return The application name
     */
    String getApplicationName();
    
    /**
     * Set the application name.
     *
     * @param name The application name to set
     */
    void setApplicationName(String name);
    
    /**
     * Get the package name.
     *
     * @return The package name
     */
    String getPackageName();
    
    /**
     * Set the package name.
     *
     * @param packageName The package name to set
     */
    void setPackageName(String packageName);
    
    /**
     * Get the version name.
     *
     * @return The version name
     */
    String getVersionName();
    
    /**
     * Set the version name.
     *
     * @param versionName The version name to set
     */
    void setVersionName(String versionName);
    
    /**
     * Get the version code.
     *
     * @return The version code
     */
    int getVersionCode();
    
    /**
     * Set the version code.
     *
     * @param versionCode The version code to set
     */
    void setVersionCode(int versionCode);
    
    /**
     * Get a metadata value.
     *
     * @param key The metadata key
     * @return The metadata value
     */
    Object getMetadata(String key);
    
    /**
     * Set a metadata value.
     *
     * @param key The metadata key
     * @param value The metadata value
     */
    void setMetadata(String key, Object value);
    
    /**
     * Get all metadata.
     *
     * @return Map of metadata keys to values
     */
    Map<String, Object> getAllMetadata();
    
    /**
     * Set all metadata.
     *
     * @param metadata Map of metadata keys to values
     */
    void setAllMetadata(Map<String, Object> metadata);
    
    /**
     * Default implementation of ApplicationInfo.
     */
    public static class DefaultApplicationInfo implements ApplicationInfo {
        private String applicationName;
        private String packageName;
        private String versionName;
        private int versionCode;
        private Map<String, Object> metadata;
        
        /**
         * Create a new DefaultApplicationInfo.
         */
        public DefaultApplicationInfo() {
            this("Unknown App", "unknown.package", "1.0", 1);
        }
        
        /**
         * Create a new DefaultApplicationInfo with the specified values.
         *
         * @param applicationName The application name
         * @param packageName The package name
         * @param versionName The version name
         * @param versionCode The version code
         */
        public DefaultApplicationInfo(String applicationName, String packageName, String versionName, int versionCode) {
            this.applicationName = applicationName;
            this.packageName = packageName;
            this.versionName = versionName;
            this.versionCode = versionCode;
            this.metadata = new HashMap<>();
        }
        
        @Override
        public String getApplicationName() {
            return applicationName;
        }
        
        @Override
        public void setApplicationName(String name) {
            this.applicationName = name;
        }
        
        @Override
        public String getPackageName() {
            return packageName;
        }
        
        @Override
        public void setPackageName(String packageName) {
            this.packageName = packageName;
        }
        
        @Override
        public String getVersionName() {
            return versionName;
        }
        
        @Override
        public void setVersionName(String versionName) {
            this.versionName = versionName;
        }
        
        @Override
        public int getVersionCode() {
            return versionCode;
        }
        
        @Override
        public void setVersionCode(int versionCode) {
            this.versionCode = versionCode;
        }
        
        @Override
        public Object getMetadata(String key) {
            return metadata.get(key);
        }
        
        @Override
        public void setMetadata(String key, Object value) {
            metadata.put(key, value);
        }
        
        @Override
        public Map<String, Object> getAllMetadata() {
            return new HashMap<>(metadata);
        }
        
        @Override
        public void setAllMetadata(Map<String, Object> metadata) {
            this.metadata = new HashMap<>(metadata);
        }
        
        @Override
        public String toString() {
            return "DefaultApplicationInfo{" +
                    "applicationName='" + applicationName + '\'' +
                    ", packageName='" + packageName + '\'' +
                    ", versionName='" + versionName + '\'' +
                    ", versionCode=" + versionCode +
                    '}';
        }
    }
}