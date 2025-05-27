package utils;

import android.util.Log;

/**
 * Helper for Process
 * This class provides methods for working with Process objects
 */
public class ProcessHelper {
    private static final String TAG = "ProcessHelper";
    
    /**
     * Wait for a Process to complete
     * @param process The Process object
     * @return The exit value
     */
    public static int waitFor(java.lang.Process process) {
        if (process == null) {
            return -1;
        }
        
        try {
            return process.waitFor();
        } catch (Exception e) {
            Log.e(TAG, "Error waiting for process: " + e.getMessage());
            return -1;
        }
    }
    
    /**
     * Wait for a Process to complete with a timeout
     * @param process The Process object
     * @param timeout The timeout in milliseconds
     * @return The exit value, or -1 if timeout
     */
    public static int waitFor(java.lang.Process process, long timeout) {
        if (process == null) {
            return -1;
        }
        
        try {
            boolean completed = process.waitFor(timeout, java.util.concurrent.TimeUnit.MILLISECONDS);
            if (completed) {
                return process.exitValue();
            } else {
                return -1;
            }
        } catch (Exception e) {
            Log.e(TAG, "Error waiting for process with timeout: " + e.getMessage());
            return -1;
        }
    }
    
    /**
     * Get the exit value of a Process
     * @param process The Process object
     * @return The exit value
     */
    public static int getExitValue(java.lang.Process process) {
        if (process == null) {
            return -1;
        }
        
        try {
            return process.exitValue();
        } catch (Exception e) {
            Log.e(TAG, "Error getting exit value: " + e.getMessage());
            return -1;
        }
    }
    
    /**
     * Check if a Process is alive
     * @param process The Process object
     * @return True if the Process is alive, false otherwise
     */
    public static boolean isAlive(java.lang.Process process) {
        if (process == null) {
            return false;
        }
        
        try {
            return process.isAlive();
        } catch (Exception e) {
            Log.e(TAG, "Error checking if process is alive: " + e.getMessage());
            return false;
        }
    }
    
    /**
     * Destroy a Process
     * @param process The Process object
     */
    public static void destroy(java.lang.Process process) {
        if (process == null) {
            return;
        }
        
        try {
            process.destroy();
        } catch (Exception e) {
            Log.e(TAG, "Error destroying process: " + e.getMessage());
        }
    }
    
    /**
     * Destroy a Process forcibly
     * @param process The Process object
     */
    public static void destroyForcibly(java.lang.Process process) {
        if (process == null) {
            return;
        }
        
        try {
            process.destroyForcibly();
        } catch (Exception e) {
            Log.e(TAG, "Error forcibly destroying process: " + e.getMessage());
        }
    }
    
    /**
     * Get the input stream of a Process
     * @param process The Process object
     * @return The input stream
     */
    public static java.io.InputStream getInputStream(java.lang.Process process) {
        if (process == null) {
            return null;
        }
        
        try {
            return process.getInputStream();
        } catch (Exception e) {
            Log.e(TAG, "Error getting input stream: " + e.getMessage());
            return null;
        }
    }
    
    /**
     * Get the error stream of a Process
     * @param process The Process object
     * @return The error stream
     */
    public static java.io.InputStream getErrorStream(java.lang.Process process) {
        if (process == null) {
            return null;
        }
        
        try {
            return process.getErrorStream();
        } catch (Exception e) {
            Log.e(TAG, "Error getting error stream: " + e.getMessage());
            return null;
        }
    }
    
    /**
     * Get the output stream of a Process
     * @param process The Process object
     * @return The output stream
     */
    public static java.io.OutputStream getOutputStream(java.lang.Process process) {
        if (process == null) {
            return null;
        }
        
        try {
            return process.getOutputStream();
        } catch (Exception e) {
            Log.e(TAG, "Error getting output stream: " + e.getMessage());
            return null;
        }
    }
}