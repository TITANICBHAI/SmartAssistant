package android.content;

import android.net.Uri;
import android.os.Bundle;
import android.os.Parcel;
import android.os.Parcelable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

/**
 * Mock implementation of Android Intent class for development outside of Android.
 * An intent is an abstract description of an operation to be performed.
 */
public class Intent implements Parcelable {
    /**
     * Standard activity action: Display the data to the user.
     */
    public static final String ACTION_VIEW = "android.intent.action.VIEW";
    
    /**
     * Standard activity action: Start as the main entry point.
     */
    public static final String ACTION_MAIN = "android.intent.action.MAIN";
    
    /**
     * Standard activity action: Send data to someone else.
     */
    public static final String ACTION_SEND = "android.intent.action.SEND";
    
    /**
     * Intent extra used to define the content for ACTION_SEND intents.
     */
    public static final String EXTRA_TEXT = "android.intent.extra.TEXT";
    
    /**
     * Intent extra used to define the email addresses for ACTION_SEND intents.
     */
    public static final String EXTRA_EMAIL = "android.intent.extra.EMAIL";
    
    /**
     * Intent extra used to define the email subject for ACTION_SEND intents.
     */
    public static final String EXTRA_SUBJECT = "android.intent.extra.SUBJECT";
    
    /**
     * Standard launcher category.  An activity with this category appears in the launcher.
     */
    public static final String CATEGORY_LAUNCHER = "android.intent.category.LAUNCHER";
    
    /**
     * The action of this intent.
     */
    private String mAction;
    
    /**
     * The data this intent is operating on.
     */
    private Uri mData;
    
    /**
     * The type of the data this intent is operating on.
     */
    private String mType;
    
    /**
     * The name of the package that this intent is targeted to.
     */
    private String mPackage;
    
    /**
     * The name of the component that this intent is targeted to.
     */
    private ComponentName mComponent;
    
    /**
     * The set of categories for this intent.
     */
    private ArrayList<String> mCategories;
    
    /**
     * The map of extended data for this intent.
     */
    private Bundle mExtras;
    
    /**
     * Create an empty intent.
     */
    public Intent() {
        mExtras = new Bundle();
    }
    
    /**
     * Create an intent with a given action.
     * 
     * @param action The Intent action, such as ACTION_VIEW.
     */
    public Intent(String action) {
        this();
        mAction = action;
    }
    
    /**
     * Create an intent for a specific component.
     * 
     * @param packageContext The package that contains the class.
     * @param cls The component class to launch.
     */
    public Intent(Context packageContext, Class<?> cls) {
        this();
        mComponent = new ComponentName(packageContext.getPackageName(), cls.getName());
    }
    
    /**
     * Create an intent based on a URI.
     * 
     * @param action The Intent action, such as ACTION_VIEW.
     * @param uri The URI to act on.
     */
    public Intent(String action, Uri uri) {
        this();
        mAction = action;
        mData = uri;
    }
    
    /**
     * Copy constructor.
     * 
     * @param o The original intent.
     */
    public Intent(Intent o) {
        this();
        mAction = o.mAction;
        mData = o.mData;
        mType = o.mType;
        mPackage = o.mPackage;
        mComponent = o.mComponent;
        if (o.mCategories != null) {
            mCategories = new ArrayList<>(o.mCategories);
        }
        if (o.mExtras != null) {
            mExtras = new Bundle(o.mExtras);
        }
    }
    
    /**
     * Set the general action to be performed.
     * 
     * @param action The action to perform.
     * @return Returns the same Intent object, for chaining.
     */
    public Intent setAction(String action) {
        mAction = action;
        return this;
    }
    
    /**
     * Retrieve the general action to be performed, such as ACTION_VIEW.
     * 
     * @return The action for this intent.
     */
    public String getAction() {
        return mAction;
    }
    
    /**
     * Set the data this intent is operating on.
     * 
     * @param data The URI to act on.
     * @return Returns the same Intent object, for chaining.
     */
    public Intent setData(Uri data) {
        mData = data;
        return this;
    }
    
    /**
     * Retrieve the data this intent is operating on.
     * 
     * @return The URI of the data.
     */
    public Uri getData() {
        return mData;
    }
    
    /**
     * Set an explicit MIME data type.
     * 
     * @param type The MIME data type.
     * @return Returns the same Intent object, for chaining.
     */
    public Intent setType(String type) {
        mType = type;
        return this;
    }
    
