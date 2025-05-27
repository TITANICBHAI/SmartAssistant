package utils;

/**
 * Helper class for logging. Provides compatibility between Android Log and custom logging.
 */
public class LogHelper {
    
    /**
     * Log an error message with a tag and an exception.
     * 
     * @param tag The tag to use
     * @param message The message to log
     * @param throwable The exception to log
     */
    public static void e(String tag, String message, Throwable throwable) {
        // First log the message
        android.util.Log.e(tag, message);
        
        // Then log the exception separately
        if (throwable != null) {
            android.util.Log.e(tag, "Exception: " + throwable.getMessage());
        }
    }
    
    /**
     * Log a warning message with a tag and an exception.
     * 
     * @param tag The tag to use
     * @param message The message to log
     * @param throwable The exception to log
     */
    public static void w(String tag, String message, Throwable throwable) {
        // First log the message
        android.util.Log.w(tag, message);
        
        // Then log the exception separately
        if (throwable != null) {
            android.util.Log.w(tag, "Exception: " + throwable.getMessage());
        }
    }
    
    /**
     * Log an info message with a tag and an exception.
     * 
     * @param tag The tag to use
     * @param message The message to log
     * @param throwable The exception to log
     */
    public static void i(String tag, String message, Throwable throwable) {
        // First log the message
        android.util.Log.i(tag, message);
        
        // Then log the exception separately
        if (throwable != null) {
            android.util.Log.i(tag, "Exception: " + throwable.getMessage());
        }
    }
    
    /**
     * Log a debug message with a tag and an exception.
     * 
     * @param tag The tag to use
     * @param message The message to log
     * @param throwable The exception to log
     */
    public static void d(String tag, String message, Throwable throwable) {
        // First log the message
        android.util.Log.d(tag, message);
        
        // Then log the exception separately
        if (throwable != null) {
            android.util.Log.d(tag, "Exception: " + throwable.getMessage());
        }
    }
    
    /**
     * Log a verbose message with a tag and an exception.
     * 
     * @param tag The tag to use
     * @param message The message to log
     * @param throwable The exception to log
     */
    public static void v(String tag, String message, Throwable throwable) {
        // First log the message
        android.util.Log.v(tag, message);
        
        // Then log the exception separately
        if (throwable != null) {
            android.util.Log.v(tag, "Exception: " + throwable.getMessage());
        }
    }
    
    /**
     * Log an error message with a tag.
     * 
     * @param tag The tag to use
     * @param message The message to log
     */
    public static void e(String tag, String message) {
        android.util.Log.e(tag, message);
    }
    
    /**
     * Log a warning message with a tag.
     * 
     * @param tag The tag to use
     * @param message The message to log
     */
    public static void w(String tag, String message) {
        android.util.Log.w(tag, message);
    }
    
    /**
     * Log an info message with a tag.
     * 
     * @param tag The tag to use
     * @param message The message to log
     */
    public static void i(String tag, String message) {
        android.util.Log.i(tag, message);
    }
    
    /**
     * Log a debug message with a tag.
     * 
     * @param tag The tag to use
     * @param message The message to log
     */
    public static void d(String tag, String message) {
        android.util.Log.d(tag, message);
    }
    
    /**
     * Log a verbose message with a tag.
     * 
     * @param tag The tag to use
     * @param message The message to log
     */
    public static void v(String tag, String message) {
        android.util.Log.v(tag, message);
    }
}