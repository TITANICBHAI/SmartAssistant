package models;

import android.content.Context;
// Use our custom Bitmap implementation instead of Android's
import utils.Bitmap;
// Use our models.GameType instead of utils.GameType
import models.GameType;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import utils.DeepRLModelHelper;
import utils.GameRuleUnderstandingHelper;
import models.SuggestionListener;
import models.PredictionCallback;

/**
 * System for predicting optimal actions in games.
 */
public class PredictiveActionSystem {
    /**
     * Represents a game state for prediction purposes
     */
    public static class GameState {
        private final String gameId;
        private Map<String, Object> stateData;
        private Bitmap currentScreenshot;
        private GameType gameType;
        
        /**
         * Create a new game state
         * 
         * @param gameId Game identifier
         */
        public GameState(String gameId) {
            this.gameId = gameId;
            this.stateData = new HashMap<>();
            this.gameType = GameType.UNKNOWN;
        }
        
        /**
         * Get the game ID
         * 
         * @return The game ID
         */
        public String getGameId() {
            return gameId;
        }
        
        /**
         * Get the state data
         * 
         * @return Map containing state data
         */
        public Map<String, Object> getStateData() {
            return new HashMap<>(stateData);
        }
        
        /**
         * Update the state with new data
         * 
         * @param newData New state data
         */
        public void updateState(Map<String, Object> newData) {
            if (newData != null) {
                this.stateData.putAll(newData);
            }
        }
        
        /**
         * Set the state data completely replacing any existing data
         * 
         * @param stateData The new state data
         */
        public void setStateData(Map<String, Object> stateData) {
            if (stateData != null) {
                this.stateData = new HashMap<>(stateData);
            } else {
                this.stateData = new HashMap<>();
            }
        }
        
        /**
         * Get the current screenshot
         * 
         * @return The current screenshot
         */
        public Bitmap getCurrentScreenshot() {
            return currentScreenshot;
        }
        
        /**
         * Set the current screenshot
         * 
         * @param screenshot The new screenshot
         */
        public void setCurrentScreenshot(Bitmap screenshot) {
            this.currentScreenshot = screenshot;
        }
        
        /**
         * Get the game type
         * 
         * @return The game type
         */
        public GameType getGameType() {
            return gameType;
        }
        
        /**
         * Set the game type
         * 
         * @param gameType The new game type
         */
        public void setGameType(GameType gameType) {
            this.gameType = gameType;
        }
    }
    
    private static PredictiveActionSystem instance;
    private Context context;
    private DeepRLModel rlModel;
    private GameRuleUnderstanding ruleSystem;
    private Map<String, List<SuggestionListener>> suggestionListeners;
    private Map<String, GameState> gameStates;
    private boolean isActive;
    private int processedFrameCount = 0;
    private double lastPredictionConfidence = 0.0;
    private String currentGameType;
    private Map<String, Object> stats = new HashMap<>();
    
    /**
     * Private constructor for singleton pattern
     */
    private PredictiveActionSystem(Context context) {
        this.context = context;
        this.rlModel = DeepRLModelHelper.getInstance(context);
        this.ruleSystem = GameRuleUnderstandingHelper.getInstance(utils.ContextConverter.toUtilsContext(context));
        this.suggestionListeners = new HashMap<>();
        this.gameStates = new HashMap<>();
        this.isActive = false;
    }
    
    /**
     * Get the singleton instance
     * 
     * @param context Application context
     * @return PredictiveActionSystem instance
     */
    public static synchronized PredictiveActionSystem getInstance(Context context) {
        if (instance == null) {
            instance = new PredictiveActionSystem(context);
        }
        return instance;
    }
    
    /**
     * Get the singleton instance (no-arg version for backward compatibility)
     */
    public static synchronized PredictiveActionSystem getInstance() {
        if (instance == null) {
            throw new IllegalStateException("PredictiveActionSystem not initialized. Call getInstance(Context) first.");
        }
        return instance;
    }
    
    /**
     * Start the prediction system
     */
    public void start() {
        isActive = true;
        rlModel.start();
        ruleSystem.start();
    }
    
    /**
     * Start the prediction process (alias for start)
     */
    public void startPrediction() {
        start();
    }
    
    /**
     * Stop the prediction system
     */
    public void stop() {
        isActive = false;
        rlModel.stop();
        ruleSystem.stop();
    }
    
