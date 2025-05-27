package android.graphics;

import android.os.Parcel;
import android.os.Parcelable;

/**
 * A Parcelable wrapper for Path objects
 */
public class PathParcelable implements Parcelable {
    private Path mPath;
    
    /**
     * Constructor
     * @param path The path to wrap
     */
    public PathParcelable(Path path) {
        mPath = path;
    }
    
    /**
     * Get the wrapped path
     * @return The Path object
     */
    public Path getPath() {
        return mPath;
    }
    
    @Override
    public int describeContents() {
        return 0;
    }
    
    @Override
    public void writeToParcel(Parcel dest, int flags) {
        // In a real implementation, we would serialize the path
        // For now we just serialize a placeholder
        dest.writeString("PATH_PLACEHOLDER");
    }
    
    /**
     * Creator for Parcelable
     */
    public static final Parcelable.Creator<PathParcelable> CREATOR = 
        new Parcelable.Creator<PathParcelable>() {
            @Override
            public PathParcelable createFromParcel(Parcel source) {
                // Read the placeholder string
                source.readString();
                // Return a new Path
                return new PathParcelable(new Path());
            }
            
            @Override
            public PathParcelable[] newArray(int size) {
                return new PathParcelable[size];
            }
        };
}