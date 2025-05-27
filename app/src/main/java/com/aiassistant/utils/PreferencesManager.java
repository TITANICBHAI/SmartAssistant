package com.aiassistant.utils;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import java.util.Set;

/**
 * Preferences Manager
 * Utility for managing shared preferences
 */
public class PreferencesManager {
    private static final String TAG = "PreferencesManager";
    
    // Preferences file name
    private static final String PREFS_NAME = "aiassistant_prefs";
    
    // Singleton instance
    private static PreferencesManager instance;
    
    // Shared preferences
    private SharedPreferences prefs;
    
    /**
     * Get the singleton instance
     */
    public static synchronized PreferencesManager getInstance(Context context) {
        if (instance == null) {
            instance = new PreferencesManager(context.getApplicationContext());
        }
        return instance;
    }
    
    /**
     * Private constructor
     */
    private PreferencesManager(Context context) {
        prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
    }
    
    /**
     * Put a string value
     */
    public void putString(String key, String value) {
        prefs.edit().putString(key, value).apply();
    }
    
    /**
     * Get a string value
     */
    public String getString(String key, String defaultValue) {
        return prefs.getString(key, defaultValue);
    }
    
    /**
     * Put an integer value
     */
    public void putInt(String key, int value) {
        prefs.edit().putInt(key, value).apply();
    }
    
    /**
     * Get an integer value
     */
    public int getInt(String key, int defaultValue) {
        return prefs.getInt(key, defaultValue);
    }
    
    /**
     * Put a long value
     */
    public void putLong(String key, long value) {
        prefs.edit().putLong(key, value).apply();
    }
    
    /**
     * Get a long value
     */
    public long getLong(String key, long defaultValue) {
        return prefs.getLong(key, defaultValue);
    }
    
    /**
     * Put a float value
     */
    public void putFloat(String key, float value) {
        prefs.edit().putFloat(key, value).apply();
    }
    
    /**
     * Get a float value
     */
    public float getFloat(String key, float defaultValue) {
        return prefs.getFloat(key, defaultValue);
    }
    
    /**
     * Put a boolean value
     */
    public void putBoolean(String key, boolean value) {
        prefs.edit().putBoolean(key, value).apply();
    }
    
    /**
     * Get a boolean value
     */
    public boolean getBoolean(String key, boolean defaultValue) {
        return prefs.getBoolean(key, defaultValue);
    }
    
    /**
     * Put a string set
     */
    public void putStringSet(String key, Set<String> values) {
        prefs.edit().putStringSet(key, values).apply();
    }
    
    /**
     * Get a string set
     */
    public Set<String> getStringSet(String key, Set<String> defaultValues) {
        return prefs.getStringSet(key, defaultValues);
    }
    
    /**
     * Check if a key exists
     */
    public boolean contains(String key) {
        return prefs.contains(key);
    }
    
    /**
     * Remove a key
     */
    public void remove(String key) {
        prefs.edit().remove(key).apply();
    }
    
    /**
     * Clear all preferences
     */
    public void clear() {
        prefs.edit().clear().apply();
        Log.i(TAG, "Preferences cleared");
    }
}