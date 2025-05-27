package android.content;

import java.util.Map;
import java.util.Set;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

/**
 * Mock implementation of Android SharedPreferences class for development outside of Android.
 * Interface for accessing and modifying preference data returned by Context.getSharedPreferences().
 */
public interface SharedPreferences {
    /**
     * Retrieve all values from the preferences.
     * 
     * @return A map containing all preferences.
     */
    @NonNull
    Map<String, ?> getAll();
    
    /**
     * Retrieve a String value from the preferences.
     * 
     * @param key The name of the preference to retrieve.
     * @param defValue Value to return if this preference does not exist.
     * @return The preference value if it exists, or defValue.
     */
    @Nullable
    String getString(String key, @Nullable String defValue);
    
    /**
     * Retrieve a set of String values from the preferences.
     * 
     * @param key The name of the preference to retrieve.
     * @param defValues Values to return if this preference does not exist.
     * @return The preference values if they exist, or defValues.
     */
    @Nullable
    Set<String> getStringSet(String key, @Nullable Set<String> defValues);
    
    /**
     * Retrieve an int value from the preferences.
     * 
     * @param key The name of the preference to retrieve.
     * @param defValue Value to return if this preference does not exist.
     * @return The preference value if it exists, or defValue.
     */
    int getInt(String key, int defValue);
    
    /**
     * Retrieve a long value from the preferences.
     * 
     * @param key The name of the preference to retrieve.
     * @param defValue Value to return if this preference does not exist.
     * @return The preference value if it exists, or defValue.
     */
    long getLong(String key, long defValue);
    
    /**
     * Retrieve a float value from the preferences.
     * 
     * @param key The name of the preference to retrieve.
     * @param defValue Value to return if this preference does not exist.
     * @return The preference value if it exists, or defValue.
     */
    float getFloat(String key, float defValue);
    
    /**
     * Retrieve a boolean value from the preferences.
     * 
     * @param key The name of the preference to retrieve.
     * @param defValue Value to return if this preference does not exist.
     * @return The preference value if it exists, or defValue.
     */
    boolean getBoolean(String key, boolean defValue);
    
    /**
     * Check whether the preferences contains a preference.
     * 
     * @param key The name of the preference to check.
     * @return True if the preference exists.
     */
    boolean contains(String key);
    
    /**
     * Create a new Editor for these preferences.
     * 
     * @return A new instance of the Editor interface.
     */
    @NonNull
    Editor edit();
    
    /**
     * Register a callback to be invoked when a preference is changed.
     * 
     * @param listener The callback to be invoked.
     */
    void registerOnSharedPreferenceChangeListener(OnSharedPreferenceChangeListener listener);
    
    /**
     * Unregister a previously registered callback.
     * 
     * @param listener The callback to be unregistered.
     */
    void unregisterOnSharedPreferenceChangeListener(OnSharedPreferenceChangeListener listener);
    
    /**
     * Interface used for modifying values in a SharedPreferences object. 
     * All changes you make in an editor are batched, and not copied back to the original
     * SharedPreferences until you call commit() or apply().
     */
    interface Editor {
        /**
         * Set a String value in the preferences editor.
         * 
         * @param key The name of the preference to modify.
         * @param value The new value for the preference.
         * @return A reference to the same Editor object.
         */
        @NonNull
        Editor putString(String key, @Nullable String value);
        
        /**
         * Set a set of String values in the preferences editor.
         * 
         * @param key The name of the preference to modify.
         * @param values The new values for the preference.
         * @return A reference to the same Editor object.
         */
        @NonNull
        Editor putStringSet(String key, @Nullable Set<String> values);
        
        /**
         * Set an int value in the preferences editor.
         * 
         * @param key The name of the preference to modify.
         * @param value The new value for the preference.
         * @return A reference to the same Editor object.
         */
        @NonNull
        Editor putInt(String key, int value);
        
        /**
         * Set a long value in the preferences editor.
         * 
         * @param key The name of the preference to modify.
         * @param value The new value for the preference.
         * @return A reference to the same Editor object.
         */
        @NonNull
        Editor putLong(String key, long value);
        
        /**
         * Set a float value in the preferences editor.
         * 
         * @param key The name of the preference to modify.
         * @param value The new value for the preference.
         * @return A reference to the same Editor object.
         */
        @NonNull
        Editor putFloat(String key, float value);
        
        /**
         * Set a boolean value in the preferences editor.
         * 
         * @param key The name of the preference to modify.
         * @param value The new value for the preference.
         * @return A reference to the same Editor object.
         */
        @NonNull
        Editor putBoolean(String key, boolean value);
        
        /**
         * Mark a preference to be removed.
         * 
         * @param key The name of the preference to remove.
         * @return A reference to the same Editor object.
         */
        @NonNull
        Editor remove(String key);
        
        /**
         * Mark all preferences to be removed.
         * 
         * @return A reference to the same Editor object.
         */
        @NonNull
        Editor clear();
        
        /**
         * Commit your changes back to the SharedPreferences object.
         * 
         * @return True if the changes were successfully written to persistent storage.
         */
        boolean commit();
        
        /**
         * Asynchronously commit the changes to the SharedPreferences object.
         */
        void apply();
    }
    
    /**
     * Callback interface for listening to changes to the SharedPreferences.
     */
    interface OnSharedPreferenceChangeListener {
        /**
         * Called when a shared preference is changed, added, or removed.
         * 
         * @param sharedPreferences The SharedPreferences that received the change.
         * @param key The key of the preference that was changed, added, or removed.
         */
        void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key);
    }
}