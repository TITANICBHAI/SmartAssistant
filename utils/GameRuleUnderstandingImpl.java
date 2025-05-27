package utils;

import android.content.Context;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import models.GameRuleUnderstanding;
import models.RuleExtractionSystem;

/**
 * Implementation of GameRuleUnderstanding for Android context.
 */
public class GameRuleUnderstandingImpl implements GameRuleUnderstanding {
    private static GameRuleUnderstandingImpl instance;
    private final Context context;
    private List<GameObservation> observations;
    private List<GamePattern> patterns;
    private List<GameRule> rules;
    private boolean isActive;
    private RuleExtractionSystem ruleExtractionSystem;
    
    /**
     * Private constructor for singleton pattern
     */
    private GameRuleUnderstandingImpl(Context context) {
        this.context = context;
        this.observations = new ArrayList<>();
        this.patterns = new ArrayList<>();
        this.rules = new ArrayList<>();
        this.isActive = false;
        try {
            this.ruleExtractionSystem = RuleExtractionSystem.getInstance(context);
        } catch (Exception e) {
            LogHelper.e("GameRuleUnderstandingImpl", "Failed to get RuleExtractionSystem instance", e);
            this.ruleExtractionSystem = null;
        }
    }
    
    /**
     * Get the singleton instance with context.
     * 
     * @param context Application context
     * @return GameRuleUnderstandingImpl instance
     */
    public static synchronized GameRuleUnderstandingImpl getInstance(Context context) {
        if (instance == null) {
            if (context == null) {
                throw new IllegalStateException("Context cannot be null for GameRuleUnderstandingImpl initialization");
            }
            instance = new GameRuleUnderstandingImpl(context);
        }
        return instance;
    }
    
    /**
     * Get the singleton instance (no-arg version for backward compatibility)
     */
    public static synchronized GameRuleUnderstandingImpl getInstance() {
        if (instance == null) {
            throw new IllegalStateException("GameRuleUnderstandingImpl not initialized. Call getInstance(Context) first.");
        }
        return instance;
    }
    
    @Override
    public void processObservation(GameObservation observation) {
        if (observation != null && isActive) {
            observations.add(observation);
            
            // Update patterns based on the new observation
            updatePatterns();
            
            // Generate rules based on updated patterns
            updateRules();
            
            // If we have a rule extraction system, process the observation there too
            if (ruleExtractionSystem != null) {
                // Create a default game ID
                String gameId = "default";
                
                // Extract data from the observation
                Map<String, Object> beforeState = observation.getBeforeState();
                String action = observation.getAction();
                Map<String, Object> afterState = observation.getAfterState();
                double reward = observation.getReward();
                
                // Process using the appropriate method
                ruleExtractionSystem.processObservation(gameId, 
                    new models.GameRuleUnderstanding.GameObservation(
                        beforeState, action, afterState, (float)reward));
            }
        }
    }
    
    @Override
    public List<GameRule> getMatchingRules(GameObservation observation) {
        List<GameRule> matchingRules = new ArrayList<>();
        
        if (observation == null || !isActive) {
            return matchingRules;
        }
        
        for (GameRule rule : rules) {
            if (rule.matches(observation)) {
                matchingRules.add(rule);
            }
        }
        
        return matchingRules;
    }
    
    @Override
    public List<GamePattern> getPatterns() {
        return new ArrayList<>(patterns);
    }
    
    @Override
    public List<GameRule> getRules() {
        return new ArrayList<>(rules);
    }
    
    @Override
    public void start() {
        isActive = true;
        if (ruleExtractionSystem != null) {
            ruleExtractionSystem.start();
        }
    }
    
    @Override
    public void stop() {
        isActive = false;
        if (ruleExtractionSystem != null) {
            ruleExtractionSystem.stop();
        }
    }
    
    @Override
    public Map<String, Object> predictOutcome(String gameId, Map<String, Object> currentState, String action) {
        if (!isActive || currentState == null || action == null || action.isEmpty()) {
            return new HashMap<>();
        }
        
        // Create a basic prediction
        Map<String, Object> prediction = new HashMap<>(currentState);
        prediction.put("predicted", true);
        prediction.put("action", action);
        
        // Default prediction values
        double predictedReward = 0.0;
        
        // Check if we have rules that match this state and action
        for (GameRule rule : rules) {
            if (rule.getPattern() != null && rule.getPattern().matchesInitialState(currentState, action)) {
                // Apply the rule to enhance the prediction
                rule.getPattern().enhancePrediction(prediction);
                
                // Update predicted reward based on pattern's average reward
                predictedReward += rule.getPattern().getAverageReward() * rule.getConfidence();
            }
        }
        
        // Use the rule extraction system if available
        if (ruleExtractionSystem != null) {
            Map<String, Object> systemPrediction = ruleExtractionSystem.predictOutcome(gameId, currentState, action);
            if (systemPrediction != null && !systemPrediction.isEmpty()) {
                // Merge the predictions, favoring the system prediction
                prediction.putAll(systemPrediction);
                
                // Update predicted reward if available
                if (systemPrediction.containsKey("predicted_reward")) {
                    try {
                        double systemReward = Double.parseDouble(systemPrediction.get("predicted_reward").toString());
                        // Weight system prediction more heavily
                        predictedReward = (predictedReward + systemReward * 2) / 3.0;
                    } catch (NumberFormatException e) {
                        LogHelper.w("GameRuleUnderstandingImpl", "Invalid predicted_reward format", e);
                    }
                }
            }
        }
        
        // Store the final predicted reward
        prediction.put("predicted_reward", predictedReward);
        
        return prediction;
    }
    
    @Override
    public void recordObservation(String gameId, Map<String, Object> stateBefore, String action, 
                                 Map<String, Object> stateAfter, double reward) {
        if (!isActive || stateBefore == null || action == null || action.isEmpty() || stateAfter == null) {
            return;
        }
        
        // Create and process a new observation
        GameObservation observation = new GameObservation(stateBefore, action, stateAfter, reward);
        processObservation(observation);
        
        // Record in the rule extraction system if available
        if (ruleExtractionSystem != null) {
            ruleExtractionSystem.recordObservation(gameId, stateBefore, action, stateAfter, reward);
        }
    }
    
    /**
     * Update patterns based on observations
     */
    private void updatePatterns() {
        // For demonstration, just use the helper to extract patterns
        patterns = GameRuleUnderstandingHelper.extractPatterns(observations);
    }
    
    /**
     * Update rules based on patterns
     */
    private void updateRules() {
        rules.clear();
        
        for (GamePattern pattern : patterns) {
            GameRule rule = new GameRule();
            rule.setPattern(pattern);
            rule.setName("Rule for " + pattern.getAction());
            rule.setDescription("When action is " + pattern.getAction());
            rule.setCondition("action == \"" + pattern.getAction() + "\"");
            rule.setEffect("Apply " + pattern.getAction() + " effect");
            rule.setConfidence(0.7);  // Default confidence
            rules.add(rule);
        }
    }
}