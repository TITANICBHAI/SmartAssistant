package android.content.pm;

import android.os.Parcel;
import android.os.Parcelable;

/**
 * Base class containing information common to all package items.
 */
public class PackageItemInfo implements Parcelable {
    /**
     * The name of the item.
     */
    public String name;
    
    /**
     * A string resource identifier that corresponds to the name of the item.
     */
    public int labelRes;
    
    /**
     * A string that provides a non-localized label for the item.
     */
    public String nonLocalizedLabel;
    
    /**
     * A drawable resource identifier that represents the icon for the item.
     */
    public int icon;
    
    /**
     * The package name of this item.
     */
    public String packageName;
    
    /**
     * Default constructor.
     */
    public PackageItemInfo() {
        // Empty constructor
    }
    
    /**
     * Copy constructor.
     */
    public PackageItemInfo(PackageItemInfo orig) {
        name = orig.name;
        labelRes = orig.labelRes;
        nonLocalizedLabel = orig.nonLocalizedLabel;
        icon = orig.icon;
        packageName = orig.packageName;
    }
    
    /**
     * Constructor used when reading from a Parcel.
     */
    protected PackageItemInfo(Parcel source) {
        name = source.readString();
        labelRes = source.readInt();
        nonLocalizedLabel = source.readString();
        icon = source.readInt();
        packageName = source.readString();
    }
    
    @Override
    public int describeContents() {
        return 0;
    }
    
    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(name);
        dest.writeInt(labelRes);
        dest.writeString(nonLocalizedLabel);
        dest.writeInt(icon);
        dest.writeString(packageName);
    }
    
    @Override
    public String toString() {
        return "PackageItemInfo{" + packageName + "/" + name + "}";
    }
}