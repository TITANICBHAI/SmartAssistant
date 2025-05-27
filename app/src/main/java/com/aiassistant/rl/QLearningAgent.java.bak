package com.aiassistant.rl;

import android.util.Log;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.Random;

/**
 * Q-Learning agent - tabular implementation optimized for mobile
 */
public class QLearningAgent extends RLAgent {
    private static final String TAG = "QLearningAgent";
    
    // Q-table using hash map for sparse storage
    private Map<String, float[]> qTable;
    
    // Random number generator
    private Random random;
    
    // Experience memory
    private List<Experience> experiences;
    private int maxMemorySize = 1000;
    
    // State discretization
    private int binsPerDimension = 10;
    
    /**
     * Experience tuple for memory replay
     */
    private static class Experience {
        public float[] state;
        public int action;
        public float reward;
        public float[] nextState;
        public boolean done;
        
        public Experience(float[] state, int action, float reward, float[] nextState, boolean done) {
            this.state = state;
            this.action = action;
            this.reward = reward;
            this.nextState = nextState;
            this.done = done;
        }
    }
    
    /**
     * Initialize Q-Learning agent
     */
    public QLearningAgent(int stateSize, int actionSize) {
        super(stateSize, actionSize);
        
        this.qTable = new HashMap<>();
        this.random = new Random();
        this.experiences = new ArrayList<>();
        
        // Higher exploration for Q-learning
        this.explorationRate = 0.2f;
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
        // Store experience
        storeExperience(state, action, reward, nextState, done);
        
        // Discretize states
        String stateKey = discretizeState(state);
        String nextStateKey = discretizeState(nextState);
        
        // Get Q-values for current state
        float[] qValues = getQValues(stateKey);
        
        // Get Q-values for next state
        float[] nextQValues = getQValues(nextStateKey);
        
        // Calculate maximum Q-value for next state
        float maxNextQ = 0.0f;
        for (float qValue : nextQValues) {
            maxNextQ = Math.max(maxNextQ, qValue);
        }
        
        // Calculate target Q-value
        float targetQ = reward;
        if (!done) {
            targetQ += discountFactor * maxNextQ;
        }
        
        // Update Q-value for current state-action pair
        qValues[action] += learningRate * (targetQ - qValues[action]);
        
        // Store updated Q-values
        qTable.put(stateKey, qValues);
        
        // Experience replay (batch learning)
        if (experiences.size() >= 10) {
            replayExperiences(5);
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
            
            ois.close();
            fis.close();
            
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
     * Store experience in replay memory
     */
    private void storeExperience(float[] state, int action, float reward, float[] nextState, boolean done) {
        // Create experience
        Experience experience = new Experience(
                state.clone(),
                action,
                reward,
                nextState.clone(),
                done
        );
        
        // Add to experiences
        experiences.add(experience);
        
        // If memory is full, remove oldest experiences
        while (experiences.size() > maxMemorySize) {
            experiences.remove(0);
        }
    }
    
    /**
     * Replay experiences for batch learning
     */
    private void replayExperiences(int batchSize) {
        // Make sure we have enough experiences
        if (experiences.size() < batchSize) {
            return;
        }
        
        // Sample random experiences
        for (int i = 0; i < batchSize; i++) {
            int index = random.nextInt(experiences.size());
            Experience exp = experiences.get(index);
            
            // Update Q-values using this experience
            update(exp.state, exp.action, exp.reward, exp.nextState, exp.done);
        }
    }
    
    /**
     * Set number of bins per dimension for state discretization
     */
    public void setBinsPerDimension(int bins) {
        this.binsPerDimension = Math.max(2, bins);
    }
    
    /**
     * Set maximum memory size
     */
    public void setMaxMemorySize(int size) {
        this.maxMemorySize = Math.max(10, size);
        
        // Trim memory if needed
        while (experiences.size() > maxMemorySize) {
            experiences.remove(0);
        }
    }
}