    /**
     * Stop the prediction process (alias for stop)
     */
    public void stopPrediction() {
        stop();
    }
    
    /**
     * Set the game type
     * 
     * @param gameType Game type name
     */
    public void setGameType(String gameType) {
        if (rlModel != null) {
            rlModel.setGameType(gameType);
        }
    }
    
    /**
     * Register a listener for action suggestions
     * 
     * @param gameId Game identifier
     * @param listener Listener to register
     */
    public void registerSuggestionListener(String gameId, SuggestionListener listener) {
        if (gameId == null || listener == null) {
            return;
        }
        
        if (!suggestionListeners.containsKey(gameId)) {
            suggestionListeners.put(gameId, new ArrayList<>());
        }
        
        List<SuggestionListener> listeners = suggestionListeners.get(gameId);
        if (!listeners.contains(listener)) {
            listeners.add(listener);
        }
    }
    
    /**
     * Unregister a listener
     * 
     * @param gameId Game identifier
     * @param listener Listener to unregister
     */
    public void unregisterSuggestionListener(String gameId, SuggestionListener listener) {
        if (gameId == null || listener == null) {
            return;
        }
        
        if (suggestionListeners.containsKey(gameId)) {
            suggestionListeners.get(gameId).remove(listener);
        }
    }
    
    /**
     * Predict action for a given game state
     *
     * @param state Current game state
     * @return Predicted action or null if prediction failed
     */
    public ActionPrediction predictAction(GameState state) {
        if (!isActive || state == null) {
            return null;
        }
        
        List<ActionPrediction> predictions = generatePredictions(state);
        
        if (predictions.isEmpty()) {
            return null;
        }
        
        // Return the highest confidence prediction
        return predictions.get(0);
    }
    
    /**
     * Process a frame with UI elements
     * 
     * @param image The current frame/screenshot
     * @param elements List of detected UI elements
     * @return Predicted actions for the current frame
     */
    public List<ActionPrediction> processFrame(Bitmap image, List<utils.UIElement> elements) {
        if (!isActive || image == null || elements == null) {
            return new ArrayList<>();
        }
        
        // Create a default game ID for this frame
        String gameId = "default";
        
        // Extract features from the image - convert utils.Bitmap to android.graphics.Bitmap
        android.graphics.Bitmap androidBitmap = utils.BitmapConverter.toAndroidBitmap(image);
        Map<String, Object> features = rlModel.processImage(androidBitmap);
        
        // Add UI element information to features
        Map<String, Object> elementFeatures = new HashMap<>();
        int elementCount = 0;
        for (utils.UIElement element : elements) {
            Map<String, Object> elementData = new HashMap<>();
            elementData.put("type", element.getType().toString());
            elementData.put("bounds", element.getBoundsArray());
            elementData.put("text", element.getText());
            elementData.put("id", element.getId());
            elementData.put("clickable", element.isClickable());
            elementData.put("scrollable", element.isScrollable());
            elementData.put("focused", element.isFocused());
            elementData.put("enabled", element.isEnabled());
            
            elementFeatures.put("element_" + elementCount, elementData);
            elementCount++;
        }
        features.put("ui_elements", elementFeatures);
        features.put("element_count", elementCount);
        
        // Create or update game state
        GameState state = getOrCreateGameState(gameId);
        state.updateState(features);
        state.setCurrentScreenshot(image);
        
        // Increment the processed frame count
        processedFrameCount++;
        
        // Generate predictions
        List<ActionPrediction> predictions = generatePredictions(state);
        
        // Notify listeners
        notifyListeners(gameId, predictions);
        
        return predictions;
    }
    
