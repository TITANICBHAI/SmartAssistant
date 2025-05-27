package android.graphics;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

/**
 * Mock implementation of Android Canvas class for development outside of Android.
 * The Canvas class holds the draw calls. To draw something, you need 4 basic components:
 * 1. A Bitmap to hold the pixels
 * 2. A Canvas to host the draw calls
 * 3. A drawing primitive (e.g. Rect, Path, etc.)
 * 4. A Paint (to describe the colors and styles for the drawing)
 */
public class Canvas {
    private Bitmap bitmap;
    
    /**
     * Construct an empty canvas, not associated with any bitmap
     */
    public Canvas() {
    }
    
    /**
     * Construct a canvas that draws into the specified bitmap
     * 
     * @param bitmap The bitmap to draw into
     */
    public Canvas(@Nullable Bitmap bitmap) {
        this.bitmap = bitmap;
    }
    
    /**
     * Specify a bitmap for the canvas to draw into
     * 
     * @param bitmap The bitmap to draw into
     */
    public void setBitmap(@Nullable Bitmap bitmap) {
        this.bitmap = bitmap;
    }
    
    /**
     * Return the current bitmap
     * 
     * @return The current bitmap, or null if none
     */
    @Nullable
    public Bitmap getBitmap() {
        return bitmap;
    }
    
    /**
     * Save the current matrix and clip onto a private stack
     * 
     * @return The value to pass to restoreToCount to balance this save
     */
    public int save() {
        return 1;
    }
    
    /**
     * This call balances a previous call to save(), and is used to remove
     * all modifications to the matrix/clip state since the last save call
     */
    public void restore() {
    }
    
    /**
     * Restore the matrix/clip state to the specified count
     * 
     * @param saveCount The save count to restore to
     */
    public void restoreToCount(int saveCount) {
    }
    
    /**
     * Preconcat the current matrix with the specified matrix
     * 
     * @param matrix The matrix to preconcat with the current matrix
     */
    public void concat(@Nullable Matrix matrix) {
    }
    
    /**
     * Rotate the current matrix the specified number of degrees
     * 
     * @param degrees The amount to rotate, in degrees
     */
    public void rotate(float degrees) {
    }
    
    /**
     * Rotate the current matrix the specified number of degrees
     * 
     * @param degrees The amount to rotate, in degrees
     * @param px The x-coordinate of the pivot point
     * @param py The y-coordinate of the pivot point
     */
    public void rotate(float degrees, float px, float py) {
    }
    
    /**
     * Scale the current matrix by the specified amounts
     * 
     * @param sx The amount to scale in X
     * @param sy The amount to scale in Y
     */
    public void scale(float sx, float sy) {
    }
    
    /**
     * Scale the current matrix by the specified amounts
     * 
     * @param sx The amount to scale in X
     * @param sy The amount to scale in Y
     * @param px The x-coordinate of the pivot point
     * @param py The y-coordinate of the pivot point
     */
    public void scale(float sx, float sy, float px, float py) {
    }
    
    /**
     * Skew the current matrix by the specified amounts
     * 
     * @param sx The amount to skew in X
     * @param sy The amount to skew in Y
     */
    public void skew(float sx, float sy) {
    }
    
    /**
     * Translate the current matrix by the specified amounts
     * 
     * @param dx The amount to translate in X
     * @param dy The amount to translate in Y
     */
    public void translate(float dx, float dy) {
    }
    
    /**
     * Clear the entire canvas to the specified color
     * 
     * @param color The color to clear to
     */
    public void drawColor(int color) {
        // For a mock, we're not actually drawing, just pretending
    }
    
    /**
     * Draw the specified bitmap, with its top/left corner at (left, top),
     * using the specified paint
     * 
     * @param bitmap The bitmap to draw
     * @param left The position of the left side of the bitmap
     * @param top The position of the top side of the bitmap
     * @param paint The paint used to draw the bitmap
     */
    public void drawBitmap(@NonNull Bitmap bitmap, float left, float top, @Nullable Paint paint) {
        // For a mock, we're not actually drawing, just pretending
    }
    
