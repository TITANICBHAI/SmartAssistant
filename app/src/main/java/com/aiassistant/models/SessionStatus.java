package com.aiassistant.models;

/**
 * Session Status
 * Status of learning sessions
 */
public enum SessionStatus {
    /**
     * Session is currently running
     */
    RUNNING,
    
    /**
     * Session is paused
     */
    PAUSED,
    
    /**
     * Session is completed successfully
     */
    COMPLETED,
    
    /**
     * Session failed
     */
    FAILED,
    
    /**
     * Session is scheduled (not yet started)
     */
    SCHEDULED
}