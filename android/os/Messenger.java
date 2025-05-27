package android.os;

/**
 * Mock implementation of Android's Messenger class.
 * Reference to a Handler that can be sent to a remote process.
 * This class serves as a light-weight wrapper around Handler and is intended to
 * be used across processes.
 */
public final class Messenger {
    private final Handler mHandler;
    
    /**
     * Create a new Messenger pointing to the given Handler.
     * 
     * @param target The Handler that will receive messages sent to this Messenger.
     */
    public Messenger(Handler target) {
        mHandler = target;
    }
    
    /**
     * Send a Message to this Messenger's Handler.
     * 
     * @param message The Message to send.
     * @throws RemoteException
     */
    public void send(Message message) throws RemoteException {
        mHandler.sendMessage(message);
    }
    
    /**
     * Retrieve the IBinder that this Messenger uses to communicate with its
     * associated Handler.
     * 
     * @return IBinder The IBinder backing this Messenger.
     */
    public IBinder getBinder() {
        return null; // In a real implementation, this would return an actual IBinder
    }
    
    /**
     * Test if two Messengers point to the same Handler.
     */
    @Override
    public boolean equals(Object o) {
        if (o == null) {
            return false;
        }
        if (o == this) {
            return true;
        }
        if (!(o instanceof Messenger)) {
            return false;
        }
        return mHandler == ((Messenger)o).mHandler;
    }
    
    /**
     * Get a hash code for this Messenger.
     */
    @Override
    public int hashCode() {
        return mHandler.hashCode();
    }
}