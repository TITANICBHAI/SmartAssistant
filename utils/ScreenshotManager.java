package utils;

import android.graphics.Bitmap;

/**
 * Interface for taking screenshots.
 */
public interface ScreenshotManager {
    /**
     * Take a screenshot of the current display.
     * 
     * @return A bitmap representation of the screen
     */
    Bitmap takeScreenshot();
    
    /**
     * Takes a screenshot and saves it to the specified path.
     * 
     * @param path The file path to save the screenshot to
     * @return True if the screenshot was successfully saved
     */
    boolean saveScreenshotToFile(String path);
    
    /**
     * Set the screenshot quality.
     * 
     * @param quality A value from 0 to 100
     */
    void setQuality(int quality);
    
    /**
     * Sets the screenshot format.
     * 
     * @param format The format to use for screenshots
     */
    void setFormat(android.graphics.Bitmap.CompressFormat format);
    
    /**
     * Gets the current screenshot format.
     * 
     * @return The current format used for screenshots
     */
    android.graphics.Bitmap.CompressFormat getFormat();
    
    /**
     * Gets the current screenshot quality.
     * 
     * @return A value from 0 to 100
     */
    int getQuality();
    
    /**
     * Sets a delay before taking the screenshot.
     * 
     * @param delayMs The delay in milliseconds
     */
    void setDelay(int delayMs);
    
    /**
     * Gets the current delay.
     * 
     * @return The delay in milliseconds
     */
    int getDelay();
}