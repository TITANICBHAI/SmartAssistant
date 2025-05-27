package android.content.pm;

import android.os.Parcel;
import android.os.Parcelable;

/**
 * Information about a service that has been installed on the system.
 */
public class ServiceInfo extends ComponentInfo implements Parcelable {
    /**
     * Optional name of a permission required to be able to access this
     * service.  From the "permission" attribute.
     */
    public String permission;
    
    /**
     * Flags associated with the service.
     */
    public int flags;
    
    /**
     * Default constructor.
     */
    public ServiceInfo() {
        super();
    }
    
    /**
     * Constructor from a Parcel.
     */
    protected ServiceInfo(Parcel source) {
        super(source);
        permission = source.readString();
        flags = source.readInt();
    }
    
    @Override
    public void writeToParcel(Parcel dest, int parcelableFlags) {
        super.writeToParcel(dest, parcelableFlags);
        dest.writeString(permission);
        dest.writeInt(flags);
    }
    
    @Override
    public String toString() {
        return "ServiceInfo{" + name + "}";
    }
    
    public static final Parcelable.Creator<ServiceInfo> CREATOR =
            new Parcelable.Creator<ServiceInfo>() {
        @Override
        public ServiceInfo createFromParcel(Parcel source) {
            return new ServiceInfo(source);
        }
        
        @Override
        public ServiceInfo[] newArray(int size) {
            return new ServiceInfo[size];
        }
    };
}