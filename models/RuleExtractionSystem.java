package models;

import android.content.Context;
import android.graphics.Bitmap;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * System for extracting game rules from observations.
 */
public class RuleExtractionSystem {
    private static RuleExtractionSystem instance;
    private Context context;
    private Map<String, List<GameRuleUnderstanding.GameRule>> extractedRules;
    private boolean isActive;
    
    /**
     * Private constructor for singleton pattern
     */
    private RuleExtractionSystem(Context context) {
        this.context = context;
        this.extractedRules = new HashMap<>();
        this.isActive = false;
    }
    
    /**
     * Get the singleton instance
     * 
     * @param context Application context
     * @return RuleExtractionSystem instance
     */
    public static synchronized RuleExtractionSystem getInstance(Context context) {
        if (instance == null) {
            instance = new RuleExtractionSystem(context);
        }
        return instance;
    }
    
    /**
     * Get the singleton instance (no-arg version for backward compatibility)
     */
    public static synchronized RuleExtractionSystem getInstance() {
        if (instance == null) {
            throw new IllegalStateException("RuleExtractionSystem not initialized. Call getInstance(Context) first.");
        }
        return instance;
    }
    
    /**
     * Start rule extraction
     */
    public void start() {
        isActive = true;
    }
    
    /**
     * Stop rule extraction
     */
    public void stop() {
        isActive = false;
    }
    
    /**
     * Process an observation to extract rules
     * 
     * @param gameId Game identifier
     * @param observation Observation data
     * @return Extracted rules
     */
    public List<GameRuleUnderstanding.GameRule> processObservation(String gameId, 
                                                        GameRuleUnderstanding.GameObservation observation) {
        if (!isActive || observation == null) {
            return new ArrayList<>();
        }
        
        // Initialize rules for this game if not already done
        if (!extractedRules.containsKey(gameId)) {
            extractedRules.put(gameId, new ArrayList<>());
        }
        
        List<GameRuleUnderstanding.GameRule> gameRules = extractedRules.get(gameId);
        
        // Check if the observation confirms or contradicts existing rules
        updateExistingRules(gameRules, observation);
        
        // Try to extract new rules from the observation
        List<GameRuleUnderstanding.GameRule> newRules = extractNewRules(observation);
        
        // Add new rules if they don't contradict existing ones
        for (GameRuleUnderstanding.GameRule newRule : newRules) {
            if (!ruleExists(gameRules, newRule)) {
                gameRules.add(newRule);
            }
        }
        
        return gameRules;
    }
    
    /**
     * Update existing rules based on new observation
     * 
     * @param rules Existing rules
     * @param observation New observation
     */
    private void updateExistingRules(List<GameRuleUnderstanding.GameRule> rules, 
                                   GameRuleUnderstanding.GameObservation observation) {
        for (GameRuleUnderstanding.GameRule rule : rules) {
            // Increase confidence if observation supports the rule
            if (observationSupportsRule(observation, rule)) {
                rule.setConfidence(Math.min(1.0, rule.getConfidence() + 0.1));
            }
            // Decrease confidence if observation contradicts the rule
            else if (observationContradictsRule(observation, rule)) {
                rule.setConfidence(Math.max(0.0, rule.getConfidence() - 0.2));
            }
        }
        
        // Remove rules with very low confidence
        rules.removeIf(rule -> rule.getConfidence() < 0.1);
    }
    
