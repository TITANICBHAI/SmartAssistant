package utils;

/**
 * Configuration for video processor
 */
public class VideoProcessorConfig {
    private boolean enableElementDetection = true;
    private boolean enableActionDetection = false;
    private boolean enableTextRecognition = false;
    private boolean enableFaceRecognition = false;
    private boolean enableObjectTracking = false;
    private int frameWidth = 1080;
    private int frameHeight = 1920;
    private int frameRate = 30;
    private String encodingFormat = "YUV420";
    private int detectionThreshold = 70; // Confidence threshold (0-100)
    
    /**
     * Default constructor
     */
    public VideoProcessorConfig() {
        // Default values initialized in field declarations
    }
    
    /**
     * Check if element detection is enabled
     * 
     * @return True if enabled, false otherwise
     */
    public boolean isElementDetectionEnabled() {
        return enableElementDetection;
    }
    
    /**
     * Set element detection enabled
     * 
     * @param enableElementDetection True to enable, false to disable
     */
    public void setEnableElementDetection(boolean enableElementDetection) {
        this.enableElementDetection = enableElementDetection;
    }
    
    /**
     * Check if action detection is enabled
     * 
     * @return True if enabled, false otherwise
     */
    public boolean isActionDetectionEnabled() {
        return enableActionDetection;
    }
    
    /**
     * Set action detection enabled
     * 
     * @param enableActionDetection True to enable, false to disable
     */
    public void setEnableActionDetection(boolean enableActionDetection) {
        this.enableActionDetection = enableActionDetection;
    }
    
    /**
     * Check if text recognition is enabled
     * 
     * @return True if enabled, false otherwise
     */
    public boolean isTextRecognitionEnabled() {
        return enableTextRecognition;
    }
    
    /**
     * Set text recognition enabled
     * 
     * @param enableTextRecognition True to enable, false to disable
     */
    public void setEnableTextRecognition(boolean enableTextRecognition) {
        this.enableTextRecognition = enableTextRecognition;
    }
    
    /**
     * Check if face recognition is enabled
     * 
     * @return True if enabled, false otherwise
     */
    public boolean isFaceRecognitionEnabled() {
        return enableFaceRecognition;
    }
    
    /**
     * Set face recognition enabled
     * 
     * @param enableFaceRecognition True to enable, false to disable
     */
    public void setEnableFaceRecognition(boolean enableFaceRecognition) {
        this.enableFaceRecognition = enableFaceRecognition;
    }
    
    /**
     * Check if object tracking is enabled
     * 
     * @return True if enabled, false otherwise
     */
    public boolean isObjectTrackingEnabled() {
        return enableObjectTracking;
    }
    
    /**
     * Set object tracking enabled
     * 
     * @param enableObjectTracking True to enable, false to disable
     */
    public void setEnableObjectTracking(boolean enableObjectTracking) {
        this.enableObjectTracking = enableObjectTracking;
    }
    
    /**
     * Get frame width
     * 
     * @return Frame width in pixels
     */
    public int getFrameWidth() {
        return frameWidth;
    }
    
    /**
     * Set frame width
     * 
     * @param frameWidth Frame width in pixels
     */
    public void setFrameWidth(int frameWidth) {
        this.frameWidth = frameWidth;
    }
    
    /**
     * Get frame height
     * 
     * @return Frame height in pixels
     */
    public int getFrameHeight() {
        return frameHeight;
    }
    
    /**
     * Set frame height
     * 
     * @param frameHeight Frame height in pixels
     */
    public void setFrameHeight(int frameHeight) {
        this.frameHeight = frameHeight;
    }
    
    /**
     * Get frame rate
     * 
     * @return Frame rate in frames per second
     */
    public int getFrameRate() {
        return frameRate;
    }
    
    /**
     * Set frame rate
     * 
     * @param frameRate Frame rate in frames per second
     */
    public void setFrameRate(int frameRate) {
        this.frameRate = frameRate;
    }
    
    /**
     * Get encoding format
     * 
     * @return The encoding format
     */
    public String getEncodingFormat() {
        return encodingFormat;
    }
    
    /**
     * Set encoding format
     * 
     * @param encodingFormat The encoding format
     */
    public void setEncodingFormat(String encodingFormat) {
        this.encodingFormat = encodingFormat;
    }
    
    /**
     * Get detection threshold
     * 
     * @return Detection threshold (0-100)
     */
    public int getDetectionThreshold() {
        return detectionThreshold;
    }
    
    /**
     * Set detection threshold
     * 
     * @param detectionThreshold Detection threshold (0-100)
     */
    public void setDetectionThreshold(int detectionThreshold) {
        if (detectionThreshold < 0) {
            this.detectionThreshold = 0;
        } else if (detectionThreshold > 100) {
            this.detectionThreshold = 100;
        } else {
            this.detectionThreshold = detectionThreshold;
        }
    }
}