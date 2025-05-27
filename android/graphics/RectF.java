package android.graphics;

import android.os.Parcel;
import android.os.Parcelable;

/**
 * Mock implementation of Android RectF class for development outside of Android.
 * RectF holds four float coordinates for a rectangle.
 */
public class RectF implements Parcelable {
    /**
     * The X coordinate of the left side of the rectangle.
     */
    public float left;
    
    /**
     * The Y coordinate of the top of the rectangle.
     */
    public float top;
    
    /**
     * The X coordinate of the right side of the rectangle.
     */
    public float right;
    
    /**
     * The Y coordinate of the bottom of the rectangle.
     */
    public float bottom;
    
    /**
     * Create a new empty RectF.
     */
    public RectF() {
        left = right = top = bottom = 0;
    }
    
    /**
     * Create a new rectangle with the specified coordinates.
     */
    public RectF(float left, float top, float right, float bottom) {
        this.left = left;
        this.top = top;
        this.right = right;
        this.bottom = bottom;
    }
    
    /**
     * Create a new rectangle, initialized with the values in the specified
     * rectangle.
     */
    public RectF(RectF r) {
        if (r == null) {
            left = top = right = bottom = 0;
        } else {
            left = r.left;
            top = r.top;
            right = r.right;
            bottom = r.bottom;
        }
    }
    
    /**
     * Create a new rectangle, initialized with the values in the specified
     * rectangle.
     */
    public RectF(Rect r) {
        if (r == null) {
            left = top = right = bottom = 0;
        } else {
            left = r.left;
            top = r.top;
            right = r.right;
            bottom = r.bottom;
        }
    }
    
    /**
     * Returns a copy of the rectangle.
     */
    public RectF copy() {
        return new RectF(this);
    }
    
    /**
     * Set the rectangle to the specified coordinates.
     */
    public void set(float left, float top, float right, float bottom) {
        this.left = left;
        this.top = top;
        this.right = right;
        this.bottom = bottom;
    }
    
    /**
     * Copy the coordinates from src into this rectangle.
     */
    public void set(RectF src) {
        if (src == null) {
            left = top = right = bottom = 0;
        } else {
            left = src.left;
            top = src.top;
            right = src.right;
            bottom = src.bottom;
        }
    }
    
    /**
     * Copy the coordinates from src into this rectangle.
     */
    public void set(Rect src) {
        if (src == null) {
            left = top = right = bottom = 0;
        } else {
            left = src.left;
            top = src.top;
            right = src.right;
            bottom = src.bottom;
        }
    }
    
    /**
     * Set the rectangle to (0,0,0,0).
     */
    public void setEmpty() {
        left = top = right = bottom = 0;
    }
    
    /**
     * Returns true if the rectangle is empty (left >= right or top >= bottom)
     */
    public boolean isEmpty() {
        return left >= right || top >= bottom;
    }
    
    /**
     * Returns the rectangle's width. This does not check for a valid rectangle
     * (i.e. left <= right) so the result may be negative.
     */
    public float width() {
        return right - left;
    }
    
    /**
     * Returns the rectangle's height. This does not check for a valid rectangle
     * (i.e. top <= bottom) so the result may be negative.
     */
    public float height() {
        return bottom - top;
    }
    
    /**
     * Returns the horizontal center of the rectangle.
     */
    public float centerX() {
        return (left + right) * 0.5f;
    }
    
    /**
     * Returns the vertical center of the rectangle.
     */
    public float centerY() {
        return (top + bottom) * 0.5f;
    }
    
    /**
     * Returns true if (x,y) is inside the rectangle.
     */
    public boolean contains(float x, float y) {
        return left < right && top < bottom && x >= left && x < right && y >= top && y < bottom;
    }
    
    /**
     * Returns true if the rectangle contains the specified point.
     */
    public boolean contains(PointF p) {
        return contains(p.x, p.y);
    }
    
    /**
     * Returns true if the rectangle contains the specified rectangle.
     */
    public boolean contains(RectF r) {
        if (r == null) return false;
        return left <= r.left && top <= r.top && right >= r.right && bottom >= r.bottom;
    }
    
    /**
     * Returns true if the specified rectangle intersects this rectangle.
     */
    public boolean intersects(float left, float top, float right, float bottom) {
        return this.left < right && left < this.right && this.top < bottom && top < this.bottom;
    }
    
    /**
     * Returns true if the specified rectangle intersects this rectangle.
     */
    public boolean intersects(RectF r) {
        return r != null && intersects(r.left, r.top, r.right, r.bottom);
    }
    
