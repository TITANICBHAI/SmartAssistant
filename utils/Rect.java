package utils;

/**
 * A rectangle representation in the utils package to match functionality with android.graphics.Rect
 */
public class Rect {
    public int left;
    public int top;
    public int right;
    public int bottom;
    
    /**
     * Create an empty rectangle
     */
    public Rect() {
        this.left = 0;
        this.top = 0;
        this.right = 0;
        this.bottom = 0;
    }
    
    /**
     * Create a rectangle with the specified coordinates
     * 
     * @param left The left coordinate
     * @param top The top coordinate
     * @param right The right coordinate
     * @param bottom The bottom coordinate
     */
    public Rect(int left, int top, int right, int bottom) {
        this.left = left;
        this.top = top;
        this.right = right;
        this.bottom = bottom;
    }
    
    /**
     * Copy constructor
     * 
     * @param r The rectangle to copy
     */
    public Rect(Rect r) {
        if (r == null) {
            this.left = 0;
            this.top = 0;
            this.right = 0;
            this.bottom = 0;
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
    public void set(int left, int top, int right, int bottom) {
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
    public int width() {
        return right - left;
    }
    
    /**
     * Get the height of the rectangle
     * 
     * @return The height of the rectangle
     */
    public int height() {
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
    public void offset(int dx, int dy) {
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
    public boolean contains(int x, int y) {
        return left < right && top < bottom &&
               x >= left && x < right && y >= top && y < bottom;
    }
    
    /**
     * Check if the rectangle contains the specified rectangle
     * 
     * @param r The rectangle to check
     * @return True if the rectangle contains the specified rectangle, false otherwise
     */
    public boolean contains(Rect r) {
        if (r == null) {
            return false;
        }
        
        return left <= r.left && top <= r.top &&
               right >= r.right && bottom >= r.bottom;
    }
    
    /**
     * Check if the rectangle intersects with the specified rectangle
     * 
     * @param r The rectangle to check
     * @return True if the rectangles intersect, false otherwise
     */
    public boolean intersects(Rect r) {
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
    public boolean intersect(Rect r) {
        if (r == null) {
            return false;
        }
        
        int newLeft = Math.max(left, r.left);
        int newTop = Math.max(top, r.top);
        int newRight = Math.min(right, r.right);
        int newBottom = Math.min(bottom, r.bottom);
        
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
    public void union(Rect r) {
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
        
        Rect r = (Rect) o;
        return left == r.left && top == r.top &&
               right == r.right && bottom == r.bottom;
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
}