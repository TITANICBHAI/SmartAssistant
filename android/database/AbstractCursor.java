package android.database;

import android.content.ContentResolver;
import android.net.Uri;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * Mock implementation of Android AbstractCursor class for development outside of Android.
 * This is an abstract cursor implementation that provides common functionality.
 * Subclassing this class is encouraged for all custom cursors.
 */
public abstract class AbstractCursor implements Cursor {
    /**
     * The current position of the cursor.
     */
    protected int mPos;
    
    /**
     * The list of registered content observers.
     */
    protected final List<ContentObserver> mContentObservers = new ArrayList<>();
    
    /**
     * The list of registered data set observers.
     */
    protected final List<DataSetObserver> mDataSetObservers = new ArrayList<>();
    
    /**
     * The extras bundle.
     */
    private Bundle mExtras = Bundle.EMPTY;
    
    /**
     * The notification URI.
     */
    private Uri mNotifyUri;
    
    /**
     * The content resolver.
     */
    private ContentResolver mContentResolver;
    
    /**
     * Whether the cursor is closed.
     */
    private boolean mClosed;
    
    /**
     * Constructor
     */
    public AbstractCursor() {
        mPos = -1;
    }
    
    @Override
    public int getPosition() {
        return mPos;
    }
    
    /**
     * Sets the cursor position to the specified index.
     * 
     * @param position The position to move to.
     * @return True if the move was successful, false otherwise.
     */
    @Override
    public boolean moveToPosition(int position) {
        // Make sure position isn't past the end of the cursor
        final int count = getCount();
        if (position >= count) {
            mPos = count;
            return false;
        }
        
        // Make sure position isn't before the beginning of the cursor
        if (position < 0) {
            mPos = -1;
            return false;
        }
        
        // Check for no-op moves, and skip them
        if (position == mPos) {
            return true;
        }
        
        mPos = position;
        return true;
    }
    
    @Override
    public final boolean moveToFirst() {
        return moveToPosition(0);
    }
    
    @Override
    public final boolean moveToLast() {
        return moveToPosition(getCount() - 1);
    }
    
    @Override
    public final boolean moveToNext() {
        return moveToPosition(mPos + 1);
    }
    
    @Override
    public final boolean moveToPrevious() {
        return moveToPosition(mPos - 1);
    }
    
    @Override
    public final boolean isFirst() {
        return mPos == 0 && getCount() != 0;
    }
    
    @Override
    public final boolean isLast() {
        int count = getCount();
        return mPos == count - 1 && count != 0;
    }
    
    @Override
    public final boolean isBeforeFirst() {
        if (getCount() == 0) {
            return true;
        }
        return mPos == -1;
    }
    
    @Override
    public final boolean isAfterLast() {
        if (getCount() == 0) {
            return true;
        }
        return mPos == getCount();
    }
    
    @Override
    public int getColumnIndex(@NonNull String columnName) {
        // Simplified implementation. In a real implementation, this would look up
        // the index of the column with the given name.
        return -1;
    }
    
    @Override
    public int getColumnIndexOrThrow(@NonNull String columnName) {
        int index = getColumnIndex(columnName);
        if (index < 0) {
            throw new IllegalArgumentException("Column '" + columnName + "' does not exist.");
        }
        return index;
    }
    
    @Override
    @NonNull
    public String getColumnName(int columnIndex) {
        String[] names = getColumnNames();
        if (columnIndex < 0 || columnIndex >= names.length) {
            throw new IllegalArgumentException("Column index out of bounds: " + columnIndex);
        }
        return names[columnIndex];
    }
    
    /**
     * Returns data type of the given column's value.
     * 
     * @param columnIndex The zero-based index of the target column.
     * @return The data type for the value in the column.
     */
    @Override
    public int getType(int columnIndex) {
        // Default implementation returns NULL type
        return FIELD_TYPE_NULL;
    }
    
