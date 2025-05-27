package android.content;

import android.net.Uri;
import android.os.Parcel;
import android.os.Parcelable;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

/**
 * Mock implementation of Android ContentProviderOperation class for development outside of Android.
 * Used to batch operations on a ContentProvider.
 */
public class ContentProviderOperation implements Parcelable {
    /**
     * The type of operation: insert, update, delete, or assert
     */
    private final int mType;
    
    /**
     * The target URI for the operation
     */
    @NonNull
    private final Uri mUri;
    
    /**
     * The selection string for the operation, may be null
     */
    @Nullable
    private final String mSelection;
    
    /**
     * The selection arguments for the operation, may be null
     */
    @Nullable
    private final String[] mSelectionArgs;
    
    /**
     * The values for insert and update operations, may be null
     */
    @Nullable
    private final ContentValues mValues;
    
    /**
     * The value back references for insert and update operations
     */
    @Nullable
    private final ContentValues mValuesBackReferences;
    
    /**
     * The URI of the back reference for the operation
     */
    private final int mValueBackRefIndex;
    
    /**
     * The expected count for assert operations
     */
    private final Integer mExpectedCount;
    
    /**
     * The type code for insert operations
     */
    public static final int TYPE_INSERT = 1;
    
    /**
     * The type code for update operations
     */
    public static final int TYPE_UPDATE = 2;
    
    /**
     * The type code for delete operations
     */
    public static final int TYPE_DELETE = 3;
    
    /**
     * The type code for assert operations
     */
    public static final int TYPE_ASSERT = 4;
    
    private ContentProviderOperation(Builder builder) {
        mType = builder.mType;
        mUri = builder.mUri;
        mSelection = builder.mSelection;
        mSelectionArgs = builder.mSelectionArgs;
        mValues = builder.mValues;
        mValuesBackReferences = builder.mValuesBackReferences;
        mValueBackRefIndex = builder.mValueBackRefIndex;
        mExpectedCount = builder.mExpectedCount;
    }
    
    /**
     * Create a ContentProviderOperation from a Parcel.
     * 
     * @param source The Parcel to read from.
     */
    public ContentProviderOperation(@NonNull Parcel source) {
        mType = source.readInt();
        mUri = Uri.CREATOR.createFromParcel(source);
        mSelection = source.readString();
        mSelectionArgs = source.createStringArray();
        mValues = source.readParcelable(ContentValues.class.getClassLoader());
        mValuesBackReferences = source.readParcelable(ContentValues.class.getClassLoader());
        mValueBackRefIndex = source.readInt();
        if (source.readInt() != 0) {
            mExpectedCount = source.readInt();
        } else {
            mExpectedCount = null;
        }
    }
    
    /**
     * Return a Builder that is initialized to create an operation that performs
     * the same action as this ContentProviderOperation.
     */
    @NonNull
    public Builder buildUpon() {
        Builder builder = new Builder(mUri);
        builder.mType = mType;
        builder.mSelection = mSelection;
        builder.mSelectionArgs = mSelectionArgs;
        builder.mValues = mValues;
        builder.mValuesBackReferences = mValuesBackReferences;
        builder.mValueBackRefIndex = mValueBackRefIndex;
        builder.mExpectedCount = mExpectedCount;
        return builder;
    }
    
    /**
     * Return the Uri that is the target of this operation.
     */
    @NonNull
    public Uri getUri() {
        return mUri;
    }
    
    @Override
    public int describeContents() {
        return 0;
    }
    
    @Override
    public void writeToParcel(@NonNull Parcel dest, int flags) {
        dest.writeInt(mType);
        mUri.writeToParcel(dest, flags);
        dest.writeString(mSelection);
        dest.writeStringArray(mSelectionArgs);
        dest.writeParcelable(mValues, flags);
        dest.writeParcelable(mValuesBackReferences, flags);
        dest.writeInt(mValueBackRefIndex);
        if (mExpectedCount != null) {
            dest.writeInt(1);
            dest.writeInt(mExpectedCount);
        } else {
            dest.writeInt(0);
        }
    }
    
