package android.graphics;

import android.os.Parcel;
import android.os.Parcelable;

/**
 * Mock implementation of Android Rect class for development outside of Android.
 * Rect holds four integer coordinates for a rectangle.
 */
public class Rect implements Parcelable {
    /**
     * The X coordinate of the left side of the rectangle.
     */
    public int left;
    
    /**
     * The Y coordinate of the top of the rectangle.
     */
    public int top;
    
    /**
     * The X coordinate of the right side of the rectangle.
     */
    public int right;
    
    /**
     * The Y coordinate of the bottom of the rectangle.
     */
    public int bottom;
    
    /**
     * Create a new empty Rect.
     */
    public Rect() {
        left = right = top = bottom = 0;
    }
    
    /**
     * Create a new rectangle with the specified coordinates.
     */
    public Rect(int left, int top, int right, int bottom) {
        this.left = left;
        this.top = top;
        this.right = right;
        this.bottom = bottom;
    }
    
    /**
     * Create a new rectangle, initialized with the values in the specified
     * rectangle.
     */
    public Rect(Rect r) {
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
    public Rect copy() {
        return new Rect(this);
    }
    
    /**
     * Set the rectangle to the specified coordinates.
     */
    public void set(int left, int top, int right, int bottom) {
        this.left = left;
        this.top = top;
        this.right = right;
        this.bottom = bottom;
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
    public int width() {
        return right - left;
    }
    
    /**
     * Returns the rectangle's height. This does not check for a valid rectangle
     * (i.e. top <= bottom) so the result may be negative.
     */
    public int height() {
        return bottom - top;
    }
    
    /**
     * Returns the horizontal center of the rectangle.
     */
    public int centerX() {
        return (left + right) >> 1;
    }
    
    /**
     * Returns the vertical center of the rectangle.
     */
    public int centerY() {
        return (top + bottom) >> 1;
    }
    
    /**
     * Returns true if (x,y) is inside the rectangle.
     */
    public boolean contains(int x, int y) {
        return left < right && top < bottom && x >= left && x < right && y >= top && y < bottom;
    }
    
    /**
     * Returns true if the rectangle contains the specified point.
     */
    public boolean contains(Point p) {
        return contains(p.x, p.y);
    }
    
    /**
     * Returns true if the rectangle contains the specified rectangle.
     */
    public boolean contains(Rect r) {
        if (r == null) {
            return false;
        }
        return left <= r.left && top <= r.top && right >= r.right && bottom >= r.bottom;
    }
    
    /**
     * Returns true if the specified rectangle intersects this rectangle.
     */
    public boolean intersects(int left, int top, int right, int bottom) {
        return this.left < right && left < this.right && this.top < bottom && top < this.bottom;
    }
    
    /**
     * Returns true if the specified rectangle intersects this rectangle.
     */
    public boolean intersects(Rect r) {
        return r != null && intersects(r.left, r.top, r.right, r.bottom);
    }
    
    /**
     * Returns the result of intersecting this rectangle with the specified
     * rectangle. If they do not intersect, an empty rectangle is returned.
     */
    public boolean intersect(int left, int top, int right, int bottom) {
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
    public boolean intersect(Rect r) {
        return r != null && intersect(r.left, r.top, r.right, r.bottom);
    }
    
    /**
     * Update this Rect to enclose itself and the specified rectangle.
     */
    public void union(int left, int top, int right, int bottom) {
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
     * Update this Rect to enclose itself and the specified rectangle.
     */
    public void union(Rect r) {
        union(r.left, r.top, r.right, r.bottom);
    }
    
    /**
     * Update this Rect to enclose itself and the [x,y] coordinate.
     */
    public void union(int x, int y) {
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
            int temp = left;
            left = right;
            right = temp;
        }
        if (top > bottom) {
            int temp = top;
            top = bottom;
            bottom = temp;
        }
    }
    
    /**
     * Scales the rectangle by the specified scale.
     */
    public void scale(float scale) {
        if (scale != 1.0f) {
            left = (int) (left * scale + 0.5f);
            top = (int) (top * scale + 0.5f);
            right = (int) (right * scale + 0.5f);
            bottom = (int) (bottom * scale + 0.5f);
        }
    }
    
    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null) return false;
        if (getClass() != obj.getClass()) return false;
        
        Rect r = (Rect) obj;
        return left == r.left && top == r.top && right == r.right && bottom == r.bottom;
    }
    
    @Override
    public int hashCode() {
        int result = left;
        result = 31 * result + top;
        result = 31 * result + right;
        result = 31 * result + bottom;
        return result;
    }
    
    @Override
    public String toString() {
        return "Rect(" + left + ", " + top + ", " + right + ", " + bottom + ")";
    }
    
    @Override
    public int describeContents() {
        return 0;
    }
    
    @Override
    public void writeToParcel(Parcel out, int flags) {
        out.writeInt(left);
        out.writeInt(top);
        out.writeInt(right);
        out.writeInt(bottom);
    }
    
    public static final Parcelable.Creator<Rect> CREATOR = new Parcelable.Creator<Rect>() {
        @Override
        public Rect createFromParcel(Parcel in) {
            Rect r = new Rect();
            r.readFromParcel(in);
            return r;
        }
        
        @Override
        public Rect[] newArray(int size) {
            return new Rect[size];
        }
    };
    
    public void readFromParcel(Parcel in) {
        left = in.readInt();
        top = in.readInt();
        right = in.readInt();
        bottom = in.readInt();
    }
}