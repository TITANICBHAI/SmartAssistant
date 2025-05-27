package com.aiassistant.models;

/**
 * Enumeration of AI Assistant operating modes
 */
public enum AIMode {
    /**
     * Auto AI - Assistant takes full control after period of inactivity
     */
    AUTO_AI,
    
    /**
     * Copilot - Assistant suggests actions but waits for confirmation
     */
    COPILOT,
    
    /**
     * Passive - Assistant only observes and learns from user actions
     */
    PASSIVE
}