    /**
     * Draw the specified bitmap, scaling/translating to fill the dst rect,
     * using the specified paint
     * 
     * @param bitmap The bitmap to draw
     * @param src May be null. The subset of the bitmap to draw
     * @param dst The rectangle to draw the bitmap into
     * @param paint The paint used to draw the bitmap
     */
    public void drawBitmap(@NonNull Bitmap bitmap, @Nullable Rect src, @NonNull Rect dst, @Nullable Paint paint) {
        // For a mock, we're not actually drawing, just pretending
    }
    
    /**
     * Draw the specified bitmap, scaling/translating to fill the dst rect,
     * using the specified paint
     * 
     * @param bitmap The bitmap to draw
     * @param src May be null. The subset of the bitmap to draw
     * @param dst The rectangle to draw the bitmap into
     * @param paint The paint used to draw the bitmap
     */
    public void drawBitmap(@NonNull Bitmap bitmap, @Nullable Rect src, @NonNull RectF dst, @Nullable Paint paint) {
        // For a mock, we're not actually drawing, just pretending
    }
    
    /**
     * Draw the specified bitmap, with its top/left corner at (x, y),
     * with the specified matrix applied to it
     * 
     * @param bitmap The bitmap to draw
     * @param matrix The matrix to apply to the bitmap
     * @param paint The paint used to draw the bitmap
     */
    public void drawBitmap(@NonNull Bitmap bitmap, @NonNull Matrix matrix, @Nullable Paint paint) {
        // For a mock, we're not actually drawing, just pretending
    }
    
    /**
     * Draw the specified bitmap, scaling to the dst dimensions
     * 
     * @param bitmap The bitmap to draw
     * @param left The position of the left side of the bitmap
     * @param top The position of the top side of the bitmap
     * @param width The width to scale the bitmap to
     * @param height The height to scale the bitmap to
     * @param paint The paint used to draw the bitmap
     */
    public void drawBitmap(@NonNull Bitmap bitmap, float left, float top, float width, float height, @Nullable Paint paint) {
        // For a mock, we're not actually drawing, just pretending
    }
    
    /**
     * Draw a circle at (cx, cy) with the specified radius and paint
     * 
     * @param cx The x-coordinate of the center of the circle
     * @param cy The y-coordinate of the center of the circle
     * @param radius The radius of the circle
     * @param paint The paint used to draw the circle
     */
    public void drawCircle(float cx, float cy, float radius, @NonNull Paint paint) {
        // For a mock, we're not actually drawing, just pretending
    }
    
    /**
     * Draw a line from (x1, y1) to (x2, y2) with the specified paint
     * 
     * @param x1 The x-coordinate of the start point
     * @param y1 The y-coordinate of the start point
     * @param x2 The x-coordinate of the end point
     * @param y2 The y-coordinate of the end point
     * @param paint The paint used to draw the line
     */
    public void drawLine(float x1, float y1, float x2, float y2, @NonNull Paint paint) {
        // For a mock, we're not actually drawing, just pretending
    }
    
    /**
     * Draw a series of lines from the specified array of points with the specified paint
     * 
     * @param pts The array of points to draw [x1, y1, x2, y2, ...]
     * @param paint The paint used to draw the lines
     */
    public void drawLines(@NonNull float[] pts, @NonNull Paint paint) {
        drawLines(pts, 0, pts.length, paint);
    }
    
    /**
     * Draw a series of lines from the specified array of points with the specified paint
     * 
     * @param pts The array of points to draw [x1, y1, x2, y2, ...]
     * @param offset The offset into the array of the first point to read
     * @param count The number of values in the array to read
     * @param paint The paint used to draw the lines
     */
    public void drawLines(@NonNull float[] pts, int offset, int count, @NonNull Paint paint) {
        // For a mock, we're not actually drawing, just pretending
    }
    
    /**
     * Draw an oval bounded by the specified rectangle with the specified paint
     * 
     * @param oval The rectangular bounds of the oval to draw
     * @param paint The paint used to draw the oval
     */
    public void drawOval(@NonNull RectF oval, @NonNull Paint paint) {
        // For a mock, we're not actually drawing, just pretending
    }
    
    /**
     * Draw a path with the specified paint
     * 
     * @param path The path to draw
     * @param paint The paint used to draw the path
     */
    public void drawPath(@NonNull Path path, @NonNull Paint paint) {
        // For a mock, we're not actually drawing, just pretending
    }
    
