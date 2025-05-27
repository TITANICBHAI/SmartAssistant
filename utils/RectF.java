package utils;

/**
 * A rectangle with float coordinates
 */
public class RectF {
    public float left;
    public float top;
    public float right;
    public float bottom;
    
    /**
     * Create an empty rectangle
     */
    public RectF() {
        this.left = 0.0f;
        this.top = 0.0f;
        this.right = 0.0f;
        this.bottom = 0.0f;
    }
    
    /**
     * Create a rectangle with the specified coordinates
     * 
     * @param left The left coordinate
     * @param top The top coordinate
     * @param right The right coordinate
     * @param bottom The bottom coordinate
     */
    public RectF(float left, float top, float right, float bottom) {
        this.left = left;
        this.top = top;
        this.right = right;
        this.bottom = bottom;
    }
    
    /**
     * Create a rectangle from a Rect
     * 
     * @param r The rectangle to copy
     */
    public RectF(Rect r) {
        if (r == null) {
            this.left = 0.0f;
            this.top = 0.0f;
            this.right = 0.0f;
            this.bottom = 0.0f;
        } else {
            this.left = r.left;
            this.top = r.top;
            this.right = r.right;
            this.bottom = r.bottom;
        }
    }
    
    /**
     * Copy constructor
     * 
     * @param r The rectangle to copy
     */
    public RectF(RectF r) {
        if (r == null) {
            this.left = 0.0f;
            this.top = 0.0f;
            this.right = 0.0f;
            this.bottom = 0.0f;
        } else {
            this.left = r.left;
            this.top = r.top;
            this.right = r.right;
            this.bottom = r.bottom;
        }
    }
    
    /**
     * Set the rectangle's coordinates to the specified values
     * 
     * @param left The left coordinate
     * @param top The top coordinate
     * @param right The right coordinate
     * @param bottom The bottom coordinate
     */
    public void set(float left, float top, float right, float bottom) {
        this.left = left;
        this.top = top;
        this.right = right;
        this.bottom = bottom;
    }
    
    /**
     * Copy the coordinates from the source rectangle
     * 
     * @param src The source rectangle
     */
    public void set(RectF src) {
        if (src != null) {
            this.left = src.left;
            this.top = src.top;
            this.right = src.right;
            this.bottom = src.bottom;
        }
    }
    
    /**
     * Copy the coordinates from the source rectangle
     * 
     * @param src The source rectangle
     */
    public void set(Rect src) {
        if (src != null) {
            this.left = src.left;
            this.top = src.top;
            this.right = src.right;
            this.bottom = src.bottom;
        }
    }
    
    /**
     * Get the width of the rectangle
     * 
     * @return The width of the rectangle
     */
    public float width() {
        return right - left;
    }
    
    /**
     * Get the height of the rectangle
     * 
     * @return The height of the rectangle
     */
    public float height() {
        return bottom - top;
    }
    
    /**
     * Check if the rectangle is empty
     * 
     * @return True if the rectangle is empty, false otherwise
     */
    public boolean isEmpty() {
        return left >= right || top >= bottom;
    }
    
    /**
     * Set the rectangle to empty
     */
    public void setEmpty() {
        left = right = top = bottom = 0;
    }
    
    /**
     * Offset the rectangle by the specified amount
     * 
     * @param dx The x offset
     * @param dy The y offset
     */
    public void offset(float dx, float dy) {
        left += dx;
        top += dy;
        right += dx;
        bottom += dy;
    }
    
    /**
     * Check if the rectangle contains the specified point
     * 
     * @param x The x coordinate
     * @param y The y coordinate
     * @return True if the rectangle contains the point, false otherwise
     */
    public boolean contains(float x, float y) {
        return left < right && top < bottom &&
               x >= left && x < right && y >= top && y < bottom;
    }
    
    /**
     * Check if the rectangle contains the specified rectangle
     * 
     * @param r The rectangle to check
     * @return True if the rectangle contains the specified rectangle, false otherwise
     */
    public boolean contains(RectF r) {
        if (r == null) {
            return false;
        }
        
        return left <= r.left && top <= r.top &&
               right >= r.right && bottom >= r.bottom;
    }
    
    /**
     * Convert to Rect
     * 
     * @return A new Rect with rounded coordinates
     */
    public Rect toRect() {
        return new Rect(
            Math.round(left),
            Math.round(top),
            Math.round(right),
            Math.round(bottom)
        );
    }
    
    /**
     * Check if the rectangle intersects with the specified rectangle
     * 
     * @param r The rectangle to check
     * @return True if the rectangles intersect, false otherwise
     */
    public boolean intersects(RectF r) {
        if (r == null) {
            return false;
        }
        
        return left < r.right && r.left < right && 
               top < r.bottom && r.top < bottom;
    }
    
    /**
     * Compute the intersection of this rectangle with the specified rectangle
     * 
     * @param r The rectangle to intersect with
     * @return True if the rectangles intersect, false otherwise
     */
    public boolean intersect(RectF r) {
        if (r == null) {
            return false;
        }
        
        float newLeft = Math.max(left, r.left);
        float newTop = Math.max(top, r.top);
        float newRight = Math.min(right, r.right);
        float newBottom = Math.min(bottom, r.bottom);
        
        if (newLeft < newRight && newTop < newBottom) {
            left = newLeft;
            top = newTop;
            right = newRight;
            bottom = newBottom;
            return true;
        }
        
        return false;
    }
    
    /**
     * Compute the union of this rectangle with the specified rectangle
     * 
     * @param r The rectangle to union with
     */
    public void union(RectF r) {
        if (r == null) {
            return;
        }
        
        if (isEmpty()) {
            set(r);
            return;
        }
        
        if (r.isEmpty()) {
            return;
        }
        
        left = Math.min(left, r.left);
        top = Math.min(top, r.top);
        right = Math.max(right, r.right);
        bottom = Math.max(bottom, r.bottom);
    }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        
        RectF r = (RectF) o;
        return Float.compare(r.left, left) == 0 && Float.compare(r.top, top) == 0 &&
               Float.compare(r.right, right) == 0 && Float.compare(r.bottom, bottom) == 0;
    }
    
    @Override
    public int hashCode() {
        int result = (left != +0.0f ? Float.floatToIntBits(left) : 0);
        result = 31 * result + (top != +0.0f ? Float.floatToIntBits(top) : 0);
        result = 31 * result + (right != +0.0f ? Float.floatToIntBits(right) : 0);
        result = 31 * result + (bottom != +0.0f ? Float.floatToIntBits(bottom) : 0);
        return result;
    }
    
    @Override
    public String toString() {
        return "RectF(" + left + ", " + top + ", " + right + ", " + bottom + ")";
    }
}