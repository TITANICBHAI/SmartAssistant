package com.aiassistant.rl;

import android.os.Build;
import android.util.Log;

import java.util.HashMap;
import java.util.Map;

/**
 * Selects appropriate RL algorithm based on device capabilities and requirements
 * Based on algorithm_selector.py
 */
public class AlgorithmSelector {
    private static final String TAG = "AlgorithmSelector";
    
    // Algorithm types
    public enum AlgorithmType {
        Q_LEARNING,
        SARSA,
        DQN,
        PPO,
        DYNA_Q
    }
    
    // Current device resources
    private boolean lowPowerMode;
    private float availableMemoryMB;
    private float batteryPct;
    
    // Algorithm requirements
    private Map<AlgorithmType, ResourceRequirements> algorithmRequirements;
    
    /**
     * Resource requirements for an algorithm
     */
    private static class ResourceRequirements {
        public float memoryRequiredMB;
        public float cpuRequiredGHz;
        public boolean requiresGPU;
        public float batteryDrainRating;  // 0-1 scale
        
        public ResourceRequirements(float memoryRequiredMB, float cpuRequiredGHz, 
                boolean requiresGPU, float batteryDrainRating) {
            this.memoryRequiredMB = memoryRequiredMB;
            this.cpuRequiredGHz = cpuRequiredGHz;
            this.requiresGPU = requiresGPU;
            this.batteryDrainRating = batteryDrainRating;
        }
    }
    
    /**
     * Initialize algorithm selector
     */
    public AlgorithmSelector() {
        this(false, 256.0f, 100.0f);
    }
    
    /**
     * Initialize algorithm selector with specific resource constraints
     */
    public AlgorithmSelector(boolean lowPowerMode, float availableMemoryMB, float batteryPct) {
        this.lowPowerMode = lowPowerMode;
        this.availableMemoryMB = availableMemoryMB;
        this.batteryPct = batteryPct;
        
        // Initialize algorithm requirements
        initializeRequirements();
        
        // Check device resources
        checkResources();
    }
    
    /**
     * Initialize algorithm requirements
     */
    private void initializeRequirements() {
        algorithmRequirements = new HashMap<>();
        
        // Q-Learning: Lightweight, tabular method
        algorithmRequirements.put(AlgorithmType.Q_LEARNING, 
                new ResourceRequirements(20.0f, 0.5f, false, 0.2f));
        
        // SARSA: Slightly more complex than Q-Learning
        algorithmRequirements.put(AlgorithmType.SARSA, 
                new ResourceRequirements(25.0f, 0.6f, false, 0.25f));
        
        // DQN: Deep learning, more resource intensive
        algorithmRequirements.put(AlgorithmType.DQN, 
                new ResourceRequirements(150.0f, 1.2f, true, 0.7f));
        
        // PPO: Most resource intensive
        algorithmRequirements.put(AlgorithmType.PPO, 
                new ResourceRequirements(200.0f, 1.5f, true, 0.8f));
        
        // Dyna-Q: Memory intensive but CPU moderate
        algorithmRequirements.put(AlgorithmType.DYNA_Q, 
                new ResourceRequirements(100.0f, 0.8f, false, 0.5f));
    }
    
    /**
     * Check device resources
     */
    private void checkResources() {
        Log.d(TAG, "Device state - Low power mode: " + lowPowerMode + 
                ", Available memory: " + availableMemoryMB + " MB" +
                ", Battery: " + batteryPct + "%");
    }
    
    /**
     * Get appropriate algorithm based on task and device state
     */
    public RLAgent getAlgorithm(AlgorithmType preferredType, int stateSize, int actionSize) {
        // Start with preferred algorithm
        AlgorithmType selectedType = preferredType;
        
        // Check if preferred algorithm is compatible with current resources
        if (!isCompatible(preferredType)) {
            // Fallback to most appropriate algorithm
            selectedType = findBestCompatibleAlgorithm();
        }
        
        // Create and return appropriate agent
        return createAgent(selectedType, stateSize, actionSize);
    }
    
