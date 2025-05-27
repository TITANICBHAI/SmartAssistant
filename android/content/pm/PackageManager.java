package android.content.pm;

import android.content.Intent;
import android.content.ComponentName;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Mock implementation of Android PackageManager class for development outside of Android.
 * Class for retrieving various information related to the application packages
 * installed on the device.
 */
public class PackageManager {
    /**
     * Flag for getApplicationInfo and getPackageInfo: return information about
     * the package in all installed users.
     */
    public static final int GET_UNINSTALLED_PACKAGES = 0x00000200;
    
    /**
     * Flag for getApplicationInfo and getPackageInfo: return information about
     * the package even if it's disabled.
     */
    public static final int GET_DISABLED_COMPONENTS = 0x00000100;
    
    /**
     * Flag for getApplicationInfo and getPackageInfo: return meta-data about
     * the package.
     */
    public static final int GET_META_DATA = 0x00000080;
    
    /**
     * Flag parameter to queryIntentActivities(android.content.Intent, int) that
     * includes in the results meta-information about the activities.
     */
    public static final int GET_RESOLVED_FILTER = 0x00000040;
    
    /**
     * Mock storage for application information.
     */
    private final Map<String, ApplicationInfo> mApplicationInfos = new HashMap<>();
    
    /**
     * Mock storage for activity information.
     */
    private final Map<String, ActivityInfo> mActivityInfos = new HashMap<>();
    
    /**
     * Add mock application info to the package manager.
     * 
     * @param packageName The package name.
     * @param appInfo The application info to add.
     */
    public void addApplicationInfo(String packageName, ApplicationInfo appInfo) {
        mApplicationInfos.put(packageName, appInfo);
    }
    
    /**
     * Add mock activity info to the package manager.
     * 
     * @param componentName The component name (format: "package/class").
     * @param activityInfo The activity info to add.
     */
    public void addActivityInfo(String componentName, ActivityInfo activityInfo) {
        mActivityInfos.put(componentName, activityInfo);
    }
    
    /**
     * Retrieve all of the information we know about a particular package.
     * 
     * @param packageName The package to query.
     * @param flags Additional options to modify the data returned.
     * @return Information about the package.
     * @throws NameNotFoundException if the package cannot be found on the system.
     */
    public ApplicationInfo getApplicationInfo(String packageName, int flags) throws NameNotFoundException {
        ApplicationInfo info = mApplicationInfos.get(packageName);
        if (info == null) {
            throw new NameNotFoundException("Package " + packageName + " not found");
        }
        return info;
    }
    
    /**
     * Return a list of all packages that are installed for the current user.
     * 
     * @param flags Additional flags about which packages to return.
     * @return A List of PackageInfo objects, one for each installed package.
     */
    public List<ApplicationInfo> getInstalledApplications(int flags) {
        return new ArrayList<>(mApplicationInfos.values());
    }
    
    /**
     * Retrieve all activities that can be performed for the given intent.
     * 
     * @param intent The intent to find activities for.
     * @param flags Additional options to modify the data returned.
     * @return List of resolved activities.
     */
    public List<ResolveInfo> queryIntentActivities(Intent intent, int flags) {
        List<ResolveInfo> result = new ArrayList<>();
        
        // Simplified implementation for mocking
        String action = intent.getAction();
        String packageName = intent.getPackage();
        
        for (Map.Entry<String, ActivityInfo> entry : mActivityInfos.entrySet()) {
            ActivityInfo activityInfo = entry.getValue();
            
            // Match package if specified
            if (packageName != null && !packageName.equals(activityInfo.packageName)) {
                continue;
            }
            
            // Create a ResolveInfo for this ActivityInfo
            ResolveInfo resolveInfo = new ResolveInfo();
            resolveInfo.activityInfo = activityInfo;
            result.add(resolveInfo);
        }
        
        return result;
    }
    
    /**
     * Retrieve information about a particular activity class.
     * 
     * @param component The component name for the activity.
     * @param flags Additional flags about which components to return.
     * @return ActivityInfo containing information about the component.
     * @throws NameNotFoundException If the component cannot be found in the system.
     */
    public ActivityInfo getActivityInfo(ComponentName component, int flags) throws NameNotFoundException {
        String componentKey = component.getPackageName() + "/" + component.getClassName();
        ActivityInfo info = mActivityInfos.get(componentKey);
        if (info == null) {
            throw new NameNotFoundException("Component " + componentKey + " not found");
        }
        return info;
    }
    
    /**
     * Exception thrown when a package or component cannot be found.
     */
    public static class NameNotFoundException extends Exception {
        public NameNotFoundException() {
            super();
        }
        
        public NameNotFoundException(String name) {
            super(name);
        }
    }
    
    /**
     * Information that is returned from resolving an intent.
     */
    public static class ResolveInfo {
        /**
         * The activity that corresponds to this resolution match.
         */
        public ActivityInfo activityInfo;
        
        /**
         * The filter that matched the resolved activity.
         */
        public String filter;
        
        /**
         * The priority of the resolved activity.
         */
        public int priority;
        
        /**
         * The preferred order of the resolved activity.
         */
        public int preferredOrder;
        
        /**
         * The match level of the resolved activity.
         */
        public int match;
        
        /**
         * Default constructor.
         */
        public ResolveInfo() {
        }
        
        @Override
        public String toString() {
            return "ResolveInfo{" +
                    "activityInfo=" + activityInfo +
                    '}';
        }
    }
}