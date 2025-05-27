package com.aiassistant.rl;

import android.util.Log;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;

/**
 * SARSA (State-Action-Reward-State-Action) agent
 * On-policy reinforcement learning optimized for mobile
 */
public class SARSAAgent extends RLAgent {
    private static final String TAG = "SARSAAgent";
    
    // Q-table using hash map for sparse storage
    private Map<String, float[]> qTable;
    
    // Random number generator
    private Random random;
    
    // Previous state-action
    private String prevStateKey;
    private int prevAction;
    private boolean hasPrevStateAction;
    
    // State discretization
    private int binsPerDimension = 10;
    
    // Eligibility traces (lambda-SARSA)
    private Map<String, float[]> eligibilityTraces;
    private float lambda = 0.9f;
    private boolean useLambda = true;
    
    /**
     * Initialize SARSA agent
     */
    public SARSAAgent(int stateSize, int actionSize) {
        super(stateSize, actionSize);
        
        this.qTable = new HashMap<>();
        this.eligibilityTraces = new HashMap<>();
        this.random = new Random();
        this.hasPrevStateAction = false;
        
        // Appropriate parameters for SARSA
        this.explorationRate = 0.15f;
        this.learningRate = 0.1f;
    }
    
    @Override
    public int selectAction(float[] state) {
        // Discretize state
        String stateKey = discretizeState(state);
        
        // With probability explorationRate, choose a random action (exploration)
        if (random.nextFloat() < explorationRate) {
            return random.nextInt(actionSize);
        }
        
        // Otherwise, choose the action with highest Q-value (exploitation)
        return getBestAction(stateKey);
    }
    
    @Override
    public void update(float[] state, int action, float reward, float[] nextState, boolean done) {
        // Discretize states
        String stateKey = discretizeState(state);
        String nextStateKey = null;
        
        // Only get next state if not done
        if (!done) {
            nextStateKey = discretizeState(nextState);
        }
        
        // Handle first update
        if (!hasPrevStateAction) {
            prevStateKey = stateKey;
            prevAction = action;
            hasPrevStateAction = true;
            return;
        }
        
        // Get Q-values for current state-action
        float[] qValues = getQValues(stateKey);
        
        // SARSA is on-policy, so we use the action that was actually taken
        float currentQ = qValues[action];
        
        // Calculate target Q-value
        float targetQ = reward;
        
        if (!done && nextStateKey != null) {
            // Get next action using current policy
            int nextAction = selectAction(nextState);
            
            // Get Q-values for next state
            float[] nextQValues = getQValues(nextStateKey);
            
            // SARSA uses the next action from policy
            targetQ += discountFactor * nextQValues[nextAction];
        }
        
        // Calculate TD error
        float tdError = targetQ - currentQ;
        
        if (useLambda) {
            // Update with eligibility traces (lambda-SARSA)
            updateWithEligibilityTraces(stateKey, action, tdError);
        } else {
            // Standard SARSA update
            qValues[action] += learningRate * tdError;
            qTable.put(stateKey, qValues);
        }
        
        // Save current state-action as previous for next update
        prevStateKey = stateKey;
        prevAction = action;
        
        // If episode ended, reset eligibility traces and previous state-action
        if (done) {
            eligibilityTraces.clear();
            hasPrevStateAction = false;
        }
    }
    
    @Override
    public int[] getTopActions(float[] state, int n) {
        // Discretize state
        String stateKey = discretizeState(state);
        
        // Get Q-values for state
        float[] qValues = getQValues(stateKey);
        
        // Create array of indices
        Integer[] indices = new Integer[actionSize];
        for (int i = 0; i < actionSize; i++) {
            indices[i] = i;
        }
        
        // Sort indices by Q-values (descending)
        Arrays.sort(indices, (a, b) -> Float.compare(qValues[b], qValues[a]));
        
        // Take top n actions
        n = Math.min(n, actionSize);
        int[] topActions = new int[n];
        for (int i = 0; i < n; i++) {
            topActions[i] = indices[i];
        }
        
        return topActions;
    }
    
    @Override
    public float[] getActionProbabilities(float[] state, int[] actions) {
        // Discretize state
        String stateKey = discretizeState(state);
        
        // Get Q-values for state
        float[] qValues = getQValues(stateKey);
        
        // Calculate softmax probabilities for the given actions
        float[] probabilities = new float[actions.length];
        float sum = 0.0f;
        
        // First pass: calculate exponentials
        for (int i = 0; i < actions.length; i++) {
            int action = actions[i];
            probabilities[i] = (float) Math.exp(qValues[action]);
            sum += probabilities[i];
        }
        
        // Second pass: normalize
        if (sum > 0) {
            for (int i = 0; i < probabilities.length; i++) {
                probabilities[i] /= sum;
            }
        } else {
            // If all values are very negative, use uniform distribution
            Arrays.fill(probabilities, 1.0f / actions.length);
        }
        
        return probabilities;
    }
    
    @Override
    public boolean saveModel(String filePath) {
        try {
            File file = new File(filePath);
            FileOutputStream fos = new FileOutputStream(file);
            ObjectOutputStream oos = new ObjectOutputStream(fos);
            
            oos.writeObject(qTable);
            oos.writeFloat(explorationRate);
            oos.writeFloat(learningRate);
            oos.writeFloat(discountFactor);
            oos.writeFloat(lambda);
            oos.writeBoolean(useLambda);
            oos.writeInt(binsPerDimension);
            
            oos.close();
            fos.close();
            
            Log.d(TAG, "Model saved to " + filePath);
            return true;
        } catch (Exception e) {
            Log.e(TAG, "Error saving model: " + e.getMessage());
            return false;
        }
    }
    
