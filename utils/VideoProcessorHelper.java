package utils;

/**
 * Helper class for video processing
 */
public class VideoProcessorHelper {
    private static VideoProcessor defaultProcessor;
    
    /**
     * Get the default video processor
     * 
     * @return The default video processor
     */
    public static VideoProcessor getDefaultProcessor() {
        if (defaultProcessor == null) {
            defaultProcessor = new VideoProcessorImpl();
        }
        return defaultProcessor;
    }
    
    /**
     * Initialize a video processor with configuration
     * 
     * @param processor The processor to initialize
     * @param config The configuration
     */
    public static void initializeProcessor(VideoProcessor processor, VideoProcessorConfig config) {
        if (processor == null || config == null) {
            return;
        }
        
        // Apply configuration
        processor.setFrameRate(config.getFrameRate());
        processor.setRealTimeProcessing(true);
        
        // Additional configuration if the processor is our implementation
        if (processor instanceof VideoProcessorImpl) {
            VideoProcessorImpl impl = (VideoProcessorImpl) processor;
            impl.setEnableElementDetection(config.isElementDetectionEnabled());
            impl.setEnableActionDetection(config.isActionDetectionEnabled());
        }
    }
}