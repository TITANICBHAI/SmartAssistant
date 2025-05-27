package android.content;

import android.os.Bundle;

/**
 * Mock implementation of Android BroadcastReceiver class for development outside of Android.
 * Base class for code that will receive intents sent by sendBroadcast().
 */
public abstract class BroadcastReceiver {
    /**
     * Constructor.
     */
    public BroadcastReceiver() {
    }
    
    /**
     * This method is called when the BroadcastReceiver is receiving an Intent broadcast.
     * 
     * @param context The Context in which the receiver is running.
     * @param intent The Intent being received.
     */
    public abstract void onReceive(Context context, Intent intent);
    
    /**
     * Wrapper class holding the state of a receiver in an application.
     */
    public static class PendingResult {
        /**
         * Set the result data that will be sent to the next broadcast receiver.
         * 
         * @param code The result code.
         * @param data The result data.
         * @param extras The result extras.
         */
        public void setResult(int code, String data, Bundle extras) {
            // Mock implementation
        }
        
        /**
         * Finish this broadcast receiver; the pending result will be sent to the next broadcast receiver.
         */
        public void finish() {
            // Mock implementation
        }
    }
}