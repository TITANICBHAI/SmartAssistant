package android.content.pm;

import android.os.Parcel;
import android.os.Parcelable;

/**
 * Base class containing information common to all application components.
 */
public class ComponentInfo implements Parcelable {
    /**
     * The name of the component.
     */
    public String name;
    
    /**
     * The package that the component belongs to.
     */
    public String packageName;
    
    /**
     * A string resource identifier that represents the label of the component.
     */
    public int labelRes;
    
    /**
     * An optional string that supplies a label string.
     */
    public String nonLocalizedLabel;
    
    /**
     * A drawable resource identifier that points to the component's icon.
     */
    public int icon;
    
    /**
     * Whether this component is enabled or not.
     */
    public boolean enabled = true;
    
    /**
     * The application that this component is a part of.
     */
    public ApplicationInfo applicationInfo;
    
    /**
     * Default constructor.
     */
    public ComponentInfo() {
        // Empty constructor
    }
    
    /**
     * Copy constructor.
     */
    protected ComponentInfo(ComponentInfo orig) {
        name = orig.name;
        packageName = orig.packageName;
        labelRes = orig.labelRes;
        nonLocalizedLabel = orig.nonLocalizedLabel;
        icon = orig.icon;
        enabled = orig.enabled;
        applicationInfo = orig.applicationInfo;
    }
    
    /**
     * Constructor used when reading from a Parcel.
     */
    protected ComponentInfo(Parcel source) {
        name = source.readString();
        packageName = source.readString();
        labelRes = source.readInt();
        nonLocalizedLabel = source.readString();
        icon = source.readInt();
        enabled = source.readInt() != 0;
        applicationInfo = source.readParcelable(ApplicationInfo.class.getClassLoader());
    }
    
    /**
     * Return the class name of this component.
     */
    public String getClassName() {
        return name;
    }
    
    @Override
    public int describeContents() {
        return 0;
    }
    
    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(name);
        dest.writeString(packageName);
        dest.writeInt(labelRes);
        dest.writeString(nonLocalizedLabel);
        dest.writeInt(icon);
        dest.writeInt(enabled ? 1 : 0);
        dest.writeParcelable(applicationInfo, flags);
    }
    
    @Override
    public String toString() {
        return "ComponentInfo{" + packageName + "/" + name + "}";
    }
}