    /**
     * Retrieve the explicit MIME data type.
     * 
     * @return The MIME data type, or null if unspecified.
     */
    public String getType() {
        return mType;
    }
    
    /**
     * Set the data URI and MIME type.
     * 
     * @param data The URI to act on.
     * @param type The MIME data type.
     * @return Returns the same Intent object, for chaining.
     */
    public Intent setDataAndType(Uri data, String type) {
        mData = data;
        mType = type;
        return this;
    }
    
    /**
     * Add a category to an intent.
     * 
     * @param category The category to add.
     * @return Returns the same Intent object, for chaining.
     */
    public Intent addCategory(String category) {
        if (mCategories == null) {
            mCategories = new ArrayList<>();
        }
        if (!mCategories.contains(category)) {
            mCategories.add(category);
        }
        return this;
    }
    
    /**
     * Set the component for this intent.
     * 
     * @param component The component for this intent.
     * @return Returns the same Intent object, for chaining.
     */
    public Intent setComponent(ComponentName component) {
        mComponent = component;
        return this;
    }
    
    /**
     * Retrieve the component for this intent.
     * 
     * @return The component, or null if unspecified.
     */
    public ComponentName getComponent() {
        return mComponent;
    }
    
    /**
     * Set the package for this intent.
     * 
     * @param packageName The package for this intent.
     * @return Returns the same Intent object, for chaining.
     */
    public Intent setPackage(String packageName) {
        mPackage = packageName;
        return this;
    }
    
    /**
     * Retrieve the package for this intent.
     * 
     * @return The package, or null if unspecified.
     */
    public String getPackage() {
        return mPackage;
    }
    
    /**
     * Add extended data to the intent.
     * 
     * @param name The name of the extended data.
     * @param value The value of the extended data.
     * @return Returns the same Intent object, for chaining.
     */
    public Intent putExtra(String name, String value) {
        if (mExtras == null) {
            mExtras = new Bundle();
        }
        mExtras.putString(name, value);
        return this;
    }
    
    /**
     * Add extended data to the intent.
     * 
     * @param name The name of the extended data.
     * @param value The value of the extended data.
     * @return Returns the same Intent object, for chaining.
     */
    public Intent putExtra(String name, boolean value) {
        if (mExtras == null) {
            mExtras = new Bundle();
        }
        mExtras.putBoolean(name, value);
        return this;
    }
    
    /**
     * Add extended data to the intent.
     * 
     * @param name The name of the extended data.
     * @param value The value of the extended data.
     * @return Returns the same Intent object, for chaining.
     */
    public Intent putExtra(String name, int value) {
        if (mExtras == null) {
            mExtras = new Bundle();
        }
        mExtras.putInt(name, value);
        return this;
    }
    
    /**
     * Add extended data to the intent.
     * 
     * @param name The name of the extended data.
     * @param value The value of the extended data.
     * @return Returns the same Intent object, for chaining.
     */
    public Intent putExtra(String name, long value) {
        if (mExtras == null) {
            mExtras = new Bundle();
        }
        mExtras.putLong(name, value);
        return this;
    }
    
    /**
     * Add extended data to the intent.
     * 
     * @param name The name of the extended data.
     * @param value The value of the extended data.
     * @return Returns the same Intent object, for chaining.
     */
    public Intent putExtra(String name, float value) {
        if (mExtras == null) {
            mExtras = new Bundle();
        }
        mExtras.putFloat(name, value);
        return this;
    }
    
    /**
     * Add extended data to the intent.
     * 
     * @param name The name of the extended data.
     * @param value The value of the extended data.
     * @return Returns the same Intent object, for chaining.
     */
    public Intent putExtra(String name, double value) {
        if (mExtras == null) {
            mExtras = new Bundle();
        }
        mExtras.putDouble(name, value);
        return this;
    }
    
    /**
     * Add extended data to the intent.
     * 
     * @param name The name of the extended data.
     * @param value The value of the extended data.
     * @return Returns the same Intent object, for chaining.
     */
    public Intent putExtra(String name, String[] value) {
        if (mExtras == null) {
            mExtras = new Bundle();
        }
        mExtras.putStringArray(name, value);
        return this;
    }
    
