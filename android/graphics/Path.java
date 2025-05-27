package android.graphics;

/**
 * Mock implementation of Android Path class for development outside of Android.
 * The Path class encapsulates compound (multiple contour) geometric paths.
 */
public class Path {
    /**
     * Direction for how to interpret the path.
     */
    public enum Direction {
        /**
         * Path direction going clockwise (matches winding direction of type 1 fonts)
         */
        CW,
        /**
         * Path direction going counter-clockwise
         */
        CCW
    }
    
    /**
     * Specifies the way to fill a path when it intersects with itself.
     */
    public enum FillType {
        /**
         * Specifies that "inside" is computed by a non-zero sum of signed edge crossings
         */
        WINDING,
        /**
         * Specifies that "inside" is computed by an odd number of edge crossings
         */
        EVEN_ODD,
        /**
         * Same as WINDING, but draws outside of the path, rather than inside
         */
        INVERSE_WINDING,
        /**
         * Same as EVEN_ODD, but draws outside of the path, rather than inside
         */
        INVERSE_EVEN_ODD
    }
    
    /**
     * Constructs an empty path.
     */
    public Path() {
        // Empty constructor
    }
    
    /**
     * Constructs a path by copying another path.
     * 
     * @param src The path to copy
     */
    public Path(Path src) {
        set(src);
    }
    
    /**
     * Returns true if the path is empty (contains no lines or curves).
     * 
     * @return true if the path is empty
     */
    public boolean isEmpty() {
        return true; // Mock implementation
    }
    
    /**
     * Sets this path to a copy of the specified path.
     * 
     * @param src The path to copy
     */
    public void set(Path src) {
        // Mock implementation
    }
    
    /**
     * Resets the path to empty.
     */
    public void reset() {
        // Mock implementation
    }
    
    /**
     * Sets the fill type of the path.
     * 
     * @param ft The new fill type
     */
    public void setFillType(FillType ft) {
        // Mock implementation
    }
    
    /**
     * Returns the path's fill type.
     * 
     * @return The path's fill type
     */
    public FillType getFillType() {
        return FillType.WINDING; // Mock implementation
    }
    
    /**
     * Returns true if the filltype is one of the INVERSE variants.
     * 
     * @return true if the path's fill type is inverted
     */
    public boolean isInverseFillType() {
        FillType ft = getFillType();
        return ft == FillType.INVERSE_WINDING || ft == FillType.INVERSE_EVEN_ODD;
    }
    
    /**
     * Toggles the INVERSE state of the fill type.
     */
    public void toggleInverseFillType() {
        // Mock implementation
    }
    
    /**
     * Sets the beginning of the next contour to the point (x,y).
     * 
     * @param x The x-coordinate of the start of a new contour
     * @param y The y-coordinate of the start of a new contour
     */
    public void moveTo(float x, float y) {
        // Mock implementation
    }
    
    /**
     * Adds a line from the last point to the specified point (x,y).
     * If no moveTo() call has been made for this contour, the first point is
     * automatically set to (0,0).
     * 
     * @param x The x-coordinate of the end of a line
     * @param y The y-coordinate of the end of a line
     */
    public void lineTo(float x, float y) {
        // Mock implementation
    }
    
    /**
     * Adds a quadratic bezier from the last point, approaching control point
     * (x1,y1), and ending at (x2,y2). If no moveTo() call has been made for
     * this contour, the first point is automatically set to (0,0).
     * 
     * @param x1 The x-coordinate of the control point on a quadratic curve
     * @param y1 The y-coordinate of the control point on a quadratic curve
     * @param x2 The x-coordinate of the end point on a quadratic curve
     * @param y2 The y-coordinate of the end point on a quadratic curve
     */
    public void quadTo(float x1, float y1, float x2, float y2) {
        // Mock implementation
    }
    
    /**
     * Adds a cubic bezier from the last point, approaching control points
     * (x1,y1) and (x2,y2), and ending at (x3,y3). If no moveTo() call has been
     * made for this contour, the first point is automatically set to (0,0).
     * 
     * @param x1 The x-coordinate of the 1st control point on a cubic curve
     * @param y1 The y-coordinate of the 1st control point on a cubic curve
     * @param x2 The x-coordinate of the 2nd control point on a cubic curve
     * @param y2 The y-coordinate of the 2nd control point on a cubic curve
     * @param x3 The x-coordinate of the end point on a cubic curve
     * @param y3 The y-coordinate of the end point on a cubic curve
     */
    public void cubicTo(float x1, float y1, float x2, float y2, float x3, float y3) {
        // Mock implementation
    }
    
    /**
     * Close the current contour. If the current point is not equal to the first
     * point of the contour, a line segment is automatically added.
     */
    public void close() {
        // Mock implementation
    }
    
    /**
     * Adds a closed rectangle contour to the path.
     * 
     * @param left The left side of a rectangle to add to the path
     * @param top The top of a rectangle to add to the path
     * @param right The right side of a rectangle to add to the path
     * @param bottom The bottom of a rectangle to add to the path
     * @param dir The direction to wind the rectangle's contour
     */
    public void addRect(float left, float top, float right, float bottom, Direction dir) {
        // Mock implementation
    }
    
    /**
     * Adds a closed rectangle contour to the path.
     * 
     * @param rect The rectangle to add as a closed contour to the path
     * @param dir The direction to wind the rectangle's contour
     */
    public void addRect(RectF rect, Direction dir) {
        addRect(rect.left, rect.top, rect.right, rect.bottom, dir);
    }
    
    /**
     * Adds a closed oval contour to the path.
     * 
     * @param oval The bounds of the oval to add as a closed contour to the path
     * @param dir The direction to wind the oval's contour
     */
    public void addOval(RectF oval, Direction dir) {
        // Mock implementation
    }
    
    /**
     * Adds a closed circle contour to the path.
     * 
     * @param x The x-coordinate of the center of a circle to add to the path
     * @param y The y-coordinate of the center of a circle to add to the path
     * @param radius The radius of a circle to add to the path
     * @param dir The direction to wind the circle's contour
     */
    public void addCircle(float x, float y, float radius, Direction dir) {
        // Mock implementation
    }
    
    /**
     * Adds a closed round-rectangle contour to the path.
     * 
     * @param rect The bounds of a round-rectangle to add to the path
     * @param rx The x-radius of the rounded corners on the round-rectangle
     * @param ry The y-radius of the rounded corners on the round-rectangle
     * @param dir The direction to wind the round-rectangle's contour
     */
    public void addRoundRect(RectF rect, float rx, float ry, Direction dir) {
        // Mock implementation
    }
    
    /**
     * Adds a copy of the src path to this path.
     * 
     * @param src The path to add
     * @param matrix Matrix to apply to src, or null
     */
    public void addPath(Path src, Matrix matrix) {
        // Mock implementation
    }
    
    /**
     * Adds a copy of the src path to this path.
     * 
     * @param src The path to add
     */
    public void addPath(Path src) {
        addPath(src, null);
    }
    
    /**
     * Offset the path by (dx,dy).
     * 
     * @param dx The amount in the X direction to offset the entire path
     * @param dy The amount in the Y direction to offset the entire path
     */
    public void offset(float dx, float dy) {
        // Mock implementation
    }
    
    /**
     * Transforms the path by the matrix.
     * 
     * @param matrix The matrix to apply to the path
     */
    public void transform(Matrix matrix) {
        // Mock implementation
    }
}