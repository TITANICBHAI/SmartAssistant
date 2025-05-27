package com.aiassistant.ui.fragments;

import android.app.admin.DevicePolicyManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Settings;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.RadioButton;
import android.widget.Spinner;
import android.widget.Switch;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.aiassistant.R;
import com.aiassistant.core.AIController;
import com.aiassistant.core.SecurityBypassManager;
import models.AIState;
import models.PerformanceMode;
import com.aiassistant.receivers.DeviceAdminReceiver;

/**
 * Settings Fragment
 * Manages application settings and permissions
 */
public class SettingsFragment extends Fragment {
    private Button buttonDeviceAdmin;
    private Button buttonAccessibility;
    private Button buttonStoragePermissions;
    private Button buttonOverlayPermissions;
    private Button buttonUsageStatsPermissions;
    private Switch switchAdvancedSecurity;
    private RadioButton radioPerformanceBattery;
    private RadioButton radioPerformanceBalanced;
    private RadioButton radioPerformanceHigh;
    private Switch switchLowMemoryMode;
    private Spinner spinnerRLAlgorithm;
    private Switch switchAutoSelectAlgorithm;
    private Switch switchVideoLearning;
    private Button buttonAdvancedAISettings;
    private Button buttonSaveSettings;
    
    private AIController aiController;
    private SecurityBypassManager securityManager;
    
