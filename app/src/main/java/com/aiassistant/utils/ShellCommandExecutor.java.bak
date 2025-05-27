package com.aiassistant.utils;

import android.util.Log;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.concurrent.TimeUnit;

/**
 * Shell Command Executor
 * Utility for executing shell commands
 */
public class ShellCommandExecutor {
    private static final String TAG = "ShellCommandExecutor";
    
    // Default timeout in seconds
    private static final int DEFAULT_TIMEOUT = 10;
    
    // Process result
    private StringBuilder output;
    private StringBuilder error;
    
    /**
     * Execute a shell command
     */
    public int execute(String command) throws IOException, InterruptedException {
        return execute(command, DEFAULT_TIMEOUT);
    }
    
    /**
     * Execute a shell command with timeout
     */
    public int execute(String command, int timeoutSeconds) throws IOException, InterruptedException {
        Log.d(TAG, "Executing command: " + command);
        
        // Clear previous output
        output = new StringBuilder();
        error = new StringBuilder();
        
        // Start the process
        Process process = Runtime.getRuntime().exec(command);
        
        // Read output and error streams in separate threads
        StreamReader outputReader = new StreamReader(process.getInputStream(), output);
        StreamReader errorReader = new StreamReader(process.getErrorStream(), error);
        
        outputReader.start();
        errorReader.start();
        
        // Wait for the process to complete with timeout
        boolean completed = process.waitFor(timeoutSeconds, TimeUnit.SECONDS);
        
        // Wait for readers to finish
        outputReader.join();
        errorReader.join();
        
        // If the process didn't complete in time, force termination
        if (!completed) {
            process.destroyForcibly();
            Log.w(TAG, "Command timed out after " + timeoutSeconds + " seconds");
            return -1;
        }
        
        int exitValue = process.exitValue();
        Log.d(TAG, "Command completed with exit value: " + exitValue);
        
        return exitValue;
    }
    
    /**
     * Get command output
     */
    public String getOutput() {
        return output != null ? output.toString() : "";
    }
    
    /**
     * Get command error
     */
    public String getError() {
        return error != null ? error.toString() : "";
    }
    
    /**
     * Stream Reader
     * Helper class to read process output/error streams
     */
    private static class StreamReader extends Thread {
        private final BufferedReader reader;
        private final StringBuilder output;
        
        public StreamReader(java.io.InputStream stream, StringBuilder output) {
            this.reader = new BufferedReader(new InputStreamReader(stream));
            this.output = output;
        }
        
        @Override
        public void run() {
            try {
                String line;
                while ((line = reader.readLine()) != null) {
                    output.append(line).append("\n");
                }
            } catch (IOException e) {
                Log.e(TAG, "Error reading stream", e);
            } finally {
                try {
                    reader.close();
                } catch (IOException e) {
                    Log.e(TAG, "Error closing reader", e);
                }
            }
        }
    }
}