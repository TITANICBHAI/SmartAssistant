package utils;

/**
 * Interface for event listeners
 */
public interface EventListener {
    
    /**
     * Handle an event
     * 
     * @param eventType Type of event
     * @param data Event data
     * @return True if event was handled
     */
    boolean onEvent(String eventType, Object data);
    
    /**
     * Check if this listener is interested in an event
     * 
     * @param eventType Type of event
     * @return True if interested
     */
    boolean isInterestedIn(String eventType);
}