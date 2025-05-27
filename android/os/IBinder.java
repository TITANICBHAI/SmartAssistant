package android.os;

/**
 * Mock implementation of Android IBinder interface for development outside of Android.
 * Base interface for a remotable object, the core part of a lightweight remote procedure call mechanism.
 */
public interface IBinder {
    /**
     * Flag to transact: this is a one-way call, meaning that the caller returns immediately, without waiting for a result.
     */
    int FLAG_ONEWAY = 0x00000001;
    
    /**
     * Get the interface descriptor string.
     * 
     * @return A String that uniquely identifies the interface.
     */
    String getInterfaceDescriptor();
    
    /**
     * Check to see if the object still exists.
     * 
     * @return True if the object is alive.
     */
    boolean pingBinder();
    
    /**
     * Check to see if this object implements the interface with the given identifier.
     * 
     * @param descriptor The interface name.
     * @return True if the object implements the interface.
     */
    boolean isBinderAlive();
    
    /**
     * Attempt to retrieve a local implementation of an interface that this Binder object implements.
     * 
     * @param descriptor The interface name.
     * @return The object that implements the given descriptor, or null if it does not.
     */
    IInterface queryLocalInterface(String descriptor);
    
    /**
     * Perform a generic operation with the object.
     * 
     * @param code The operation to perform.
     * @param data Marshalled data to send to the target.
     * @param reply Marshalled data to be received from the target.
     * @param flags Additional options for the operation.
     * @return Result code from the operation.
     */
    boolean transact(int code, Parcel data, Parcel reply, int flags);
    
    /**
     * Interface for receiving a callback when the process hosting this Binder dies.
     */
    interface DeathRecipient {
        /**
         * Called when the process that hosts the IBinder has died.
         */
        void binderDied();
    }
    
    /**
     * Register the recipient for a notification if this binder goes away.
     * 
     * @param recipient The object to receive the death notification.
     * @param flags Additional options for the operation.
     * @return True if successful.
     */
    boolean linkToDeath(DeathRecipient recipient, int flags);
    
    /**
     * Unregister a previously registered death notification.
     * 
     * @param recipient The object that should no longer receive death notifications.
     * @param flags Additional options for the operation.
     * @return True if successful.
     */
    boolean unlinkToDeath(DeathRecipient recipient, int flags);
}