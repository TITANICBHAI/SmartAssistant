package com.aiassistant.viewmodels;

import android.app.Application;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

/**
 * ViewModel for managing app settings.
 */
public class SettingsViewModel extends AndroidViewModel {
    
    private static final String PREF_START_ON_BOOT = "pref_start_on_boot";
    private static final String PREF_DATA_COLLECTION = "pref_data_collection";
    private static final String PREF_LEARNING_RATE = "pref_learning_rate";
    private static final String PREF_MODEL_PATH = "pref_model_path";
    private static final String PREF_DEBUG_MODE = "pref_debug_mode";
    
    private final SharedPreferences preferences;
    
    private final MutableLiveData<Boolean> startOnBoot = new MutableLiveData<>();
    private final MutableLiveData<Boolean> dataCollectionEnabled = new MutableLiveData<>();
    private final MutableLiveData<Integer> learningRate = new MutableLiveData<>();
    private final MutableLiveData<String> modelPath = new MutableLiveData<>();
    private final MutableLiveData<Boolean> debugMode = new MutableLiveData<>();
    
    public SettingsViewModel(@NonNull Application application) {
        super(application);
        
        preferences = PreferenceManager.getDefaultSharedPreferences(application);
        
        // Load initial values from SharedPreferences
        loadSettings();
    }
    
    /**
     * Load settings from SharedPreferences
     */
    private void loadSettings() {
        startOnBoot.setValue(preferences.getBoolean(PREF_START_ON_BOOT, true));
        dataCollectionEnabled.setValue(preferences.getBoolean(PREF_DATA_COLLECTION, true));
        learningRate.setValue(preferences.getInt(PREF_LEARNING_RATE, 50));
        modelPath.setValue(preferences.getString(PREF_MODEL_PATH, "/sdcard/Android/data/com.aiassistant/models"));
        debugMode.setValue(preferences.getBoolean(PREF_DEBUG_MODE, false));
    }
    
    /**
     * Save a setting to SharedPreferences
     */
    private void saveSetting(String key, Object value) {
        SharedPreferences.Editor editor = preferences.edit();
        
        if (value instanceof Boolean) {
            editor.putBoolean(key, (Boolean) value);
        } else if (value instanceof Integer) {
            editor.putInt(key, (Integer) value);
        } else if (value instanceof String) {
            editor.putString(key, (String) value);
        }
        
        editor.apply();
    }
    
    /**
     * Set start on boot
     */
    public void setStartOnBoot(boolean startOnBoot) {
        this.startOnBoot.setValue(startOnBoot);
        saveSetting(PREF_START_ON_BOOT, startOnBoot);
    }
    
    /**
     * Set data collection enabled
     */
    public void setDataCollectionEnabled(boolean enabled) {
        this.dataCollectionEnabled.setValue(enabled);
        saveSetting(PREF_DATA_COLLECTION, enabled);
    }
    
    /**
     * Set learning rate (0-100)
     */
    public void setLearningRate(int rate) {
        this.learningRate.setValue(rate);
        saveSetting(PREF_LEARNING_RATE, rate);
    }
    
    /**
     * Set model path
     */
    public void setModelPath(String path) {
        this.modelPath.setValue(path);
        saveSetting(PREF_MODEL_PATH, path);
    }
    
    /**
     * Set debug mode
     */
    public void setDebugMode(boolean enabled) {
        this.debugMode.setValue(enabled);
        saveSetting(PREF_DEBUG_MODE, enabled);
    }
    
    /**
     * Get start on boot setting
     */
    public LiveData<Boolean> getStartOnBoot() {
        return startOnBoot;
    }
    
    /**
     * Get data collection enabled setting
     */
    public LiveData<Boolean> getDataCollectionEnabled() {
        return dataCollectionEnabled;
    }
    
    /**
     * Get learning rate setting
     */
    public LiveData<Integer> getLearningRate() {
        return learningRate;
    }
    
    /**
     * Get model path setting
     */
    public LiveData<String> getModelPath() {
        return modelPath;
    }
    
    /**
     * Get debug mode setting
     */
    public LiveData<Boolean> getDebugMode() {
        return debugMode;
    }
    
    /**
     * Reset all settings to defaults
     */
    public void resetToDefaults() {
        setStartOnBoot(true);
        setDataCollectionEnabled(true);
        setLearningRate(50);
        setModelPath("/sdcard/Android/data/com.aiassistant/models");
        setDebugMode(false);
    }
}