    // Request codes for permissions
    private static final int REQUEST_DEVICE_ADMIN = 1;
    private static final int REQUEST_OVERLAY_PERMISSION = 2;
    private static final int REQUEST_USAGE_STATS = 3;
    private static final int REQUEST_STORAGE_PERMISSION = 4;
    
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_settings, container, false);
    }
    
    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        
        // Initialize controllers
        aiController = AIController.getInstance(requireContext());
        securityManager = SecurityBypassManager.getInstance(requireContext());
        
        // Initialize views
        initializeViews(view);
        
        // Setup algorithm spinner
        setupAlgorithmSpinner();
        
        // Set up button listeners
        setupButtonListeners();
        
        // Update UI with current settings
        updateSettingsUI();
    }
    
    private void initializeViews(View view) {
        buttonDeviceAdmin = view.findViewById(R.id.button_device_admin);
        buttonAccessibility = view.findViewById(R.id.button_accessibility);
        buttonStoragePermissions = view.findViewById(R.id.button_storage_permissions);
        buttonOverlayPermissions = view.findViewById(R.id.button_overlay_permissions);
        buttonUsageStatsPermissions = view.findViewById(R.id.button_usage_stats_permissions);
        switchAdvancedSecurity = view.findViewById(R.id.switch_advanced_security);
        radioPerformanceBattery = view.findViewById(R.id.radio_performance_battery);
        radioPerformanceBalanced = view.findViewById(R.id.radio_performance_balanced);
        radioPerformanceHigh = view.findViewById(R.id.radio_performance_high);
        switchLowMemoryMode = view.findViewById(R.id.switch_low_memory_mode);
        spinnerRLAlgorithm = view.findViewById(R.id.spinner_rl_algorithm);
        switchAutoSelectAlgorithm = view.findViewById(R.id.switch_auto_select_algorithm);
        switchVideoLearning = view.findViewById(R.id.switch_video_learning);
        buttonAdvancedAISettings = view.findViewById(R.id.button_advanced_ai_settings);
        buttonSaveSettings = view.findViewById(R.id.button_save_settings);
    }
    
    private void setupAlgorithmSpinner() {
        // Create an ArrayAdapter for the spinner
        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(
                requireContext(),
                R.array.rl_algorithms,
                android.R.layout.simple_spinner_item
        );
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerRLAlgorithm.setAdapter(adapter);
    }
    
    private void setupButtonListeners() {
        // Device admin button
        buttonDeviceAdmin.setOnClickListener(v -> requestDeviceAdmin());
        
        // Accessibility button
        buttonAccessibility.setOnClickListener(v -> openAccessibilitySettings());
        
        // Storage permissions button
        buttonStoragePermissions.setOnClickListener(v -> requestStoragePermissions());
        
        // Overlay permissions button
        buttonOverlayPermissions.setOnClickListener(v -> requestOverlayPermission());
        
        // Usage stats permissions button
        buttonUsageStatsPermissions.setOnClickListener(v -> requestUsageStatsPermission());
        
        // Advanced security switch
        switchAdvancedSecurity.setOnCheckedChangeListener((buttonView, isChecked) -> {
            securityManager.setAdvancedSecurityEnabled(isChecked);
        });
        
        // Auto-select algorithm switch
        switchAutoSelectAlgorithm.setOnCheckedChangeListener((buttonView, isChecked) -> {
            spinnerRLAlgorithm.setEnabled(!isChecked);
        });
        
        // Advanced AI settings button
        buttonAdvancedAISettings.setOnClickListener(v -> {
            // In a real implementation, open a dialog with advanced settings
            Toast.makeText(requireContext(), "Advanced AI Settings (To be implemented)", Toast.LENGTH_SHORT).show();
        });
        
        // Save settings button
        buttonSaveSettings.setOnClickListener(v -> saveSettings());
    }
    
    private void requestDeviceAdmin() {
        ComponentName deviceAdmin = new ComponentName(requireContext(), DeviceAdminReceiver.class);
        Intent intent = new Intent(DevicePolicyManager.ACTION_ADD_DEVICE_ADMIN);
        intent.putExtra(DevicePolicyManager.EXTRA_DEVICE_ADMIN, deviceAdmin);
        intent.putExtra(DevicePolicyManager.EXTRA_ADD_EXPLANATION, 
                getString(R.string.device_admin_explanation));
        startActivityForResult(intent, REQUEST_DEVICE_ADMIN);
    }
    
    private void openAccessibilitySettings() {
        Intent intent = new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS);
        startActivity(intent);
    }
    
    private void requestStoragePermissions() {
        // In a real implementation, request permissions using ActivityCompat
        // For now, just pretend we have them
        Toast.makeText(requireContext(), "Storage permissions requested", Toast.LENGTH_SHORT).show();
    }
    
    private void requestOverlayPermission() {
        if (!Settings.canDrawOverlays(requireContext())) {
            Intent intent = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                    Uri.parse("package:" + requireContext().getPackageName()));
            startActivityForResult(intent, REQUEST_OVERLAY_PERMISSION);
        } else {
            Toast.makeText(requireContext(), "Overlay permission already granted", Toast.LENGTH_SHORT).show();
        }
    }
    
    private void requestUsageStatsPermission() {
        Intent intent = new Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS);
        startActivityForResult(intent, REQUEST_USAGE_STATS);
    }
    
    private void saveSettings() {
        // Get selected performance mode
        PerformanceMode performanceMode;
        if (radioPerformanceBattery.isChecked()) {
            performanceMode = PerformanceMode.BATTERY_SAVER;
        } else if (radioPerformanceHigh.isChecked()) {
            performanceMode = PerformanceMode.HIGH_PERFORMANCE;
        } else {
            performanceMode = PerformanceMode.BALANCED;
        }
        
        // Set performance mode
        aiController.setPerformanceMode(performanceMode);
        
        // Set low memory mode
        aiController.setLowMemoryMode(switchLowMemoryMode.isChecked());
        
        // Set RL algorithm
        if (!switchAutoSelectAlgorithm.isChecked()) {
            String algorithm = spinnerRLAlgorithm.getSelectedItem().toString();
            aiController.setRLAlgorithm(algorithm);
        }
        
        // Set auto-select algorithm
        aiController.setAutoSelectAlgorithm(switchAutoSelectAlgorithm.isChecked());
        
        // Set video learning enabled
        aiController.setVideoLearningEnabled(switchVideoLearning.isChecked());
        
        // Show confirmation
        Toast.makeText(requireContext(), "Settings saved", Toast.LENGTH_SHORT).show();
    }
    
    private void updateSettingsUI() {
        if (aiController == null || !isAdded()) {
            return;
        }
        
        // Get current AI state
        AIState aiState = aiController.getAIState();
        
        // Update security switch
        switchAdvancedSecurity.setChecked(securityManager.isAdvancedSecurityEnabled());
        
        // Update performance mode radio buttons
        switch (aiState.getPerformanceMode()) {
            case BATTERY_SAVER:
                radioPerformanceBattery.setChecked(true);
                break;
            case HIGH_PERFORMANCE:
                radioPerformanceHigh.setChecked(true);
                break;
            case BALANCED:
            default:
                radioPerformanceBalanced.setChecked(true);
                break;
        }
        
        // Update low memory mode switch
        switchLowMemoryMode.setChecked(aiState.isLowMemoryMode());
        
        // Update algorithm spinner
        String rlAlgorithm = aiState.getRlAlgorithm();
        if (rlAlgorithm != null) {
            ArrayAdapter adapter = (ArrayAdapter) spinnerRLAlgorithm.getAdapter();
            int position = adapter.getPosition(rlAlgorithm);
            if (position >= 0) {
                spinnerRLAlgorithm.setSelection(position);
            }
        }
        
        // Update auto-select algorithm switch
        switchAutoSelectAlgorithm.setChecked(aiState.isAutoSelectAlgorithm());
        spinnerRLAlgorithm.setEnabled(!aiState.isAutoSelectAlgorithm());
        
        // Update video learning switch
        switchVideoLearning.setChecked(aiState.isVideoLearningEnabled());
    }
    
    @Override
    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        
        switch (requestCode) {
            case REQUEST_DEVICE_ADMIN:
                updateDeviceAdminStatus();
                break;
            case REQUEST_OVERLAY_PERMISSION:
                updateOverlayPermissionStatus();
                break;
            case REQUEST_USAGE_STATS:
                updateUsageStatsPermissionStatus();
                break;
            case REQUEST_STORAGE_PERMISSION:
                updateStoragePermissionStatus();
                break;
        }
    }
    
    private void updateDeviceAdminStatus() {
        DevicePolicyManager dpm = (DevicePolicyManager) 
                requireContext().getSystemService(Context.DEVICE_POLICY_SERVICE);
        ComponentName adminComponent = new ComponentName(requireContext(), DeviceAdminReceiver.class);
        
        boolean isAdmin = dpm.isAdminActive(adminComponent);
        AIState aiState = aiController.getAIState();
        aiState.setDeviceAdminEnabled(isAdmin);
        
        Toast.makeText(requireContext(), 
                isAdmin ? "Device admin enabled" : "Device admin not enabled", 
                Toast.LENGTH_SHORT).show();
    }
    
    private void updateOverlayPermissionStatus() {
        boolean hasPermission = Settings.canDrawOverlays(requireContext());
        AIState aiState = aiController.getAIState();
        aiState.setOverlayPermissionEnabled(hasPermission);
        
        Toast.makeText(requireContext(), 
                hasPermission ? "Overlay permission granted" : "Overlay permission denied", 
                Toast.LENGTH_SHORT).show();
    }
    
    private void updateUsageStatsPermissionStatus() {
        // In a real implementation, check if the permission is actually granted
        // For now, just pretend it is
        AIState aiState = aiController.getAIState();
        aiState.setUsageStatsPermissionEnabled(true);
        
        Toast.makeText(requireContext(), "Usage stats permission updated", Toast.LENGTH_SHORT).show();
    }
    
    private void updateStoragePermissionStatus() {
        // In a real implementation, check if the permission is actually granted
        // For now, just pretend it is
        AIState aiState = aiController.getAIState();
        aiState.setStoragePermissionEnabled(true);
        
        Toast.makeText(requireContext(), "Storage permission updated", Toast.LENGTH_SHORT).show();
    }
    
    @Override
    public void onResume() {
        super.onResume();
        updateSettingsUI();
    }
}