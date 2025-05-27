package com.aiassistant.models;

/**
 * Action Type
 * Types of actions that can be performed by the AI assistant
 */
public enum ActionType {
    /**
     * Click on a UI element
     */
    CLICK,
    
    /**
     * Long press on a UI element
     */
    LONG_PRESS,
    
    /**
     * Enter text into a UI element
     */
    ENTER_TEXT,
    
    /**
     * Swipe the screen in a direction
     */
    SWIPE,
    
    /**
     * Scroll the screen up or down
     */
    SCROLL,
    
    /**
     * Wait for a specific time
     */
    WAIT,
    
    /**
     * Wait for a specific UI element to appear
     */
    WAIT_FOR_ELEMENT,
    
    /**
     * Take a screenshot
     */
    SCREENSHOT,
    
    /**
     * Launch an application
     */
    LAUNCH_APP,
    
    /**
     * Close an application
     */
    CLOSE_APP,
    
    /**
     * Press a hardware key (back, home, etc.)
     */
    PRESS_KEY,
    
    /**
     * Change a system setting
     */
    CHANGE_SETTING,
    
    /**
     * Show a notification
     */
    SHOW_NOTIFICATION,
    
    /**
     * Send an intent
     */
    SEND_INTENT,
    
    /**
     * Execute a shell command (requires root)
     */
    SHELL_COMMAND,
    
    /**
     * Custom action (defined by the user)
     */
    CUSTOM
}