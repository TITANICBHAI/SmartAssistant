package utils;

/**
 * Interface for callbacks related to detection events in video processing
 */
public interface DetectionCallback {
    /**
     * Called when a detection event occurs
     *
     * @param detectionType the type of detection event
     * @param confidence the confidence level of the detection (0.0-1.0)
     * @param timestamp the timestamp when the detection occurred
     */
    void onDetectionEvent(String detectionType, float confidence, long timestamp);
    
    /**
     * Called when the detection process has made progress
     *
     * @param detectionType the type of detection in progress
     * @param progressPercent the progress percentage (0-100)
     * @param statusMessage a status message describing the current progress
     */
    void onDetectionProgress(String detectionType, int progressPercent, String statusMessage);
    
    /**
     * Called when the detection process is completed
     *
     * @param detectionType the type of detection that was performed
     * @param successful whether the detection process was successful
     * @param message a message providing details about the completion
     */
    void onDetectionCompleted(String detectionType, boolean successful, String message);
}