    /**
     * Process a game state update
     * 
     * @param gameId Game identifier
     * @param gameType Game type
     * @param screenshot Current screenshot
     * @return Predicted actions
     */
    public List<ActionPrediction> processGameState(String gameId, 
                                              GameType gameType, 
                                              Bitmap screenshot) {
        if (!isActive || gameId == null || screenshot == null) {
            return new ArrayList<>();
        }
        
        // Configure models for this game type if needed
        // Convert models.GameType to utils.GameType since DeepRLModel expects utils.GameType
        utils.GameType utilsGameType = gameType.toUtilsGameType();
        rlModel.setGameType(utilsGameType.toString());
        
        // Extract features from screenshot - convert utils.Bitmap to android.graphics.Bitmap
        android.graphics.Bitmap androidBitmap = utils.BitmapConverter.toAndroidBitmap(screenshot);
        Map<String, Object> features = rlModel.processImage(androidBitmap);
        
        // Create or update game state
        GameState state = getOrCreateGameState(gameId);
        state.updateState(features);
        state.setGameType(gameType);
        // Convert screenshot to utils.Bitmap format
        utils.Bitmap utilsBitmap = utils.BitmapConverter.fromAny(screenshot);
        if (utilsBitmap != null) {
            state.setCurrentScreenshot(utilsBitmap);
        }
        
        // Generate predictions
        List<ActionPrediction> predictions = generatePredictions(state);
        
        // Notify listeners
        notifyListeners(gameId, predictions);
        
        return predictions;
    }
    
    /**
     * Generate action predictions based on current state
     * 
     * @param state Current game state
     * @return List of predicted actions
     */
    public List<ActionPrediction> generatePredictions(GameState state) {
        List<ActionPrediction> predictions = new ArrayList<>();
        
        // Get recommendations from the RL model
        List<DeepRLModel.ActionRecommendation> modelRecommendations = 
            rlModel.processState(state.getStateData());
        
        // Convert to our ActionPrediction format
        for (DeepRLModel.ActionRecommendation rec : modelRecommendations) {
            predictions.add(new ActionPrediction(
                rec.getAction(),
                rec.getConfidence(),
                "RL Model: " + rec.getReasoning()
            ));
        }
        
        // Try to get additional predictions from the rule system
        Map<String, Object> currentState = state.getStateData();
        for (ActionPrediction prediction : predictions) {
            Map<String, Object> outcome = 
                ruleSystem.predictOutcome(state.getGameId(), currentState, prediction.getAction());
            
            if (outcome.containsKey("predicted_reward")) {
                double predictedReward = (double) outcome.get("predicted_reward");
                
                // Adjust confidence based on predicted reward
                if (predictedReward > 0) {
                    prediction.setConfidence(Math.min(1.0, prediction.getConfidence() + 0.2));
                    prediction.addReasoning("Rule system predicts positive outcome");
                } else if (predictedReward < 0) {
                    prediction.setConfidence(Math.max(0.0, prediction.getConfidence() - 0.2));
                    prediction.addReasoning("Rule system predicts negative outcome");
                }
            }
        }
        
        // Sort by confidence
        predictions.sort((a, b) -> Double.compare(b.getConfidence(), a.getConfidence()));
        
        // Track confidence of the best prediction
        if (!predictions.isEmpty()) {
            lastPredictionConfidence = predictions.get(0).getConfidence();
        }
        
        // If we have no predictions, add a default one
        if (predictions.isEmpty()) {
            // Create a default action based on game type
            String defaultAction;
            if (state.getGameType().requiresQuickReflexes()) {
                defaultAction = "tap_center";
            } else if (state.getGameType().isStrategic()) {
                defaultAction = "analyze";
            } else {
                defaultAction = "explore";
            }
            
            predictions.add(new ActionPrediction(
                defaultAction,
                0.5,
                "Default action based on game type"
            ));
        }
        
        return predictions;
    }
    
    /**
     * Notify listeners about new predictions
     * 
     * @param gameId Game identifier
     * @param predictions New predictions
     */
    public void notifyListeners(String gameId, List<ActionPrediction> predictions) {
        if (!suggestionListeners.containsKey(gameId)) {
            return;
        }
        
        List<SuggestionListener> listeners = suggestionListeners.get(gameId);
        for (SuggestionListener listener : listeners) {
            listener.onActionSuggestions(gameId, predictions);
        }
    }
    