    /**
     * Extract new rules from an observation
     * 
     * @param observation Game observation
     * @return List of extracted rules
     */
    private List<GameRuleUnderstanding.GameRule> extractNewRules(GameRuleUnderstanding.GameObservation observation) {
        List<GameRuleUnderstanding.GameRule> newRules = new ArrayList<>();
        
        // This is a simplified mock implementation.
        // In a real system, this would use much more sophisticated
        // rule extraction algorithms.
        
        if (observation.getBeforeState() != null && observation.getAfterState() != null 
                && observation.getAction() != null) {
            
            Map<String, Object> before = observation.getBeforeState();
            Map<String, Object> after = observation.getAfterState();
            String action = observation.getAction();
            
            // Look for state changes after the action
            for (String key : after.keySet()) {
                if (before.containsKey(key) && !before.get(key).equals(after.get(key))) {
                    // Create a rule based on the observed change
                    GameRuleUnderstanding.GameRule rule = new GameRuleUnderstanding.GameRule();
                    rule.setName("Rule-" + System.currentTimeMillis());
                    rule.setDescription(action + " changes " + key + " from " + before.get(key) + 
                                     " to " + after.get(key));
                    rule.setCondition(action + " & " + key + "=" + before.get(key));
                    rule.setEffect(key + "=" + after.get(key));
                    rule.setConfidence(0.5); // Initial confidence
                    
                    newRules.add(rule);
                }
            }
        }
        
        return newRules;
    }
    
    /**
     * Check if rule exists in the list
     * 
     * @param rules List of rules
     * @param rule Rule to check
     * @return True if rule exists
     */
    private boolean ruleExists(List<GameRuleUnderstanding.GameRule> rules, GameRuleUnderstanding.GameRule rule) {
        for (GameRuleUnderstanding.GameRule existingRule : rules) {
            if (existingRule.getCondition().equals(rule.getCondition()) && 
                existingRule.getEffect().equals(rule.getEffect())) {
                return true;
            }
        }
        return false;
    }
    
    /**
     * Check if observation supports a rule
     * 
     * @param observation Observation
     * @param rule Rule to check
     * @return True if supported
     */
    private boolean observationSupportsRule(GameRuleUnderstanding.GameObservation observation, 
                                          GameRuleUnderstanding.GameRule rule) {
        // Simplified example: check if the action and condition match,
        // and the effect was observed
        if (observation.getBeforeState() == null || observation.getAfterState() == null || 
            observation.getAction() == null) {
            return false;
        }
        
        // Parse the condition (very simplified)
        String condition = rule.getCondition();
        if (condition.contains(observation.getAction())) {
            // Extract key=value part from condition
            if (condition.contains("&")) {
                String statePart = condition.split("&")[1].trim();
                if (statePart.contains("=")) {
                    String key = statePart.split("=")[0].trim();
                    String value = statePart.split("=")[1].trim();
                    
                    // Check if before-state matches condition
                    if (observation.getBeforeState().containsKey(key) && 
                        value.equals(String.valueOf(observation.getBeforeState().get(key)))) {
                        
                        // Parse the effect (very simplified)
                        String effect = rule.getEffect();
                        if (effect.contains("=")) {
                            String effectKey = effect.split("=")[0].trim();
                            String effectValue = effect.split("=")[1].trim();
                            
                            // Check if after-state matches effect
                            if (observation.getAfterState().containsKey(effectKey) && 
                                effectValue.equals(String.valueOf(observation.getAfterState().get(effectKey)))) {
                                return true;
                            }
                        }
                    }
                }
            }
        }
        
        return false;
    }
    
    /**
     * Check if observation contradicts a rule
     * 
     * @param observation Observation
     * @param rule Rule to check
     * @return True if contradicted
     */
    private boolean observationContradictsRule(GameRuleUnderstanding.GameObservation observation, 
                                             GameRuleUnderstanding.GameRule rule) {
        // Simplified example: check if the action and condition match,
        // but the effect was different
        if (observation.getBeforeState() == null || observation.getAfterState() == null || 
            observation.getAction() == null) {
            return false;
        }
        
        // Parse the condition (very simplified)
        String condition = rule.getCondition();
        if (condition.contains(observation.getAction())) {
            // Extract key=value part from condition
            if (condition.contains("&")) {
                String statePart = condition.split("&")[1].trim();
                if (statePart.contains("=")) {
                    String key = statePart.split("=")[0].trim();
                    String value = statePart.split("=")[1].trim();
                    
                    // Check if before-state matches condition
                    if (observation.getBeforeState().containsKey(key) && 
                        value.equals(String.valueOf(observation.getBeforeState().get(key)))) {
                        
                        // Parse the effect (very simplified)
                        String effect = rule.getEffect();
                        if (effect.contains("=")) {
                            String effectKey = effect.split("=")[0].trim();
                            String effectValue = effect.split("=")[1].trim();
                            
                            // Check if after-state DOES NOT match effect
                            if (observation.getAfterState().containsKey(effectKey) && 
                                !effectValue.equals(String.valueOf(observation.getAfterState().get(effectKey)))) {
                                return true;
                            }
                        }
                    }
                }
            }
        }
        
        return false;
    }
    
