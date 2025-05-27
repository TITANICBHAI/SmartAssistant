package com.aiassistant.rl;

import java.io.Serializable;
import java.util.Arrays;

/**
 * Observation vector for reinforcement learning
 * Represents the current state of the environment
 */
public class RLObservation implements Serializable {
    private static final long serialVersionUID = 1L;
    
    private float[] state;
    
    /**
     * Create observation with state vector
     */
    public RLObservation(float[] state) {
        this.state = state != null ? state.clone() : new float[0];
    }
    
    /**
     * Get state vector
     */
    public float[] getState() {
        return state.clone();
    }
    
    /**
     * Get state dimension
     */
    public int getDimension() {
        return state.length;
    }
    
    /**
     * Normalize state to [0, 1] range
     */
    public void normalize() {
        if (state.length == 0) return;
        
        // Find min and max
        float min = state[0];
        float max = state[0];
        
        for (float value : state) {
            min = Math.min(min, value);
            max = Math.max(max, value);
        }
        
        // Normalize if range is non-zero
        float range = max - min;
        if (range > 1e-6) {
            for (int i = 0; i < state.length; i++) {
                state[i] = (state[i] - min) / range;
            }
        } else {
            // If all values are the same, set to 0.5
            Arrays.fill(state, 0.5f);
        }
    }
    
    /**
     * Calculate distance to another observation
     */
    public float distanceTo(RLObservation other) {
        if (other == null || other.state.length != state.length) {
            return Float.MAX_VALUE;
        }
        
        float sumSquared = 0.0f;
        for (int i = 0; i < state.length; i++) {
            float diff = state[i] - other.state[i];
            sumSquared += diff * diff;
        }
        
        return (float) Math.sqrt(sumSquared);
    }
    
    @Override
    public String toString() {
        if (state.length <= 5) {
            return "RLObservation" + Arrays.toString(state);
        } else {
            // For large states, show only first 5 values
            return "RLObservation[" + 
                    state[0] + ", " + 
                    state[1] + ", " + 
                    state[2] + ", " + 
                    state[3] + ", " + 
                    state[4] + ", ... (" + 
                    state.length + " total)]";
        }
    }
}