    /**
     * Record feedback about an action
     * 
     * @param gameId Game identifier
     * @param action Action that was taken
     * @param reward Reward or feedback (-1 to 1)
     * @param newScreenshot New screenshot after action
     */
    public void recordActionFeedback(String gameId, String action, 
                                 double reward, Bitmap newScreenshot) {
        if (!isActive || gameId == null || action == null || newScreenshot == null) {
            return;
        }
        
        GameState state = getGameState(gameId);
        if (state == null) {
            return;
        }
        
        // Extract features from the new screenshot - convert utils.Bitmap to android.graphics.Bitmap
        android.graphics.Bitmap androidBitmap = utils.BitmapConverter.toAndroidBitmap(newScreenshot);
        Map<String, Object> newFeatures = rlModel.processImage(androidBitmap);
        
        // Record the observation with the rule system
        ruleSystem.recordObservation(gameId, state.getStateData(), action, newFeatures, reward);
        
        // Update the RL model
        rlModel.updateModel(state.getStateData(), action, reward, newFeatures);
        
        // Update state
        state.updateState(newFeatures);
        // Convert screenshot to utils.Bitmap format
        utils.Bitmap utilsNewScreenshot = utils.BitmapConverter.fromAny(newScreenshot);
        if (utilsNewScreenshot != null) {
            state.setCurrentScreenshot(utilsNewScreenshot);
        }
    }
    
    /**
     * Get or create a game state
     * 
     * @param gameId Game identifier
     * @return Game state
     */
    public GameState getOrCreateGameState(String gameId) {
        if (!gameStates.containsKey(gameId)) {
            // Use GameStateHelper to create a new GameState with compatible constructor,
            // but we need to convert it to our inner GameState type
            utils.GameStateHelper helper = utils.GameStateHelper.getInstance();
            models.GameState modelState = helper.createGameState(gameId);
            
            // Convert to our inner GameState type
            GameState innerState = new GameState(gameId);
            // Update the basic data
            if (modelState != null) {
                // Convert utils.GameType to models.GameType
                utils.GameType utilsGameType = modelState.getGameType();
                if (utilsGameType != null) {
                    innerState.setGameType(GameTypeConverter.fromUtilsGameType(utilsGameType));
                }
                if (modelState.getScreenshot() != null) {
                    // Convert android.graphics.Bitmap to utils.Bitmap
                    innerState.setCurrentScreenshot(
                        BitmapConverter.toUtilsBitmap(modelState.getScreenshot())
                    );
                }
                
                // Copy metadata to state data
                Map<String, Object> stateData = new HashMap<>();
                stateData.putAll(modelState.getMetadata());
                innerState.setStateData(stateData);
            }
            
            gameStates.put(gameId, innerState);
        }
        return gameStates.get(gameId);
    }
    
    /**
     * Get a game state
     * 
     * @param gameId Game identifier
     * @return Game state or null if not found
     */
    public GameState getGameState(String gameId) {
        return gameStates.get(gameId);
    }
    
