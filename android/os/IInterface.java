package android.os;

/**
 * Mock implementation of Android IInterface interface for development outside of Android.
 * Base class for Binder interfaces. When defining a new interface, 
 * you must derive it from IInterface.
 */
public interface IInterface {
    /**
     * Retrieve the Binder object associated with this interface.
     * 
     * @return the IBinder instance.
     */
    IBinder asBinder();
}