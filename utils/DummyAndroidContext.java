package utils;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.BroadcastReceiver;
import android.content.PackageManager;
import android.content.AssetManager;
import android.os.Bundle;
import android.os.Handler;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.util.HashMap;
import java.util.Map;

/**
 * A concrete implementation of Android's Context abstract class.
 * This provides a dummy context for testing and development outside of Android.
 */
public class DummyAndroidContext extends Context {
    
    private Map<String, Object> systemServices;
    private String packageName;
    private File filesDir;
    private File cacheDir;
    
    /**
     * Constructor for DummyAndroidContext.
     */
    public DummyAndroidContext() {
        this("com.aiassistant.app");
    }
    
    /**
     * Constructor for DummyAndroidContext with a specific package name.
     * 
     * @param packageName The package name
     */
    public DummyAndroidContext(String packageName) {
        this.packageName = packageName;
        this.systemServices = new HashMap<>();
        this.filesDir = new File(System.getProperty("java.io.tmpdir") + "/files");
        this.cacheDir = new File(System.getProperty("java.io.tmpdir") + "/cache");
        
        // Create directories if they don't exist
        this.filesDir.mkdirs();
        this.cacheDir.mkdirs();
    }
    
    /**
     * Register a system service.
     * 
     * @param name The name of the service
     * @param service The service
     */
    public void registerSystemService(String name, Object service) {
        systemServices.put(name, service);
    }
    
    @Override
    public PackageManager getPackageManager() {
        return new DummyPackageManager();
    }
    
    @Override
    public String getPackageName() {
        return packageName;
    }
    
    @Override
    public File getFilesDir() {
        return filesDir;
    }
    
    @Override
    public File getNoBackupFilesDir() {
        return new File(filesDir, "no_backup");
    }
    
    @Override
    public File getExternalFilesDir(String type) {
        return new File(filesDir, "external");
    }
    
    @Override
    public File[] getExternalFilesDirs(String type) {
        return new File[] { getExternalFilesDir(type) };
    }
    
    @Override
    public File getExternalCacheDir() {
        return new File(cacheDir, "external");
    }
    
    @Override
    public File[] getExternalCacheDirs() {
        return new File[] { getExternalCacheDir() };
    }
    
    @Override
    public File getExternalMediaDirs() {
        return new File(filesDir, "media");
    }
    
    @Override
    public File getDataDir() {
        return new File(filesDir, "data");
    }
    
    @Override
    public File getPackageCodePath() {
        return new File(filesDir, "code");
    }
    
    @Override
    public File getCacheDir() {
        return cacheDir;
    }
    
    @Override
    public File getCodeCacheDir() {
        return new File(cacheDir, "code");
    }
    
    @Override
    public FileInputStream openFileInput(String name) {
        try {
            File file = new File(filesDir, name);
            return new FileInputStream(file);
        } catch (Exception e) {
            return null;
        }
    }
    
    @Override
    public FileOutputStream openFileOutput(String name, int mode) {
        try {
            File file = new File(filesDir, name);
            return new FileOutputStream(file);
        } catch (Exception e) {
            return null;
        }
    }
    
    @Override
    public boolean deleteFile(String name) {
        File file = new File(filesDir, name);
        return file.delete();
    }
    
    @Override
    public String[] fileList() {
        return filesDir.list();
    }
    
    @Override
    public AssetManager getAssets() {
        return new DummyAssetManager();
    }
    
    @Override
    public Object getSystemService(String name) {
        return systemServices.get(name);
    }
    
    // Define permission constants for our mock
    public static final int PERMISSION_GRANTED = 0;
    public static final int PERMISSION_DENIED = -1;
    
    @Override
    public int checkPermission(String permission, int pid, int uid) {
        // Always grant permission in the dummy implementation
        return PERMISSION_GRANTED;
    }
    
    @Override
    public void startActivity(Intent intent) {
        // Do nothing in the dummy implementation
    }
    
    @Override
    public void startActivity(Intent intent, Bundle options) {
        // Do nothing in the dummy implementation
    }
    
    @Override
    public void sendBroadcast(Intent intent) {
        // Do nothing in the dummy implementation
    }
    
    @Override
    public void sendBroadcast(Intent intent, String receiverPermission) {
        // Do nothing in the dummy implementation
    }
    
    @Override
    public void sendOrderedBroadcast(Intent intent, String receiverPermission) {
        // Do nothing in the dummy implementation
    }
    
    @Override
    public void sendOrderedBroadcast(Intent intent, String receiverPermission,
            BroadcastReceiver resultReceiver, Handler scheduler, int initialCode,
            String initialData, Bundle initialExtras) {
        // Do nothing in the dummy implementation
    }
    
    @Override
    public ComponentName startService(Intent service) {
        // Do nothing in the dummy implementation
        return new ComponentName(this, "DummyService");
    }
    
    @Override
    public boolean stopService(Intent service) {
        // Do nothing in the dummy implementation
        return true;
    }
    
    @Override
    public boolean bindService(Intent service, ServiceConnection conn, int flags) {
        // Do nothing in the dummy implementation
        return true;
    }
    
    @Override
    public void unbindService(ServiceConnection conn) {
        // Do nothing in the dummy implementation
    }
    
    /**
     * Dummy PackageManager implementation.
     */
    private class DummyPackageManager extends PackageManager {
        // Minimal implementation as needed
        static final int PERMISSION_GRANTED = 0;
    }
    
    /**
     * Dummy AssetManager implementation.
     */
    private class DummyAssetManager extends AssetManager {
        // Minimal implementation as needed
    }
}