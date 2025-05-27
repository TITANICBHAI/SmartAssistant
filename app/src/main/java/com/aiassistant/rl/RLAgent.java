package com.aiassistant.rl;

/**
 * Base class for reinforcement learning agents
 * Provides common interface for all RL algorithms
 */
public abstract class RLAgent {
    protected int stateSize;
    protected int actionSize;
    protected float explorationRate;
    protected float learningRate;
    protected float discountFactor;
    
    /**
     * Initialize agent
     */
    public RLAgent(int stateSize, int actionSize) {
        this.stateSize = stateSize;
        this.actionSize = actionSize;
        this.explorationRate = 0.1f;
        this.learningRate = 0.01f;
        this.discountFactor = 0.99f;
    }
    
    /**
     * Select an action based on state
     */
    public abstract int selectAction(float[] state);
    
    /**
     * Update the agent with a new experience
     */
    public abstract void update(float[] state, int action, float reward, float[] nextState, boolean done);
    
    /**
     * Get the top N actions for a state
     */
    public abstract int[] getTopActions(float[] state, int n);
    
    /**
     * Get probabilities for specific actions
     */
    public abstract float[] getActionProbabilities(float[] state, int[] actions);
    
    /**
     * Save the agent's model to a file
     */
    public abstract boolean saveModel(String filePath);
    
    /**
     * Load the agent's model from a file
     */
    public abstract boolean loadModel(String filePath);
    
    /**
     * Set exploration rate
     */
    public void setExplorationRate(float rate) {
        this.explorationRate = Math.max(0.0f, Math.min(1.0f, rate));
    }
    
    /**
     * Set learning rate
     */
    public void setLearningRate(float rate) {
        this.learningRate = Math.max(0.0001f, Math.min(1.0f, rate));
    }
    
    /**
     * Set discount factor
     */
    public void setDiscountFactor(float factor) {
        this.discountFactor = Math.max(0.0f, Math.min(1.0f, factor));
    }
    
    /**
     * Get exploration rate
     */
    public float getExplorationRate() {
        return explorationRate;
    }
    
    /**
     * Get learning rate
     */
    public float getLearningRate() {
        return learningRate;
    }
    
    /**
     * Get discount factor
     */
    public float getDiscountFactor() {
        return discountFactor;
    }
    
    /**
     * Get state size
     */
    public int getStateSize() {
        return stateSize;
    }
    
    /**
     * Get action size
     */
    public int getActionSize() {
        return actionSize;
    }
}