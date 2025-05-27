package utils;

/**
 * Interface for callbacks related to user actions and interactions
 */
public interface ActionCallback {
    /**
     * Called when an action is completed
     *
     * @param actionId the identifier of the action
     * @param success whether the action was successful
     * @param message additional information about the action result
     */
    void onActionCompleted(String actionId, boolean success, String message);
    
    /**
     * Called when an action is in progress
     *
     * @param actionId the identifier of the action
     * @param progressPercent the percentage of completion (0-100)
     * @param statusMessage a message describing the current status
     */
    void onActionProgress(String actionId, int progressPercent, String statusMessage);
    
    /**
     * Called when an action starts
     *
     * @param actionId the identifier of the action
     * @param actionType the type of action being performed
     */
    void onActionStarted(String actionId, String actionType);
    
    /**
     * Called when an error occurs during an action
     *
     * @param errorMessage the error message
     */
    void onActionError(String errorMessage);
    
    /**
     * Called when an error occurs during an action
     *
     * @param actionId the identifier of the action
     * @param errorMessage the error message
     */
    default void onActionError(String actionId, String errorMessage) {
        onActionError(errorMessage);
    }
    
    /**
     * Called when an error occurs during an action
     *
     * @param actionId the identifier of the action
     * @param errorCode the error code
     * @param errorMessage the error message
     */
    default void onActionError(String actionId, int errorCode, String errorMessage) {
        onActionError(actionId, errorMessage);
    }
    
    /**
     * Called when an action is complete
     *
     * @param success whether the action was successful
     */
    default void onActionComplete(boolean success) {
        onActionCompleted("action", success, success ? "Action completed successfully" : "Action failed");
    }
    
    /**
     * Called when an action is complete
     *
     * @param actionId the identifier of the action
     * @param success whether the action was successful
     */
    default void onActionComplete(String actionId, boolean success) {
        onActionCompleted(actionId, success, success ? "Action completed successfully" : "Action failed");
    }
}