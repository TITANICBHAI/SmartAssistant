package com.aiassistant.models;

/**
 * Performance Mode
 * Defines the performance modes for the AI assistant
 */
public enum PerformanceMode {
    /**
     * Battery Saver mode - Reduce AI processing to save battery
     */
    BATTERY_SAVER,
    
    /**
     * Balanced mode - Balance performance and battery usage
     */
    BALANCED,
    
    /**
     * High Performance mode - Maximum AI processing with higher battery usage
     */
    HIGH_PERFORMANCE
}