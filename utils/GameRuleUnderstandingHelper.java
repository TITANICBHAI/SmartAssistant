package utils;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.HashMap;
import models.GameRuleUnderstanding;
import models.GameContext;
import models.GameApplicationInfo;

/**
 * Helper class for game rule understanding.
 */
public class GameRuleUnderstandingHelper {
    
    /**
     * Get a singleton instance of GameRuleUnderstanding.
     * 
     * @return A GameRuleUnderstanding instance
     */
    public static GameRuleUnderstanding getInstance() {
        try {
            return GameRuleUnderstandingImpl.getInstance();
        } catch (IllegalStateException e) {
            // Fallback to default if not initialized with context
            return new DefaultGameRuleUnderstanding();
        }
    }
    
    /**
     * Get a singleton instance of GameRuleUnderstanding for the specified context.
     * 
     * @param context The context to use
     * @return A GameRuleUnderstanding instance
     */
    public static GameRuleUnderstanding getInstance(Context context) {
        android.content.Context androidContext = ContextConverter.toAndroidContext(context);
        return GameRuleUnderstandingImpl.getInstance(androidContext);
    }
    
    /**
     * Process an action with Game Rule Understanding
     * 
     * @param gameRules The game rules to use
     * @param action The action to process
     * @param context The context of the action
     */
    public static void processAction(GameRuleUnderstanding gameRules, String action, Map<String, Object> context) {
        if (gameRules == null || action == null || action.isEmpty() || context == null) {
            return;
        }
        
        // Create a game observation from the context
        GameRuleUnderstanding.GameObservation observation = new GameRuleUnderstanding.GameObservation();
        
        // Process the action using the game rules
        gameRules.processObservation(observation);
        
        // Check if any rules match
        List<GameRuleUnderstanding.GameRule> matchingRules = gameRules.getMatchingRules(observation);
        
        // Apply the first matching rule if any
        if (!matchingRules.isEmpty()) {
            GameRuleUnderstanding.GameRule rule = matchingRules.get(0);
            // Apply the rule (in a real implementation, this would modify the game state)
        }
    }
    
    /**
     * Extract patterns from game observations.
     * 
     * @param observations List of game observations
     * @return List of extracted patterns
     */
    public static List<GameRuleUnderstanding.GamePattern> extractPatterns(List<GameRuleUnderstanding.GameObservation> observations) {
        List<GameRuleUnderstanding.GamePattern> patterns = new ArrayList<>();
        
        if (observations == null || observations.isEmpty()) {
            return patterns;
        }
        
        // Simple pattern extraction logic (mock implementation)
        Map<String, Integer> actionCounts = new HashMap<>();
        
        for (GameRuleUnderstanding.GameObservation obs : observations) {
            String action = obs.getAction();
            if (action != null && !action.isEmpty()) {
                actionCounts.put(action, actionCounts.getOrDefault(action, 0) + 1);
            }
        }
        
        // Create patterns for frequent actions
        for (Map.Entry<String, Integer> entry : actionCounts.entrySet()) {
            if (entry.getValue() >= 2) { // Threshold for pattern detection
                GameRuleUnderstanding.GamePattern pattern = new GameRuleUnderstanding.GamePattern();
                pattern.setAction(entry.getKey());
                patterns.add(pattern);
            }
        }
        
        return patterns;
    }
    
    /**
     * Default implementation of GameRuleUnderstanding.
     */
    private static class DefaultGameRuleUnderstanding implements GameRuleUnderstanding {
        private List<GameObservation> observations = new ArrayList<>();
        private List<GamePattern> patterns = new ArrayList<>();
        private List<GameRule> rules = new ArrayList<>();
        private boolean isActive = false;
        
        @Override
        public void processObservation(GameObservation observation) {
            if (observation != null) {
                observations.add(observation);
                
                // Update patterns based on the new observation
                patterns = extractPatterns(observations);
                
                // Generate rules based on updated patterns
                updateRules();
            }
        }
        
        @Override
        public List<GameRule> getMatchingRules(GameObservation observation) {
            List<GameRule> matchingRules = new ArrayList<>();
            
            if (observation == null) {
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
            return patterns;
        }
        
        @Override
        public List<GameRule> getRules() {
            return rules;
        }
        
        @Override
        public void start() {
            isActive = true;
        }
        
        @Override
        public void stop() {
            isActive = false;
        }
        
        @Override
        public Map<String, Object> predictOutcome(String gameId, Map<String, Object> currentState, String action) {
            // Simple prediction logic
            Map<String, Object> predictedState = new HashMap<>(currentState);
            
            // Add a prediction marker
            predictedState.put("predicted", true);
            predictedState.put("actionPerformed", action);
            
            return predictedState;
        }
        
        @Override
        public void recordObservation(String gameId, Map<String, Object> stateBefore, String action, 
                                     Map<String, Object> stateAfter, double reward) {
            GameObservation observation = new GameObservation(stateBefore, action, stateAfter, reward);
            processObservation(observation);
        }
        
        private void updateRules() {
            // Simple rule generation logic based on patterns
            rules.clear();
            
            for (GamePattern pattern : patterns) {
                GameRule rule = new GameRule();
                rule.setPattern(pattern);
                rules.add(rule);
            }
        }
    }
}