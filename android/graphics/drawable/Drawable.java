package android.graphics.drawable;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.ColorFilter;
import android.graphics.Rect;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

/**
 * Mock implementation of Android Drawable class for development outside of Android.
 * A Drawable is a general abstraction for "something that can be drawn."
 */
public abstract class Drawable {
    /**
     * Draw in its bounds (set via setBounds) respecting optional effects.
     * 
     * @param canvas The canvas to draw into.
     */
    public abstract void draw(@NonNull Canvas canvas);
    
    /**
     * Return the drawable's intrinsic width.
     * 
     * @return The intrinsic width.
     */
    public abstract int getIntrinsicWidth();
    
    /**
     * Return the drawable's intrinsic height.
     * 
     * @return The intrinsic height.
     */
    public abstract int getIntrinsicHeight();
    
    /**
     * Return the opacity/transparency of this Drawable.
     * 
     * @return The opacity.
     */
    public abstract int getOpacity();
    
    /**
     * Set a color filter for this drawable.
     * 
     * @param colorFilter The color filter to apply.
     */
    public abstract void setColorFilter(@Nullable ColorFilter colorFilter);
    
    /**
     * Set the alpha level for this drawable.
     * 
     * @param alpha The alpha level to use, between 0 and 255.
     */
    public abstract void setAlpha(int alpha);
    
    /**
     * Specify a set of bounds for the drawable.
     * 
     * @param bounds The bounds to set.
     */
    public void setBounds(@NonNull Rect bounds) {
        setBounds(bounds.left, bounds.top, bounds.right, bounds.bottom);
    }
    
    /**
     * Specify a set of bounds for the drawable.
     * 
     * @param left The left bound.
     * @param top The top bound.
     * @param right The right bound.
     * @param bottom The bottom bound.
     */
    public abstract void setBounds(int left, int top, int right, int bottom);
    
    /**
     * Return the drawable's bounds.
     * 
     * @param outRect The Rect to receive the bounds.
     */
    public abstract void copyBounds(@NonNull Rect outRect);
    
    /**
     * Return a copy of the drawable's bounds.
     * 
     * @return A copy of the bounds.
     */
    @NonNull
    public abstract Rect getBounds();
    
    /**
     * Create a drawable from a bitmap, setting initial target density.
     * 
     * @param bitmap The bitmap to create a drawable from.
     * @return A new bitmap drawable.
     */
    @NonNull
    public static Drawable createFromBitmap(@NonNull Bitmap bitmap) {
        // In a real implementation, this would return a BitmapDrawable.
        // For mock purposes, we return a simple implementation.
        return new BitmapDrawable(bitmap);
    }
    
    /**
     * Simple mock implementation of BitmapDrawable.
     */
    private static class BitmapDrawable extends Drawable {
        private final Bitmap mBitmap;
        private Rect mBounds = new Rect();
        
        private BitmapDrawable(Bitmap bitmap) {
            mBitmap = bitmap;
        }
        
        @Override
        public void draw(@NonNull Canvas canvas) {
            if (mBitmap != null) {
                canvas.drawBitmap(mBitmap, null, mBounds, null);
            }
        }
        
        @Override
        public int getIntrinsicWidth() {
            return mBitmap != null ? mBitmap.getWidth() : -1;
        }
        
        @Override
        public int getIntrinsicHeight() {
            return mBitmap != null ? mBitmap.getHeight() : -1;
        }
        
        @Override
        public int getOpacity() {
            return 0; // Mock implementation, always returns 0
        }
        
        @Override
        public void setColorFilter(@Nullable ColorFilter colorFilter) {
            // Mock implementation, does nothing
        }
        
        @Override
        public void setAlpha(int alpha) {
            // Mock implementation, does nothing
        }
        
        @Override
        public void setBounds(int left, int top, int right, int bottom) {
            mBounds.set(left, top, right, bottom);
        }
        
        @Override
        public void copyBounds(@NonNull Rect outRect) {
            outRect.set(mBounds);
        }
        
        @NonNull
        @Override
        public Rect getBounds() {
            return new Rect(mBounds);
        }
    }
}