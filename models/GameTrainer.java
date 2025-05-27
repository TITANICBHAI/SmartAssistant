package models;

import android.content.Context;
import android.graphics.Bitmap;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import utils.DeepRLModelHelper;
import utils.GameRuleUnderstandingHelper;

/**
 * System for training agents to play games.
 */
public class GameTrainer {
    private static GameTrainer instance;
    private Context context;
    private DeepRLModel rlModel;
    private GameRuleUnderstanding ruleSystem;
    private Map<String, TrainingSession> activeSessions;
    private boolean isActive;
    
    /**
     * Private constructor for singleton pattern
     */
    private GameTrainer(Context context) {
        this.context = context;
        this.rlModel = DeepRLModelHelper.getInstance(context);
        this.ruleSystem = GameRuleUnderstandingHelper.getInstance(utils.ContextConverter.toUtilsContext(context));
        this.activeSessions = new HashMap<>();
        this.isActive = false;
    }
    
    /**
     * Get the singleton instance
     * 
     * @param context Application context
     * @return GameTrainer instance
     */
    public static synchronized GameTrainer getInstance(Context context) {
        if (instance == null) {
            instance = new GameTrainer(context);
        }
        return instance;
    }
    
    /**
     * Get the singleton instance (no-arg version for backward compatibility)
     */
    public static synchronized GameTrainer getInstance() {
        if (instance == null) {
            throw new IllegalStateException("GameTrainer not initialized. Call getInstance(Context) first.");
        }
        return instance;
    }
    
    /**
     * Start the trainer
     */
    public void start() {
        isActive = true;
        rlModel.start();
        ruleSystem.start();
    }
    
    /**
     * Stop the trainer
     */
    public void stop() {
        isActive = false;
        rlModel.stop();
        ruleSystem.stop();
        
        // Stop all active sessions
        for (TrainingSession session : activeSessions.values()) {
            session.stop();
        }
    }
    
    /**
     * Start a new training session
     * 
     * @param gameId Game identifier
     * @param gameType Type of game
     * @return Session ID
     */
    public String startTrainingSession(String gameId, String gameType) {
        if (!isActive) {
            return null;
        }
        
        String sessionId = "session_" + System.currentTimeMillis();
        TrainingSession session = new TrainingSession(sessionId, gameId, gameType);
        activeSessions.put(sessionId, session);
        
        return sessionId;
    }
    
    /**
     * Stop a training session
     * 
     * @param sessionId Session to stop
     */
    public void stopTrainingSession(String sessionId) {
        if (activeSessions.containsKey(sessionId)) {
            TrainingSession session = activeSessions.get(sessionId);
            session.stop();
            activeSessions.remove(sessionId);
        }
    }
    
    /**
     * Process a game screenshot
     * 
     * @param sessionId Training session
     * @param screenshot Game screenshot
     * @return Recommended actions
     */
    public List<ActionRecommendation> processScreenshot(String sessionId, Bitmap screenshot) {
        if (!isActive || !activeSessions.containsKey(sessionId) || screenshot == null) {
            return new ArrayList<>();
        }
        
        TrainingSession session = activeSessions.get(sessionId);
        return session.processScreenshot(screenshot);
    }
    
    /**
     * Record action results
     * 
     * @param sessionId Training session
     * @param actionTaken Action that was taken
     * @param reward Reward received
     * @param newScreenshot New screenshot after action
     */
    public void recordActionResult(String sessionId, String actionTaken, 
                                double reward, Bitmap newScreenshot) {
        if (!isActive || !activeSessions.containsKey(sessionId)) {
            return;
        }
        
        TrainingSession session = activeSessions.get(sessionId);
        session.recordActionResult(actionTaken, reward, newScreenshot);
    }
    
    /**
     * Get active sessions
     */
    public List<String> getActiveSessionIds() {
        return new ArrayList<>(activeSessions.keySet());
    }
    
    /**
     * Get the RL model
     */
    public DeepRLModel getRLModel() {
        return rlModel;
    }
    
    /**
     * Get the rule system
     */
    public GameRuleUnderstanding getRuleSystem() {
        return ruleSystem;
    }
    
