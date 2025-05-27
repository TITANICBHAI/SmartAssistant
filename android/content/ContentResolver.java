package android.content;

import android.database.Cursor;
import android.net.Uri;

/**
 * Mock implementation of Android ContentResolver class for development outside of Android.
 * The abstract class that provides access to content providers.
 */
public abstract class ContentResolver {
    /**
     * Constructor.
     */
    public ContentResolver() {
    }
    
    /**
     * Query the given URI, returning a Cursor over the result set.
     * 
     * @param uri The URI to query.
     * @param projection The list of columns to put into the cursor.
     * @param selection The SQL WHERE clause.
     * @param selectionArgs The arguments for the selection.
     * @param sortOrder The SQL ORDER BY clause.
     * @return A Cursor object, which is positioned before the first entry.
     */
    public abstract Cursor query(Uri uri, String[] projection, String selection, String[] selectionArgs, String sortOrder);
    
    /**
     * Insert a row into a table at the given URI.
     * 
     * @param uri The URI to insert into.
     * @param values The initial values for the newly inserted row.
     * @return The URI for the newly inserted row.
     */
    public abstract Uri insert(Uri uri, ContentValues values);
    
    /**
     * Update row(s) in a table at the given URI.
     * 
     * @param uri The URI to update.
     * @param values The values to update.
     * @param selection The SQL WHERE clause.
     * @param selectionArgs The arguments for the selection.
     * @return The number of rows affected.
     */
    public abstract int update(Uri uri, ContentValues values, String selection, String[] selectionArgs);
    
    /**
     * Delete row(s) from a table at the given URI.
     * 
     * @param uri The URI to delete from.
     * @param selection The SQL WHERE clause.
     * @param selectionArgs The arguments for the selection.
     * @return The number of rows affected.
     */
    public abstract int delete(Uri uri, String selection, String[] selectionArgs);
    
    /**
     * Register an observer for a content URI.
     * 
     * @param uri The URI to observe.
     * @param notifyForDescendants Whether to notify for changes to descendant URIs of the given URI.
     * @param observer The observer to register.
     */
    public abstract void registerContentObserver(Uri uri, boolean notifyForDescendants, ContentObserver observer);
    
    /**
     * Unregister a change observer.
     * 
     * @param observer The observer to unregister.
     */
    public abstract void unregisterContentObserver(ContentObserver observer);
    
    /**
     * Notify registered observers of a change.
     * 
     * @param uri The URI that has changed.
     * @param observer The observer that is responsible for the change.
     */
    public abstract void notifyChange(Uri uri, ContentObserver observer);
}