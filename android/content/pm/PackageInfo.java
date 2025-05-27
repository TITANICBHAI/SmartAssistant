package android.content.pm;

import android.content.ComponentName;

/**
 * Information about a package that has been installed.
 */
public class PackageInfo {
    public String packageName;
    public String versionName;
    public int versionCode;
    
    /**
     * Information about an activity.
     */
    public static class ActivityInfo {
        public String name;
        public String packageName;
        public ComponentName componentName;
        public String permission;
        public int flags;
        
        public ActivityInfo() {
            // Default constructor
        }
        
        public ActivityInfo(ActivityInfo orig) {
            this.name = orig.name;
            this.packageName = orig.packageName;
            this.componentName = orig.componentName;
            this.permission = orig.permission;
            this.flags = orig.flags;
        }
    }
    
    /**
     * Information about a service.
     */
    public static class ServiceInfo {
        public String name;
        public String packageName;
        public ComponentName componentName;
        public String permission;
        public int flags;
        
        public ServiceInfo() {
            // Default constructor
        }
        
        public ServiceInfo(ServiceInfo orig) {
            this.name = orig.name;
            this.packageName = orig.packageName;
            this.componentName = orig.componentName;
            this.permission = orig.permission;
            this.flags = orig.flags;
        }
    }
    
    /**
     * Information about a content provider.
     */
    public static class ProviderInfo {
        public String name;
        public String packageName;
        public ComponentName componentName;
        public String authority;
        public String permission;
        public int flags;
        
        public ProviderInfo() {
            // Default constructor
        }
        
        public ProviderInfo(ProviderInfo orig) {
            this.name = orig.name;
            this.packageName = orig.packageName;
            this.componentName = orig.componentName;
            this.authority = orig.authority;
            this.permission = orig.permission;
            this.flags = orig.flags;
        }
    }
}