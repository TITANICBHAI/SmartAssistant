package android.content;

import android.os.Bundle;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;

/**
 * Mock implementation of Android's Context class.
 */
public abstract class Context {
    /**
     * Flag for {@link #bindService}: automatically create the service as long as the binding exists.
     */
    public static final int BIND_AUTO_CREATE = 0x0001;
    
    /**
     * Flag for {@link #bindService}: include debugging help for mismatched calls.
     */
    public static final int BIND_DEBUG_UNBIND = 0x0002;
    
    /**
     * Flag for {@link #bindService}: don't allow this binding to raise the target service's process to foreground.
     */
    public static final int BIND_NOT_FOREGROUND = 0x0004;
    
    /**
     * Flag for {@link #bindService}: indicates that the client application binding to this service considers the service to be more important than the app itself.
     */
    public static final int BIND_ABOVE_CLIENT = 0x0008;
    
    /**
     * Flag for {@link #bindService}: allow the process hosting the bound service to be upgraded in priority.
     */
    public static final int BIND_ALLOW_OOM_MANAGEMENT = 0x0010;
    
    /**
     * Flag for {@link #bindService}: don't impact the scheduling or memory management priority of the target service's hosting process.
     */
    public static final int BIND_WAIVE_PRIORITY = 0x0020;
    
    /**
     * Flag for {@link #bindService}: this service is very important to the client and the system should make sure it is always running.
     */
    public static final int BIND_IMPORTANT = 0x0040;
    
    /**
     * Flag for {@link #bindService}: this service may be bound by an application that is in the foreground.
     */
    public static final int BIND_FOREGROUND_SERVICE = 0x0080;
    
    /**
     * Return PackageManager instance to find global package information.
     * 
     * @return The package manager
     */
    public abstract PackageManager getPackageManager();
    
    /**
     * Return the name of this application's package.
     * 
     * @return The package name
     */
    public abstract String getPackageName();
    
    /**
     * Return absolute path to the directory on the filesystem where files created with
     * {@link #openFileOutput} are stored.
     * 
     * @return The path of the directory
     */
    public abstract File getFilesDir();
    
    /**
     * Return absolute path to the directory on the filesystem similar to
     * {@link #getFilesDir()}. The difference is that files placed under this
     * directory will be excluded from automatic backup to remote storage.
     * 
     * @return The path of the directory
     */
    public abstract File getNoBackupFilesDir();
    
    /**
     * Return the path to a directory on the filesystem similar to
     * {@link #getFilesDir()}.
     * 
     * @param name The name of the directory
     * @return The path of the directory
     */
    public abstract File getExternalFilesDir(String name);
    
    /**
     * Returns absolute paths to application-specific directories on all
     * external storage devices where the application can place persistent files
     * it owns.
     * 
     * @param name The name of the directory
     * @return The paths of the directories
     */
    public abstract File[] getExternalFilesDirs(String name);
    
    /**
     * Return the dir path to the directory holding application cache files on
     * external storage.
     * 
     * @return The path of the directory
     */
    public abstract File getExternalCacheDir();
    
    /**
     * Return absolute paths to application-specific directories on all
     * external storage devices where the application can place cache files it owns.
     * 
     * @return The paths of the directories
     */
    public abstract File[] getExternalCacheDirs();
    
    /**
     * Return the dir path to the directory holding application media files on
     * external storage.
     * 
     * @param type The type of files directory to return
     * @return The path of the directory
     */
    public abstract File getExternalMediaDirs();
    
    /**
     * Return the absolute path to the directory on the filesystem where all
     * private files belonging to this app are stored.
     * 
     * @return The path of the directory
     */
    public abstract File getDataDir();
    
    /**
     * Return the absolute path to the directory on the filesystem where all
     * private files belonging to this app package are stored.
     * 
     * @return The path of the directory
     */
    public abstract File getPackageCodePath();
    
    /**
     * Return the absolute path to the directory on the filesystem where cached
     * code is stored.
     * 
     * @return The path of the directory
     */
    public abstract File getCacheDir();
    
    /**
     * Return the absolute path to the directory on the filesystem designed for
     * storing caches of code.
     * 
     * @return The path of the directory
     */
    public abstract File getCodeCacheDir();
    
    /**
     * Open a private file associated with this Context's application package
     * for reading.
     * 
     * @param name The name of the file to open
     * @return The input stream
     */
    public abstract FileInputStream openFileInput(String name);
    
    /**
     * Open a private file associated with this Context's application package
     * for writing.
     * 
     * @param name The name of the file to open
     * @param mode The mode to open the file with
     * @return The output stream
     */
    public abstract FileOutputStream openFileOutput(String name, int mode);
    
