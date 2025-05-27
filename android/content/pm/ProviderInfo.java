package android.content.pm;

import android.os.Parcel;
import android.os.Parcelable;

/**
 * Information about a content provider that has been installed on the system.
 */
public class ProviderInfo extends ComponentInfo implements Parcelable {
    /**
     * The authority string of a provider determines where its data is inserted.
     * For a provider to be useful, at least one authority must be specified.
     */
    public String authority;
    
    /**
     * Whether or not this provider is syncable.
     */
    public boolean isSyncable;
    
    /**
     * Optional name of a permission required for read-only access to this content
     * provider.
     */
    public String readPermission;
    
    /**
     * Optional name of a permission required for write access to this content
     * provider.
     */
    public String writePermission;
    
    /**
     * If true, this content provider allows pattern matching.
     */
    public boolean grantUriPermissions;
    
    /**
     * Default constructor.
     */
    public ProviderInfo() {
        super();
    }
    
    /**
     * Copy constructor.
     */
    public ProviderInfo(ProviderInfo orig) {
        super(orig);
        authority = orig.authority;
        isSyncable = orig.isSyncable;
        readPermission = orig.readPermission;
        writePermission = orig.writePermission;
        grantUriPermissions = orig.grantUriPermissions;
    }
    
    /**
     * Constructor from a Parcel.
     */
    protected ProviderInfo(Parcel source) {
        super(source);
        authority = source.readString();
        isSyncable = source.readInt() != 0;
        readPermission = source.readString();
        writePermission = source.readString();
        grantUriPermissions = source.readInt() != 0;
    }
    
    @Override
    public void writeToParcel(Parcel dest, int flags) {
        super.writeToParcel(dest, flags);
        dest.writeString(authority);
        dest.writeInt(isSyncable ? 1 : 0);
        dest.writeString(readPermission);
        dest.writeString(writePermission);
        dest.writeInt(grantUriPermissions ? 1 : 0);
    }
    
    @Override
    public String toString() {
        return "ProviderInfo{" + name + "}";
    }
    
    public static final Parcelable.Creator<ProviderInfo> CREATOR =
            new Parcelable.Creator<ProviderInfo>() {
        @Override
        public ProviderInfo createFromParcel(Parcel source) {
            return new ProviderInfo(source);
        }
        
        @Override
        public ProviderInfo[] newArray(int size) {
            return new ProviderInfo[size];
        }
    };
}