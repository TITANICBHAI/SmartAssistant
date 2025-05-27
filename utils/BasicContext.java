package utils;

import java.util.HashMap;
import java.util.Map;

/**
 * A basic implementation of Context that can be used for non-Android environments
 */
public class BasicContext implements Context {
    private String packageName;
    private Map<String, Object> systemServices;
    private Map<String, Object> properties;
    private PackageManager packageManager;
    
    /**
     * Create a basic context with a default package name
     */
    public BasicContext() {
        this("app.package");
    }
    
    /**
     * Create a basic context with a specified package name
     * @param packageName The package name
     */
    public BasicContext(String packageName) {
        this.packageName = packageName;
        this.systemServices = new HashMap<>();
        this.properties = new HashMap<>();
        this.packageManager = new BasicPackageManager();
    }
    
    /**
     * Create a basic context from a DummyAndroidContext
     * @param androidContext The Android context to wrap
     */
    public BasicContext(DummyAndroidContext androidContext) {
        this.packageName = androidContext.getPackageName();
        this.systemServices = new HashMap<>();
        this.properties = new HashMap<>();
        this.packageManager = new BasicPackageManager();
        
        // Copy any relevant data from the Android context
        // For now just using the package name is sufficient
    }
    
    @Override
    public Object getSystemService(String name) {
        return systemServices.get(name);
    }
    
    /**
     * Register a system service
     * @param name The service name
     * @param service The service object
     */
    public void addSystemService(String name, Object service) {
        systemServices.put(name, service);
    }
    
    @Override
    public String getPackageName() {
        return packageName;
    }
    
    /**
     * Set the package name
     * @param packageName The package name
     */
    public void setPackageName(String packageName) {
        this.packageName = packageName;
    }
    
    // No @Override - this is a mock implementation
    public String getString(int resId) {
        // This is a very simple implementation that just returns a placeholder
        return "string_" + resId;
    }
    
    // No @Override - this is a mock implementation
    public boolean checkPermission(String permission) {
        // Default implementation assumes all permissions are granted
        return true;
    }
    
    /**
     * Get the package manager
     * @return The package manager
     */
    public PackageManager getPackageManager() {
        return packageManager;
    }
    
    /**
     * Set a property
     * @param name The property name
     * @param value The property value
     */
    public void setProperty(String name, Object value) {
        properties.put(name, value);
    }
    
    /**
     * Get a property
     * @param name The property name
     * @return The property value or null if not found
     */
    public Object getProperty(String name) {
        return properties.get(name);
    }
    
    /**
     * Set the app version
     * @param version The app version
     */
    public void setAppVersion(String version) {
        setProperty("appVersion", version);
    }
    
    /**
     * A simple implementation of PackageManager
     */
    private class BasicPackageManager extends PackageManager {
        @Override
        public ApplicationInfo getApplicationInfo(String packageName, int flags) throws NameNotFoundException {
            if (BasicContext.this.packageName.equals(packageName)) {
                ApplicationInfo info = new ApplicationInfo();
                info.setPackageName(packageName);
                return info;
            }
            throw new NameNotFoundException("Package " + packageName + " not found");
        }
    }
}