    /**
     * Delete the given private file associated with this Context's
     * application package.
     * 
     * @param name The name of the file to delete
     * @return True if the file was successfully deleted
     */
    public abstract boolean deleteFile(String name);
    
    /**
     * Returns a list of strings naming the private files associated with
     * this Context's application package.
     * 
     * @return The list of file names
     */
    public abstract String[] fileList();
    
    /**
     * Retrieve assets for the application.
     * 
     * @return The assets manager
     */
    public abstract AssetManager getAssets();
    
    /**
     * Get the system service of the given type.
     * 
     * @param name The name of the service to get
     * @return The system service
     */
    public abstract Object getSystemService(String name);
    
    /**
     * Check if the given permission is allowed for a particular process and user ID running in
     * the system.
     * 
     * @param permission The permission to check
     * @param pid The process ID
     * @param uid The user ID
     * @return The permission check result
     */
    public abstract int checkPermission(String permission, int pid, int uid);
    
    /**
     * Start an activity.
     * 
     * @param intent The intent to start
     */
    public abstract void startActivity(Intent intent);
    
    /**
     * Start an activity.
     * 
     * @param intent The intent to start
     * @param options Additional options for how the activity should be started
     */
    public abstract void startActivity(Intent intent, Bundle options);
    
    /**
     * Start an activity for a result.
     * 
     * @param intent The intent to start
     * @param requestCode The request code
     * @param options Additional options for how the activity should be started
     */
    public void startActivityForResult(Intent intent, int requestCode, Bundle options) {
        // Default implementation that subclasses can override
    }
    
    /**
     * Broadcasts the given intent to all interested BroadcastReceivers.
     * 
     * @param intent The intent to broadcast
     */
    public abstract void sendBroadcast(Intent intent);
    
    /**
     * Broadcasts the given intent to all interested BroadcastReceivers, allowing
     * an optional required permission.
     * 
     * @param intent The intent to broadcast
     * @param receiverPermission The permission that a receiver must hold in order to receive the broadcast
     */
    public abstract void sendBroadcast(Intent intent, String receiverPermission);
    
    /**
     * Broadcasts the given intent to all interested BroadcastReceivers, delivering
     * them one at a time.
     * 
     * @param intent The intent to broadcast
     */
    public abstract void sendOrderedBroadcast(Intent intent, String receiverPermission);
    
    /**
     * Broadcasts the given intent to all interested BroadcastReceivers, delivering
     * them one at a time.
     * 
     * @param intent The intent to broadcast
     * @param receiverPermission The permission that a receiver must hold in order to receive the broadcast
     * @param resultReceiver The BroadcastReceiver that will receive the final result
     * @param scheduler The Handler that will be used to deliver the final result
     * @param initialCode The initial value for the result code
     * @param initialData The initial value for the result data
     * @param initialExtras The initial value for the result extras
     */
    public abstract void sendOrderedBroadcast(Intent intent, String receiverPermission,
            BroadcastReceiver resultReceiver, android.os.Handler scheduler, int initialCode,
            String initialData, Bundle initialExtras);
    
    /**
     * Perform a Context.startService() operation.
     * 
     * @param service The service to start
     * @return The ComponentName of the service that is started
     */
    public abstract ComponentName startService(Intent service);
    
    /**
     * Perform a Context.stopService() operation.
     * 
     * @param service The service to stop
     * @return True if the service was stopped
     */
    public abstract boolean stopService(Intent service);
    
    /**
     * Bind to a service, creating it if needed.
     * 
     * @param service The service to bind to
     * @param conn The ServiceConnection object that will receive the service object when it is created
     * @param flags Operation options for the binding
     * @return True if the binding was successful
     */
    public abstract boolean bindService(Intent service, ServiceConnection conn, int flags);
    
    /**
     * Unbind from a service.
     * 
     * @param conn The ServiceConnection object that was used to bind to the service
     */
    public abstract void unbindService(ServiceConnection conn);
    
    /**
     * Interface to global information about an application environment.
     */
    public static class ServiceConnection {
        /**
         * Called when a connection to the Service has been established.
         * 
         * @param name The component name of the service that has been connected
         * @param service The IBinder of the Service's communication channel
         */
        public void onServiceConnected(ComponentName name, IBinder service) {
            // Empty implementation
        }
        
        /**
         * Called when a connection to the Service has been lost.
         * 
         * @param name The component name of the service whose connection has been lost
         */
        public void onServiceDisconnected(ComponentName name) {
            // Empty implementation
        }
    }
    
    /**
     * Base interface for a remotable object, the core part of a lightweight
     * remote procedure call mechanism designed for high performance when
     * performing in-process and cross-process calls.
     */
    public interface IBinder {
        // Empty interface for mocking purposes
    }
    