    /**
     * Class representing a game training session
     */
    private class TrainingSession {
        private String sessionId;
        private String gameId;
        private String gameType;
        private boolean isActive;
        private Map<String, Object> currentState;
        private Bitmap currentScreenshot;
        private int actionCount;
        private double totalReward;
        
        public TrainingSession(String sessionId, String gameId, String gameType) {
            this.sessionId = sessionId;
            this.gameId = gameId;
            this.gameType = gameType;
            this.isActive = true;
            this.currentState = new HashMap<>();
            this.actionCount = 0;
            this.totalReward = 0.0;
            
            // Configure the RL model for this game type
            rlModel.setGameType(gameType);
        }
        
        public void stop() {
            isActive = false;
        }
        
        public List<ActionRecommendation> processScreenshot(Bitmap screenshot) {
            if (!isActive || screenshot == null) {
                return new ArrayList<>();
            }
            
            // Store the current screenshot
            currentScreenshot = screenshot;
            
            // Process the image with the RL model to extract features
            Map<String, Object> features = rlModel.processImage(screenshot);
            
            // Update the current state with these features
            currentState.putAll(features);
            
            // Use the RL model to get action recommendations
            List<DeepRLModel.ActionRecommendation> modelRecommendations = 
                rlModel.processState(currentState);
            
            // Convert to our ActionRecommendation format
            List<ActionRecommendation> recommendations = new ArrayList<>();
            for (DeepRLModel.ActionRecommendation rec : modelRecommendations) {
                recommendations.add(new ActionRecommendation(
                    rec.getAction(),
                    rec.getConfidence(),
                    rec.getReasoning()
                ));
            }
            
            // Also try to predict outcomes using rule understanding
            if (!recommendations.isEmpty()) {
                addRulePredictions(recommendations);
            }
            
            return recommendations;
        }
        
        public void recordActionResult(String actionTaken, double reward, Bitmap newScreenshot) {
            if (!isActive) {
                return;
            }
            
            actionCount++;
            totalReward += reward;
            
            // Extract features from the new state
            Map<String, Object> newState = rlModel.processImage(newScreenshot);
            
            // Record the observation with the rule system
            ruleSystem.recordObservation(gameId, currentState, actionTaken, newState, reward);
            
            // Update the RL model
            rlModel.updateModel(currentState, actionTaken, reward, newState);
            
            // Update current state and screenshot
            currentState = newState;
            currentScreenshot = newScreenshot;
        }
        
        private void addRulePredictions(List<ActionRecommendation> recommendations) {
            // For each recommendation, try to predict outcome using rule system
            for (ActionRecommendation rec : recommendations) {
                Map<String, Object> prediction = 
                    ruleSystem.predictOutcome(gameId, currentState, rec.getAction());
                
                if (prediction.containsKey("predicted_reward")) {
                    double predictedReward = (double) prediction.get("predicted_reward");
                    
                    // Adjust confidence based on predicted reward
                    if (predictedReward > 0) {
                        rec.setConfidence(Math.min(1.0, rec.getConfidence() + 0.2));
                        rec.setReasoning(rec.getReasoning() + 
                                      " (Rule system predicts positive outcome)");
                    } else if (predictedReward < 0) {
                        rec.setConfidence(Math.max(0.0, rec.getConfidence() - 0.2));
                        rec.setReasoning(rec.getReasoning() + 
                                      " (Rule system predicts negative outcome)");
                    }
                }
            }
            
            // Sort by confidence
            recommendations.sort((a, b) -> Double.compare(b.getConfidence(), a.getConfidence()));
        }
    }
    
    /**
     * Class representing an action recommendation
     */
    public static class ActionRecommendation {
        private String action;
        private double confidence;
        private String reasoning;
        
        public ActionRecommendation(String action, double confidence, String reasoning) {
            this.action = action;
            this.confidence = confidence;
            this.reasoning = reasoning;
        }
        
        public String getAction() {
            return action;
        }
        
        public double getConfidence() {
            return confidence;
        }
        
        public void setConfidence(double confidence) {
            this.confidence = confidence;
        }
        
        public String getReasoning() {
            return reasoning;
        }
        
        public void setReasoning(String reasoning) {
            this.reasoning = reasoning;
        }
    }
}