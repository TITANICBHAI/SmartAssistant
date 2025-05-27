package utils;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

/**
 * Helper class for context compatibility between Android and our custom context implementations.
 * This class provides methods to convert between Android Context and utils.Context.
 * 
 * Note: This implementation mocks the Android-specific classes for non-Android environments.
 */
public class ContextCompatHelper {
    private static final String TAG = "ContextCompatHelper";
    
    /**
     * Mock Android Context class for compilation in non-Android environments
     */
    public static class AndroidContextWrapper {
        private String packageName;
        private Map<String, Object> properties = new HashMap<>();
        private AndroidPackageManagerWrapper packageManager;
        
        public AndroidContextWrapper(String packageName) {
            this.packageName = packageName;
            this.packageManager = new AndroidPackageManagerWrapper();
        }
        
        public String getPackageName() {
            return packageName;
        }
        
        public AndroidPackageManagerWrapper getPackageManager() {
            return packageManager;
        }
        
        public File getFilesDir() {
            return new File(System.getProperty("java.io.tmpdir"), "files");
        }
        
        public File getCacheDir() {
            return new File(System.getProperty("java.io.tmpdir"), "cache");
        }
        
        public File getExternalCacheDir() {
            return new File(System.getProperty("java.io.tmpdir"), "externalCache");
        }
        
        public File getDataDir() {
            return new File(System.getProperty("java.io.tmpdir"), "data");
        }
    }
    
    /**
     * Mock Android PackageManager class for compilation in non-Android environments
     */
    public static class AndroidPackageManagerWrapper {
        public PackageInfo getPackageInfo(String packageName, int flags) {
            return new PackageInfo();
        }
    }
    
    /**
     * Mock Android PackageInfo class for compilation in non-Android environments
     */
    public static class PackageInfo {
        public String versionName = "1.0.0";
    }
    
    /**
     * Mock Android Build class for compilation in non-Android environments
     */
    public static class AndroidBuild {
        public static final String MANUFACTURER = "Generic";
        public static final String MODEL = "Generic Device";
        
        public static class VERSION {
            public static final String RELEASE = "1.0";
            public static final int SDK_INT = 1;
        }
        
        public static class VERSION_CODES {
            public static final int N = 24;
        }
    }
    
    /**
     * Convert an Android Context to a utils.Context
     * @param androidContext The Android Context (or our mock wrapper)
     * @return The utils.Context
     */
    public static utils.Context fromAndroidContext(Object androidContext) {
        if (androidContext == null) {
            System.out.println(TAG + ": Cannot convert null Android context");
            return new utils.BasicContext("android");
        }
        
        utils.BasicContext context = new utils.BasicContext("android");
        
        try {
            // Handle our mock wrapper and real Android context
            String packageName;
            if (androidContext instanceof AndroidContextWrapper) {
                AndroidContextWrapper wrapper = (AndroidContextWrapper) androidContext;
                packageName = wrapper.getPackageName();
                
                // Set the app version from our mock
                PackageInfo packageInfo = wrapper.getPackageManager().getPackageInfo(packageName, 0);
                String versionName = packageInfo.versionName;
                context.setAppVersion(versionName);
                
                // Add some standard properties
                context.setProperty("filesDir", wrapper.getFilesDir().getAbsolutePath());
                context.setProperty("cacheDir", wrapper.getCacheDir().getAbsolutePath());
                context.setProperty("externalCacheDir", wrapper.getExternalCacheDir().getAbsolutePath());
                
                // If API level is N or above (24+)
                if (AndroidBuild.VERSION.SDK_INT >= AndroidBuild.VERSION_CODES.N) {
                    context.setProperty("dataDir", wrapper.getDataDir().getAbsolutePath());
                }
                
                // Add device info
                context.setProperty("deviceManufacturer", AndroidBuild.MANUFACTURER);
                context.setProperty("deviceModel", AndroidBuild.MODEL);
                context.setProperty("androidVersion", AndroidBuild.VERSION.RELEASE);
                context.setProperty("androidSdkVersion", AndroidBuild.VERSION.SDK_INT);
            } else {
                // Use reflection for actual Android Context when available
                packageName = "android.app";
                context.setAppVersion("1.0");
                context.setProperty("deviceManufacturer", AndroidBuild.MANUFACTURER);
                context.setProperty("deviceModel", AndroidBuild.MODEL);
                context.setProperty("androidVersion", AndroidBuild.VERSION.RELEASE);
                context.setProperty("androidSdkVersion", AndroidBuild.VERSION.SDK_INT);
            }
            
            context.setPackageName(packageName);
        } catch (Exception e) {
            System.err.println(TAG + ": Error converting Android context: " + e.getMessage());
        }
        
        return context;
    }
    
