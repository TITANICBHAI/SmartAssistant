package utils;

import java.util.Map;

/**
 * Interface for handling actions on UI elements.
 * Implementations of this interface can execute specific actions such as clicking, typing,
 * swiping, etc., on identified UI elements.
 */
public interface ActionHandler {
    
    /**
     * Handle an action with the given parameters.
     * 
     * @param parameters A map of parameters for the action. Common parameters might include:
     *                  - "elementId": The ID of the UI element to act on
     *                  - "text": Text to enter if the action is a text input
     *                  - "x", "y": Coordinates for click or touch actions
     *                  - "duration": Duration for long press or animation
     *                  - "direction": Direction for swipe or scroll actions
     * @return True if the action was handled successfully, false otherwise
     */
    boolean handleAction(Map<String, Object> parameters);
    
    /**
     * Get the type of action this handler can handle.
     * 
     * @return The action type as a string (e.g., "click", "type", "swipe", etc.)
     */
    String getActionType();
    
    /**
     * Check if this handler requires a specific UI element to perform its action.
     * 
     * @return True if this handler requires a UI element, false otherwise
     */
    boolean requiresElement();
    
    /**
     * Get the required parameter names for this action handler.
     * 
     * @return An array of parameter names that are required for this action
     */
    String[] getRequiredParameters();
    
    /**
     * Validate that the given parameters are sufficient for this action handler.
     * 
     * @param parameters The parameters to validate
     * @return True if the parameters are valid, false otherwise
     */
    default boolean validateParameters(Map<String, Object> parameters) {
        if (parameters == null) {
            return false;
        }
        
        String[] requiredParams = getRequiredParameters();
        if (requiredParams == null || requiredParams.length == 0) {
            return true;
        }
        
        for (String param : requiredParams) {
            if (!parameters.containsKey(param)) {
                return false;
            }
        }
        
        if (requiresElement() && !parameters.containsKey("elementId")) {
            return false;
        }
        
        return true;
    }
}