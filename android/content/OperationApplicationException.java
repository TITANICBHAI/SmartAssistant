package android.content;

/**
 * Mock implementation of Android OperationApplicationException class for development outside of Android.
 * Exception thrown when a ContentProviderOperation fails to apply correctly.
 */
public class OperationApplicationException extends Exception {
    private final int mNumSuccessfulOperations;
    
    /**
     * Create a new OperationApplicationException.
     *
     * @param message The exception message.
     */
    public OperationApplicationException(String message) {
        super(message);
        mNumSuccessfulOperations = 0;
    }
    
    /**
     * Create a new OperationApplicationException.
     *
     * @param message The exception message.
     * @param numSuccessfulOperations The number of operations that succeeded before this exception was thrown.
     */
    public OperationApplicationException(String message, int numSuccessfulOperations) {
        super(message);
        mNumSuccessfulOperations = numSuccessfulOperations;
    }
    
    /**
     * Get the number of operations that succeeded before this exception was thrown.
     *
     * @return The number of successful operations.
     */
    public int getNumSuccessfulOperations() {
        return mNumSuccessfulOperations;
    }
}