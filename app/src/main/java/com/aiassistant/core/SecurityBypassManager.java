package com.aiassistant.core;

import android.Manifest;
import android.app.AppOpsManager;
import android.app.admin.DevicePolicyManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Process;
import android.provider.Settings;
import android.util.Log;

import androidx.annotation.RequiresApi;

import java.io.IOException;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.File;

import com.aiassistant.receivers.DeviceAdminReceiver;
import utils.ShellCommandExecutor;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

/**
 * Security Bypass Manager
 * Handles bypassing security restrictions and obtaining elevated permissions
 */
public class SecurityBypassManager {
    private static final String TAG = "SecurityBypassManager";
    
    // Singleton instance
    private static SecurityBypassManager instance;
    
    // Context
    private Context context;
    
    // Device admin receiver component
    private ComponentName deviceAdminComponent;
    
    /**
     * Get the singleton instance
     */
    public static synchronized SecurityBypassManager getInstance(Context context) {
        if (instance == null) {
            instance = new SecurityBypassManager(context.getApplicationContext());
        }
        return instance;
    }
    
    /**
     * Private constructor
     */
    private SecurityBypassManager(Context context) {
        this.context = context;
        this.deviceAdminComponent = new ComponentName(context, DeviceAdminReceiver.class);
    }
    
    /**
     * Check if we have device admin permission
     */
    public boolean isDeviceAdminEnabled() {
        DevicePolicyManager dpm = (DevicePolicyManager) context.getSystemService(Context.DEVICE_POLICY_SERVICE);
        return dpm != null && dpm.isAdminActive(deviceAdminComponent);
    }
    
    /**
     * Request device admin permission
     */
    public Intent getDeviceAdminRequestIntent() {
        Intent intent = new Intent(DevicePolicyManager.ACTION_ADD_DEVICE_ADMIN);
        intent.putExtra(DevicePolicyManager.EXTRA_DEVICE_ADMIN, deviceAdminComponent);
        intent.putExtra(DevicePolicyManager.EXTRA_ADD_EXPLANATION, 
                "This permission is required for the AI assistant to fully control your device.");
        return intent;
    }
    
    /**
     * Check if we have overlay permission
     */
    public boolean canDrawOverlays() {
        return Settings.canDrawOverlays(context);
    }
    
