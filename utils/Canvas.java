package utils;

import android.graphics.Bitmap;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.RectF;

/**
 * Canvas utility class for drawing operations
 */
public class Canvas {
    private Bitmap mBitmap;
    private android.graphics.Canvas mCanvas;
    
    /**
     * Create a new Canvas for drawing onto the specified bitmap
     * @param bitmap The bitmap to draw onto
     */
    public Canvas(Bitmap bitmap) {
        if (bitmap == null) {
            throw new IllegalArgumentException("Bitmap cannot be null");
        }
        
        this.mBitmap = bitmap;
        this.mCanvas = new android.graphics.Canvas(bitmap);
    }
    
    /**
     * Draw a rectangle on the canvas
     * @param rect The rectangle to draw
     * @param paint The paint to use
     */
    public void drawRect(Rect rect, Paint paint) {
        if (mBitmap == null || mBitmap.isRecycled()) {
            return;
        }
        
        if (rect == null || paint == null) {
            return;
        }
        
        mCanvas.drawRect(rect, paint);
    }
    
    /**
     * Draw a rectangle on the canvas
     * @param rect The rectangle to draw
     * @param paint The paint to use
     */
    public void drawRect(RectF rect, Paint paint) {
        if (mBitmap == null || mBitmap.isRecycled()) {
            return;
        }
        
        if (rect == null || paint == null) {
            return;
        }
        
        mCanvas.drawRect(rect, paint);
    }
    
    /**
     * Draw a circle on the canvas
     * @param cx The x-coordinate of the center of the circle
     * @param cy The y-coordinate of the center of the circle
     * @param radius The radius of the circle
     * @param paint The paint to use
     */
    public void drawCircle(float cx, float cy, float radius, Paint paint) {
        if (mBitmap == null || mBitmap.isRecycled()) {
            return;
        }
        
        if (paint == null) {
            return;
        }
        
        mCanvas.drawCircle(cx, cy, radius, paint);
    }
    
    /**
     * Draw a line on the canvas
     * @param startX The x-coordinate of the start point
     * @param startY The y-coordinate of the start point
     * @param stopX The x-coordinate of the end point
     * @param stopY The y-coordinate of the end point
     * @param paint The paint to use
     */
    public void drawLine(float startX, float startY, float stopX, float stopY, Paint paint) {
        if (mBitmap == null || mBitmap.isRecycled()) {
            return;
        }
        
        if (paint == null) {
            return;
        }
        
        mCanvas.drawLine(startX, startY, stopX, stopY, paint);
    }
    
    /**
     * Draw text on the canvas
     * @param text The text to draw
     * @param x The x-coordinate of the origin of the text
     * @param y The y-coordinate of the baseline of the text
     * @param paint The paint to use
     */
    public void drawText(String text, float x, float y, Paint paint) {
        if (mBitmap == null || mBitmap.isRecycled()) {
            return;
        }
        
        if (text == null || paint == null) {
            return;
        }
        
        mCanvas.drawText(text, x, y, paint);
    }
    
    /**
     * Get the bitmap associated with this canvas
     * @return The bitmap
     */
    public Bitmap getBitmap() {
        return mBitmap;
    }
}