package android.content;

import android.net.Uri;
import android.os.Parcel;
import android.os.Parcelable;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

/**
 * Mock implementation of Android ContentProviderResult class for development outside of Android.
 * Represents the result of a ContentProvider operation.
 */
public class ContentProviderResult implements Parcelable {
    @Nullable
    public final Uri uri;
    public final int count;
    
    /**
     * Creates a result for an insert operation.
     * 
     * @param uri The URI of the inserted item.
     */
    public ContentProviderResult(@NonNull Uri uri) {
        this.uri = uri;
        this.count = 0;
    }
    
    /**
     * Creates a result for an update or delete operation.
     * 
     * @param count The number of rows affected.
     */
    public ContentProviderResult(int count) {
        this.uri = null;
        this.count = count;
    }
    
    /**
     * Creates a ContentProviderResult from a Parcel.
     * 
     * @param source The Parcel to read from.
     */
    public ContentProviderResult(@NonNull Parcel source) {
        uri = source.readParcelable(Uri.class.getClassLoader());
        count = source.readInt();
    }
    
    @Override
    public int describeContents() {
        return 0;
    }
    
    @Override
    public void writeToParcel(@NonNull Parcel dest, int flags) {
        dest.writeParcelable(uri, flags);
        dest.writeInt(count);
    }
    
    /**
     * The Parcelable creator for this class.
     */
    public static final Creator<ContentProviderResult> CREATOR = new Creator<ContentProviderResult>() {
        @Override
        public ContentProviderResult createFromParcel(Parcel source) {
            return new ContentProviderResult(source);
        }
        
        @Override
        public ContentProviderResult[] newArray(int size) {
            return new ContentProviderResult[size];
        }
    };
    
    @Override
    @NonNull
    public String toString() {
        if (uri != null) {
            return "ContentProviderResult(uri=" + uri + ")";
        } else {
            return "ContentProviderResult(count=" + count + ")";
        }
    }
}