    /**
     * Interface used to receive intents broadcast by sendBroadcast().
     */
    public static class BroadcastReceiver {
        /**
         * This method is called when the BroadcastReceiver is receiving an Intent
         * broadcast.
         * 
         * @param context The Context in which the receiver is running
         * @param intent The Intent being received
         */
        public void onReceive(Context context, Intent intent) {
            // Empty implementation
        }
    }
    
    /**
     * Intents are matched against content://uri patterns; they are an action to be
     * performed on the content of a Uniform Resource Identifier.
     */
    public static class Intent {
        /**
         * Create an empty intent.
         */
        public Intent() {
            // Empty constructor
        }
        
        /**
         * Create an intent with a given action.
         * 
         * @param action The Intent action, such as ACTION_VIEW
         */
        public Intent(String action) {
            // Constructor with action
        }
        
        /**
         * Create an intent with a given action and uri.
         * 
         * @param action The Intent action, such as ACTION_VIEW
         * @param uri The Intent data URI
         */
        public Intent(String action, android.net.Uri uri) {
            // Constructor with action and uri
        }
        
        /**
         * Add extended data to the intent.
         * 
         * @param name The name of the extra data
         * @param value The boolean data value
         * @return This same Intent object, for chaining multiple calls into a single statement
         */
        public Intent putExtra(String name, boolean value) {
            return this;
        }
        
        /**
         * Add extended data to the intent.
         * 
         * @param name The name of the extra data
         * @param value The byte data value
         * @return This same Intent object, for chaining multiple calls into a single statement
         */
        public Intent putExtra(String name, byte value) {
            return this;
        }
        
        /**
         * Add extended data to the intent.
         * 
         * @param name The name of the extra data
         * @param value The char data value
         * @return This same Intent object, for chaining multiple calls into a single statement
         */
        public Intent putExtra(String name, char value) {
            return this;
        }
        
        /**
         * Add extended data to the intent.
         * 
         * @param name The name of the extra data
         * @param value The short data value
         * @return This same Intent object, for chaining multiple calls into a single statement
         */
        public Intent putExtra(String name, short value) {
            return this;
        }
        
        /**
         * Add extended data to the intent.
         * 
         * @param name The name of the extra data
         * @param value The int data value
         * @return This same Intent object, for chaining multiple calls into a single statement
         */
        public Intent putExtra(String name, int value) {
            return this;
        }
        
        /**
         * Add extended data to the intent.
         * 
         * @param name The name of the extra data
         * @param value The long data value
         * @return This same Intent object, for chaining multiple calls into a single statement
         */
        public Intent putExtra(String name, long value) {
            return this;
        }
        
        /**
         * Add extended data to the intent.
         * 
         * @param name The name of the extra data
         * @param value The float data value
         * @return This same Intent object, for chaining multiple calls into a single statement
         */
        public Intent putExtra(String name, float value) {
            return this;
        }
        
        /**
         * Add extended data to the intent.
         * 
         * @param name The name of the extra data
         * @param value The double data value
         * @return This same Intent object, for chaining multiple calls into a single statement
         */
        public Intent putExtra(String name, double value) {
            return this;
        }
        
        /**
         * Add extended data to the intent.
         * 
         * @param name The name of the extra data
         * @param value The String data value
         * @return This same Intent object, for chaining multiple calls into a single statement
         */
        public Intent putExtra(String name, String value) {
            return this;
        }
        
        /**
         * Add extended data to the intent.
         * 
         * @param name The name of the extra data
         * @param value The Bundle data value
         * @return This same Intent object, for chaining multiple calls into a single statement
         */
        public Intent putExtra(String name, Bundle value) {
            return this;
        }
    }
    
    /**
     * Immutable package information.
     */
    public class PackageManager {
        // Empty class for mocking purposes
    }
    
    /**
     * Interface to global asset manager.
     */
    public class AssetManager {
        /**
         * Open an asset using Asset.ACCESS_STREAMING mode.
         * 
         * @param fileName The name of the asset to open
         * @return The input stream for the asset
         */
        public InputStream open(String fileName) {
            return null;
        }
    }
    
    /**
     * Identifier for a specific application component.
     */
    public static class ComponentName {
        /**
         * Create a new component identifier.
         * 
         * @param pkg The package name
         * @param cls The class name
         */
        public ComponentName(String pkg, String cls) {
            // Constructor with package and class
        }
        
        /**
         * Create a new component identifier from a Context and class name.
         * 
         * @param pkg The Context of the package
         * @param cls The class name
         */
        public ComponentName(Context pkg, String cls) {
            // Constructor with context and class
        }
        
        /**
         * Create a new component identifier from a Context and Class object.
         * 
         * @param pkg The Context of the package
         * @param cls The Class object
         */
        public ComponentName(Context pkg, Class<?> cls) {
            // Constructor with context and class object
        }
    }
}