package com.aiassistant.utils;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.PixelFormat;
import android.hardware.display.DisplayManager;
import android.hardware.display.VirtualDisplay;
import android.media.Image;
import android.media.ImageReader;
import android.media.projection.MediaProjection;
import android.os.Handler;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.WindowManager;

import androidx.annotation.NonNull;

import java.nio.ByteBuffer;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

/**
 * Utility class for capturing screenshots to analyze UI elements
 */
public class ScreenshotManager {
    private static final String TAG = "ScreenshotManager";
    
    // Singleton instance
    private static ScreenshotManager instance;
    
    // Context
    private final Context context;
    
    // Media projection
    private MediaProjection mediaProjection;
    
    // Display metrics
    private int width;
    private int height;
    private int density;
    
    // Virtual display
    private VirtualDisplay virtualDisplay;
    
    // Image reader
    private ImageReader imageReader;
    
    // Last screenshot
    private Bitmap lastScreenshot;
    
    // Screenshot callback
    public interface ScreenshotCallback {
        void onScreenshotTaken(Bitmap screenshot);
        void onError(String error);
    }
    
    /**
     * Get singleton instance
     */
    @SuppressLint("StaticFieldLeak") // Context is application context
    public static synchronized ScreenshotManager getInstance(@NonNull Context context) {
        if (instance == null) {
            instance = new ScreenshotManager(context.getApplicationContext());
        }
        return instance;
    }
    
    /**
     * Private constructor
     */
    private ScreenshotManager(Context context) {
        this.context = context;
        
        // Get display metrics
        WindowManager windowManager = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
        DisplayMetrics metrics = new DisplayMetrics();
        windowManager.getDefaultDisplay().getMetrics(metrics);
        
        this.width = metrics.widthPixels;
        this.height = metrics.heightPixels;
        this.density = metrics.densityDpi;
    }
    
    /**
     * Set media projection (obtained from MediaProjection permission activity)
     */
    public void setMediaProjection(@NonNull MediaProjection mediaProjection) {
        this.mediaProjection = mediaProjection;
        
        // Create image reader
        imageReader = ImageReader.newInstance(width, height, PixelFormat.RGBA_8888, 2);
        
        // Create virtual display
        virtualDisplay = mediaProjection.createVirtualDisplay(
                "ScreenshotVirtualDisplay",
                width, height, density,
                DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,
                imageReader.getSurface(),
                null, null);
                
        Log.i(TAG, "Media projection set up successfully");
    }
    
    /**
     * Release resources
     */
    public void release() {
        if (virtualDisplay != null) {
            virtualDisplay.release();
            virtualDisplay = null;
        }
        
        if (imageReader != null) {
            imageReader.close();
            imageReader = null;
        }
        
        if (mediaProjection != null) {
            mediaProjection.stop();
            mediaProjection = null;
        }
        
        Log.i(TAG, "Resources released");
    }
    
    /**
     * Take a screenshot synchronously
     * 
     * @return Screenshot bitmap or null if error
     */
    public Bitmap takeScreenshot() {
        if (mediaProjection == null || imageReader == null) {
            Log.e(TAG, "Cannot take screenshot: media projection not initialized");
            return null;
        }
        
        final Bitmap[] result = new Bitmap[1];
        final CountDownLatch latch = new CountDownLatch(1);
        
        // Acquire image on background thread
        final Handler handler = new Handler();
        
        imageReader.setOnImageAvailableListener(reader -> {
            try {
                Image image = reader.acquireLatestImage();
                if (image != null) {
                    try {
                        result[0] = imageToBitmap(image);
                    } finally {
                        image.close();
                    }
                }
            } catch (Exception e) {
                Log.e(TAG, "Error acquiring image: " + e.getMessage());
            } finally {
                latch.countDown();
                imageReader.setOnImageAvailableListener(null, null);
            }
        }, handler);
        
        // Trigger a new frame
        try {
            if (virtualDisplay == null) {
                // Recreate virtual display if needed
                virtualDisplay = mediaProjection.createVirtualDisplay(
                        "ScreenshotVirtualDisplay",
                        width, height, density,
                        DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,
                        imageReader.getSurface(),
                        null, null);
            }
            
            // Wait for image
            try {
                boolean success = latch.await(3, TimeUnit.SECONDS);
                if (!success) {
                    Log.e(TAG, "Timeout waiting for screenshot");
                    return null;
                }
            } catch (InterruptedException e) {
                Log.e(TAG, "Interrupted waiting for screenshot: " + e.getMessage());
                return null;
            }
            
            return result[0];
        } catch (Exception e) {
            Log.e(TAG, "Error taking screenshot: " + e.getMessage());
            return null;
        }
    }
    
    /**
     * Take screenshot asynchronously
     */
    public void takeScreenshotAsync(@NonNull ScreenshotCallback callback) {
        new Thread(() -> {
            Bitmap screenshot = takeScreenshot();
            if (screenshot != null) {
                callback.onScreenshotTaken(screenshot);
            } else {
                callback.onError("Failed to take screenshot");
            }
        }).start();
    }
    
    /**
     * Convert Image to Bitmap
     */
    private Bitmap imageToBitmap(Image image) {
        if (image == null) {
            return null;
        }
        
        Image.Plane[] planes = image.getPlanes();
        if (planes.length == 0) {
            return null;
        }
        
        ByteBuffer buffer = planes[0].getBuffer();
        int pixelStride = planes[0].getPixelStride();
        int rowStride = planes[0].getRowStride();
        int rowPadding = rowStride - pixelStride * width;
        
        // Create bitmap
        Bitmap bitmap = Bitmap.createBitmap(
                width + rowPadding / pixelStride, 
                height, 
                Bitmap.Config.ARGB_8888);
        
        bitmap.copyPixelsFromBuffer(buffer);
        
        // Crop if needed
        if (bitmap.getWidth() != width || bitmap.getHeight() != height) {
            Bitmap croppedBitmap = Bitmap.createBitmap(bitmap, 0, 0, width, height);
            bitmap.recycle();
            return croppedBitmap;
        }
        
        return bitmap;
    }
    
    /**
     * Get screen width
     */
    public int getWidth() {
        return width;
    }
    
    /**
     * Get screen height
     */
    public int getHeight() {
        return height;
    }
    
    /**
     * Check if media projection is ready
     */
    public boolean isReady() {
        return mediaProjection != null && imageReader != null && virtualDisplay != null;
    }
    
    /**
     * Get the width of the last screenshot taken
     * @return Width in pixels or 0 if no screenshot has been taken
     */
    public int getLastScreenshotWidth() {
        if (lastScreenshot != null) {
            return lastScreenshot.getWidth();
        } else if (width > 0) {
            return width;
        }
        return 0;
    }
    
    /**
     * Get the height of the last screenshot taken
     * @return Height in pixels or 0 if no screenshot has been taken
     */
    public int getLastScreenshotHeight() {
        if (lastScreenshot != null) {
            return lastScreenshot.getHeight();
        } else if (height > 0) {
            return height;
        }
        return 0;
    }
    
    /**
     * Get the last screenshot taken
     * @return Bitmap of the last screenshot or null if none has been taken
     */
    public Bitmap getLastScreenshot() {
        return lastScreenshot;
    }
    
    /**
     * Store the given bitmap as the last screenshot
     * @param bitmap The bitmap to store
     */
    public void setLastScreenshot(Bitmap bitmap) {
        this.lastScreenshot = bitmap;
    }
}