    /**
     * Returns a bundle of extra values that the cursor contains.
     * 
     * @return A bundle of cursor extras, or Bundle.EMPTY if no extras are available.
     */
    @Override
    @NonNull
    public Bundle getExtras() {
        return mExtras;
    }
    
    /**
     * Sets a bundle of extra values for the cursor.
     * 
     * @param extras A bundle of extra values.
     */
    @Override
    public void setExtras(@NonNull Bundle extras) {
        mExtras = extras != null ? extras : Bundle.EMPTY;
    }
    
    @Override
    @NonNull
    public Bundle respond(@NonNull Bundle extras) {
        return Bundle.EMPTY;
    }
    
    @Override
    public void registerContentObserver(@NonNull ContentObserver observer) {
        if (!mClosed) {
            mContentObservers.add(observer);
        }
    }
    
    @Override
    public void unregisterContentObserver(@NonNull ContentObserver observer) {
        if (!mClosed) {
            mContentObservers.remove(observer);
        }
    }
    
    @Override
    public void registerDataSetObserver(@NonNull DataSetObserver observer) {
        if (!mClosed) {
            mDataSetObservers.add(observer);
        }
    }
    
    @Override
    public void unregisterDataSetObserver(@NonNull DataSetObserver observer) {
        if (!mClosed) {
            mDataSetObservers.remove(observer);
        }
    }
    
    @Override
    public void setNotificationUri(@NonNull ContentResolver cr, @NonNull Uri uri) {
        mContentResolver = cr;
        mNotifyUri = uri;
    }
    
    @Override
    @Nullable
    public Uri getNotificationUri() {
        return mNotifyUri;
    }
    
    @Override
    public boolean isClosed() {
        return mClosed;
    }
    
    @Override
    public void close() {
        mClosed = true;
        mContentObservers.clear();
        mDataSetObservers.clear();
    }
    
    /**
     * Notifies all registered content observers that the data has changed.
     * 
     * @param selfChange True if the change is due to a self-operation.
     */
    protected void notifyContentObservers(boolean selfChange) {
        for (ContentObserver observer : mContentObservers) {
            observer.dispatchChange(selfChange, mNotifyUri);
        }
    }
    
    /**
     * Notifies all registered data set observers that the data set has changed.
     */
    protected void notifyDataSetChanged() {
        for (DataSetObserver observer : mDataSetObservers) {
            observer.onChanged();
        }
    }
    
    /**
     * Notifies all registered data set observers that the data set has been invalidated.
     */
    protected void notifyDataSetInvalidated() {
        for (DataSetObserver observer : mDataSetObservers) {
            observer.onInvalidated();
        }
    }
    
    @Override
    @Deprecated
    public void deactivate() {
        // No-op in the mock implementation
    }
    
    @Override
    @Deprecated
    public boolean requery() {
        return false;
    }
    
    // Default implementations of value getters
    
    @Override
    @Nullable
    public byte[] getBlob(int columnIndex) {
        throw new UnsupportedOperationException("getBlob not implemented");
    }
    
    @Override
    @Nullable
    public String getString(int columnIndex) {
        throw new UnsupportedOperationException("getString not implemented");
    }
    
    @Override
    public short getShort(int columnIndex) {
        return (short) getInt(columnIndex);
    }
    
    @Override
    public int getInt(int columnIndex) {
        throw new UnsupportedOperationException("getInt not implemented");
    }
    
    @Override
    public long getLong(int columnIndex) {
        throw new UnsupportedOperationException("getLong not implemented");
    }
    
    @Override
    public float getFloat(int columnIndex) {
        throw new UnsupportedOperationException("getFloat not implemented");
    }
    
    @Override
    public double getDouble(int columnIndex) {
        throw new UnsupportedOperationException("getDouble not implemented");
    }
    
    @Override
    public boolean isNull(int columnIndex) {
        throw new UnsupportedOperationException("isNull not implemented");
    }
}