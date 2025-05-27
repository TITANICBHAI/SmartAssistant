package com.aiassistant.utils;

import android.Manifest;
import android.accessibilityservice.AccessibilityServiceInfo;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.provider.Settings;
import android.util.Log;
import android.view.accessibility.AccessibilityManager;

import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.aiassistant.services.AIService;

import java.util.ArrayList;
import java.util.List;

/**
 * Helper class for checking and requesting permissions.
 */
public class PermissionHelper {
    
    private static final String TAG = "PermissionHelper";
    
    private static final String[] REQUIRED_PERMISSIONS = {
            Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.WRITE_EXTERNAL_STORAGE,
            Manifest.permission.CAMERA,
            Manifest.permission.INTERNET,
            Manifest.permission.RECORD_AUDIO,
            Manifest.permission.FOREGROUND_SERVICE
    };
    
    private static final int REQUEST_PERMISSIONS_CODE = 100;
    
    private final Context context;
    
    public PermissionHelper(Context context) {
        this.context = context;
    }
    
    /**
     * Check if all required permissions have been granted
     */
    public boolean hasAllRequiredPermissions() {
        for (String permission : REQUIRED_PERMISSIONS) {
            if (ContextCompat.checkSelfPermission(context, permission) != PackageManager.PERMISSION_GRANTED) {
                return false;
            }
        }
        return true;
    }
    
    /**
     * Request all required permissions
     */
    public void requestRequiredPermissions() {
        if (context instanceof Activity) {
            List<String> permissionsToRequest = new ArrayList<>();
            
            for (String permission : REQUIRED_PERMISSIONS) {
                if (ContextCompat.checkSelfPermission(context, permission) != PackageManager.PERMISSION_GRANTED) {
                    permissionsToRequest.add(permission);
                }
            }
            
            if (!permissionsToRequest.isEmpty()) {
                ActivityCompat.requestPermissions(
                        (Activity) context,
                        permissionsToRequest.toArray(new String[0]),
                        REQUEST_PERMISSIONS_CODE
                );
            }
        } else {
            Log.e(TAG, "Context is not an Activity, cannot request permissions");
        }
    }
    
    /**
     * Check if the app has accessibility service permission
     */
    public boolean hasAccessibilityPermission() {
        AccessibilityManager accessibilityManager = (AccessibilityManager) context.getSystemService(Context.ACCESSIBILITY_SERVICE);
        
        if (accessibilityManager != null) {
            List<AccessibilityServiceInfo> enabledServices = 
                    accessibilityManager.getEnabledAccessibilityServiceList(
                            AccessibilityServiceInfo.FEEDBACK_ALL_MASK);
            
            for (AccessibilityServiceInfo service : enabledServices) {
                if (service.getResolveInfo().serviceInfo.packageName.equals(context.getPackageName())) {
                    return true;
                }
            }
        }
        
        return false;
    }
    
    /**
     * Check if the accessibility service is enabled
     */
    public boolean isAccessibilityServiceEnabled() {
        AccessibilityManager accessibilityManager = (AccessibilityManager) context.getSystemService(Context.ACCESSIBILITY_SERVICE);
        
        if (accessibilityManager != null) {
            List<AccessibilityServiceInfo> enabledServices = 
                    accessibilityManager.getEnabledAccessibilityServiceList(
                            AccessibilityServiceInfo.FEEDBACK_ALL_MASK);
            
            for (AccessibilityServiceInfo service : enabledServices) {
                String serviceId = service.getId();
                if (serviceId != null && serviceId.contains(context.getPackageName())) {
                    return true;
                }
            }
        }
        
        return false;
    }
    
    /**
     * Check if the app has overlay permission (required for Android 6.0+)
     */
    public boolean hasOverlayPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            return Settings.canDrawOverlays(context);
        }
        return true; // Not required for lower Android versions
    }
    
    /**
     * Open accessibility settings
     */
    public void openAccessibilitySettings() {
        Intent intent = new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS);
        if (context instanceof Activity) {
            context.startActivity(intent);
        } else {
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            context.startActivity(intent);
        }
    }
    
    /**
     * Open overlay permission settings
     */
    public void openOverlaySettings() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            Intent intent = new Intent(
                    Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                    Uri.parse("package:" + context.getPackageName())
            );
            if (context instanceof Activity) {
                context.startActivity(intent);
            } else {
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                context.startActivity(intent);
            }
        }
    }
}
