package android.content.pm;

import android.os.Parcel;
import android.os.Parcelable;

/**
 * Mock implementation of Android ApplicationInfo class for development outside of Android.
 * Information you can retrieve about a particular application. This corresponds to information
 * collected from the AndroidManifest.xml's &lt;application&gt; tag.
 */
public class ApplicationInfo implements Parcelable {
    /**
     * Default task affinity of all activities in this application.
     */
    public String taskAffinity;
    
    /**
     * Class implementing the Application object.
     */
    public String className;
    
    /**
     * The name of the package that this application came from.
     */
    public String packageName;
    
    /**
     * Public name of this application.
     */
    public String name;
    
    /**
     * Full path to the location of this package.
     */
    public String sourceDir;
    
    /**
     * Full path to the publicly available parts of this package (excluding libraries).
     */
    public String publicSourceDir;
    
    /**
     * Paths to all shared libraries this application is linked against.
     */
    public String[] sharedLibraryFiles;
    
    /**
     * Full path to the directory where native JNI libraries are stored.
     */
    public String nativeLibraryDir;
    
    /**
     * The minimum SDK version this application targets.
     */
    public int minSdkVersion;
    
    /**
     * The SDK version this application targets.
     */
    public int targetSdkVersion;
    
    /**
     * Default constructor.
     */
    public ApplicationInfo() {
        minSdkVersion = 1;
        targetSdkVersion = 1;
    }
    
    /**
     * Copy constructor.
     */
    public ApplicationInfo(ApplicationInfo orig) {
        taskAffinity = orig.taskAffinity;
        className = orig.className;
        packageName = orig.packageName;
        name = orig.name;
        sourceDir = orig.sourceDir;
        publicSourceDir = orig.publicSourceDir;
        sharedLibraryFiles = orig.sharedLibraryFiles != null ? orig.sharedLibraryFiles.clone() : null;
        nativeLibraryDir = orig.nativeLibraryDir;
        minSdkVersion = orig.minSdkVersion;
        targetSdkVersion = orig.targetSdkVersion;
    }
    
    @Override
    public String toString() {
        return "ApplicationInfo{" +
                "packageName='" + packageName + '\'' +
                ", name='" + name + '\'' +
                '}';
    }
    
    @Override
    public int describeContents() {
        return 0;
    }
    
    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(taskAffinity);
        dest.writeString(className);
        dest.writeString(packageName);
        dest.writeString(name);
        dest.writeString(sourceDir);
        dest.writeString(publicSourceDir);
        dest.writeStringArray(sharedLibraryFiles);
        dest.writeString(nativeLibraryDir);
        dest.writeInt(minSdkVersion);
        dest.writeInt(targetSdkVersion);
    }
    
    public static final Creator<ApplicationInfo> CREATOR = new Creator<ApplicationInfo>() {
        @Override
        public ApplicationInfo createFromParcel(Parcel source) {
            ApplicationInfo info = new ApplicationInfo();
            info.taskAffinity = source.readString();
            info.className = source.readString();
            info.packageName = source.readString();
            info.name = source.readString();
            info.sourceDir = source.readString();
            info.publicSourceDir = source.readString();
            info.sharedLibraryFiles = source.readStringArray();
            info.nativeLibraryDir = source.readString();
            info.minSdkVersion = source.readInt();
            info.targetSdkVersion = source.readInt();
            return info;
        }
        
        @Override
        public ApplicationInfo[] newArray(int size) {
            return new ApplicationInfo[size];
        }
    };
}