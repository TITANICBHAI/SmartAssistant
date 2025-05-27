package android.os;

/**
 * Mock implementation of Android's SystemClock class.
 * This class provides access to various system clocks.
 */
public class SystemClock {
    private static long bootTimeMillis = System.currentTimeMillis();
    
    /**
     * Get the current time in milliseconds, from the same clock as System.currentTimeMillis().
     * This is the wall clock time, which can be changed by the user or the network.
     * 
     * @return The current time in milliseconds
     */
    public static long currentTimeMillis() {
        return System.currentTimeMillis();
    }
    
    /**
     * Get the current monotonic time in milliseconds.
     * This time is guaranteed to be monotonic, but it may not be tied to any wall clock time.
     * 
     * @return The monotonic time in milliseconds
     */
    public static long elapsedRealtime() {
        return System.currentTimeMillis() - bootTimeMillis;
    }
    
    /**
     * Get the current monotonic time in nanoseconds.
     * This time is guaranteed to be monotonic, but it may not be tied to any wall clock time.
     * 
     * @return The monotonic time in nanoseconds
     */
    public static long elapsedRealtimeNanos() {
        return (System.currentTimeMillis() - bootTimeMillis) * 1000000;
    }
    
    /**
     * Get the current CPU time in milliseconds.
     * This time only increases when the CPU is running, and is not affected by deep sleep.
     * 
     * @return The CPU time in milliseconds
     */
    public static long uptimeMillis() {
        return System.currentTimeMillis() - bootTimeMillis;
    }
    
    /**
     * Set the boot time of the system.
     * This is used only for testing.
     * 
     * @param bootTimeMillis The boot time in milliseconds
     */
    public static void setBootTimeMillisForTest(long bootTimeMillis) {
        SystemClock.bootTimeMillis = bootTimeMillis;
    }
    
    /**
     * Sleep for the specified number of milliseconds.
     * This method is similar to Thread.sleep(), but it ignores InterruptedException.
     * 
     * @param ms The number of milliseconds to sleep
     */
    public static void sleep(long ms) {
        try {
            Thread.sleep(ms);
        } catch (InterruptedException e) {
            // Ignore
        }
    }
}