package models;

import android.content.Context;
import android.graphics.Bitmap;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Deep Reinforcement Learning model for game automation.
 */
public class DeepRLModel {
    private static final String TAG = "DeepRLModel";
    private static final int STATE_VECTOR_SIZE = 32; // Size of the feature vector for state representation
    
    private static DeepRLModel instance;
    private Context context;
    private boolean isInitialized;
    private boolean isActive;
    private String gameType;
    private Map<String, Double> valueFunctions;
    private Map<String, Map<String, Double>> qValues;
    private int actionCount = 8; // Number of possible actions
    private double learningRate = 0.1; // Alpha value for learning
    private double discountFactor = 0.9; // Gamma value for discounting future rewards
    
    /**
     * Private constructor for singleton pattern
     */
    private DeepRLModel(Context context) {
        this.context = context;
        this.isInitialized = true;
        this.isActive = false;
        this.gameType = "unknown";
        this.valueFunctions = new HashMap<>();
        this.qValues = new HashMap<>();
    }
    
    /**
     * Get the singleton instance
     * 
     * @param context Application context
     * @return DeepRLModel instance
     */
    public static synchronized DeepRLModel getInstance(Context context) {
        if (instance == null) {
            instance = new DeepRLModel(context);
        }
        return instance;
    }
    
    /**
     * Get the singleton instance (no-arg version for backward compatibility)
     * 
     * @return DeepRLModel instance
     * @throws IllegalStateException if getInstance(Context) hasn't been called first
     */
    public static synchronized DeepRLModel getInstance() {
        if (instance == null) {
            throw new IllegalStateException("DeepRLModel not initialized. Call getInstance(Context) first.");
        }
        return instance;
    }
    
    /**
     * Start the model
     */
    public void start() {
        if (!isActive) {
            isActive = true;
        }
    }
    
    /**
     * Stop the model
     */
    public void stop() {
        if (isActive) {
            isActive = false;
        }
    }
    
    /**
     * Set the game type for model specialization
     * 
     * @param gameType Type of game
     */
    public void setGameType(String gameType) {
        if (gameType != null && !gameType.isEmpty()) {
            this.gameType = gameType;
        }
    }
    
    /**
     * Process an image to extract features
     * 
     * @param image Screenshot or image to process
     * @return Map of features
     */
    public Map<String, Object> processImage(Bitmap image) {
        if (!isActive || image == null) {
            return new HashMap<>();
        }
        
        // In a real implementation, this would run feature extraction on the image
        // For this mock implementation, we'll just return some simulated features
        
        Map<String, Object> features = new HashMap<>();
        
        // Basic image properties
        features.put("width", image.getWidth());
        features.put("height", image.getHeight());
        
        // Game-specific features (simulated)
        features.put("player_position_x", 0.5);  // Normalized position (0-1)
        features.put("player_position_y", 0.7);
        features.put("score", 1250);
        features.put("lives", 3);
        features.put("level", 5);
        features.put("enemies_count", 4);
        features.put("nearest_enemy_distance", 0.3);
        features.put("nearest_item_distance", 0.6);
        features.put("health_percentage", 0.8);
        
        return features;
    }
    
    /**
     * Process a game state to get action recommendations
     * 
     * @param state Current game state
     * @return List of recommended actions
     */
    public List<ActionRecommendation> processState(Map<String, Object> state) {
        if (!isActive || state == null) {
            return new ArrayList<>();
        }
        
        // In a real implementation, this would use the RL model to evaluate actions
        // For this mock implementation, we'll just return some simulated recommendations
        
        List<ActionRecommendation> recommendations = new ArrayList<>();
        
        // Calculate state hash for lookup
        String stateHash = calculateStateHash(state);
        
        // If we don't have Q-values for this state, create some
        if (!qValues.containsKey(stateHash)) {
            createInitialQValues(stateHash);
        }
        
        // Get the Q-values for this state
        Map<String, Double> actionValues = qValues.get(stateHash);
        
        // Convert to recommendations
        for (Map.Entry<String, Double> entry : actionValues.entrySet()) {
            String action = entry.getKey();
            double value = entry.getValue();
            
            // Convert value to confidence (normalized to 0-1)
            double confidence = normalizeValue(value);
            
            // Generate reasoning
            String reasoning = generateReasoning(action, state);
            
            recommendations.add(new ActionRecommendation(action, confidence, reasoning));
        }
        
        // Sort by confidence
        recommendations.sort((a, b) -> Double.compare(b.getConfidence(), a.getConfidence()));
        
        return recommendations;
    }
    
