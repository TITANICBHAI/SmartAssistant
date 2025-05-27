package android.accessibilityservice;

import android.graphics.Path;
import android.graphics.PathParcelable;
import android.os.Parcel;
import android.os.Parcelable;

/**
 * Mock implementation of Android's GestureDescription class.
 * This class is used to describe gestures for accessibility services.
 */
public final class GestureDescription implements Parcelable {
    /**
     * Maximum number of strokes in a gesture.
     */
    public static final int MAX_STROKE_COUNT = 100;
    
    /**
     * Maximum gesture duration.
     */
    public static final long MAX_GESTURE_DURATION_MS = 60000; // 1 minute
    
    private final Builder mBuilder;
    
    private GestureDescription(Builder builder) {
        mBuilder = builder;
    }
    
    /**
     * Get the strokes in this gesture.
     * 
     * @return The strokes
     */
    public StrokeDescription[] getStrokes() {
        return mBuilder.getStrokes();
    }
    
    /**
     * Get the number of strokes in this gesture.
     * 
     * @return The number of strokes
     */
    public int getStrokeCount() {
        return mBuilder.getStrokeCount();
    }
    
    /**
     * Get the duration of this gesture.
     * 
     * @return The duration in milliseconds
     */
    public long getDuration() {
        return mBuilder.getDuration();
    }
    
    /**
     * A builder for GestureDescription objects.
     */
    public static final class Builder {
        private StrokeDescription[] mStrokes;
        private int mStrokeCount;
        private long mDuration;
        
        /**
         * Create a new builder.
         */
        public Builder() {
            mStrokes = new StrokeDescription[MAX_STROKE_COUNT];
            mStrokeCount = 0;
            mDuration = 0;
        }
        
        /**
         * Add a stroke to the gesture.
         * 
         * @param stroke The stroke to add
         * @return This builder
         */
        public Builder addStroke(StrokeDescription stroke) {
            if (mStrokeCount >= MAX_STROKE_COUNT) {
                throw new IllegalStateException("Gesture has too many strokes");
            }
            
            mStrokes[mStrokeCount++] = stroke;
            mDuration = Math.max(mDuration, stroke.getEndTime());
            
            return this;
        }
        
        /**
         * Get the strokes in this gesture.
         * 
         * @return The strokes
         */
        public StrokeDescription[] getStrokes() {
            StrokeDescription[] result = new StrokeDescription[mStrokeCount];
            System.arraycopy(mStrokes, 0, result, 0, mStrokeCount);
            return result;
        }
        
        /**
         * Get the number of strokes in this gesture.
         * 
         * @return The number of strokes
         */
        public int getStrokeCount() {
            return mStrokeCount;
        }
        
        /**
         * Get the duration of this gesture.
         * 
         * @return The duration in milliseconds
         */
        public long getDuration() {
            return mDuration;
        }
        
        /**
         * Build a GestureDescription.
         * 
         * @return The new GestureDescription
         */
        public GestureDescription build() {
            if (mStrokeCount == 0) {
                throw new IllegalStateException("Gesture has no strokes");
            }
            
            return new GestureDescription(this);
        }
    }
    
    /**
     * A single stroke in a gesture.
     */
    public static final class StrokeDescription implements Parcelable {
        private final Path mPath;
        private final long mStartTime;
        private final long mDuration;
        
        /**
         * Create a new stroke.
         * 
         * @param path The path of the stroke
         * @param startTime The start time of the stroke
         * @param duration The duration of the stroke
         */
        public StrokeDescription(Path path, long startTime, long duration) {
            if (path == null) {
                throw new IllegalArgumentException("Path cannot be null");
            }
            
            if (duration <= 0) {
                throw new IllegalArgumentException("Duration must be positive");
            }
            
            if (startTime < 0) {
                throw new IllegalArgumentException("Start time cannot be negative");
            }
            
            if (startTime + duration > MAX_GESTURE_DURATION_MS) {
                throw new IllegalArgumentException("Gesture duration too long");
            }
            
            mPath = new Path(path); // Make a copy to avoid modification
            mStartTime = startTime;
            mDuration = duration;
        }
        
        /**
         * Get the path of this stroke.
         * 
         * @return The path
         */
        public Path getPath() {
            return mPath;
        }
        
        /**
         * Get the start time of this stroke.
         * 
         * @return The start time in milliseconds
         */
        public long getStartTime() {
            return mStartTime;
        }
        
        /**
         * Get the duration of this stroke.
         * 
         * @return The duration in milliseconds
         */
        public long getDuration() {
            return mDuration;
        }
        
        /**
         * Get the end time of this stroke.
         * 
         * @return The end time in milliseconds
         */
        public long getEndTime() {
            return mStartTime + mDuration;
        }
        
        /**
         * Create a continuation of this stroke.
         * 
         * @param path The continuation path
         * @param startTime The start time of the continuation
         * @param duration The duration of the continuation
         * @return A new stroke that continues this one
         */
        public StrokeDescription continueStroke(Path path, long startTime, long duration) {
            if (startTime != getEndTime()) {
                throw new IllegalArgumentException("Continuation must start at end of previous stroke");
            }
            
            Path continuedPath = new Path(mPath);
            continuedPath.addPath(path);
            
            return new StrokeDescription(continuedPath, mStartTime, mDuration + duration);
        }
        
        @Override
        public int describeContents() {
            return 0;
        }
        
        @Override
        public void writeToParcel(Parcel dest, int flags) {
            dest.writeParcelable(new PathParcelable(mPath), flags);
            dest.writeLong(mStartTime);
            dest.writeLong(mDuration);
        }
        
        public static final Parcelable.Creator<StrokeDescription> CREATOR = new Parcelable.Creator<StrokeDescription>() {
            @Override
            public StrokeDescription createFromParcel(Parcel source) {
                PathParcelable pathParcelable = source.readParcelable(PathParcelable.class.getClassLoader());
                Path path = (pathParcelable != null) ? pathParcelable.getPath() : new Path();
                long startTime = source.readLong();
                long duration = source.readLong();
                
                return new StrokeDescription(path, startTime, duration);
            }
            
            @Override
            public StrokeDescription[] newArray(int size) {
                return new StrokeDescription[size];
            }
        };
    }
    
    @Override
    public int describeContents() {
        return 0;
    }
    
    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeTypedArray(getStrokes(), flags);
        dest.writeLong(getDuration());
    }
    
    public static final Parcelable.Creator<GestureDescription> CREATOR = new Parcelable.Creator<GestureDescription>() {
        @Override
        public GestureDescription createFromParcel(Parcel source) {
            StrokeDescription[] strokes = source.createTypedArray(StrokeDescription.CREATOR);
            Builder builder = new Builder();
            
            for (StrokeDescription stroke : strokes) {
                builder.addStroke(stroke);
            }
            
            return builder.build();
        }
        
        @Override
        public GestureDescription[] newArray(int size) {
            return new GestureDescription[size];
        }
    };
}