    /**
     * Draw a point at (x, y) with the specified paint
     * 
     * @param x The x-coordinate
     * @param y The y-coordinate
     * @param paint The paint used to draw the point
     */
    public void drawPoint(float x, float y, @NonNull Paint paint) {
        // For a mock, we're not actually drawing, just pretending
    }
    
    /**
     * Draw a series of points from the specified array of points with the specified paint
     * 
     * @param pts The array of points to draw [x1, y1, x2, y2, ...]
     * @param paint The paint used to draw the points
     */
    public void drawPoints(@NonNull float[] pts, @NonNull Paint paint) {
        drawPoints(pts, 0, pts.length, paint);
    }
    
    /**
     * Draw a series of points from the specified array of points with the specified paint
     * 
     * @param pts The array of points to draw [x1, y1, x2, y2, ...]
     * @param offset The offset into the array of the first point to read
     * @param count The number of values in the array to read
     * @param paint The paint used to draw the points
     */
    public void drawPoints(@NonNull float[] pts, int offset, int count, @NonNull Paint paint) {
        // For a mock, we're not actually drawing, just pretending
    }
    
    /**
     * Draw a rectangle with the specified coordinates and paint
     * 
     * @param rect The rectangle to draw
     * @param paint The paint used to draw the rectangle
     */
    public void drawRect(@NonNull Rect rect, @NonNull Paint paint) {
        drawRect(rect.left, rect.top, rect.right, rect.bottom, paint);
    }
    
    /**
     * Draw a rectangle with the specified coordinates and paint
     * 
     * @param rect The rectangle to draw
     * @param paint The paint used to draw the rectangle
     */
    public void drawRect(@NonNull RectF rect, @NonNull Paint paint) {
        drawRect(rect.left, rect.top, rect.right, rect.bottom, paint);
    }
    
    /**
     * Draw a rectangle with the specified coordinates and paint
     * 
     * @param left The left side of the rectangle
     * @param top The top side of the rectangle
     * @param right The right side of the rectangle
     * @param bottom The bottom side of the rectangle
     * @param paint The paint used to draw the rectangle
     */
    public void drawRect(float left, float top, float right, float bottom, @NonNull Paint paint) {
        // For a mock, we're not actually drawing, just pretending
    }
    
    /**
     * Draw the specified text at the specified position with the specified paint
     * 
     * @param text The text to draw
     * @param x The x-coordinate of the origin of the text
     * @param y The y-coordinate of the origin of the text
     * @param paint The paint used to draw the text
     */
    public void drawText(@NonNull String text, float x, float y, @NonNull Paint paint) {
        // For a mock, we're not actually drawing, just pretending
    }
    
    /**
     * Draw the specified text substring at the specified position with the specified paint
     * 
     * @param text The text to draw
     * @param start The index of the first character to draw
     * @param end The index after the last character to draw
     * @param x The x-coordinate of the origin of the text
     * @param y The y-coordinate of the origin of the text
     * @param paint The paint used to draw the text
     */
    public void drawText(@NonNull String text, int start, int end, float x, float y, @NonNull Paint paint) {
        // For a mock, we're not actually drawing, just pretending
    }
    
    /**
     * Draw the specified text at the specified position with the specified paint
     * 
     * @param text The text to draw
     * @param start The index of the first character to draw
     * @param count The number of characters to draw
     * @param x The x-coordinate of the origin of the text
     * @param y The y-coordinate of the origin of the text
     * @param paint The paint used to draw the text
     */
    public void drawText(@NonNull char[] text, int start, int count, float x, float y, @NonNull Paint paint) {
        // For a mock, we're not actually drawing, just pretending
    }
    
    /**
     * Get the width of the canvas
     * 
     * @return The width
     */
    public int getWidth() {
        return bitmap != null ? bitmap.getWidth() : 0;
    }
    
    /**
     * Get the height of the canvas
     * 
     * @return The height
     */
    public int getHeight() {
        return bitmap != null ? bitmap.getHeight() : 0;
    }
    
    /**
     * Fill the entire canvas with the specified color and blend mode
     * 
     * @param color The color to draw
     * @param mode The blend mode to use
     */
    public void drawARGB(int a, int r, int g, int b) {
        drawColor(Color.argb(a, r, g, b));
    }
}