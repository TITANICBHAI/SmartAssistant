package android.content;

import android.os.IBinder;

/**
 * Mock implementation of Android ServiceConnection interface for development outside of Android.
 * Interface for monitoring the state of an application service.
 */
public interface ServiceConnection {
    /**
     * Called when a connection to the Service has been established, with
     * the IBinder of the communication channel to the Service.
     *
     * @param name The concrete component name of the service that has been connected.
     * @param service The IBinder of the Service's communication channel.
     */
    void onServiceConnected(ComponentName name, IBinder service);
    
    /**
     * Called when a connection to the Service has been lost.  This typically
     * happens when the process hosting the service has crashed or been killed.
     *
     * @param name The concrete component name of the service that has been disconnected.
     */
    void onServiceDisconnected(ComponentName name);
}