    /**
     * Check if an algorithm is compatible with current resources
     */
    private boolean isCompatible(AlgorithmType type) {
        ResourceRequirements req = algorithmRequirements.get(type);
        if (req == null) return false;
        
        // Check memory requirement
        if (availableMemoryMB < req.memoryRequiredMB) {
            return false;
        }
        
        // Check battery requirement (if in low power mode)
        if (lowPowerMode && req.batteryDrainRating > 0.5f) {
            return false;
        }
        
        // Check GPU requirement
        if (req.requiresGPU && !hasGPUSupport()) {
            return false;
        }
        
        return true;
    }
    
    /**
     * Find best compatible algorithm
     */
    private AlgorithmType findBestCompatibleAlgorithm() {
        // In low power mode, prefer lightweight algorithms
        if (lowPowerMode || batteryPct < 20.0f) {
            if (isCompatible(AlgorithmType.Q_LEARNING)) {
                return AlgorithmType.Q_LEARNING;
            }
            if (isCompatible(AlgorithmType.SARSA)) {
                return AlgorithmType.SARSA;
            }
        }
        
        // Try algorithms in order of capability
        if (isCompatible(AlgorithmType.PPO)) {
            return AlgorithmType.PPO;
        }
        if (isCompatible(AlgorithmType.DQN)) {
            return AlgorithmType.DQN;
        }
        if (isCompatible(AlgorithmType.DYNA_Q)) {
            return AlgorithmType.DYNA_Q;
        }
        if (isCompatible(AlgorithmType.SARSA)) {
            return AlgorithmType.SARSA;
        }
        
        // Default to Q-Learning as most lightweight
        return AlgorithmType.Q_LEARNING;
    }
    
    /**
     * Create agent of specified type
     */
    private RLAgent createAgent(AlgorithmType type, int stateSize, int actionSize) {
        switch (type) {
            case Q_LEARNING:
                return new QLearningAgent(stateSize, actionSize);
                
            case SARSA:
                return new SARSAAgent(stateSize, actionSize);
                
            case DQN:
                return new DQNAgent(stateSize, actionSize);
                
            case PPO:
                return new PPOAgent(stateSize, actionSize);
                
            case DYNA_Q:
                // We don't have a DynaQ implementation, fall back to Q-Learning
                Log.d(TAG, "DynaQ requested but not implemented, using Q-Learning");
                return new QLearningAgent(stateSize, actionSize);
                
            default:
                Log.e(TAG, "Unknown algorithm type: " + type + ", using Q-Learning");
                return new QLearningAgent(stateSize, actionSize);
        }
    }
    
    /**
     * Check if device has GPU support
     */
    private boolean hasGPUSupport() {
        // This is a simplified check
        // A real implementation would check for specific GPU capabilities
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.N;
    }
    
    /**
     * Set low power mode
     */
    public void setLowPowerMode(boolean enabled) {
        this.lowPowerMode = enabled;
    }
    
    /**
     * Update available memory
     */
    public void setAvailableMemory(float memoryMB) {
        this.availableMemoryMB = Math.max(10.0f, memoryMB);
    }
    
    /**
     * Update battery percentage
     */
    public void setBatteryPercentage(float percentage) {
        this.batteryPct = Math.max(0.0f, Math.min(100.0f, percentage));
    }
    
    /**
     * Get compatible algorithms
     */
    public AlgorithmType[] getCompatibleAlgorithms() {
        int count = 0;
        AlgorithmType[] allTypes = AlgorithmType.values();
        boolean[] isCompatibleArray = new boolean[allTypes.length];
        
        for (int i = 0; i < allTypes.length; i++) {
            isCompatibleArray[i] = isCompatible(allTypes[i]);
            if (isCompatibleArray[i]) {
                count++;
            }
        }
        
        AlgorithmType[] result = new AlgorithmType[count];
        int index = 0;
        for (int i = 0; i < allTypes.length; i++) {
            if (isCompatibleArray[i]) {
                result[index++] = allTypes[i];
            }
        }
        
        return result;
    }
}