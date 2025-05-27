package com.aiassistant.receivers;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.util.Log;

import androidx.preference.PreferenceManager;

import com.aiassistant.services.AIBackgroundService;

/**
 * Receiver to start the AI assistant services at device boot
 */
public class BootReceiver extends BroadcastReceiver {
    private static final String TAG = "BootReceiver";
    
    @Override
    public void onReceive(Context context, Intent intent) {
        if (Intent.ACTION_BOOT_COMPLETED.equals(intent.getAction())) {
            Log.d(TAG, "Device boot completed, checking if services should be started");
            
            // Check preferences to see if background service should be started
            SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
            boolean enableBackgroundService = prefs.getBoolean("background_service", true);
            
            if (enableBackgroundService) {
                Log.d(TAG, "Starting AI background service after boot");
                Intent serviceIntent = new Intent(context, AIBackgroundService.class);
                context.startService(serviceIntent);
            }
        }
    }
}