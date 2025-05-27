package android.graphics;

import android.os.Parcel;
import android.os.Parcelable;

/**
 * Mock implementation of Android Point class for development outside of Android.
 * Point holds two integer coordinates.
 */
public class Point implements Parcelable {
    /**
     * The X coordinate.
     */
    public int x;
    
    /**
     * The Y coordinate.
     */
    public int y;
    
    /**
     * Creates a new point with the coordinates (0,0).
     */
    public Point() {
        this.x = 0;
        this.y = 0;
    }
    
    /**
     * Creates a new point with the given coordinates.
     */
    public Point(int x, int y) {
        this.x = x;
        this.y = y;
    }
    
    /**
     * Creates a new point with the same coordinates as the given point.
     */
    public Point(Point src) {
        this.x = src.x;
        this.y = src.y;
    }
    
    /**
     * Sets the coordinates of this point to the coordinates of the given point.
     */
    public void set(int x, int y) {
        this.x = x;
        this.y = y;
    }
    
    /**
     * Sets the coordinates of this point to the coordinates of the given point.
     */
    public void set(Point src) {
        this.x = src.x;
        this.y = src.y;
    }
    
    /**
     * Negate the coordinates of this point.
     */
    public void negate() {
        x = -x;
        y = -y;
    }
    
    /**
     * Offsets the coordinates of this point by adding the given values.
     */
    public void offset(int dx, int dy) {
        x += dx;
        y += dy;
    }
    
    /**
     * Returns true if the coordinates of this point equal the coordinates of the given point.
     */
    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null) return false;
        if (getClass() != obj.getClass()) return false;
        
        Point other = (Point) obj;
        return x == other.x && y == other.y;
    }
    
    /**
     * Returns a hash code for this point.
     */
    @Override
    public int hashCode() {
        return x * 31 + y;
    }
    
    /**
     * Returns a string representation of this point.
     */
    @Override
    public String toString() {
        return "Point(" + x + ", " + y + ")";
    }
    
    /**
     * Describe the kinds of special objects contained in this Parcelable's
     * marshalled representation.
     */
    @Override
    public int describeContents() {
        return 0;
    }
    
    /**
     * Write this point to the given parcel.
     */
    @Override
    public void writeToParcel(Parcel out, int flags) {
        out.writeInt(x);
        out.writeInt(y);
    }
    
    /**
     * Creator for Point objects.
     */
    public static final Parcelable.Creator<Point> CREATOR = new Parcelable.Creator<Point>() {
        @Override
        public Point createFromParcel(Parcel in) {
            Point r = new Point();
            r.readFromParcel(in);
            return r;
        }
        
        @Override
        public Point[] newArray(int size) {
            return new Point[size];
        }
    };
    
    /**
     * Read this point from the given parcel.
     */
    public void readFromParcel(Parcel in) {
        x = in.readInt();
        y = in.readInt();
    }
}