package utils;

import org.opencv.core.Mat;

/**
 * Utility for processing images before analysis.
 */
public class ImageProcessor {
    private boolean enhanceContrast;
    private boolean removeNoise;
    private boolean convertToGrayscale;
    
    /**
     * Constructor with default settings.
     */
    public ImageProcessor() {
        this.enhanceContrast = true;
        this.removeNoise = true;
        this.convertToGrayscale = false;
    }
    
    /**
     * Enable or disable contrast enhancement.
     *
     * @param enhance True to enable enhancement
     */
    public void setEnhanceContrast(boolean enhance) {
        this.enhanceContrast = enhance;
    }
    
    /**
     * Enable or disable noise removal.
     *
     * @param remove True to enable noise removal
     */
    public void setRemoveNoise(boolean remove) {
        this.removeNoise = remove;
    }
    
    /**
     * Enable or disable grayscale conversion.
     *
     * @param convert True to convert to grayscale
     */
    public void setConvertToGrayscale(boolean convert) {
        this.convertToGrayscale = convert;
    }
    
    /**
     * Process an image for analysis.
     *
     * @param image The input bitmap image
     * @return The processed bitmap image
     */
    public utils.Bitmap processImage(utils.Bitmap image) {
        // In a real implementation, this would use OpenCV operations
        // This is a simplified implementation for development purposes
        
        // Clone the bitmap to avoid modifying the original
        utils.Bitmap processed = image.copy(image.getConfig(), true);
        
        // Apply processing steps based on settings
        if (convertToGrayscale) {
            // Would convert to grayscale using OpenCV
            // For development purposes, we just return the original
        }
        
        if (enhanceContrast) {
            // Would enhance contrast using OpenCV
            // For development purposes, we just return the original
        }
        
        if (removeNoise) {
            // Would apply noise reduction using OpenCV
            // For development purposes, we just return the original
        }
        
        return processed;
    }
    
    /**
     * Process an Android bitmap image for analysis.
     *
     * @param androidBitmap The input Android bitmap image
     * @return The processed Android bitmap image
     */
    public android.graphics.Bitmap processAndroidImage(android.graphics.Bitmap androidBitmap) {
        // Convert Android bitmap to utils bitmap
        utils.Bitmap utilsBitmap = BitmapHelper.fromAndroidBitmap(androidBitmap);
        
        // Process the utils bitmap
        utils.Bitmap processedUtilsBitmap = processImage(utilsBitmap);
        
        // Convert back to Android bitmap
        return BitmapHelper.toAndroidBitmap(processedUtilsBitmap);
    }
    
    /**
     * Convert OpenCV Mat to utils Bitmap.
     *
     * @param mat The OpenCV Mat
     * @return utils Bitmap
     */
    public utils.Bitmap matToBitmap(Mat mat) {
        // In a real implementation, this would use OpenCV's Utils class
        // For development purposes, we return a placeholder
        return utils.Bitmap.createBitmap(mat.width(), mat.height(), utils.Bitmap.Config.ARGB_8888);
    }
    
    /**
     * Convert utils Bitmap to OpenCV Mat.
     *
     * @param bitmap The utils Bitmap
     * @return OpenCV Mat
     */
    public Mat bitmapToMat(utils.Bitmap bitmap) {
        // In a real implementation, this would use OpenCV's Utils class
        // For development purposes, we return a placeholder
        return new Mat(bitmap.getHeight(), bitmap.getWidth(), 24); // 24 = CV_8UC3
    }
    
    /**
     * Convert Android Bitmap to OpenCV Mat.
     *
     * @param androidBitmap The Android Bitmap
     * @return OpenCV Mat
     */
    public Mat androidBitmapToMat(android.graphics.Bitmap androidBitmap) {
        // Convert Android bitmap to utils bitmap first
        utils.Bitmap utilsBitmap = BitmapHelper.fromAndroidBitmap(androidBitmap);
        
        // Then convert utils bitmap to Mat
        return bitmapToMat(utilsBitmap);
    }
    
    /**
     * Resize a utils bitmap image.
     *
     * @param image The input utils bitmap
     * @param width Target width
     * @param height Target height
     * @return Resized utils bitmap
     */
    public utils.Bitmap resizeImage(utils.Bitmap image, int width, int height) {
        return utils.Bitmap.createScaledBitmap(image, width, height, true);
    }
    
    /**
     * Resize an Android bitmap image.
     *
     * @param androidBitmap The input Android bitmap
     * @param width Target width
     * @param height Target height
     * @return Resized Android bitmap
     */
    public android.graphics.Bitmap resizeAndroidImage(android.graphics.Bitmap androidBitmap, int width, int height) {
        // Use alternative bitmap scaling method to avoid method signature issues
        if (androidBitmap == null) {
            return null;
        }
        
        // Create a new Bitmap with the specified dimensions
        android.graphics.Bitmap scaledBitmap = android.graphics.Bitmap.createBitmap(width, height, androidBitmap.getConfig());
        
        // Create a Canvas to draw on the new Bitmap
        android.graphics.Canvas canvas = new android.graphics.Canvas(scaledBitmap);
        
        // Create a Matrix for scaling
        android.graphics.Matrix matrix = new android.graphics.Matrix();
        matrix.setScale((float) width / androidBitmap.getWidth(), (float) height / androidBitmap.getHeight());
        
        // Create a Paint object for drawing
        android.graphics.Paint paint = new android.graphics.Paint();
        paint.setFilterBitmap(true);
        
        // Draw the source bitmap onto the target bitmap with scaling
        canvas.drawBitmap(androidBitmap, matrix, paint);
        
        return scaledBitmap;
    }
}