    /**
     * The Parcelable creator for this class.
     */
    public static final Creator<ContentProviderOperation> CREATOR = new Creator<ContentProviderOperation>() {
        @Override
        @NonNull
        public ContentProviderOperation createFromParcel(Parcel source) {
            return new ContentProviderOperation(source);
        }
        
        @Override
        @NonNull
        public ContentProviderOperation[] newArray(int size) {
            return new ContentProviderOperation[size];
        }
    };
    
    /**
     * Apply this operation using the given provider.
     * 
     * @param provider The ContentProvider on which to operate.
     * @param backRefs The back references.
     * @param numBackRefs The number of back references.
     */
    @NonNull
    public ContentProviderResult apply(@NonNull ContentProvider provider, @Nullable ContentProviderResult[] backRefs,
                                      int numBackRefs) throws OperationApplicationException {
        ContentValues values = null;
        if (mValues != null) {
            values = new ContentValues(mValues);
        }
        
        // mock implementation, just simulate the results of different types of operations
        try {
            switch (mType) {
                case TYPE_INSERT:
                    // Simulate an insert returning a URI
                    return new ContentProviderResult(Uri.parse(mUri.toString() + "/new_item"));
                    
                case TYPE_UPDATE:
                    // Simulate an update affecting 1 row
                    return new ContentProviderResult(1);
                    
                case TYPE_DELETE:
                    // Simulate a delete affecting 1 row
                    return new ContentProviderResult(1);
                    
                case TYPE_ASSERT:
                    // Simulate an assert that passes
                    return new ContentProviderResult(mExpectedCount != null ? mExpectedCount : 1);
                    
                default:
                    throw new UnsupportedOperationException("Unknown operation type: " + mType);
            }
        } catch (Exception e) {
            throw new OperationApplicationException("Failed to apply operation: " + e.getMessage());
        }
    }
    
    /**
     * Return the type of operation.
     */
    public int getType() {
        return mType;
    }
    
    /**
     * A builder for ContentProviderOperation objects.
     */
    public static class Builder {
        /**
         * The operation type
         */
        int mType;
        
        /**
         * The URI for the operation
         */
        @NonNull
        final Uri mUri;
        
        /**
         * The selection string for update, delete, assert operations
         */
        @Nullable
        String mSelection;
        
        /**
         * The selection arguments for update, delete, assert operations
         */
        @Nullable
        String[] mSelectionArgs;
        
        /**
         * The values for insert and update operations
         */
        @Nullable
        ContentValues mValues;
        
        /**
         * The value back references for insert and update operations
         */
        @Nullable
        ContentValues mValuesBackReferences;
        
        /**
         * The URI back reference index for the operation
         */
        int mValueBackRefIndex = -1;
        
        /**
         * The expected count for assert operations
         */
        @Nullable
        Integer mExpectedCount;
        
        /**
         * Create a new builder for a ContentProviderOperation.
         * 
         * @param uri The URI to target with this operation.
         */
        public Builder(@NonNull Uri uri) {
            mUri = uri;
        }
        
        /**
         * Build the ContentProviderOperation.
         * 
         * @return A ContentProviderOperation with the given properties.
         */
        @NonNull
        public ContentProviderOperation build() {
            if (mType == TYPE_INSERT && mValues == null && mValuesBackReferences == null) {
                throw new IllegalArgumentException("Insert operation must have values or value back references");
            }
            return new ContentProviderOperation(this);
        }
        
        /**
         * Set this operation to be an insert.
         * 
         * @return This Builder object to allow for chaining of calls to builder methods.
         */
        @NonNull
        public Builder withInsert() {
            mType = TYPE_INSERT;
            return this;
        }
        
        /**
         * Set this operation to be an update.
         * 
         * @return This Builder object to allow for chaining of calls to builder methods.
         */
        @NonNull
        public Builder withUpdate() {
            mType = TYPE_UPDATE;
            return this;
        }
        
        /**
         * Set this operation to be a delete.
         * 
         * @return This Builder object to allow for chaining of calls to builder methods.
         */
        @NonNull
        public Builder withDelete() {
            mType = TYPE_DELETE;
            return this;
        }
        
        /**
         * Set this operation to be an assert.
         * 
         * @return This Builder object to allow for chaining of calls to builder methods.
         */
        @NonNull
        public Builder withAssert() {
            mType = TYPE_ASSERT;
            return this;
        }
        
