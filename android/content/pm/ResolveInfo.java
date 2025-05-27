package android.content.pm;

/**
 * Mock implementation of Android ResolveInfo for development outside of Android.
 * This class holds information about a specific component that matches an intent filter.
 */
public class ResolveInfo {
    public PackageInfo.ActivityInfo activityInfo;
    public PackageInfo.ServiceInfo serviceInfo;
    public PackageInfo.ProviderInfo providerInfo;
    public int priority;
    public int preferredOrder;
    public boolean isDefault;
    public int match;
    public String resolvePackageName;
    public int labelRes;
    public CharSequence nonLocalizedLabel;
    public int icon;
    
    /**
     * Default constructor
     */
    public ResolveInfo() {
    }
    
    /**
     * Constructor with activity info
     * 
     * @param activityInfo The activity info
     */
    public ResolveInfo(PackageInfo.ActivityInfo activityInfo) {
        this.activityInfo = activityInfo;
    }
    
    /**
     * Constructor with service info
     * 
     * @param serviceInfo The service info
     */
    public ResolveInfo(PackageInfo.ServiceInfo serviceInfo) {
        this.serviceInfo = serviceInfo;
    }
    
    /**
     * Constructor with provider info
     * 
     * @param providerInfo The provider info
     */
    public ResolveInfo(PackageInfo.ProviderInfo providerInfo) {
        this.providerInfo = providerInfo;
    }
    
    /**
     * Get the activity or service or provider info
     * 
     * @return An object representing the component
     */
    public Object getComponentInfo() {
        if (activityInfo != null) {
            return activityInfo;
        } else if (serviceInfo != null) {
            return serviceInfo;
        } else if (providerInfo != null) {
            return providerInfo;
        } else {
            return null;
        }
    }
    
    /**
     * Return the component's package name
     * 
     * @return The package name
     */
    public String getComponentPackageName() {
        if (activityInfo != null) {
            return activityInfo.packageName;
        } else if (serviceInfo != null) {
            return serviceInfo.packageName;
        } else if (providerInfo != null) {
            return providerInfo.packageName;
        } else {
            return null;
        }
    }
    
    /**
     * Return the component's name
     * 
     * @return The component name
     */
    public String getComponentName() {
        if (activityInfo != null) {
            return activityInfo.name;
        } else if (serviceInfo != null) {
            return serviceInfo.name;
        } else if (providerInfo != null) {
            return providerInfo.name;
        } else {
            return null;
        }
    }
}