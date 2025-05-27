package models;

import java.util.List;

/**
 * Interface for receiving suggestion callbacks.
 * This is used for getting suggestions from the AI system.
 */
public interface SuggestionListener {
    /**
     * Called when a suggestion is available.
     * 
     * @param suggestion The suggestion object, may be of various types depending on the AI system
     */
    void onSuggestionAvailable(Object suggestion);
    
    /**
     * Called when multiple suggestions are available.
     * 
     * @param suggestions Array of suggestion objects
     */
    void onSuggestionsAvailable(Object[] suggestions);
    
    /**
     * Called when a suggestion fails.
     * 
     * @param errorMessage The error message
     */
    void onSuggestionFailed(String errorMessage);
    
    /**
     * Called when the suggestion system is ready.
     */
    void onSuggestionSystemReady();
    
    /**
     * Called when the suggestion system has an error.
     * 
     * @param errorMessage The error message
     */
    void onSuggestionSystemError(String errorMessage);
    
    /**
     * Called when action suggestions for a specific game are available.
     * 
     * @param gameId The game identifier
     * @param predictions The list of action predictions
     */
    default void onActionSuggestions(String gameId, List<ActionPrediction> predictions) {
        // Default implementation converts to array and calls the general method
        if (predictions != null && !predictions.isEmpty()) {
            onSuggestionsAvailable(predictions.toArray());
        }
    }
}