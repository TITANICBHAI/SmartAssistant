package android.database;

import android.net.Uri;
import android.os.Handler;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

/**
 * Mock implementation of Android ContentObserver class for development outside of Android.
 * Receives callbacks for changes to data at a given URI.
 * To receive callbacks the ContentObserver must be registered with
 * {@link android.content.ContentResolver#registerContentObserver(Uri, boolean, ContentObserver)}.
 */
public abstract class ContentObserver {
    private final Handler mHandler;
    private final Object mLock = new Object();
    
    /**
     * Creates a content observer with a null handler. The onChange handler
     * method will be called directly without dispatching to a specified
     * handler.
     */
    public ContentObserver() {
        mHandler = null;
    }
    
    /**
     * Creates a content observer with a handler.
     * 
     * @param handler The handler to run {@link #onChange} on, or null if none.
     */
    public ContentObserver(@Nullable Handler handler) {
        mHandler = handler;
    }
    
    /**
     * Return the associated handler.
     * 
     * @return The associated handler.
     */
    @Nullable
    public final Handler getHandler() {
        return mHandler;
    }
    
    /**
     * Called when a content change occurs.
     * 
     * @param selfChange True if this is a self-change notification.
     */
    public void onChange(boolean selfChange) {
        // Default implementation is empty. Subclasses should override.
    }
    
    /**
     * Called when a content change occurs.
     * 
     * @param selfChange True if this is a self-change notification.
     * @param uri The Uri of the changed content, or null if unknown.
     */
    public void onChange(boolean selfChange, @Nullable Uri uri) {
        onChange(selfChange);
    }
    
    /**
     * Called when a content change occurs.
     * 
     * @param selfChange True if this is a self-change notification.
     * @param uri The Uri of the changed content, or null if unknown.
     * @param flags Additional flags about this change, if any.
     */
    public void onChange(boolean selfChange, @Nullable Uri uri, int flags) {
        onChange(selfChange, uri);
    }
    
    /**
     * Called to deliver a change notification.
     * 
     * @param selfChange True if this is a self-change notification.
     * @param uri The Uri of the changed content, or null if unknown.
     * @param userId The user whose content was changed.
     */
    public void dispatchChange(boolean selfChange, @Nullable Uri uri, int userId) {
        onChange(selfChange, uri);
    }
    
    /**
     * Called to deliver a change notification.
     * 
     * @param selfChange True if this is a self-change notification.
     */
    public void dispatchChange(boolean selfChange) {
        onChange(selfChange);
    }
    
    /**
     * Called to deliver a change notification.
     * 
     * @param selfChange True if this is a self-change notification.
     * @param uri The Uri of the changed content, or null if unknown.
     */
    public void dispatchChange(boolean selfChange, @Nullable Uri uri) {
        onChange(selfChange, uri);
    }
    
    /**
     * Called to give the observer the option to delay dispatching of
     * the onChange method by returning a non-null task.
     * If the function returns null, dispatching will happen immediately
     * and no more tasks for the same Uri will be received
     * until the observer releases the Uri.
     * 
     * @param uri The Uri of the changed content, or null if unknown.
     * @return A task that should be executed to actually dispatch the change,
     *         or null if the dispatching should be done immediately.
     */
    @Nullable
    public Object waitForCommit(@NonNull Uri uri) {
        return null;
    }
    
    /**
     * Called when the observer is released.
     * This method is guaranteed to be called when the observer is released.
     */
    public void releaseObserver() {
        // Do nothing by default.
    }
}