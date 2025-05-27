package android.content;

import android.os.Parcel;
import android.os.Parcelable;

/**
 * Mock implementation of Android ComponentName class for development outside of Android.
 * Identifier for a specific application component, such as an Activity, 
 * that is available for use by other applications.
 */
public class ComponentName implements Parcelable {
    private final String mPackage;
    private final String mClass;
    
    /**
     * Create a new component identifier from a package name and class name.
     * 
     * @param pkg The name of the package that the component exists in.
     * @param cls The name of a class inside of the package that implements the component.
     */
    public ComponentName(String pkg, String cls) {
        if (pkg == null) throw new NullPointerException("package name is null");
        if (cls == null) throw new NullPointerException("class name is null");
        mPackage = pkg;
        mClass = cls;
    }
    
    /**
     * Create a new component identifier from a Context and class name.
     * 
     * @param pkg A Context for the package implementing the component.
     * @param cls The name of a class inside of the package that implements the component.
     */
    public ComponentName(Context pkg, String cls) {
        if (cls == null) throw new NullPointerException("class name is null");
        mPackage = pkg.getPackageName();
        mClass = cls;
    }
    
    /**
     * Create a new component identifier from a Context and Class object.
     * 
     * @param pkg A Context for the package implementing the component.
     * @param cls The Class object of the desired component.
     */
    public ComponentName(Context pkg, Class<?> cls) {
        mPackage = pkg.getPackageName();
        mClass = cls.getName();
    }
    
    /**
     * Return the package name of this component.
     * 
     * @return The package name of the component.
     */
    public String getPackageName() {
        return mPackage;
    }
    
    /**
     * Return the class name of this component.
     * 
     * @return The class name of the component.
     */
    public String getClassName() {
        return mClass;
    }
    
    /**
     * Return a ComponentName that is the same as this one, but with the class name
     * set to the given class name.
     * 
     * @param className A new class name to use in the new ComponentName.
     * @return Returns a new ComponentName with the same package name as this one, but the updated class name.
     */
    public ComponentName cloneWithClassName(String className) {
        return new ComponentName(mPackage, className);
    }
    
    /**
     * Return a formatted string containing the package and class name.
     * 
     * @return A string formatted as "package/class".
     */
    public String flattenToString() {
        return mPackage + "/" + mClass;
    }
    
    /**
     * Create a ComponentName from a string that contains both the package name and class name.
     * 
     * @param str A string formatted as "package/class".
     * @return A new ComponentName, or null if the string is not in the correct format.
     */
    public static ComponentName unflattenFromString(String str) {
        if (str == null) return null;
        int pos = str.indexOf('/');
        if (pos < 0) return null;
        return new ComponentName(str.substring(0, pos), str.substring(pos + 1));
    }
    
    @Override
    public String toString() {
        return "ComponentName{" + mPackage + "/" + mClass + "}";
    }
    
    @Override
    public boolean equals(Object obj) {
        if (obj == null) return false;
        if (obj == this) return true;
        try {
            ComponentName other = (ComponentName)obj;
            return mPackage.equals(other.mPackage) && mClass.equals(other.mClass);
        } catch (ClassCastException e) {
            return false;
        }
    }
    
    @Override
    public int hashCode() {
        return mPackage.hashCode() + mClass.hashCode();
    }
    
    @Override
    public int describeContents() {
        return 0;
    }
    
    @Override
    public void writeToParcel(Parcel out, int flags) {
        out.writeString(mPackage);
        out.writeString(mClass);
    }
    
    public static final Creator<ComponentName> CREATOR = new Creator<ComponentName>() {
        @Override
        public ComponentName createFromParcel(Parcel in) {
            return new ComponentName(in.readString(), in.readString());
        }
        
        @Override
        public ComponentName[] newArray(int size) {
            return new ComponentName[size];
        }
    };
}