    /**
     * Update the model based on observed rewards
     * 
     * @param state Previous state
     * @param action Action taken
     * @param reward Reward received
     * @param newState New state after action
     */
    public void updateModel(Map<String, Object> state, String action, 
                        double reward, Map<String, Object> newState) {
        if (!isActive || state == null || action == null || newState == null) {
            return;
        }
        
        // In a real implementation, this would update the RL model
        // For this mock implementation, we'll just update our simulated Q-values
        
        // Calculate state hashes
        String stateHash = calculateStateHash(state);
        String newStateHash = calculateStateHash(newState);
        
        // Ensure we have Q-values for both states
        if (!qValues.containsKey(stateHash)) {
            createInitialQValues(stateHash);
        }
        
        if (!qValues.containsKey(newStateHash)) {
            createInitialQValues(newStateHash);
        }
        
        // Get the Q-values
        Map<String, Double> stateQValues = qValues.get(stateHash);
        Map<String, Double> newStateQValues = qValues.get(newStateHash);
        
        // Get the current Q-value for the action
        double currentQ = stateQValues.getOrDefault(action, 0.0);
        
        // Find the max Q-value for the new state
        double maxNextQ = 0.0;
        for (double value : newStateQValues.values()) {
            maxNextQ = Math.max(maxNextQ, value);
        }
        
        // Update the Q-value using Q-learning update rule
        // Q(s,a) = Q(s,a) + alpha * (reward + gamma * max(Q(s',a')) - Q(s,a))
        // where alpha is learning rate and gamma is discount factor
        double alpha = 0.1;  // Learning rate
        double gamma = 0.9;  // Discount factor
        
        double newQ = currentQ + alpha * (reward + gamma * maxNextQ - currentQ);
        
        // Update the Q-value
        stateQValues.put(action, newQ);
        
        // Also update the value function for the state
        updateValueFunction(stateHash, stateQValues);
    }
    
    /**
     * Select an action based on the state vector
     * 
     * @param stateVector State vector (can be a Map for flexibility)
     * @return Selected action index
     */
    public int selectAction(Map<String, Object> stateVector) {
        if (!isActive || stateVector == null) {
            return 0; // Default action index
        }
        
        // Process the state to get recommendations
        List<ActionRecommendation> recommendations = processState(stateVector);
        
        if (recommendations.isEmpty()) {
            return 0; // Default action index
        }
        
        // Find the highest confidence action
        ActionRecommendation bestAction = recommendations.get(0);
        
        // Map the action to an index (simplified for this implementation)
        // In a real system, this would be more sophisticated
        String actionStr = bestAction.getAction();
        
        // Very simple action mapping
        if (actionStr.contains("left")) {
            return 1;
        } else if (actionStr.contains("right")) {
            return 2;
        } else if (actionStr.contains("up")) {
            return 3;
        } else if (actionStr.contains("down")) {
            return 4;
        } else if (actionStr.contains("jump")) {
            return 5;
        } else if (actionStr.contains("attack")) {
            return 6;
        } else if (actionStr.contains("use")) {
            return 7;
        } else {
            return 0; // Default/no-op action
        }
    }
    
    /**
     * Select an action based on state feature vector
     * 
     * @param features State features as float array
     * @return Selected action index
     */
    public int selectAction(float[] features) {
        if (!isActive || features == null) {
            return 0; // Default action index
        }
        
        // Convert feature vector to state map
        Map<String, Object> stateVector = new HashMap<>();
        for (int i = 0; i < features.length; i++) {
            stateVector.put("feature_" + i, features[i]);
        }
        
        // Process the state to get recommendations
        List<ActionRecommendation> recommendations = processState(stateVector);
        
        if (recommendations.isEmpty()) {
            return 0; // Default action index
        }
        
        // Find the highest confidence action
        ActionRecommendation bestAction = recommendations.get(0);
        
        // Map the action to an index (simplified for this implementation)
        // In a real system, this would be more sophisticated
        String actionStr = bestAction.getAction();
        
        // Very simple action mapping
        if (actionStr.contains("left")) {
            return 1;
        } else if (actionStr.contains("right")) {
            return 2;
        } else if (actionStr.contains("up")) {
            return 3;
        } else if (actionStr.contains("down")) {
            return 4;
        } else if (actionStr.contains("jump")) {
            return 5;
        } else if (actionStr.contains("attack")) {
            return 6;
        } else if (actionStr.contains("use")) {
            return 7;
        } else {
            return 0; // Default/no-op action
        }
    }
    