    /**
     * Add extended data to the intent.
     * 
     * @param name The name of the extended data.
     * @param value The value of the extended data.
     * @return Returns the same Intent object, for chaining.
     */
    public Intent putExtra(String name, Bundle value) {
        if (mExtras == null) {
            mExtras = new Bundle();
        }
        mExtras.putBundle(name, value);
        return this;
    }
    
    /**
     * Retrieve extended data from the intent.
     * 
     * @param name The name of the desired item.
     * @return The value of the item, or null if not found.
     */
    public String getStringExtra(String name) {
        return mExtras != null ? mExtras.getString(name) : null;
    }
    
    /**
     * Retrieve extended data from the intent.
     * 
     * @param name The name of the desired item.
     * @return The value of the item, or false if not found.
     */
    public boolean getBooleanExtra(String name, boolean defaultValue) {
        return mExtras != null ? mExtras.getBoolean(name, defaultValue) : defaultValue;
    }
    
    /**
     * Retrieve extended data from the intent.
     * 
     * @param name The name of the desired item.
     * @return The value of the item, or 0 if not found.
     */
    public int getIntExtra(String name, int defaultValue) {
        return mExtras != null ? mExtras.getInt(name, defaultValue) : defaultValue;
    }
    
    /**
     * Retrieve extended data from the intent.
     * 
     * @param name The name of the desired item.
     * @return The value of the item, or 0L if not found.
     */
    public long getLongExtra(String name, long defaultValue) {
        return mExtras != null ? mExtras.getLong(name, defaultValue) : defaultValue;
    }
    
    /**
     * Retrieve extended data from the intent.
     * 
     * @param name The name of the desired item.
     * @return The value of the item, or 0f if not found.
     */
    public float getFloatExtra(String name, float defaultValue) {
        return mExtras != null ? mExtras.getFloat(name, defaultValue) : defaultValue;
    }
    
    /**
     * Retrieve extended data from the intent.
     * 
     * @param name The name of the desired item.
     * @return The value of the item, or 0d if not found.
     */
    public double getDoubleExtra(String name, double defaultValue) {
        return mExtras != null ? mExtras.getDouble(name, defaultValue) : defaultValue;
    }
    
    /**
     * Retrieve extended data from the intent.
     * 
     * @param name The name of the desired item.
     * @return The value of the item, or null if not found.
     */
    public String[] getStringArrayExtra(String name) {
        return mExtras != null ? mExtras.getStringArray(name) : null;
    }
    
    /**
     * Retrieve extended data from the intent.
     * 
     * @param name The name of the desired item.
     * @return The value of the item, or null if not found.
     */
    public Bundle getBundleExtra(String name) {
        return mExtras != null ? mExtras.getBundle(name) : null;
    }
    
    /**
     * Retrieve all extended data in this intent.
     * 
     * @return The map of all extended data in this intent, or null if none exists.
     */
    public Bundle getExtras() {
        return mExtras != null ? new Bundle(mExtras) : null;
    }
    
    @Override
    public int describeContents() {
        return (mExtras != null) ? 1 : 0;
    }
    
    @Override
    public void writeToParcel(Parcel out, int flags) {
        out.writeString(mAction);
        out.writeParcelable(mData, flags);
        out.writeString(mType);
        out.writeString(mPackage);
        out.writeParcelable(mComponent, flags);
        
        if (mCategories != null) {
            out.writeInt(mCategories.size());
            out.writeStringList(mCategories);
        } else {
            out.writeInt(-1);
        }
        
        out.writeBundle(mExtras);
    }
    
    public static final Creator<Intent> CREATOR = new Creator<Intent>() {
        @Override
        public Intent createFromParcel(Parcel in) {
            Intent intent = new Intent();
            intent.mAction = in.readString();
            intent.mData = (Uri)in.readParcelable(Uri.class.getClassLoader());
            intent.mType = in.readString();
            intent.mPackage = in.readString();
            intent.mComponent = (ComponentName)in.readParcelable(ComponentName.class.getClassLoader());
            
            int N = in.readInt();
            if (N >= 0) {
                intent.mCategories = new ArrayList<String>(N);
                in.readStringList(intent.mCategories);
            }
            
            intent.mExtras = in.readBundle();
            
            return intent;
        }
        
        @Override
        public Intent[] newArray(int size) {
            return new Intent[size];
        }
    };
}