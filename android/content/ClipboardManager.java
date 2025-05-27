package android.content;

/**
 * Mock implementation of Android's ClipboardManager
 */
public class ClipboardManager {
    private ClipData primaryClip;
    
    /**
     * Default constructor
     */
    public ClipboardManager() {
        // Empty constructor
    }
    
    /**
     * Set the primary clip
     * @param clip The clip to set
     */
    public void setPrimaryClip(ClipData clip) {
        this.primaryClip = clip;
    }
    
    /**
     * Get the primary clip
     * @return The primary clip
     */
    public ClipData getPrimaryClip() {
        return primaryClip;
    }
    
    /**
     * Check if there is a primary clip
     * @return True if there is a primary clip, false otherwise
     */
    public boolean hasPrimaryClip() {
        return primaryClip != null;
    }
}