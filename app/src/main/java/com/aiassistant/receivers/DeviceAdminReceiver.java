package com.aiassistant.receivers;

import android.content.Context;
import android.content.Intent;
import android.util.Log;
import android.widget.Toast;

import com.aiassistant.R;

/**
 * Device admin receiver for advanced system control
 */
public class DeviceAdminReceiver extends android.app.admin.DeviceAdminReceiver {
    private static final String TAG = "DeviceAdminReceiver";
    
    @Override
    public void onEnabled(Context context, Intent intent) {
        super.onEnabled(context, intent);
        Log.d(TAG, "Device admin enabled");
        Toast.makeText(context, R.string.permission_granted, Toast.LENGTH_SHORT).show();
    }
    
    @Override
    public void onDisabled(Context context, Intent intent) {
        super.onDisabled(context, intent);
        Log.d(TAG, "Device admin disabled");
        Toast.makeText(context, R.string.permission_not_granted, Toast.LENGTH_SHORT).show();
    }
    
    @Override
    public CharSequence onDisableRequested(Context context, Intent intent) {
        return context.getString(R.string.pref_device_admin_summary);
    }
    
    @Override
    public void onLockTaskModeEntering(Context context, Intent intent, String pkg) {
        super.onLockTaskModeEntering(context, intent, pkg);
        Log.d(TAG, "Lock task mode entering: " + pkg);
    }
    
    @Override
    public void onLockTaskModeExiting(Context context, Intent intent) {
        super.onLockTaskModeExiting(context, intent);
        Log.d(TAG, "Lock task mode exiting");
    }
}