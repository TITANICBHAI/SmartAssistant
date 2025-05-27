package android.content;

import android.net.Uri;
import android.os.Handler;

/**
 * Mock implementation of Android ContentObserver class for development outside of Android.
 * Receives call backs when the content of a content provider changes.
 */
public abstract class ContentObserver {
    private final Handler mHandler;
    
    /**
     * Creates a content observer.
     * 
     * @param handler The handler to run callbacks on, or null if the callbacks should be run on the main thread.
     */
    public ContentObserver(Handler handler) {
        mHandler = handler;
    }
    
    /**
     * Returns true if this observer is interested in receiving self-change notifications.
     * 
     * @return true if self-change notifications should be delivered to this observer.
     */
    public boolean deliverSelfNotifications() {
        return false;
    }
    
    /**
     * This method is called when a content change occurs.
     * 
     * @param selfChange True if this is a self-change notification.
     */
    public void onChange(boolean selfChange) {
        // Do nothing by default
    }
    
    /**
     * This method is called when a content change occurs.
     * 
     * @param selfChange True if this is a self-change notification.
     * @param uri The URI of the changed content, or null if unknown.
     */
    public void onChange(boolean selfChange, Uri uri) {
        onChange(selfChange);
    }
    
    /**
     * Dispatch a change notification.
     * 
     * @param selfChange True if this is a self-change notification.
     */
    public final void dispatchChange(boolean selfChange) {
        dispatchChange(selfChange, null);
    }
    
    /**
     * Dispatch a change notification.
     * 
     * @param selfChange True if this is a self-change notification.
     * @param uri The URI of the changed content, or null if unknown.
     */
    public final void dispatchChange(boolean selfChange, Uri uri) {
        if (mHandler == null) {
            // Handle synchronously
            onChange(selfChange, uri);
        } else {
            // Post the change to the handler
            mHandler.post(new Runnable() {
                @Override
                public void run() {
                    onChange(selfChange, uri);
                }
            });
        }
    }
}