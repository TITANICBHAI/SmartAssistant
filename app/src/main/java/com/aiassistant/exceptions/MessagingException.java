package com.aiassistant.exceptions;

/**
 * Exception class for messaging-related errors
 */
public class MessagingException extends Exception {
    
    private int errorCode;
    
    /**
     * Error codes for messaging exceptions
     */
    public static final int ERROR_INVALID_ADDRESS = 1001;
    public static final int ERROR_CONNECTION_FAILED = 1002;
    public static final int ERROR_AUTHENTICATION_FAILED = 1003;
    public static final int ERROR_TIMEOUT = 1004;
    public static final int ERROR_MESSAGE_TOO_LARGE = 1005;
    public static final int ERROR_INVALID_CONTENT = 1006;
    public static final int ERROR_RATE_LIMIT = 1007;
    public static final int ERROR_UNKNOWN = 9999;
    
    /**
     * Constructor
     * 
     * @param message Error message
     */
    public MessagingException(String message) {
        super(message);
        this.errorCode = ERROR_UNKNOWN;
    }
    
    /**
     * Constructor with error code
     * 
     * @param message Error message
     * @param errorCode Error code
     */
    public MessagingException(String message, int errorCode) {
        super(message);
        this.errorCode = errorCode;
    }
    
    /**
     * Constructor with cause
     * 
     * @param message Error message
     * @param cause Root cause
     */
    public MessagingException(String message, Throwable cause) {
        super(message, cause);
        this.errorCode = ERROR_UNKNOWN;
    }
    
    /**
     * Constructor with error code and cause
     * 
     * @param message Error message
     * @param errorCode Error code
     * @param cause Root cause
     */
    public MessagingException(String message, int errorCode, Throwable cause) {
        super(message, cause);
        this.errorCode = errorCode;
    }
    
    /**
     * Get error code
     * 
     * @return Error code
     */
    public int getErrorCode() {
        return errorCode;
    }
    
    /**
     * Set error code
     * 
     * @param errorCode Error code
     */
    public void setErrorCode(int errorCode) {
        this.errorCode = errorCode;
    }
    
    /**
     * Get error name for code
     * 
     * @return Error name
     */
    public String getErrorName() {
        switch (errorCode) {
            case ERROR_INVALID_ADDRESS:
                return "INVALID_ADDRESS";
            case ERROR_CONNECTION_FAILED:
                return "CONNECTION_FAILED";
            case ERROR_AUTHENTICATION_FAILED:
                return "AUTHENTICATION_FAILED";
            case ERROR_TIMEOUT:
                return "TIMEOUT";
            case ERROR_MESSAGE_TOO_LARGE:
                return "MESSAGE_TOO_LARGE";
            case ERROR_INVALID_CONTENT:
                return "INVALID_CONTENT";
            case ERROR_RATE_LIMIT:
                return "RATE_LIMIT";
            case ERROR_UNKNOWN:
            default:
                return "UNKNOWN";
        }
    }
    
    @Override
    public String toString() {
        return "MessagingException{" +
                "message='" + getMessage() + '\'' +
                ", errorCode=" + errorCode +
                ", errorName='" + getErrorName() + '\'' +
                '}';
    }
}