    /**
     * Selects an action based on state features as float array and returns ActionRecommendation
     * 
     * @param features State features as float array
     * @return ActionRecommendation containing the recommended action
     */
    public ActionRecommendation selectAction(float[] features, boolean returnRecommendation) {
        if (!isActive || features == null) {
            return new ActionRecommendation("none", 0.0, "Model inactive or null features");
        }
        
        // Convert feature vector to state map
        Map<String, Object> stateVector = new HashMap<>();
        for (int i = 0; i < features.length; i++) {
            stateVector.put("feature_" + i, features[i]);
        }
        
        // Process the state to get recommendations
        List<ActionRecommendation> recommendations = processState(stateVector);
        
        if (recommendations.isEmpty()) {
            return new ActionRecommendation("none", 0.0, "No recommendations available");
        }
        
        // Return the highest confidence action recommendation
        return recommendations.get(0);
    }
    
    /**
     * Convenience method that always returns ActionRecommendation
     * 
     * @param features State features as float array
     * @return ActionRecommendation containing the recommended action
     */
    public ActionRecommendation getActionRecommendation(float[] features) {
        return selectAction(features, true);
    }
    
    /**
     * Process a state vector to get action recommendations
     * This method handles float[] input directly for compatibility with systems using float arrays
     * 
     * @param stateVector State features as float array
     * @return List of recommended actions
     */
    public List<ActionRecommendation> processStateVector(float[] stateVector) {
        if (!isActive || stateVector == null) {
            return new ArrayList<>();
        }
        
        // Convert float array to map for processing
        Map<String, Object> stateMap = new HashMap<>();
        for (int i = 0; i < stateVector.length; i++) {
            stateMap.put("feature_" + i, stateVector[i]);
        }
        
        // Use existing processState method
        return processState(stateMap);
    }
    
    /**
     * Check if this model can directly process maps instead of requiring float arrays
     * Used by helper classes to determine the correct processing method
     * 
     * @return True if the model can process maps directly
     */
    public boolean canProcessMap() {
        // In this implementation, we can process maps directly
        return true;
    }
    
    /**
     * Update the value function for a state
     * 
     * @param stateHash State hash
     * @param stateQValues Q-values for the state
     */
    private void updateValueFunction(String stateHash, Map<String, Double> stateQValues) {
        if (stateQValues.isEmpty()) {
            valueFunctions.put(stateHash, 0.0);
            return;
        }
        
        // Value function is the max Q-value
        double maxQ = Double.NEGATIVE_INFINITY;
        for (double value : stateQValues.values()) {
            maxQ = Math.max(maxQ, value);
        }
        
        valueFunctions.put(stateHash, maxQ);
    }
    
    /**
     * Calculate a hash for a state
     * 
     * @param state State to hash
     * @return Hash string
     */
    private String calculateStateHash(Map<String, Object> state) {
        if (state.isEmpty()) {
            return "empty_state";
        }
        
        // Very simplified hash function - in a real implementation
        // this would be more sophisticated
        StringBuilder sb = new StringBuilder();
        
        // Add a subset of important features to the hash
        if (state.containsKey("player_position_x") && state.containsKey("player_position_y")) {
            double x = getDoubleValue(state, "player_position_x");
            double y = getDoubleValue(state, "player_position_y");
            
            // Discretize positions to reduce state space
            int xBin = (int) (x * 10);
            int yBin = (int) (y * 10);
            
            sb.append("p").append(xBin).append("_").append(yBin);
        }
        
        if (state.containsKey("level")) {
            sb.append("_l").append(state.get("level"));
        }
        
        if (state.containsKey("lives")) {
            sb.append("_lives").append(state.get("lives"));
        }
        
        if (state.containsKey("enemies_count")) {
            sb.append("_e").append(state.get("enemies_count"));
        }
        
        if (state.containsKey("health_percentage")) {
            double health = getDoubleValue(state, "health_percentage");
            int healthBin = (int) (health * 10);
            sb.append("_h").append(healthBin);
        }
        
        String hash = sb.toString();
        return hash.isEmpty() ? "default_state" : hash;
    }
    
