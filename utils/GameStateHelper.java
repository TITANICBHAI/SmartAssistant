package utils;

import java.util.HashMap;
import java.util.Map;
import java.util.ArrayList;
import java.util.List;
import models.GameState;
import models.StandardizedUIElement;
import models.GameType;

/**
 * Helper class for creating and manipulating GameState objects.
 * This class provides compatibility methods for different constructor patterns.
 */
public class GameStateHelper {
    private static GameStateHelper instance;
    
    /**
     * Get the singleton instance
     * @return GameStateHelper instance
     */
    public static synchronized GameStateHelper getInstance() {
        if (instance == null) {
            instance = new GameStateHelper();
        }
        return instance;
    }
    
    /**
     * Private constructor to enforce singleton pattern
     */
    private GameStateHelper() {
        // Private constructor
    }
    
    /**
     * Create a new GameState with just a game ID
     * @param gameId The game ID
     * @return A new GameState
     */
    public GameState createGameState(String gameId) {
        GameState state = new GameState();
        state.setMetadataValue("game_id", gameId);
        return state;
    }
    
    /**
     * Create a new GameState from a context map
     * @param context The context map
     * @return A new GameState
     */
    public GameState createGameState(Map<String, Object> context) {
        GameState state = new GameState();
        
        if (context.containsKey("game_type")) {
            String gameTypeStr = (String) context.get("game_type");
            GameType gameType = GameType.UNKNOWN;
            
            try {
                // Try to parse as a GameType enum
                gameType = GameType.valueOf(gameTypeStr.toUpperCase());
            } catch (IllegalArgumentException e) {
                // If not a valid enum, try generic conversion
                gameType = GameTypeHelper.fromString(gameTypeStr);
            }
            
            state.setGameType(gameType);
        }
        
        if (context.containsKey("screen_type")) {
            state.setScreenType((String) context.get("screen_type"));
        }
        
        state.setMetadata(context);
        return state;
    }
    
    /**
     * Update a GameState with new data
     * @param state The GameState to update
     * @param gameType The game type
     * @param screenshot The current screenshot
     */
    public void updateGameState(GameState state, GameType gameType, android.graphics.Bitmap screenshot) {
        if (state == null) {
            return;
        }
        
        // Make sure the GameState has the correct GameType (utils.GameType)
        state.setGameType(gameType);
        
        // For utils.GameState with android.graphics.Bitmap screenshot, 
        // we need to convert the type and then use the proper method
        if (screenshot != null) {
            // State already has a setScreenshot method that accepts android.graphics.Bitmap
            state.setScreenshot(screenshot);
        }
    }
    
    /**
     * Update a GameState with new screenshot from Android Bitmap
     * 
     * @param state The GameState to update
     * @param gameType The game type
     * @param screenshot The screenshot as an Android Bitmap
     */
    public void updateGameStateWithAndroidBitmap(GameState state, GameType gameType, android.graphics.Bitmap screenshot) {
        if (state == null) {
            return;
        }
        
        // Make sure the GameState has the correct GameType (utils.GameType)
        state.setGameType(gameType);
        
        // State already has a setScreenshot method that accepts android.graphics.Bitmap
        if (screenshot != null) {
            state.setScreenshot(screenshot);
        }
    }
    
    /**
     * Update a GameState with new features
     * @param state The GameState to update
     * @param features The new features
     */
    public void updateGameState(GameState state, Map<String, Object> features) {
        if (state == null || features == null) {
            return;
        }
        
        state.updateFromAnalysis(features);
    }
    
    /**
     * Convert a context object to a GameState
     * @param context The context object (could be String, Map, or GameState already)
     * @return A GameState object
     */
    public GameState toGameState(Object context) {
        if (context == null) {
            return createGameState("default");
        }
        
        if (context instanceof GameState) {
            return (GameState) context;
        }
        
        if (context instanceof String) {
            return createGameState((String) context);
        }
        
        if (context instanceof Map) {
            @SuppressWarnings("unchecked")
            Map<String, Object> contextMap = (Map<String, Object>) context;
            return createGameState(contextMap);
        }
        
        // Create a default GameState as fallback
        return createGameState("unknown");
    }
}