    /**
     * Clear state for a game
     * 
     * @param gameId Game identifier
     */
    public void clearGameState(String gameId) {
        gameStates.remove(gameId);
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
     * Get total count of listeners
     * @return Total number of listeners across all games
     */
    public int getTotalListenersCount() {
        int count = 0;
        for (List<SuggestionListener> listeners : suggestionListeners.values()) {
            count += listeners.size();
        }
        return count;
    }
    
    /**
     * Initialize the prediction system
     */
    public void initialize() {
        // Already initialized in constructor, this is for compatibility
        isActive = true;
    }
    
    /**
     * Release resources
     */
    public void release() {
        isActive = false;
        
        // Clear all listeners
        suggestionListeners.clear();
        
        // Clear all game states
        gameStates.clear();
    }
    
    /**
     * Get the current game state
     * 
     * @return Current game state or null if not available
     */
    public GameState getCurrentState() {
        return getGameState("default");
    }
    
    /**
     * Get the processed frame count
     * 
     * @return The number of frames processed
     */
    public int getProcessedFrameCount() {
        return processedFrameCount;
    }
    
    /**
     * Get the prediction confidence
     * 
     * @return The last prediction confidence
     */
    public double getPredictionConfidence() {
        return lastPredictionConfidence;
    }
    
    /**
     * Check if the system is active
     * 
     * @return True if the system is active, false otherwise
     */
    public boolean isActive() {
        return isActive;
    }
    
    /**
     * Get the prediction accuracy
     * 
     * @return The current prediction accuracy (0.0 to 1.0)
     */
    private float calculateBasePredictionAccuracy() {
        // In a real implementation, this would track actual vs. predicted outcomes
        // For now, return a simulated accuracy based on the number of frames processed
        float baseAccuracy = 0.7f;
        float experienceBonus = Math.min(0.25f, processedFrameCount / 1000.0f);
        return baseAccuracy + experienceBonus;
    }
    
    /**
     * Get statistics about the prediction system
     * 
     * @return Map of statistics
     */
    public Map<String, Object> getStats() {
        Map<String, Object> stats = new HashMap<>();
        stats.put("active", isActive);
        stats.put("game_states_count", gameStates.size());
        stats.put("listeners_count", getTotalListenersCount());
        stats.put("processed_frames", processedFrameCount);
        stats.put("prediction_confidence", lastPredictionConfidence);
        
        if (rlModel != null) {
            stats.put("model_trained", true);
            stats.put("model_accuracy", getPredictionAccuracy());
        }
        
        return stats;
    }
    
    /**
     * Get the current prediction accuracy of the system
     * 
     * @return The prediction accuracy as a float between 0.0 and 1.0
     */
    public float getPredictionAccuracy() {
        // If we have a real model, we could calculate this based on validation data
        // For now, we'll use the last prediction confidence if available,
        // or a reasonable default otherwise
        
        if (lastPredictionConfidence > 0) {
            return (float)lastPredictionConfidence;
        }
        
        // Default accuracy based on the current state of the system
        if (rlModel != null && processedFrameCount > 100) {
            return 0.85f; // More mature system
        } else if (processedFrameCount > 50) {
            return 0.7f;  // System with some data
        } else {
            return 0.5f;  // New system with limited data
        }
    }
    

    
    /**
     * Add a suggestion listener
     * 
     * @param listener Listener to add
     */
    public void addSuggestionListener(SuggestionListener listener) {
        // Since we don't have a game ID, use a default one
        registerSuggestionListener("default", listener);
    }
    
    /**
     * Predict the next best action based on current context
     * 
     * @param currentContext Map containing context information
     * @param callback Optional callback for async prediction completion
     * @return Predicted action or null if prediction failed
     */
    public ActionPrediction predictNextAction(Map<String, Object> currentContext, PredictionCallback callback) {
        if (!isActive || currentContext == null) {
            return null;
        }
        
        // Create a temporary model GameState from context using the helper
        utils.GameStateHelper helper = utils.GameStateHelper.getInstance();
        models.GameState modelState = helper.createGameState(currentContext);
        
        // Convert to our inner GameState format
        GameState state = new GameState("temp_" + System.currentTimeMillis());
        // Update the basic data
        if (modelState != null) {
            // Convert utils.GameType to models.GameType 
            utils.GameType utilsGameType = modelState.getGameType();
            if (utilsGameType != null) {
                state.setGameType(GameTypeConverter.fromUtilsGameType(utilsGameType));
            }
            if (modelState.getScreenshot() != null) {
                // Convert android.graphics.Bitmap to utils.Bitmap
                state.setCurrentScreenshot(
                    BitmapConverter.toUtilsBitmap(modelState.getScreenshot())
                );
            }
            
            // Copy metadata to state data
            Map<String, Object> stateData = new HashMap<>();
            stateData.putAll(modelState.getMetadata());
            state.setStateData(stateData);
        }
        
        // Generate predictions
        List<ActionPrediction> predictions = generatePredictions(state);
        
        // Get the best prediction
        ActionPrediction bestAction = predictions.isEmpty() ? null : predictions.get(0);
        
        // Call the callback if provided
        if (callback != null && bestAction != null) {
            Map<String, Object> context = new HashMap<>();
            context.put("confidence", bestAction.getConfidence());
            callback.onPredictionGenerated(bestAction, context);
        }
        
        return bestAction;
    }
    

    
    /**
     * Get suggestions for a specific app
     * 
     * @param appInfo App information
     * @param contextData Additional context data
     */
    public void getSuggestions(Object appInfo, Map<String, Object> contextData) {
        if (!isActive || contextData == null) {
            return;
        }
        
        // Use context data as features
        GameState state = getOrCreateGameState("default");
        state.updateState(contextData);
        
        // Generate predictions
        List<ActionPrediction> predictions = generatePredictions(state);
        
        // Notify listeners
        notifyListeners("default", predictions);
    }
    
    // SuggestionListener is now a separate interface in models/SuggestionListener.java
    // PredictionCallback is now a separate interface in models/PredictionCallback.java
}