    /**
     * Returns the result of intersecting this rectangle with the specified
     * rectangle. If they do not intersect, an empty rectangle is returned.
     */
    public boolean intersect(float left, float top, float right, float bottom) {
        if (this.left < right && left < this.right && this.top < bottom && top < this.bottom) {
            if (this.left < left) this.left = left;
            if (this.top < top) this.top = top;
            if (this.right > right) this.right = right;
            if (this.bottom > bottom) this.bottom = bottom;
            return true;
        }
        return false;
    }
    
    /**
     * Returns the result of intersecting this rectangle with the specified
     * rectangle. If they do not intersect, an empty rectangle is returned.
     */
    public boolean intersect(RectF r) {
        return r != null && intersect(r.left, r.top, r.right, r.bottom);
    }
    
    /**
     * Update this rectangle to enclose itself and the specified rectangle.
     */
    public void union(float left, float top, float right, float bottom) {
        if ((left < right) && (top < bottom)) {
            if ((this.left < this.right) && (this.top < this.bottom)) {
                if (this.left > left) this.left = left;
                if (this.top > top) this.top = top;
                if (this.right < right) this.right = right;
                if (this.bottom < bottom) this.bottom = bottom;
            } else {
                this.left = left;
                this.top = top;
                this.right = right;
                this.bottom = bottom;
            }
        }
    }
    
    /**
     * Update this rectangle to enclose itself and the specified rectangle.
     */
    public void union(RectF r) {
        union(r.left, r.top, r.right, r.bottom);
    }
    
    /**
     * Update this rectangle to enclose itself and the [x,y] coordinate.
     */
    public void union(float x, float y) {
        if (x < left) {
            left = x;
        } else if (x > right) {
            right = x;
        }
        if (y < top) {
            top = y;
        } else if (y > bottom) {
            bottom = y;
        }
    }
    
    /**
     * Swap top/bottom or left/right if there are flipped.
     */
    public void sort() {
        if (left > right) {
            float temp = left;
            left = right;
            right = temp;
        }
        if (top > bottom) {
            float temp = top;
            top = bottom;
            bottom = temp;
        }
    }
    
    /**
     * Scales the rectangle by the specified scale.
     */
    public void scale(float scale) {
        if (scale != 1.0f) {
            left *= scale;
            top *= scale;
            right *= scale;
            bottom *= scale;
        }
    }
    
    /**
     * Rounds the rectangle to integer coordinates.
     */
    public void round(Rect dst) {
        dst.left = Math.round(left);
        dst.top = Math.round(top);
        dst.right = Math.round(right);
        dst.bottom = Math.round(bottom);
    }
    
    /**
     * Offsets the rectangle by adding dx to its left and right coordinates, and
     * adding dy to its top and bottom coordinates.
     */
    public void offset(float dx, float dy) {
        left += dx;
        top += dy;
        right += dx;
        bottom += dy;
    }
    
    /**
     * Insets the rectangle by (dx,dy). If dx is positive, then the sides are
     * moved inwards, making the rectangle narrower. If dx is negative, then the
     * sides are moved outwards, making the rectangle wider. The same holds true
     * for dy and the top and bottom.
     */
    public void inset(float dx, float dy) {
        left += dx;
        top += dy;
        right -= dx;
        bottom -= dy;
    }
    
    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null) return false;
        if (getClass() != obj.getClass()) return false;
        
        RectF r = (RectF) obj;
        return Float.compare(left, r.left) == 0 && Float.compare(top, r.top) == 0 &&
                Float.compare(right, r.right) == 0 && Float.compare(bottom, r.bottom) == 0;
    }
    
    @Override
    public int hashCode() {
        int result = Float.floatToIntBits(left);
        result = 31 * result + Float.floatToIntBits(top);
        result = 31 * result + Float.floatToIntBits(right);
        result = 31 * result + Float.floatToIntBits(bottom);
        return result;
    }
    
    @Override
    public String toString() {
        return "RectF(" + left + ", " + top + ", " + right + ", " + bottom + ")";
    }
    
    @Override
    public int describeContents() {
        return 0;
    }
    
    @Override
    public void writeToParcel(Parcel out, int flags) {
        out.writeFloat(left);
        out.writeFloat(top);
        out.writeFloat(right);
        out.writeFloat(bottom);
    }
    
    public static final Parcelable.Creator<RectF> CREATOR = new Parcelable.Creator<RectF>() {
        @Override
        public RectF createFromParcel(Parcel in) {
            RectF r = new RectF();
            r.readFromParcel(in);
            return r;
        }
        
        @Override
        public RectF[] newArray(int size) {
            return new RectF[size];
        }
    };
    
    public void readFromParcel(Parcel in) {
        left = in.readFloat();
        top = in.readFloat();
        right = in.readFloat();
        bottom = in.readFloat();
    }
}