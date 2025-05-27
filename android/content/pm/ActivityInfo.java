package android.content.pm;

import android.os.Parcel;
import android.os.Parcelable;

/**
 * Mock implementation of Android ActivityInfo class for development outside of Android.
 * Information you can retrieve about a particular activity or receiver.
 * This corresponds to information collected from the AndroidManifest.xml's
 * &lt;activity&gt; and &lt;receiver&gt; tags.
 */
public class ActivityInfo implements Parcelable {
    /**
     * The permission required to be able to access this activity.
     */
    public String permission;
    
    /**
     * The name of the taskAffinity to override that of the activity class.
     */
    public String taskAffinity;
    
    /**
     * The component name of this activity.
     */
    public String name;
    
    /**
     * The package that this activity is a part of.
     */
    public String packageName;
    
    /**
     * The configured launch mode of the activity.
     */
    public int launchMode;
    
    /**
     * Application that contains this activity.
     */
    public ApplicationInfo applicationInfo;
    
    /**
     * Default constructor.
     */
    public ActivityInfo() {
    }
    
    /**
     * Copy constructor.
     */
    public ActivityInfo(ActivityInfo orig) {
        permission = orig.permission;
        taskAffinity = orig.taskAffinity;
        name = orig.name;
        packageName = orig.packageName;
        launchMode = orig.launchMode;
        applicationInfo = orig.applicationInfo != null ? new ApplicationInfo(orig.applicationInfo) : null;
    }
    
    public static final int LAUNCH_MULTIPLE = 0;
    public static final int LAUNCH_SINGLE_TOP = 1;
    public static final int LAUNCH_SINGLE_TASK = 2;
    public static final int LAUNCH_SINGLE_INSTANCE = 3;
    
    @Override
    public String toString() {
        return "ActivityInfo{" +
                "name='" + name + '\'' +
                ", packageName='" + packageName + '\'' +
                '}';
    }
    
    @Override
    public int describeContents() {
        return 0;
    }
    
    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(permission);
        dest.writeString(taskAffinity);
        dest.writeString(name);
        dest.writeString(packageName);
        dest.writeInt(launchMode);
        dest.writeParcelable(applicationInfo, flags);
    }
    
    public static final Creator<ActivityInfo> CREATOR = new Creator<ActivityInfo>() {
        @Override
        public ActivityInfo createFromParcel(Parcel source) {
            ActivityInfo info = new ActivityInfo();
            info.permission = source.readString();
            info.taskAffinity = source.readString();
            info.name = source.readString();
            info.packageName = source.readString();
            info.launchMode = source.readInt();
            info.applicationInfo = source.readParcelable(ApplicationInfo.class.getClassLoader());
            return info;
        }
        
        @Override
        public ActivityInfo[] newArray(int size) {
            return new ActivityInfo[size];
        }
    };
}