    /**
     * Convert a utils.Context to an Android Context
     * Note: This is not a true conversion as we can't create an Android Context from scratch.
     * This method is provided for symmetry and future expansion.
     * 
     * @param context The utils.Context
     * @param androidContext The existing Android Context to use as a base
     * @return The Android Context or AndroidContextWrapper in non-Android environments
     */
    public static Object toAndroidContext(utils.Context context, Object androidContext) {
        if (context == null || androidContext == null) {
            System.out.println(TAG + ": Cannot convert with null context(s)");
            return androidContext;
        }
        
        // If it's our mock wrapper, we can update it
        if (androidContext instanceof AndroidContextWrapper) {
            AndroidContextWrapper wrapper = (AndroidContextWrapper) androidContext;
            // In a real implementation, we would copy properties from context to wrapper
            return wrapper;
        }
        
        // We can't really create a new Android Context, so we just return the existing one
        // This method is primarily a placeholder for future functionality
        return androidContext;
    }
    
    /**
     * Create a basic context with just a package name
     * @param packageName The package name
     * @return A basic utils.Context
     */
    public static utils.Context createBasicContext(String packageName) {
        utils.BasicContext context = new utils.BasicContext("android");
        
        if (packageName != null && !packageName.isEmpty()) {
            context.setPackageName(packageName);
        }
        
        // Add system info
        context.setProperty("deviceManufacturer", AndroidBuild.MANUFACTURER);
        context.setProperty("deviceModel", AndroidBuild.MODEL);
        context.setProperty("androidVersion", AndroidBuild.VERSION.RELEASE);
        context.setProperty("androidSdkVersion", AndroidBuild.VERSION.SDK_INT);
        
        System.out.println(TAG + ": Created basic context for package: " + packageName);
        
        return context;
    }
    
    /**
     * Get a resource identifier from a utils.Context
     * @param context The utils.Context
     * @param androidContext The Android Context or our mock wrapper
     * @param resourceName The resource name
     * @param resourceType The resource type (drawable, string, layout, etc.)
     * @return The resource identifier
     */
    public static int getResourceId(utils.Context context, Object androidContext, String resourceName, String resourceType) {
        if (androidContext == null) {
            System.out.println(TAG + ": Cannot get resource ID with null Android context");
            return 0;
        }
        
        try {
            if (androidContext instanceof AndroidContextWrapper) {
                AndroidContextWrapper wrapper = (AndroidContextWrapper) androidContext;
                String packageName = wrapper.getPackageName();
                // For mock implementation, just return a simple hash of the resource name
                return resourceName.hashCode() & 0x7FFFFFFF; // Positive int
            } else {
                // Use reflection for actual Android context (for future implementation)
                return 0;
            }
        } catch (Exception e) {
            System.err.println(TAG + ": Error getting resource ID: " + e.getMessage());
            return 0;
        }
    }
    
    /**
     * Create a Context wrapper from an Android context object
     * 
     * @param androidContext Any Android context object (or null)
     * @return A Context wrapper
     */
    public static Context createContextWrapper(Object androidContext) {
        if (androidContext == null) {
            // Return an empty context if no Android context is provided
            return new EmptyContext();
        }
        
        // Create a wrapper Context
        return fromAndroidContext(androidContext);
    }
    
    /**
     * Create an empty context for cases where no context is available
     * 
     * @return An empty context implementation
     */
    public static Context createEmptyContext() {
        return new EmptyContext();
    }
    
    /**
     * A basic empty context implementation for cases where no context is available
     */
    public static class EmptyContext implements Context {
        private Map<String, Object> configs = new HashMap<>();
        
        @Override
        public Object getSystemService(String name) {
            return null;
        }
        
        @Override
        public String getPackageName() {
            return "empty.context";
        }
        
        // No @Override - this is a mock implementation
        public String getString(int resId) {
            return null;
        }
        
        // No @Override - this is a mock implementation
        public boolean checkPermission(String permission) {
            return false;
        }
        
        // Config methods - these could be part of an interface
        public Object getConfig(String key) {
            return configs.get(key);
        }
        
        public void setConfig(String key, Object value) {
            configs.put(key, value);
        }
        
        public Map<String, Object> getAllConfig() {
            return new HashMap<>(configs);
        }
        
        // No @Override - this is a mock implementation
        public android.content.Context getApplicationContext() {
            return null;
        }
        
        // No @Override - this is a mock implementation
        public PackageManager getPackageManager() {
            return null;
        }
        
        // No @Override - this is a mock implementation
        public ApplicationInfo getApplicationInfo() {
            return null;
        }
    }
    
    /**
     * Create a dummy Android context that can be used for compatibility
     * This is used when we need an android.content.Context but don't have a real one
     * 
     * @return A dummy Android context
     */
    public static android.content.Context createDummyAndroidContext() {
        // Create a dummy android.content.Context using DummyAndroidContext
        return new DummyAndroidContext();
    }
    
    /**
     * A dummy implementation of android.content.Context for compatibility
     */
    public static class DummyAndroidContext extends utils.DummyAndroidContext {
        public DummyAndroidContext() {
            super("dummy.android.context");
        }
    }
}