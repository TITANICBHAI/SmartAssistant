package utils;

/**
 * Mock implementation of the Android Log class
 */
public class Log {
    /**
     * Send a DEBUG log message.
     * 
     * @param tag Used to identify the source of a log message
     * @param msg The message you would like logged
     * @return The number of bytes written
     */
    public static int d(String tag, String msg) {
        System.out.println("DEBUG/" + tag + ": " + msg);
        return msg.length();
    }
    
    /**
     * Send a DEBUG log message and log the exception.
     * 
     * @param tag Used to identify the source of a log message
     * @param msg The message you would like logged
     * @param tr An exception to log
     * @return The number of bytes written
     */
    public static int d(String tag, String msg, Throwable tr) {
        System.out.println("DEBUG/" + tag + ": " + msg);
        tr.printStackTrace(System.out);
        return msg.length();
    }
    
    /**
     * Send an ERROR log message.
     * 
     * @param tag Used to identify the source of a log message
     * @param msg The message you would like logged
     * @return The number of bytes written
     */
    public static int e(String tag, String msg) {
        System.err.println("ERROR/" + tag + ": " + msg);
        return msg.length();
    }
    
    /**
     * Send an ERROR log message and log the exception.
     * 
     * @param tag Used to identify the source of a log message
     * @param msg The message you would like logged
     * @param tr An exception to log
     * @return The number of bytes written
     */
    public static int e(String tag, String msg, Throwable tr) {
        System.err.println("ERROR/" + tag + ": " + msg);
        tr.printStackTrace(System.err);
        return msg.length();
    }
    
    /**
     * Send an INFO log message.
     * 
     * @param tag Used to identify the source of a log message
     * @param msg The message you would like logged
     * @return The number of bytes written
     */
    public static int i(String tag, String msg) {
        System.out.println("INFO/" + tag + ": " + msg);
        return msg.length();
    }
    
    /**
     * Send an INFO log message and log the exception.
     * 
     * @param tag Used to identify the source of a log message
     * @param msg The message you would like logged
     * @param tr An exception to log
     * @return The number of bytes written
     */
    public static int i(String tag, String msg, Throwable tr) {
        System.out.println("INFO/" + tag + ": " + msg);
        tr.printStackTrace(System.out);
        return msg.length();
    }
    
    /**
     * Send a VERBOSE log message.
     * 
     * @param tag Used to identify the source of a log message
     * @param msg The message you would like logged
     * @return The number of bytes written
     */
    public static int v(String tag, String msg) {
        System.out.println("VERBOSE/" + tag + ": " + msg);
        return msg.length();
    }
    
    /**
     * Send a VERBOSE log message and log the exception.
     * 
     * @param tag Used to identify the source of a log message
     * @param msg The message you would like logged
     * @param tr An exception to log
     * @return The number of bytes written
     */
    public static int v(String tag, String msg, Throwable tr) {
        System.out.println("VERBOSE/" + tag + ": " + msg);
        tr.printStackTrace(System.out);
        return msg.length();
    }
    
    /**
     * Send a WARN log message.
     * 
     * @param tag Used to identify the source of a log message
     * @param msg The message you would like logged
     * @return The number of bytes written
     */
    public static int w(String tag, String msg) {
        System.out.println("WARN/" + tag + ": " + msg);
        return msg.length();
    }
    
    /**
     * Send a WARN log message and log the exception.
     * 
     * @param tag Used to identify the source of a log message
     * @param msg The message you would like logged
     * @param tr An exception to log
     * @return The number of bytes written
     */
    public static int w(String tag, String msg, Throwable tr) {
        System.out.println("WARN/" + tag + ": " + msg);
        tr.printStackTrace(System.out);
        return msg.length();
    }
}