        /**
         * The selection to use for this operation.
         * 
         * @param selection The selection string to use, formatted as a WHERE clause.
         * @param selectionArgs The selection arguments to use.
         * @return This Builder object to allow for chaining of calls to builder methods.
         */
        @NonNull
        public Builder withSelection(@Nullable String selection, @Nullable String[] selectionArgs) {
            mSelection = selection;
            mSelectionArgs = selectionArgs;
            return this;
        }
        
        /**
         * Add values to this operation.
         * 
         * @param values The ContentValues to add.
         * @return This Builder object to allow for chaining of calls to builder methods.
         */
        @NonNull
        public Builder withValues(@Nullable ContentValues values) {
            if (values != null) {
                if (mValues == null) {
                    mValues = new ContentValues(values);
                } else {
                    mValues.putAll(values);
                }
            }
            return this;
        }
        
        /**
         * Add a back reference to this operation.
         * 
         * @param key The ContentValues key
         * @param previousResult The index of the previous result
         * @return This Builder object to allow for chaining of calls to builder methods.
         */
        @NonNull
        public Builder withValueBackReference(@NonNull String key, int previousResult) {
            if (mValuesBackReferences == null) {
                mValuesBackReferences = new ContentValues();
            }
            mValuesBackReferences.put(key, previousResult);
            return this;
        }
        
        /**
         * Use a back reference for the URI.
         * 
         * @param previousResult The index of the previous result
         * @return This Builder object to allow for chaining of calls to builder methods.
         */
        @NonNull
        public Builder withValueBackRefIndex(int previousResult) {
            mValueBackRefIndex = previousResult;
            return this;
        }
        
        /**
         * Set the expected count for an assert operation.
         * 
         * @param count The expected count, or null for no expected count.
         * @return This Builder object to allow for chaining of calls to builder methods.
         */
        @NonNull
        public Builder withExpectedCount(@Nullable Integer count) {
            mExpectedCount = count;
            return this;
        }
    }
    
    /**
     * Creates a ContentProviderOperation that inserts a row into a table at the given URL.
     * 
     * @param uri The URI to insert into.
     * @return A Builder object to construct the ContentProviderOperation.
     */
    @NonNull
    public static Builder newInsert(@NonNull Uri uri) {
        Builder builder = new Builder(uri);
        builder.withInsert();
        return builder;
    }
    
    /**
     * Creates a ContentProviderOperation that updates rows in a table at the given URL.
     * 
     * @param uri The URI to update.
     * @return A Builder object to construct the ContentProviderOperation.
     */
    @NonNull
    public static Builder newUpdate(@NonNull Uri uri) {
        Builder builder = new Builder(uri);
        builder.withUpdate();
        return builder;
    }
    
    /**
     * Creates a ContentProviderOperation that deletes rows from a table at the given URL.
     * 
     * @param uri The URI to delete from.
     * @return A Builder object to construct the ContentProviderOperation.
     */
    @NonNull
    public static Builder newDelete(@NonNull Uri uri) {
        Builder builder = new Builder(uri);
        builder.withDelete();
        return builder;
    }
    
    /**
     * Creates a ContentProviderOperation that asserts about the number of rows in a table at the given URL.
     * 
     * @param uri The URI to assert about.
     * @return A Builder object to construct the ContentProviderOperation.
     */
    @NonNull
    public static Builder newAssertQuery(@NonNull Uri uri) {
        Builder builder = new Builder(uri);
        builder.withAssert();
        return builder;
    }
    
    @Override
    @NonNull
    public String toString() {
        StringBuilder sb = new StringBuilder();
        switch (mType) {
            case TYPE_INSERT:
                sb.append("INSERT");
                break;
            case TYPE_UPDATE:
                sb.append("UPDATE");
                break;
            case TYPE_DELETE:
                sb.append("DELETE");
                break;
            case TYPE_ASSERT:
                sb.append("ASSERT");
                break;
            default:
                sb.append("UNKNOWN");
                break;
        }
        sb.append(" ").append(mUri);
        if (mSelection != null) {
            sb.append(" WHERE ").append(mSelection);
        }
        if (mValues != null) {
            sb.append(" VALUES ").append(mValues);
        }
        return sb.toString();
    }
}