    /**
     * Create initial Q-values for a state
     * 
     * @param stateHash State hash
     */
    private void createInitialQValues(String stateHash) {
        Map<String, Double> initialValues = new HashMap<>();
        
        // Add actions based on game type
        if (gameType.toLowerCase().contains("action") || 
            gameType.toLowerCase().contains("arcade")) {
            // Action game controls
            initialValues.put("move_left", 0.2);
            initialValues.put("move_right", 0.2);
            initialValues.put("move_up", 0.2);
            initialValues.put("move_down", 0.2);
            initialValues.put("jump", 0.3);
            initialValues.put("attack", 0.4);
            initialValues.put("use_item", 0.1);
        } else if (gameType.toLowerCase().contains("strategy") || 
                  gameType.toLowerCase().contains("puzzle")) {
            // Strategy/puzzle game controls
            initialValues.put("select", 0.3);
            initialValues.put("place", 0.3);
            initialValues.put("rotate", 0.2);
            initialValues.put("combine", 0.2);
            initialValues.put("analyze", 0.4);
        } else if (gameType.toLowerCase().contains("rpg")) {
            // RPG game controls
            initialValues.put("talk", 0.3);
            initialValues.put("examine", 0.3);
            initialValues.put("open_inventory", 0.2);
            initialValues.put("equip_item", 0.2);
            initialValues.put("use_skill", 0.3);
        } else {
            // Default controls for unknown game types
            initialValues.put("tap_center", 0.3);
            initialValues.put("swipe_up", 0.2);
            initialValues.put("swipe_down", 0.2);
            initialValues.put("swipe_left", 0.2);
            initialValues.put("swipe_right", 0.2);
            initialValues.put("wait", 0.1);
            initialValues.put("explore", 0.3);
        }
        
        qValues.put(stateHash, initialValues);
    }
    
    /**
     * Generate reasoning for an action
     * 
     * @param action Action to explain
     * @param state Current state
     * @return Reasoning string
     */
    private String generateReasoning(String action, Map<String, Object> state) {
        // In a real implementation, this would use the model to explain the action
        // For this mock implementation, we'll just return some templated reasoning
        
        StringBuilder reasoning = new StringBuilder();
        
        if (action.contains("move") || action.contains("swipe")) {
            // Movement actions
            reasoning.append("Moving ");
            
            if (action.contains("left")) {
                reasoning.append("left ");
                
                if (state.containsKey("nearest_enemy_distance")) {
                    double distance = getDoubleValue(state, "nearest_enemy_distance");
                    if (distance < 0.3) {
                        reasoning.append("to avoid nearby enemy");
                    } else {
                        reasoning.append("to explore the area");
                    }
                }
            } else if (action.contains("right")) {
                reasoning.append("right ");
                
                if (state.containsKey("nearest_item_distance")) {
                    double distance = getDoubleValue(state, "nearest_item_distance");
                    if (distance < 0.4) {
                        reasoning.append("towards nearby item");
                    } else {
                        reasoning.append("to progress in the level");
                    }
                }
            } else if (action.contains("up")) {
                reasoning.append("up ");
                reasoning.append("to reach higher platform");
            } else if (action.contains("down")) {
                reasoning.append("down ");
                reasoning.append("to avoid overhead obstacles");
            }
        } else if (action.equals("jump")) {
            reasoning.append("Jumping to avoid obstacle or reach platform");
        } else if (action.equals("attack")) {
            if (state.containsKey("enemies_count")) {
                int enemies = getIntValue(state, "enemies_count");
                if (enemies > 0) {
                    reasoning.append("Attacking nearby enemy");
                    if (enemies > 1) {
                        reasoning.append("(").append(enemies).append(" enemies nearby)");
                    }
                } else {
                    reasoning.append("Preemptive attack in case of unseen threats");
                }
            } else {
                reasoning.append("Attacking to clear the path");
            }
        } else if (action.contains("item")) {
            if (state.containsKey("health_percentage")) {
                double health = getDoubleValue(state, "health_percentage");
                if (health < 0.5) {
                    reasoning.append("Using item to restore health (currently at ").append((int)(health*100)).append("%)");
                } else {
                    reasoning.append("Using item for strategic advantage");
                }
            } else {
                reasoning.append("Using item at optimal moment");
            }
        } else if (action.equals("analyze")) {
            reasoning.append("Analyzing the current situation before acting");
        } else if (action.equals("explore")) {
            reasoning.append("Exploring to discover game mechanics and opportunities");
        } else if (action.equals("wait")) {
            reasoning.append("Waiting for better opportunity or timing");
        } else {
            reasoning.append("Executing ").append(action).append(" based on learned patterns");
        }
        
        return reasoning.toString();
    }
    
    /**
     * Normalize a Q-value to a confidence score (0-1)
     * 
     * @param value Q-value
     * @return Normalized confidence
     */
    private double normalizeValue(double value) {
        // Simple normalization function
        // In a real implementation, this would be more sophisticated
        
        // Sigmoid function to map any value to 0-1 range
        return 1.0 / (1.0 + Math.exp(-value));
    }
    