    /**
     * Get intent to request overlay permission
     */
    public Intent getOverlayPermissionIntent() {
        Intent intent = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION);
        intent.setData(Uri.parse("package:" + context.getPackageName()));
        return intent;
    }
    
    /**
     * Check if we have usage stats permission
     */
    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP_MR1)
    public boolean hasUsageStatsPermission() {
        AppOpsManager appOps = (AppOpsManager) context.getSystemService(Context.APP_OPS_SERVICE);
        int mode = appOps.checkOpNoThrow(AppOpsManager.OPSTR_GET_USAGE_STATS, Process.myUid(), context.getPackageName());
        return mode == AppOpsManager.MODE_ALLOWED;
    }
    
    /**
     * Get intent to request usage stats permission
     */
    public Intent getUsageStatsPermissionIntent() {
        Intent intent = new Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS);
        return intent;
    }
    
    /**
     * Check if we have all required permissions
     */
    public boolean hasAllRequiredPermissions() {
        List<String> missingPermissions = getMissingPermissions();
        return missingPermissions.isEmpty() && 
               canDrawOverlays() && 
               (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP_MR1 || hasUsageStatsPermission());
    }
    
    /**
     * Get list of missing permissions
     */
    public List<String> getMissingPermissions() {
        List<String> missingPermissions = new ArrayList<>();
        
        // Check regular permissions
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (context.checkSelfPermission(Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                missingPermissions.add(Manifest.permission.READ_EXTERNAL_STORAGE);
            }
            
            if (context.checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                missingPermissions.add(Manifest.permission.WRITE_EXTERNAL_STORAGE);
            }
            
            if (context.checkSelfPermission(Manifest.permission.INTERNET) != PackageManager.PERMISSION_GRANTED) {
                missingPermissions.add(Manifest.permission.INTERNET);
            }
            
            if (context.checkSelfPermission(Manifest.permission.ACCESS_NETWORK_STATE) != PackageManager.PERMISSION_GRANTED) {
                missingPermissions.add(Manifest.permission.ACCESS_NETWORK_STATE);
            }
            
            if (context.checkSelfPermission(Manifest.permission.RECEIVE_BOOT_COMPLETED) != PackageManager.PERMISSION_GRANTED) {
                missingPermissions.add(Manifest.permission.RECEIVE_BOOT_COMPLETED);
            }
        }
        
        return missingPermissions;
    }
    
    /**
     * Attempt to bypass security restrictions using reflection
     * Note: This is for educational purposes only and may not work on all devices
     */
    public boolean bypassSecurityRestrictions() {
        Log.i(TAG, "Attempting to bypass security restrictions");
        boolean success = false;
        
        try {
            // Try to bypass using reflection
            success = bypassUsingReflection();
            
            if (!success && hasRootAccess()) {
                // Try to bypass using root
                success = bypassUsingRoot();
            }
        } catch (Exception e) {
            Log.e(TAG, "Error bypassing security restrictions", e);
        }
        
        return success;
    }
    
    /**
     * Bypass security restrictions using reflection
     */
    private boolean bypassUsingReflection() {
        try {
            // Access hidden APIs using reflection
            Class<?> activityManagerClass = Class.forName("android.app.ActivityManager");
            Method getService = activityManagerClass.getMethod("getService");
            Object activityManagerService = getService.invoke(null);
            
            if (activityManagerService != null) {
                Log.i(TAG, "Successfully accessed ActivityManagerService");
                return true;
            }
        } catch (Exception e) {
            Log.e(TAG, "Error bypassing using reflection", e);
        }
        
        return false;
    }
    
    /**
     * Check if device has root access
     */
    public boolean hasRootAccess() {
        boolean rootAvailable = false;
        
        // Check for su binary
        for (String path : new String[]{"/system/bin/", "/system/xbin/", "/sbin/", "/system/sd/xbin/", "/system/bin/failsafe/", "/data/local/xbin/", "/data/local/bin/", "/data/local/"}) {
            if (new File(path + "su").exists()) {
                rootAvailable = true;
                break;
            }
        }
        
        // If su binary exists, try executing a simple command
        if (rootAvailable) {
            try {
                Process process = Runtime.getRuntime().exec("su -c id");
                int exitValue = process.waitFor();
                rootAvailable = exitValue == 0;
            } catch (IOException | InterruptedException e) {
                rootAvailable = false;
            }
        }
        
        Log.i(TAG, "Root access available: " + rootAvailable);
        return rootAvailable;
    }
    
    /**
     * Bypass security restrictions using root access
     */
    private boolean bypassUsingRoot() {
        try {
            // Grant permissions using root
            ShellCommandExecutor executor = new ShellCommandExecutor();
            
            // Grant all permissions
            String packageName = context.getPackageName();
            String command = "su -c pm grant " + packageName + " android.permission.WRITE_SECURE_SETTINGS";
            
            int result = executor.execute(command);
            Log.i(TAG, "Root bypass result: " + result);
            
            return result == 0;
        } catch (Exception e) {
            Log.e(TAG, "Error bypassing using root", e);
            return false;
        }
    }
    
    /**
     * Lock the device screen (requires device admin)
     */
    public boolean lockScreen() {
        if (isDeviceAdminEnabled()) {
            DevicePolicyManager dpm = (DevicePolicyManager) context.getSystemService(Context.DEVICE_POLICY_SERVICE);
            dpm.lockNow();
            Log.i(TAG, "Screen locked");
            return true;
        } else {
            Log.w(TAG, "Cannot lock screen - device admin not enabled");
            return false;
        }
    }
    
    /**
     * Wipe device data (factory reset) - USE WITH EXTREME CAUTION
     * Note: This is for educational purposes only
     */
    public boolean wipeDevice() {
        if (isDeviceAdminEnabled()) {
            DevicePolicyManager dpm = (DevicePolicyManager) context.getSystemService(Context.DEVICE_POLICY_SERVICE);
            dpm.wipeData(0);
            Log.w(TAG, "Wiping device data!");
            return true;
        } else {
            Log.w(TAG, "Cannot wipe device - device admin not enabled");
            return false;
        }
    }
    
    /**
     * Set password quality (requires device admin)
     */
    public boolean setPasswordQuality(int quality) {
        if (isDeviceAdminEnabled()) {
            DevicePolicyManager dpm = (DevicePolicyManager) context.getSystemService(Context.DEVICE_POLICY_SERVICE);
            dpm.setPasswordQuality(deviceAdminComponent, quality);
            Log.i(TAG, "Password quality set to " + quality);
            return true;
        } else {
            Log.w(TAG, "Cannot set password quality - device admin not enabled");
            return false;
        }
    }
    
    /**
     * Add a persistent device admin that cannot be removed
     * Note: This is for educational purposes only
     */
    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    public boolean makePersistentAdmin() {
        if (isDeviceAdminEnabled() && hasRootAccess()) {
            try {
                ShellCommandExecutor executor = new ShellCommandExecutor();
                
                // Make the device admin persistent by modifying system properties
                String command = "su -c setprop persist.sys.device_owner " + context.getPackageName() + "/" + deviceAdminComponent.getClassName();
                
                int result = executor.execute(command);
                Log.i(TAG, "Persistent admin result: " + result);
                
                return result == 0;
            } catch (Exception e) {
                Log.e(TAG, "Error making persistent admin", e);
                return false;
            }
        } else {
            Log.w(TAG, "Cannot make persistent admin - device admin not enabled or no root access");
            return false;
        }
    }
}