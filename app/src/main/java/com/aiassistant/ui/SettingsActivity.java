package com.aiassistant.ui;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.PowerManager;
import android.provider.Settings;
import android.util.Log;
import android.view.MenuItem;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.preference.ListPreference;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;
import androidx.preference.PreferenceManager;
import androidx.preference.SwitchPreferenceCompat;

import com.aiassistant.R;
import com.aiassistant.services.AIBackgroundService;
import utils.PermissionManager;

/**
 * Settings activity for the AI assistant app
 * Allows users to configure AI behavior, learning settings, and other preferences
 */
public class SettingsActivity extends AppCompatActivity {

    private static final String TAG = "SettingsActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);
        
        // Add settings fragment
        if (savedInstanceState == null) {
            getSupportFragmentManager()
                    .beginTransaction()
                    .replace(R.id.settings_container, new SettingsFragment())
                    .commit();
        }
        
        // Setup action bar with back button
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
            actionBar.setTitle(R.string.settings_title);
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            onBackPressed();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    /**
     * Settings Fragment that shows the preferences
     */
    public static class SettingsFragment extends PreferenceFragmentCompat implements
            SharedPreferences.OnSharedPreferenceChangeListener {

        private PermissionManager permissionManager;

        @Override
        public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
            setPreferencesFromResource(R.xml.preferences, rootKey);
            permissionManager = new PermissionManager(requireActivity());
            
            // Initialize preference handlers
            setupBatteryOptimizationPreference();
            setupPermissionPreferences();
            setupAIModesPreference();
            setupPrivacySettings();
            setupAdvancedSettings();
        }

        @Override
        public void onResume() {
            super.onResume();
            // Register preference change listener
            PreferenceManager.getDefaultSharedPreferences(requireContext())
                    .registerOnSharedPreferenceChangeListener(this);
            
            // Update preferences that depend on system state
            updateBatteryOptimizationStatus();
            updatePermissionStatuses();
        }

        @Override
        public void onPause() {
            super.onPause();
            // Unregister preference change listener
            PreferenceManager.getDefaultSharedPreferences(requireContext())
                    .unregisterOnSharedPreferenceChangeListener(this);
        }

        @Override
        public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
            // Handle preference changes
            switch (key) {
                case "ai_mode":
                    updateAIMode(sharedPreferences.getString("ai_mode", "balanced"));
                    break;
                case "enable_learning":
                    updateLearningState(sharedPreferences.getBoolean("enable_learning", true));
                    break;
                case "background_service":
                    updateBackgroundService(sharedPreferences.getBoolean("background_service", true));
                    break;
                case "privacy_mode":
                    updatePrivacyMode(sharedPreferences.getBoolean("privacy_mode", false));
                    break;
                case "low_power_mode":
                    updateLowPowerMode(sharedPreferences.getBoolean("low_power_mode", false));
                    break;
            }
        }

        private void setupBatteryOptimizationPreference() {
            Preference batteryOptPref = findPreference("disable_battery_optimization");
            if (batteryOptPref != null) {
                batteryOptPref.setOnPreferenceClickListener(preference -> {
                    Intent intent = new Intent();
                    intent.setAction(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS);
                    intent.setData(android.net.Uri.parse("package:" + requireContext().getPackageName()));
                    startActivity(intent);
                    return true;
                });
            }
        }

        private void updateBatteryOptimizationStatus() {
            Preference batteryOptPref = findPreference("disable_battery_optimization");
            if (batteryOptPref != null) {
                PowerManager pm = (PowerManager) requireContext().getSystemService(Context.POWER_SERVICE);
                boolean isIgnoringBatteryOptimizations = 
                        pm.isIgnoringBatteryOptimizations(requireContext().getPackageName());
                
                batteryOptPref.setSummary(isIgnoringBatteryOptimizations ?
                        R.string.battery_optimization_disabled :
                        R.string.battery_optimization_enabled);
            }
        }

        private void setupPermissionPreferences() {
            setupPermissionPreference("accessibility_permission");
            setupPermissionPreference("overlay_permission");
            setupPermissionPreference("admin_permission");
            setupPermissionPreference("usage_stats_permission");
        }

        private void setupPermissionPreference(String key) {
            Preference pref = findPreference(key);
            if (pref != null) {
                pref.setOnPreferenceClickListener(preference -> {
                    switch (key) {
                        case "accessibility_permission":
                            permissionManager.requestAccessibilityPermission();
                            break;
                        case "overlay_permission":
                            permissionManager.requestOverlayPermission();
                            break;
                        case "admin_permission":
                            permissionManager.requestDeviceAdminPermission();
                            break;
                        case "usage_stats_permission":
                            permissionManager.requestUsageStatsPermission();
                            break;
                    }
                    return true;
                });
            }
        }

        private void updatePermissionStatuses() {
            updatePermissionStatus("accessibility_permission", 
                    permissionManager.hasAccessibilityPermission());
            updatePermissionStatus("overlay_permission", 
                    permissionManager.hasOverlayPermission());
            updatePermissionStatus("admin_permission", 
                    permissionManager.hasDeviceAdminPermission());
            updatePermissionStatus("usage_stats_permission", 
                    permissionManager.hasUsageStatsPermission());
        }

        private void updatePermissionStatus(String key, boolean granted) {
            Preference pref = findPreference(key);
            if (pref != null) {
                pref.setSummary(granted ? R.string.permission_granted : R.string.permission_not_granted);
            }
        }

        private void setupAIModesPreference() {
            ListPreference aiModePref = findPreference("ai_mode");
            if (aiModePref != null) {
                aiModePref.setSummaryProvider(ListPreference.SimpleSummaryProvider.getInstance());
            }
        }

        private void setupPrivacySettings() {
            SwitchPreferenceCompat privacyModePref = findPreference("privacy_mode");
            if (privacyModePref != null) {
                // Add any special handling for privacy mode changes
            }
        }

        private void setupAdvancedSettings() {
            Preference resetPref = findPreference("reset_ai");
            if (resetPref != null) {
                resetPref.setOnPreferenceClickListener(preference -> {
                    showResetConfirmation();
                    return true;
                });
            }
            
            SwitchPreferenceCompat devModePref = findPreference("developer_mode");
            if (devModePref != null) {
                // Add any special handling for developer mode
            }
        }

        private void showResetConfirmation() {
            // In a full implementation, this would show a dialog asking for confirmation
            // For now, we just display a toast
            Toast.makeText(getContext(), R.string.reset_confirmation, Toast.LENGTH_LONG).show();
        }

        private void updateAIMode(String mode) {
            Log.d(TAG, "Updating AI mode to: " + mode);
            // In a real implementation, this would update the AI mode in background service
            Intent intent = new Intent(requireContext(), AIBackgroundService.class);
            intent.setAction("UPDATE_AI_MODE");
            requireContext().startService(intent);
        }

        private void updateLearningState(boolean enabled) {
            Log.d(TAG, "Learning system enabled: " + enabled);
            // Update learning system state
        }

        private void updateBackgroundService(boolean enabled) {
            Log.d(TAG, "Background service enabled: " + enabled);
            
            Intent intent = new Intent(requireContext(), AIBackgroundService.class);
            if (enabled) {
                requireContext().startService(intent);
            } else {
                requireContext().stopService(intent);
            }
        }

        private void updatePrivacyMode(boolean enabled) {
            Log.d(TAG, "Privacy mode enabled: " + enabled);
            // Update privacy settings
        }

        private void updateLowPowerMode(boolean enabled) {
            Log.d(TAG, "Low power mode enabled: " + enabled);
            // Update performance mode
        }
    }
}