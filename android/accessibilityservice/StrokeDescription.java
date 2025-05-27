package android.accessibilityservice;

import android.os.Parcel;
import android.os.Parcelable;
import utils.Rect;
import utils.RectHelper;

/**
 * Mock implementation of Android's StrokeDescription class.
 */
public class StrokeDescription implements Parcelable {
    /**
     * The path for this stroke.
     */
    private final Path mPath;
    
    /**
     * The start time for this stroke.
     */
    private final long mStartTime;
    
    /**
     * The duration for this stroke.
     */
    private final long mDuration;
    
    /**
     * Whether this stroke is a continuation of another stroke.
     */
    private final boolean mIsContinued;
    
    /**
     * Constructor.
     * 
     * @param path The path
     * @param startTime The start time
     * @param duration The duration
     */
    public StrokeDescription(Path path, long startTime, long duration) {
        this(path, startTime, duration, false);
    }
    
    /**
     * Constructor.
     * 
     * @param path The path
     * @param startTime The start time
     * @param duration The duration
     * @param isContinued Whether this stroke is a continuation of another stroke
     */
    public StrokeDescription(Path path, long startTime, long duration, boolean isContinued) {
        if (path == null) {
            throw new IllegalArgumentException("path cannot be null");
        }
        if (startTime < 0) {
            throw new IllegalArgumentException("startTime must be >= 0");
        }
        if (duration <= 0) {
            throw new IllegalArgumentException("duration must be > 0");
        }
        
        mPath = path;
        mStartTime = startTime;
        mDuration = duration;
        mIsContinued = isContinued;
    }
    
    /**
     * Get the path for this stroke.
     * 
     * @return The path
     */
    public Path getPath() {
        return mPath;
    }
    
    /**
     * Get the start time for this stroke.
     * 
     * @return The start time
     */
    public long getStartTime() {
        return mStartTime;
    }
    
    /**
     * Get the duration for this stroke.
     * 
     * @return The duration
     */
    public long getDuration() {
        return mDuration;
    }
    
    /**
     * Get the end time for this stroke.
     * 
     * @return The end time
     */
    public long getEndTime() {
        return mStartTime + mDuration;
    }
    
    /**
     * Whether this stroke is a continuation of another stroke.
     * 
     * @return Whether this stroke is a continuation
     */
    public boolean isContinued() {
        return mIsContinued;
    }
    
    /**
     * Get a continuation of this stroke.
     * 
     * @param path The path for the continuation
     * @param startTime The start time for the continuation
     * @param duration The duration for the continuation
     * @return A new stroke description
     */
    public StrokeDescription continueStroke(Path path, long startTime, long duration) {
        if (startTime < getEndTime()) {
            throw new IllegalArgumentException("startTime must be >= end time of this stroke");
        }
        
        return new StrokeDescription(path, startTime, duration, true);
    }
    
    /**
     * Describe the contents of this object.
     */
    @Override
    public int describeContents() {
        return 0;
    }
    
    /**
     * Write this object to a parcel.
     */
    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeParcelable(mPath, flags);
        dest.writeLong(mStartTime);
        dest.writeLong(mDuration);
        dest.writeBoolean(mIsContinued);
    }
    
    /**
     * Creator for StrokeDescription.
     */
    public static final Parcelable.Creator<StrokeDescription> CREATOR = new Parcelable.Creator<StrokeDescription>() {
        @Override
        public StrokeDescription createFromParcel(Parcel source) {
            Path path = source.readParcelable(Path.class.getClassLoader());
            long startTime = source.readLong();
            long duration = source.readLong();
            boolean isContinued = source.readBoolean();
            
            return new StrokeDescription(path, startTime, duration, isContinued);
        }
        
        @Override
        public StrokeDescription[] newArray(int size) {
            return new StrokeDescription[size];
        }
    };
    
    /**
     * Mock implementation of Android's gesture path.
     */
    public static class Path implements Parcelable {
        /**
         * The rect bounds of this path.
         */
        private Rect mBounds;
        
        /**
         * The start x coordinate.
         */
        private float mStartX;
        
        /**
         * The start y coordinate.
         */
        private float mStartY;
        
        /**
         * The end x coordinate.
         */
        private float mEndX;
        
        /**
         * The end y coordinate.
         */
        private float mEndY;
        
        /**
         * Constructor.
         * 
         * @param startX The start x coordinate
         * @param startY The start y coordinate
         */
        public Path(float startX, float startY) {
            mStartX = startX;
            mStartY = startY;
            mEndX = startX;
            mEndY = startY;
            
            mBounds = new Rect((int) startX, (int) startY, (int) startX, (int) startY);
        }
        
        /**
         * Move to a point.
         * 
         * @param x The x coordinate
         * @param y The y coordinate
         * @return This path
         */
        public Path moveTo(float x, float y) {
            mStartX = x;
            mStartY = y;
            mEndX = x;
            mEndY = y;
            
            mBounds = new Rect((int) x, (int) y, (int) x, (int) y);
            
            return this;
        }
        
        /**
         * Add a line to the path.
         * 
         * @param x The x coordinate
         * @param y The y coordinate
         * @return This path
         */
        public Path lineTo(float x, float y) {
            mEndX = x;
            mEndY = y;
            
            int left = Math.min(mBounds.left, (int) x);
            int top = Math.min(mBounds.top, (int) y);
            int right = Math.max(mBounds.right, (int) x);
            int bottom = Math.max(mBounds.bottom, (int) y);
            
            mBounds = new Rect(left, top, right, bottom);
            
            return this;
        }
        
        /**
         * Get the start x coordinate.
         * 
         * @return The start x coordinate
         */
        public float getStartX() {
            return mStartX;
        }
        
        /**
         * Get the start y coordinate.
         * 
         * @return The start y coordinate
         */
        public float getStartY() {
            return mStartY;
        }
        
        /**
         * Get the end x coordinate.
         * 
         * @return The end x coordinate
         */
        public float getEndX() {
            return mEndX;
        }
        
        /**
         * Get the end y coordinate.
         * 
         * @return The end y coordinate
         */
        public float getEndY() {
            return mEndY;
        }
        
        /**
         * Get the bounds of this path.
         * 
         * @return The bounds
         */
        public Rect getBounds() {
            return mBounds;
        }
        
        /**
         * Describe the contents of this object.
         */
        @Override
        public int describeContents() {
            return 0;
        }
        
        /**
         * Write this object to a parcel.
         */
        @Override
        public void writeToParcel(Parcel dest, int flags) {
            dest.writeFloat(mStartX);
            dest.writeFloat(mStartY);
            dest.writeFloat(mEndX);
            dest.writeFloat(mEndY);
            dest.writeInt(mBounds.left);
            dest.writeInt(mBounds.top);
            dest.writeInt(mBounds.right);
            dest.writeInt(mBounds.bottom);
        }
        
        /**
         * Constructor from parcel.
         * 
         * @param source The parcel
         */
        private Path(Parcel source) {
            mStartX = source.readFloat();
            mStartY = source.readFloat();
            mEndX = source.readFloat();
            mEndY = source.readFloat();
            int left = source.readInt();
            int top = source.readInt();
            int right = source.readInt();
            int bottom = source.readInt();
            mBounds = new Rect(left, top, right, bottom);
        }
        
        /**
         * Creator for Path.
         */
        public static final Parcelable.Creator<Path> CREATOR = new Parcelable.Creator<Path>() {
            @Override
            public Path createFromParcel(Parcel source) {
                return new Path(source);
            }
            
            @Override
            public Path[] newArray(int size) {
                return new Path[size];
            }
        };
    }
}