package android.content.pm;

/**
 * Mock implementation of Android's PermissionInfo class.
 * Information you can retrieve about a permission.
 */
public class PermissionInfo {
    /** The name of the permission. */
    public String name;
    
    /** The name of the package this permission is a part of. */
    public String packageName;
    
    /** A string resource identifier containing the label for this permission. */
    public int labelRes;
    
    /** A string containing the label for this permission. */
    public String nonLocalizedLabel;
    
    /** A drawable resource identifier for the icon for this permission. */
    public int icon;
    
    /** A string resource identifier containing the description for this permission. */
    public int descriptionRes;
    
    /** A string containing the description for this permission. */
    public String nonLocalizedDescription;
    
    /** What category this permission is in. */
    public int protectionLevel;
    
    /** Types of permission protection levels */
    public static final int PROTECTION_NORMAL = 0;
    public static final int PROTECTION_DANGEROUS = 1;
    public static final int PROTECTION_SIGNATURE = 2;
    public static final int PROTECTION_SIGNATURE_OR_SYSTEM = 3;
    
    /** 
     * Copy constructor.
     * 
     * @param orig The PermissionInfo object to copy from.
     */
    public PermissionInfo(PermissionInfo orig) {
        name = orig.name;
        packageName = orig.packageName;
        labelRes = orig.labelRes;
        nonLocalizedLabel = orig.nonLocalizedLabel;
        icon = orig.icon;
        descriptionRes = orig.descriptionRes;
        nonLocalizedDescription = orig.nonLocalizedDescription;
        protectionLevel = orig.protectionLevel;
    }
    
    /** Default constructor */
    public PermissionInfo() {
        // Empty constructor
    }
    
    @Override
    public String toString() {
        return "PermissionInfo{" + packageName + "/" + name + "}";
    }
}