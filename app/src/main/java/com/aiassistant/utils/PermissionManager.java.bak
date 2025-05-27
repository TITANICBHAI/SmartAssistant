package com.aiassistant.utils;

import android.app.Activity;
import android.app.admin.DevicePolicyManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.provider.Settings;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import com.aiassistant.R;
import com.aiassistant.receivers.DeviceAdminReceiver;
import com.aiassistant.services.AIAccessibilityService;

/**
 * Manages permission requests and checks for the app
 */
public class PermissionManager {
    private static final String TAG = "PermissionManager";
    
    // Request codes for permission results
    public static final int REQUEST_CODE_ACCESSIBILITY = 1001;
    public static final int REQUEST_CODE_OVERLAY = 1002;
    public static final int REQUEST_CODE_DEVICE_ADMIN = 1003;
    public static final int REQUEST_CODE_USAGE_STATS = 1004;
    public static final int REQUEST_CODE_STORAGE = 1005;
    public static final int REQUEST_CODE_CAMERA = 1006;
    public static final int REQUEST_CODE_BATTERY_OPTIMIZATION = 1007;
    
    private final Context context;
    private Activity activity;
    
    /**
     * Constructor with context
     */
    public PermissionManager(@NonNull Context context) {
        this.context = context;
        if (context instanceof Activity) {
            this.activity = (Activity) context;
        }
    }
    
    /**
     * Constructor with activity
     */
    public PermissionManager(@NonNull Activity activity) {
        this.context = activity;
        this.activity = activity;
    }
    
    /**
     * Constructor with fragment
     */
    public PermissionManager(@NonNull Fragment fragment) {
        this.context = fragment.requireContext();
        this.activity = fragment.requireActivity();
    }
    
    /**
     * Request accessibility permission
     */
    public void requestAccessibilityPermission() {
        if (!hasAccessibilityPermission()) {
            Intent intent = new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS);
            if (activity != null) {
                activity.startActivityForResult(intent, REQUEST_CODE_ACCESSIBILITY);
            } else {
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                context.startActivity(intent);
            }
        }
    }
    
    /**
     * Check if accessibility permission is granted
     */
    public boolean hasAccessibilityPermission() {
        try {
            String accessibilityService = context.getPackageName() + "/" + 
                    AIAccessibilityService.class.getCanonicalName();
            
            int accessibilityEnabled = Settings.Secure.getInt(
                    context.getContentResolver(),
                    Settings.Secure.ACCESSIBILITY_ENABLED);
            
            if (accessibilityEnabled == 1) {
                String enabledServices = Settings.Secure.getString(
                        context.getContentResolver(),
                        Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES);
                
                if (enabledServices != null) {
                    return enabledServices.contains(accessibilityService);
                }
            }
            
            return false;
        } catch (Settings.SettingNotFoundException e) {
            Log.e(TAG, "Error checking accessibility permission", e);
            return false;
        }
    }
    
    /**
     * Request overlay permission
     */
    public void requestOverlayPermission() {
        if (!hasOverlayPermission()) {
            Intent intent = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                    Uri.parse("package:" + context.getPackageName()));
            
            if (activity != null) {
                activity.startActivityForResult(intent, REQUEST_CODE_OVERLAY);
            } else {
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                context.startActivity(intent);
            }
        }
    }
    
    /**
     * Check if overlay permission is granted
     */
    public boolean hasOverlayPermission() {
        return Settings.canDrawOverlays(context);
    }
    
    /**
     * Request device admin permission
     */
    public void requestDeviceAdminPermission() {
        if (!hasDeviceAdminPermission()) {
            ComponentName deviceAdmin = new ComponentName(context, DeviceAdminReceiver.class);
            Intent intent = new Intent(DevicePolicyManager.ACTION_ADD_DEVICE_ADMIN);
            intent.putExtra(DevicePolicyManager.EXTRA_DEVICE_ADMIN, deviceAdmin);
            intent.putExtra(DevicePolicyManager.EXTRA_ADD_EXPLANATION,
                    context.getString(R.string.pref_device_admin_summary));
            
            if (activity != null) {
                activity.startActivityForResult(intent, REQUEST_CODE_DEVICE_ADMIN);
            } else {
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                context.startActivity(intent);
            }
        }
    }
    
    /**
     * Check if device admin permission is granted
     */
    public boolean hasDeviceAdminPermission() {
        DevicePolicyManager devicePolicyManager = 
                (DevicePolicyManager) context.getSystemService(Context.DEVICE_POLICY_SERVICE);
        ComponentName deviceAdmin = new ComponentName(context, DeviceAdminReceiver.class);
        
        return devicePolicyManager != null && devicePolicyManager.isAdminActive(deviceAdmin);
    }
    
    /**
     * Request usage stats permission
     */
    public void requestUsageStatsPermission() {
        if (!hasUsageStatsPermission()) {
            Intent intent = new Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS);
            
            if (activity != null) {
                activity.startActivityForResult(intent, REQUEST_CODE_USAGE_STATS);
            } else {
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                context.startActivity(intent);
            }
        }
    }
    
    /**
     * Check if usage stats permission is granted
     */
    public boolean hasUsageStatsPermission() {
        long currentTime = System.currentTimeMillis();
        android.app.usage.UsageStatsManager usageStatsManager = (android.app.usage.UsageStatsManager)
                context.getSystemService(Context.USAGE_STATS_SERVICE);
        
        if (usageStatsManager == null) {
            return false;
        }
        
        // Query for stats in last minute
        // If we get any results, then permission is granted
        try {
            usageStatsManager.queryUsageStats(
                    android.app.usage.UsageStatsManager.INTERVAL_DAILY,
                    currentTime - 60 * 1000, currentTime);
            return true;
        } catch (SecurityException e) {
            return false;
        }
    }
    
    /**
     * Request battery optimization exemption
     */
    public void requestBatteryOptimizationExemption() {
        if (!isBatteryOptimizationExempt()) {
            Intent intent = new Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS,
                    Uri.parse("package:" + context.getPackageName()));
            
            if (activity != null) {
                activity.startActivityForResult(intent, REQUEST_CODE_BATTERY_OPTIMIZATION);
            } else {
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                context.startActivity(intent);
            }
        }
    }
    
    /**
     * Check if app is exempt from battery optimization
     */
    public boolean isBatteryOptimizationExempt() {
        android.os.PowerManager powerManager = (android.os.PowerManager)
                context.getSystemService(Context.POWER_SERVICE);
        
        return powerManager != null && 
               powerManager.isIgnoringBatteryOptimizations(context.getPackageName());
    }
    
    /**
     * Request notification permission for Android 13+
     */
    public void requestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (activity != null) {
                ActivityCompat.requestPermissions(
                        activity,
                        new String[]{"android.permission.POST_NOTIFICATIONS"},
                        1008);
            }
        }
    }
    
    /**
     * Check if notification permission is granted
     */
    public boolean hasNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            return ContextCompat.checkSelfPermission(
                    context, 
                    "android.permission.POST_NOTIFICATIONS") == 
                    android.content.pm.PackageManager.PERMISSION_GRANTED;
        }
        return true; // Permission automatically granted on older Android versions
    }
}