    /**
     * Get all rules for a game
     * 
     * @param gameId Game identifier
     * @return List of rules
     */
    public List<GameRuleUnderstanding.GameRule> getRulesForGame(String gameId) {
        if (extractedRules.containsKey(gameId)) {
            return extractedRules.get(gameId);
        }
        return new ArrayList<>();
    }
    
    /**
     * Clear rules for a game
     * 
     * @param gameId Game identifier
     */
    public void clearRulesForGame(String gameId) {
        extractedRules.remove(gameId);
    }
    
    /**
     * Clear all rules
     */
    public void clearAllRules() {
        extractedRules.clear();
    }
    
    /**
     * Get all rules across all games
     * 
     * @return List of all extracted rules
     */
    public List<GameRuleUnderstanding.GameRule> getAllRules() {
        List<GameRuleUnderstanding.GameRule> allRules = new ArrayList<>();
        
        // Combine rules from all games
        for (String gameId : extractedRules.keySet()) {
            allRules.addAll(extractedRules.get(gameId));
        }
        
        return allRules;
    }
    
    /**
     * Find relevant rules for a given state
     * 
     * @param state Current state data
     * @param callback Optional callback for rule processing (can be null)
     * @return List of relevant rules
     */
    public List<GameRuleUnderstanding.GameRule> findRelevantRules(Map<String, Object> state, Object callback) {
        if (!isActive || state == null) {
            return new ArrayList<>();
        }
        
        List<GameRuleUnderstanding.GameRule> relevantRules = new ArrayList<>();
        
        // Look through all extracted rules across all games
        for (String gameId : extractedRules.keySet()) {
            List<GameRuleUnderstanding.GameRule> gameRules = extractedRules.get(gameId);
            
            for (GameRuleUnderstanding.GameRule rule : gameRules) {
                // Check if the rule's condition could potentially apply to this state
                boolean relevant = false;
                
                // Very simplified check: if state contains keys mentioned in condition
                String condition = rule.getCondition();
                if (condition != null && condition.contains("&")) {
                    String statePart = condition.split("&")[1].trim();
                    if (statePart.contains("=")) {
                        String key = statePart.split("=")[0].trim();
                        if (state.containsKey(key)) {
                            relevant = true;
                        }
                    }
                }
                
                if (relevant) {
                    relevantRules.add(rule);
                }
            }
        }
        
        return relevantRules;
    }
    
    /**
     * Process an observation from state, action, and reward
     * 
     * @param state Current state
     * @param action Action taken
     * @param reward Reward received
     * @return List of extracted rules
     */
    public List<GameRuleUnderstanding.GameRule> processObservation(Map<String, Object> state, String action, float reward) {
        if (!isActive || state == null || action == null) {
            return new ArrayList<>();
        }
        
        // Convert the single state into an observation (same before/after for simplicity)
        GameRuleUnderstanding.GameObservation observation = 
            new GameRuleUnderstanding.GameObservation(state, action, state, reward);
        
        // Process through our existing method
        return processObservation("default", observation);
    }
    
