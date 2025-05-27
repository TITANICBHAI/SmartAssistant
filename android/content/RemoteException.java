package android.content;

/**
 * Mock implementation of Android RemoteException class for development outside of Android.
 * Exception thrown when a remote operation fails.
 */
public class RemoteException extends Exception {
    /**
     * Create a new RemoteException with the default message.
     */
    public RemoteException() {
        super("Remote operation failed");
    }
    
    /**
     * Create a new RemoteException with the given message.
     *
     * @param message The exception message.
     */
    public RemoteException(String message) {
        super(message);
    }
    
    /**
     * Create a new RemoteException with the given message and cause.
     *
     * @param message The exception message.
     * @param cause The cause of the exception.
     */
    public RemoteException(String message, Throwable cause) {
        super(message, cause);
    }
}