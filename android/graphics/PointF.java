package android.graphics;

import android.os.Parcel;
import android.os.Parcelable;

/**
 * Mock implementation of Android PointF class for development outside of Android.
 * PointF holds two float coordinates.
 */
public class PointF implements Parcelable {
    /**
     * The X coordinate.
     */
    public float x;
    
    /**
     * The Y coordinate.
     */
    public float y;
    
    /**
     * Creates a new point with the coordinates (0,0).
     */
    public PointF() {
        this.x = 0.0f;
        this.y = 0.0f;
    }
    
    /**
     * Creates a new point with the given coordinates.
     */
    public PointF(float x, float y) {
        this.x = x;
        this.y = y;
    }
    
    /**
     * Creates a new point with the same coordinates as the given point.
     */
    public PointF(PointF src) {
        this.x = src.x;
        this.y = src.y;
    }
    
    /**
     * Creates a new point with the coordinates of the given Point.
     */
    public PointF(Point src) {
        this.x = src.x;
        this.y = src.y;
    }
    
    /**
     * Sets the coordinates of this point to the coordinates of the given point.
     */
    public void set(float x, float y) {
        this.x = x;
        this.y = y;
    }
    
    /**
     * Sets the coordinates of this point to the coordinates of the given point.
     */
    public void set(PointF src) {
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
    public void offset(float dx, float dy) {
        x += dx;
        y += dy;
    }
    
    /**
     * Returns the length of the vector from the origin to this point.
     */
    public float length() {
        return (float) Math.sqrt(x * x + y * y);
    }
    
    /**
     * Returns true if the coordinates of this point equal the coordinates of the given point.
     */
    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null) return false;
        if (getClass() != obj.getClass()) return false;
        
        PointF other = (PointF) obj;
        return Float.compare(x, other.x) == 0 && Float.compare(y, other.y) == 0;
    }
    
    /**
     * Returns a hash code for this point.
     */
    @Override
    public int hashCode() {
        int result = Float.floatToIntBits(x);
        result = 31 * result + Float.floatToIntBits(y);
        return result;
    }
    
    /**
     * Returns a string representation of this point.
     */
    @Override
    public String toString() {
        return "PointF(" + x + ", " + y + ")";
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
        out.writeFloat(x);
        out.writeFloat(y);
    }
    
    /**
     * Creator for PointF objects.
     */
    public static final Parcelable.Creator<PointF> CREATOR = new Parcelable.Creator<PointF>() {
        @Override
        public PointF createFromParcel(Parcel in) {
            PointF r = new PointF();
            r.readFromParcel(in);
            return r;
        }
        
        @Override
        public PointF[] newArray(int size) {
            return new PointF[size];
        }
    };
    
    /**
     * Read this point from the given parcel.
     */
    public void readFromParcel(Parcel in) {
        x = in.readFloat();
        y = in.readFloat();
    }
}