package android.os;

/**
 * Mock implementation of Android Looper class for development outside of Android.
 * Class used to run a message loop for a thread.
 */
public final class Looper {
    private static final ThreadLocal<Looper> sThreadLocal = new ThreadLocal<>();
    private static Looper sMainLooper;
    
    private final Thread mThread;
    
    static {
        // Initialize the main looper
        sMainLooper = new Looper();
    }
    
    /**
     * Initialize the current thread as a looper, marking it as an application's main looper.
     * The main looper for your application is created by the Android environment.
     * <p>
     * Don't call this function yourself.
     * </p>
     */
    public static void prepareMainLooper() {
        // Already prepared in the static initializer
    }
    
    /**
     * For mocking purposes only - don't initialize in real code.
     */
    private Looper() {
        mThread = Thread.currentThread();
        sThreadLocal.set(this);
    }
    
    /**
     * Initialize the current thread as a looper.
     * This gives you a chance to create handlers that then reference this looper, before actually
     * starting the loop.
     */
    public static void prepare() {
        if (sThreadLocal.get() != null) {
            throw new RuntimeException("Only one Looper may be created per thread");
        }
        sThreadLocal.set(new Looper());
    }
    
    /**
     * Run the message queue in this thread.
     * Be sure to call {@link #quit()} to end the loop.
     */
    public static void loop() {
        // Mock implementation, does nothing
    }
    
    /**
     * Return the Looper object associated with the current thread.
     * 
     * @return The looper.
     */
    public static Looper myLooper() {
        return sThreadLocal.get();
    }
    
    /**
     * Return the Looper object associated with the main thread.
     * 
     * @return The main looper.
     */
    public static Looper getMainLooper() {
        return sMainLooper;
    }
    
    /**
     * Quits the looper.
     * <p>
     * Causes the {@link #loop} method to terminate without processing any more messages in the message queue.
     * </p>
     */
    public void quit() {
        // Mock implementation, does nothing
    }
    
    /**
     * Return the Thread associated with this Looper.
     * 
     * @return The thread.
     */
    public Thread getThread() {
        return mThread;
    }
    
    @Override
    public String toString() {
        return "Looper (" + mThread.getName() + ")";
    }
}