    /**
     * Process an observation between two different states
     * 
     * @param beforeState State before action
     * @param action Action taken
     * @param afterState State after action
     * @param reward Reward received
     * @return List of extracted rules
     */
    public List<GameRuleUnderstanding.GameRule> processObservation(Map<String, Object> beforeState, 
                                                              String action, 
                                                              Map<String, Object> afterState, 
                                                              double reward) {
        if (!isActive || beforeState == null || action == null || afterState == null) {
            return new ArrayList<>();
        }
        
        // Convert to an observation
        GameRuleUnderstanding.GameObservation observation = 
            new GameRuleUnderstanding.GameObservation(beforeState, action, afterState, (float)reward);
        
        // Process through our existing method
        return processObservation("default", observation);
    }
    
    /**
     * Record an observation for a specific game
     * 
     * @param gameId Identifier for the game
     * @param stateBefore State before action
     * @param action Action taken
     * @param stateAfter State after action
     * @param reward Reward received
     */
    public void recordObservation(String gameId, Map<String, Object> stateBefore, String action, 
                                  Map<String, Object> stateAfter, double reward) {
        if (!isActive || stateBefore == null || action == null || stateAfter == null) {
            return;
        }
        
        // Create observation
        GameRuleUnderstanding.GameObservation observation = 
            new GameRuleUnderstanding.GameObservation(stateBefore, action, stateAfter, (float)reward);
        
        // Process the observation
        processObservation(gameId, observation);
    }
    
    /**
     * Predict the outcome of an action in a given state
     * 
     * @param gameId Identifier for the game
     * @param currentState Current game state
     * @param action Proposed action
     * @return Predicted next state and reward
     */
    public Map<String, Object> predictOutcome(String gameId, Map<String, Object> currentState, String action) {
        if (!isActive || currentState == null || action == null) {
            return new HashMap<>();
        }
        
        Map<String, Object> prediction = new HashMap<>(currentState);
        prediction.put("predictedReward", 0.0f);
        
        // Get rules for this game
        List<GameRuleUnderstanding.GameRule> rules = getRulesForGame(gameId);
        
        // Find applicable rules
        for (GameRuleUnderstanding.GameRule rule : rules) {
            if (ruleApplies(rule, currentState, action)) {
                // Apply rule effects to prediction
                applyRuleEffect(rule, prediction);
            }
        }
        
        return prediction;
    }
    
    /**
     * Check if a rule applies to the given state and action
     * 
     * @param rule Rule to check
     * @param state Current state
     * @param action Proposed action
     * @return True if the rule applies
     */
    private boolean ruleApplies(GameRuleUnderstanding.GameRule rule, Map<String, Object> state, String action) {
        // Parse the condition (very simplified)
        String condition = rule.getCondition();
        if (condition.contains(action)) {
            // Extract key=value part from condition
            if (condition.contains("&")) {
                String statePart = condition.split("&")[1].trim();
                if (statePart.contains("=")) {
                    String key = statePart.split("=")[0].trim();
                    String value = statePart.split("=")[1].trim();
                    
                    // Check if state matches condition
                    if (state.containsKey(key) && 
                        value.equals(String.valueOf(state.get(key)))) {
                        return true;
                    }
                }
            }
        }
        
        return false;
    }
    
    /**
     * Apply a rule's effect to a state
     * 
     * @param rule Rule to apply
     * @param state State to modify
     */
    private void applyRuleEffect(GameRuleUnderstanding.GameRule rule, Map<String, Object> state) {
        // Parse the effect (very simplified)
        String effect = rule.getEffect();
        if (effect.contains("=")) {
            String key = effect.split("=")[0].trim();
            String value = effect.split("=")[1].trim();
            
            // Apply effect to state
            try {
                // Try to parse as number first
                if (value.contains(".")) {
                    state.put(key, Double.parseDouble(value));
                } else {
                    state.put(key, Integer.parseInt(value));
                }
            } catch (NumberFormatException e) {
                // If not a number, store as string
                state.put(key, value);
            }
        }
    }
}