    /**
     * Helper to get double value from state
     * 
     * @param state State map
     * @param key Key to get
     * @return Double value or 0.0 if not found
     */
    private double getDoubleValue(Map<String, Object> state, String key) {
        if (!state.containsKey(key)) {
            return 0.0;
        }
        
        Object value = state.get(key);
        if (value instanceof Number) {
            return ((Number) value).doubleValue();
        }
        
        try {
            return Double.parseDouble(value.toString());
        } catch (Exception e) {
            return 0.0;
        }
    }
    
    /**
     * Helper to get int value from state
     * 
     * @param state State map
     * @param key Key to get
     * @return Int value or 0 if not found
     */
    private int getIntValue(Map<String, Object> state, String key) {
        if (!state.containsKey(key)) {
            return 0;
        }
        
        Object value = state.get(key);
        if (value instanceof Number) {
            return ((Number) value).intValue();
        }
        
        try {
            return Integer.parseInt(value.toString());
        } catch (Exception e) {
            return 0;
        }
    }
    
    /**
     * Class representing an action recommendation.
     */
    public static class ActionRecommendation {
        private String action;
        private double confidence;
        private String reasoning;
        
        /**
         * Constructor
         * 
         * @param action Action to take
         * @param confidence Confidence in recommendation (0-1)
         * @param reasoning Reasoning behind recommendation
         */
        public ActionRecommendation(String action, double confidence, String reasoning) {
            this.action = action;
            this.confidence = confidence;
            this.reasoning = reasoning;
        }
        
        /**
         * Get the action
         */
        public String getAction() {
            return action;
        }
        
        /**
         * Get the confidence
         */
        public double getConfidence() {
            return confidence;
        }
        
        /**
         * Get the reasoning
         */
        public String getReasoning() {
            return reasoning;
        }
        
        @Override
        public String toString() {
            return action + " (" + String.format("%.2f", confidence) + "): " + reasoning;
        }
    }
    
    /**
     * Get model information
     * 
     * @return Map containing model information
     */
    public Map<String, Object> getModelInfo() {
        Map<String, Object> info = new HashMap<>();
        info.put("model_type", "DeepRL");
        info.put("actions_count", actionCount);
        info.put("trained_states", qValues.size());
        info.put("game_type", gameType);
        info.put("active", isActive);
        info.put("learning_rate", learningRate);
        info.put("discount_factor", discountFactor);
        
        return info;
    }
    
    /**
     * Convert Map<String, Object> to float[] feature vector
     * 
     * @param stateFeatures Map containing state features
     * @return float[] representation of features
     */
    public float[] convertMapToFeatureVector(Map<String, Object> stateFeatures) {
        float[] featureVector = new float[STATE_VECTOR_SIZE];
        
        // Fill default values (0.0f)
        Arrays.fill(featureVector, 0.0f);
        
        // Convert numeric values from map to appropriate positions in feature vector
        int index = 0;
        for (Map.Entry<String, Object> entry : stateFeatures.entrySet()) {
            if (entry.getValue() instanceof Number) {
                if (index < featureVector.length) {
                    featureVector[index++] = ((Number)entry.getValue()).floatValue();
                }
            } else if (entry.getValue() instanceof Boolean) {
                if (index < featureVector.length) {
                    featureVector[index++] = ((Boolean)entry.getValue()) ? 1.0f : 0.0f;
                }
            }
        }
        
        return featureVector;
    }
    
    /**
     * Convert a feature vector back to a map representation
     * This is useful for interfacing with other components that expect Map<String, Object>
     * 
     * @param featureVector The feature vector to convert
     * @return A map representation of the feature vector
     */
    public Map<String, Object> convertFeatureVectorToMap(float[] featureVector) {
        Map<String, Object> stateMap = new HashMap<>();
        
        if (featureVector == null) {
            return stateMap;
        }
        
        // Convert each feature to a named entry in the map
        // Since we don't have original feature names, we'll use generic names
        for (int i = 0; i < featureVector.length; i++) {
            stateMap.put("feature_" + i, featureVector[i]);
        }
        
        return stateMap;
    }
    
    /**
     * Get action recommendation from a map of state features
     * 
     * @param stateFeatures State features as a map
     * @return Action recommendation
     */
    public ActionRecommendation getActionRecommendationFromMap(Map<String, Object> stateFeatures) {
        // Convert Map to float[] for existing implementation
        float[] featureVector = convertMapToFeatureVector(stateFeatures);
        return getActionRecommendation(featureVector);
    }
}