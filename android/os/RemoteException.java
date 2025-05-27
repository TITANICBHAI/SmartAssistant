package android.os;

/**
 * Mock implementation of Android's RemoteException class.
 * Exception thrown when a Binder transaction fails.
 */
public class RemoteException extends Exception {
    /**
     * Construct a new RemoteException.
     */
    public RemoteException() {
        super();
    }
    
    /**
     * Construct a new RemoteException with a message.
     */
    public RemoteException(String message) {
        super(message);
    }
}