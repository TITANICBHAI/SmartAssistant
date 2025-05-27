package android.util;

/**
 * Mock implementation of Android Log class for development outside of Android.
 */
public class Log {
    // Log levels
    public static final int VERBOSE = 2;
    public static final int DEBUG = 3;
    public static final int INFO = 4;
    public static final int WARN = 5;
    public static final int ERROR = 6;
    public static final int ASSERT = 7;
    
    private static int logLevel = VERBOSE;
    
    /**
     * Set the current log level
     * 
     * @param level The log level
     */
    public static void setLogLevel(int level) {
        logLevel = level;
    }
    
    /**
     * Get the current log level
     * 
     * @return The log level
     */
    public static int getLogLevel() {
        return logLevel;
    }
    
    /**
     * Send a VERBOSE log message
     * 
     * @param tag Used to identify the source of a log message
     * @param msg The message you would like logged
     * @return The number of bytes written
     */
    public static int v(String tag, String msg) {
        return println(VERBOSE, tag, msg);
    }
    
    /**
     * Send a VERBOSE log message and log the exception
     * 
     * @param tag Used to identify the source of a log message
     * @param msg The message you would like logged
     * @param tr An exception to log
     * @return The number of bytes written
     */
    public static int v(String tag, String msg, Throwable tr) {
        return println(VERBOSE, tag, msg + '\n' + getStackTraceString(tr));
    }
    
    /**
     * Send a DEBUG log message
     * 
     * @param tag Used to identify the source of a log message
     * @param msg The message you would like logged
     * @return The number of bytes written
     */
    public static int d(String tag, String msg) {
        return println(DEBUG, tag, msg);
    }
    
    /**
     * Send a DEBUG log message and log the exception
     * 
     * @param tag Used to identify the source of a log message
     * @param msg The message you would like logged
     * @param tr An exception to log
     * @return The number of bytes written
     */
    public static int d(String tag, String msg, Throwable tr) {
        return println(DEBUG, tag, msg + '\n' + getStackTraceString(tr));
    }
    
    /**
     * Send an INFO log message
     * 
     * @param tag Used to identify the source of a log message
     * @param msg The message you would like logged
     * @return The number of bytes written
     */
    public static int i(String tag, String msg) {
        return println(INFO, tag, msg);
    }
    
    /**
     * Send an INFO log message and log the exception
     * 
     * @param tag Used to identify the source of a log message
     * @param msg The message you would like logged
     * @param tr An exception to log
     * @return The number of bytes written
     */
    public static int i(String tag, String msg, Throwable tr) {
        return println(INFO, tag, msg + '\n' + getStackTraceString(tr));
    }
    
    /**
     * Send a WARN log message
     * 
     * @param tag Used to identify the source of a log message
     * @param msg The message you would like logged
     * @return The number of bytes written
     */
    public static int w(String tag, String msg) {
        return println(WARN, tag, msg);
    }
    
    /**
     * Send a WARN log message and log the exception
     * 
     * @param tag Used to identify the source of a log message
     * @param msg The message you would like logged
     * @param tr An exception to log
     * @return The number of bytes written
     */
    public static int w(String tag, String msg, Throwable tr) {
        return println(WARN, tag, msg + '\n' + getStackTraceString(tr));
    }
    
    /**
     * Send a WARN log message and log the exception
     * 
     * @param tag Used to identify the source of a log message
     * @param tr An exception to log
     * @return The number of bytes written
     */
    public static int w(String tag, Throwable tr) {
        return println(WARN, tag, getStackTraceString(tr));
    }
    
    /**
     * Send an ERROR log message
     * 
     * @param tag Used to identify the source of a log message
     * @param msg The message you would like logged
     * @return The number of bytes written
     */
    public static int e(String tag, String msg) {
        return println(ERROR, tag, msg);
    }
    
    /**
     * Send an ERROR log message and log the exception
     * 
     * @param tag Used to identify the source of a log message
     * @param msg The message you would like logged
     * @param tr An exception to log
     * @return The number of bytes written
     */
    public static int e(String tag, String msg, Throwable tr) {
        return println(ERROR, tag, msg + '\n' + getStackTraceString(tr));
    }
    
    /**
     * Handy function to get a loggable stack trace from a Throwable
     * 
     * @param tr An exception to log
     * @return String representation of the stack trace
     */
    public static String getStackTraceString(Throwable tr) {
        if (tr == null) {
            return "";
        }
        
        StringBuilder sb = new StringBuilder();
        sb.append(tr.toString());
        sb.append("\n");
        
        for (StackTraceElement element : tr.getStackTrace()) {
            sb.append("\tat ");
            sb.append(element.toString());
            sb.append("\n");
        }
        
        Throwable cause = tr.getCause();
        if (cause != null) {
            sb.append("Caused by: ");
            sb.append(getStackTraceString(cause));
        }
        
        return sb.toString();
    }
    
    /**
     * Low-level logging call
     * 
     * @param priority The priority/type of this log message
     * @param tag Used to identify the source of a log message
     * @param msg The message you would like logged
     * @return The number of bytes written
     */
    public static int println(int priority, String tag, String msg) {
        if (priority >= logLevel) {
            String levelStr;
            switch (priority) {
                case VERBOSE:
                    levelStr = "V";
                    break;
                case DEBUG:
                    levelStr = "D";
                    break;
                case INFO:
                    levelStr = "I";
                    break;
                case WARN:
                    levelStr = "W";
                    break;
                case ERROR:
                    levelStr = "E";
                    break;
                case ASSERT:
                    levelStr = "A";
                    break;
                default:
                    levelStr = "?";
                    break;
            }
            
            String logLine = levelStr + "/" + tag + ": " + msg;
            System.out.println(logLine);
            return logLine.length();
        }
        return 0;
    }
}