    @SuppressWarnings("unchecked")
    @Override
    public boolean loadModel(String filePath) {
        try {
            File file = new File(filePath);
            if (!file.exists()) {
                Log.e(TAG, "Model file does not exist: " + filePath);
                return false;
            }
            
            FileInputStream fis = new FileInputStream(file);
            ObjectInputStream ois = new ObjectInputStream(fis);
            
            qTable = (Map<String, float[]>) ois.readObject();
            explorationRate = ois.readFloat();
            learningRate = ois.readFloat();
            discountFactor = ois.readFloat();
            lambda = ois.readFloat();
            useLambda = ois.readBoolean();
            binsPerDimension = ois.readInt();
            
            ois.close();
            fis.close();
            
            // Reset traces and previous state
            eligibilityTraces.clear();
            hasPrevStateAction = false;
            
            Log.d(TAG, "Model loaded from " + filePath);
            return true;
        } catch (Exception e) {
            Log.e(TAG, "Error loading model: " + e.getMessage());
            return false;
        }
    }
    
    /**
     * Discretize a continuous state into a discrete state key
     */
    private String discretizeState(float[] state) {
        if (state == null || state.length == 0) {
            return "empty";
        }
        
        StringBuilder sb = new StringBuilder();
        
        for (int i = 0; i < state.length; i++) {
            // Discretize each dimension
            int bin = discretize(state[i], binsPerDimension);
            
            // Append to state key
            if (i > 0) {
                sb.append("_");
            }
            sb.append(bin);
        }
        
        return sb.toString();
    }
    
    /**
     * Discretize a single value into a bin
     */
    private int discretize(float value, int bins) {
        // Clip to [0, 1] range
        float clipped = Math.max(0.0f, Math.min(1.0f, value));
        
        // Map to bin
        int bin = (int) (clipped * bins);
        if (bin == bins) {
            bin--;  // Handle edge case
        }
        
        return bin;
    }
    
    /**
     * Get Q-values for a state
     */
    private float[] getQValues(String stateKey) {
        // Get existing Q-values or create new ones
        float[] qValues = qTable.get(stateKey);
        
        if (qValues == null) {
            // Initialize with zeros
            qValues = new float[actionSize];
            qTable.put(stateKey, qValues);
        }
        
        return qValues;
    }
    
    /**
     * Get eligibility traces for a state
     */
    private float[] getEligibilityTraces(String stateKey) {
        // Get existing traces or create new ones
        float[] traces = eligibilityTraces.get(stateKey);
        
        if (traces == null) {
            // Initialize with zeros
            traces = new float[actionSize];
            eligibilityTraces.put(stateKey, traces);
        }
        
        return traces;
    }
    
    /**
     * Update with eligibility traces (lambda-SARSA)
     */
    private void updateWithEligibilityTraces(String stateKey, int action, float tdError) {
        // Get eligibility traces for current state
        float[] traces = getEligibilityTraces(stateKey);
        
        // Set trace for current action to 1
        traces[action] = 1.0f;
        
        // Update all states and actions
        for (String s : qTable.keySet()) {
            float[] qValues = qTable.get(s);
            float[] stateTraces = eligibilityTraces.getOrDefault(s, null);
            
            if (stateTraces != null) {
                for (int a = 0; a < actionSize; a++) {
                    // Update Q-value using eligibility trace
                    qValues[a] += learningRate * tdError * stateTraces[a];
                    
                    // Decay eligibility trace
                    stateTraces[a] *= discountFactor * lambda;
                }
                
                // Update Q-table
                qTable.put(s, qValues);
            }
        }
        
        // Clean up small traces
        cleanupSmallTraces();
    }
    
    /**
     * Clean up small eligibility traces
     */
    private void cleanupSmallTraces() {
        // Remove traces that are too small
        float threshold = 0.01f;
        
        // First, collect keys to remove
        List<String> keysToRemove = new ArrayList<>();
        
        for (Map.Entry<String, float[]> entry : eligibilityTraces.entrySet()) {
            String key = entry.getKey();
            float[] traces = entry.getValue();
            
            boolean allSmall = true;
            for (float trace : traces) {
                if (Math.abs(trace) >= threshold) {
                    allSmall = false;
                    break;
                }
            }
            
            if (allSmall) {
                keysToRemove.add(key);
            }
        }
        
        // Then remove them
        for (String key : keysToRemove) {
            eligibilityTraces.remove(key);
        }
    }
    
    /**
     * Get best action for a state
     */
    private int getBestAction(String stateKey) {
        float[] qValues = getQValues(stateKey);
        
        // Find action with maximum Q-value
        int bestAction = 0;
        float maxQ = qValues[0];
        
        for (int i = 1; i < qValues.length; i++) {
            if (qValues[i] > maxQ) {
                maxQ = qValues[i];
                bestAction = i;
            }
        }
        
        return bestAction;
    }
    
    /**
     * Set lambda parameter for eligibility traces
     */
    public void setLambda(float lambda) {
        this.lambda = Math.max(0.0f, Math.min(1.0f, lambda));
    }
    
    /**
     * Enable or disable eligibility traces
     */
    public void setUseLambda(boolean useLambda) {
        this.useLambda = useLambda;
        
        // Clear traces if disabled
        if (!useLambda) {
            eligibilityTraces.clear();
        }
    }
    
    /**
     * Set number of bins per dimension for state discretization
     */
    public void setBinsPerDimension(int bins) {
        this.binsPerDimension = Math.max(2, bins);
    }
}