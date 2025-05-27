package android.content;

import android.os.Parcel;
import android.os.Parcelable;

import java.util.ArrayList;
import java.util.List;

/**
 * Mock implementation of Android IntentFilter class for development outside of Android.
 * Describes what types of intents a component is capable of receiving.
 */
public class IntentFilter implements Parcelable {
    private List<String> mActions;
    private List<String> mCategories;
    private List<String> mDataSchemes;
    private List<String> mDataTypes;
    
    /**
     * Create a new IntentFilter instance with no actions or data types.
     */
    public IntentFilter() {
        mActions = new ArrayList<>();
        mCategories = new ArrayList<>();
        mDataSchemes = new ArrayList<>();
        mDataTypes = new ArrayList<>();
    }
    
    /**
     * Create a new IntentFilter instance with a single action.
     * 
     * @param action The action to match against.
     */
    public IntentFilter(String action) {
        this();
        addAction(action);
    }
    
    /**
     * Create a new IntentFilter instance with a single action and data type.
     * 
     * @param action The action to match against.
     * @param dataType The data type to match against.
     */
    public IntentFilter(String action, String dataType) {
        this(action);
        addDataType(dataType);
    }
    
    /**
     * Add a new Intent action to match against.
     * 
     * @param action The action to match against.
     */
    public void addAction(String action) {
        if (action != null && !mActions.contains(action)) {
            mActions.add(action);
        }
    }
    
    /**
     * Get the list of Intent actions that this filter matches against.
     * 
     * @return The list of actions.
     */
    public String getAction(int index) {
        return mActions.get(index);
    }
    
    /**
     * Get the number of Intent actions in this filter.
     * 
     * @return The number of actions.
     */
    public int countActions() {
        return mActions.size();
    }
    
    /**
     * Check if this filter matches the given action.
     * 
     * @param action The action to check.
     * @return True if the action is listed in this filter.
     */
    public boolean hasAction(String action) {
        return mActions.contains(action);
    }
    
    /**
     * Match this filter against an Intent's action.
     * 
     * @param action The Intent action.
     * @return True if the filter matches.
     */
    public boolean matchAction(String action) {
        return mActions.isEmpty() || mActions.contains(action);
    }
    
    /**
     * Add a new Intent category to match against.
     * 
     * @param category The category to match against.
     */
    public void addCategory(String category) {
        if (category != null && !mCategories.contains(category)) {
            mCategories.add(category);
        }
    }
    
    /**
     * Get the list of Intent categories that this filter matches against.
     * 
     * @return The list of categories.
     */
    public String getCategory(int index) {
        return mCategories.get(index);
    }
    
    /**
     * Get the number of Intent categories in this filter.
     * 
     * @return The number of categories.
     */
    public int countCategories() {
        return mCategories.size();
    }
    
    /**
     * Check if this filter matches the given category.
     * 
     * @param category The category to check.
     * @return True if the category is listed in this filter.
     */
    public boolean hasCategory(String category) {
        return mCategories.contains(category);
    }
    
    /**
     * Add a data type to match against.
     * 
     * @param type The data type to match against.
     */
    public void addDataType(String type) {
        if (type != null && !mDataTypes.contains(type)) {
            mDataTypes.add(type);
        }
    }
    
    /**
     * Get the list of data types that this filter matches against.
     * 
     * @return The list of data types.
     */
    public String getDataType(int index) {
        return mDataTypes.get(index);
    }
    
    /**
     * Get the number of data types in this filter.
     * 
     * @return The number of data types.
     */
    public int countDataTypes() {
        return mDataTypes.size();
    }
    
    /**
     * Check if this filter matches the given data type.
     * 
     * @param type The data type to check.
     * @return True if the data type is listed in this filter.
     */
    public boolean hasDataType(String type) {
        return mDataTypes.contains(type);
    }
    
    /**
     * Add a data scheme to match against.
     * 
     * @param scheme The data scheme to match against.
     */
    public void addDataScheme(String scheme) {
        if (scheme != null && !mDataSchemes.contains(scheme)) {
            mDataSchemes.add(scheme);
        }
    }
    
    /**
     * Get the list of data schemes that this filter matches against.
     * 
     * @return The list of data schemes.
     */
    public String getDataScheme(int index) {
        return mDataSchemes.get(index);
    }
    
    /**
     * Get the number of data schemes in this filter.
     * 
     * @return The number of data schemes.
     */
    public int countDataSchemes() {
        return mDataSchemes.size();
    }
    
    /**
     * Check if this filter matches the given data scheme.
     * 
     * @param scheme The data scheme to check.
     * @return True if the data scheme is listed in this filter.
     */
    public boolean hasDataScheme(String scheme) {
        return mDataSchemes.contains(scheme);
    }
    
    /**
     * Match this filter against an Intent's data type.
     * 
     * @param type The Intent data type.
     * @return True if the filter matches.
     */
    public boolean matchDataType(String type) {
        if (mDataTypes.isEmpty()) {
            return true;
        }
        
        if (type == null) {
            return false;
        }
        
        // TODO: Add mime-type matching logic
        return mDataTypes.contains(type);
    }
    
    /**
     * Match this filter against an Intent.
     * 
     * @param intent The Intent to match against.
     * @return True if the filter matches.
     */
    public boolean match(Intent intent) {
        if (!matchAction(intent.getAction())) {
            return false;
        }
        
        if (!matchDataType(intent.getType())) {
            return false;
        }
        
        return true;
    }
    
    @Override
    public int describeContents() {
        return 0;
    }
    
    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeStringList(mActions);
        dest.writeStringList(mCategories);
        dest.writeStringList(mDataSchemes);
        dest.writeStringList(mDataTypes);
    }
    
    public static final Parcelable.Creator<IntentFilter> CREATOR = new Parcelable.Creator<IntentFilter>() {
        @Override
        public IntentFilter createFromParcel(Parcel source) {
            IntentFilter filter = new IntentFilter();
            source.readStringList(filter.mActions);
            source.readStringList(filter.mCategories);
            source.readStringList(filter.mDataSchemes);
            source.readStringList(filter.mDataTypes);
            return filter;
        }
        
        @Override
        public IntentFilter[] newArray(int size) {
            return new IntentFilter[size];
        }
    };
}