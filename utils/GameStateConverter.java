package utils;

import java.util.HashMap;
import java.util.Map;
import models.GameState;
import models.GameType;
import models.PredictiveActionSystem;

/**
 * Converter utility to convert between different GameState implementations.
 * This handles the conversion between models.GameState and models.PredictiveActionSystem.GameState.
 */
public class GameStateConverter {
    
    /**
     * Convert a models.GameState to a PredictiveActionSystem.GameState
     * 
     * @param state The models.GameState to convert
     * @return The equivalent PredictiveActionSystem.GameState
     */
    public static models.PredictiveActionSystem.GameState toPasGameState(models.GameState state) {
        if (state == null) {
            return null;
        }
        
        String gameId = (String) state.getMetadataValue("game_id");
        if (gameId == null) {
            gameId = "unknown_" + System.currentTimeMillis();
        }
        
        // Create a new PredictiveActionSystem.GameState
        models.PredictiveActionSystem.GameState pasState = new models.PredictiveActionSystem.GameState(gameId);
        
        // Set game data
        Map<String, Object> stateData = new HashMap<>();
        stateData.putAll(state.getMetadata());
        stateData.put("screen_type", state.getScreenType());
        stateData.put("timestamp", state.getTimestamp());
        stateData.put("frame_id", state.getFrameId());
        pasState.setStateData(stateData);
        
        // Set game type - make sure to set the correct type
        // PAS game state expects utils.GameType
        utils.GameType utilsGameType = null;
        
        if (state.getGameType() != null) {
            // Get the models.GameType from the state
            models.GameType modelsGameType = null;
            try {
                // Handle possible ClassCastException by using a try-catch
                Object gameType = state.getGameType();
                if (gameType instanceof models.GameType) {
                    modelsGameType = (models.GameType) gameType;
                } else if (gameType instanceof utils.GameType) {
                    // If it's already a utils.GameType, convert from that
                    utilsGameType = (utils.GameType) gameType;
                }
            } catch (Exception e) {
                // In case of error, use UNKNOWN
                utilsGameType = utils.GameType.UNKNOWN;
            }
            // Only convert if modelsGameType was set and utilsGameType wasn't already set
            if (modelsGameType != null && utilsGameType == null) {
                utilsGameType = models.GameTypeConverter.toUtilsGameType(modelsGameType);
            } else if (utilsGameType == null) {
                // Default to UNKNOWN if conversion wasn't possible
                utilsGameType = utils.GameType.UNKNOWN;
            }
        } else {
            utilsGameType = utils.GameType.UNKNOWN;
        }
        
        // Since we can't directly assign utils.GameType to models.GameType field,
        // we'll use a different approach to ensure the game type information is preserved
        
        // 1. Convert to models.GameType
        models.GameType modelsGameType = GameTypeConverter.toModelsGameType(utilsGameType);
        
        // 2. Use reflection to find a compatible method to set the game type
        boolean success = false;
        
        // Try Method #1: setGameType with models.GameType parameter
        try {
            java.lang.reflect.Method method = pasState.getClass().getMethod("setGameType", models.GameType.class);
            method.invoke(pasState, modelsGameType);
            success = true;
        } catch (Exception e) {
            // Method not found or invocation failed, continue to next attempt
        }
        
        // Try Method #2: setGameType with Object parameter
        if (!success) {
            try {
                java.lang.reflect.Method method = pasState.getClass().getMethod("setGameType", Object.class);
                method.invoke(pasState, modelsGameType);
                success = true;
            } catch (Exception e) {
                // Method not found or invocation failed, continue to next attempt
            }
        }
        
        // Try Method #3: Direct field access
        if (!success) {
            try {
                java.lang.reflect.Field field = pasState.getClass().getDeclaredField("gameType");
                field.setAccessible(true);
                field.set(pasState, modelsGameType);
                success = true;
            } catch (Exception e) {
                // Field not found or access failed, continue to next attempt
            }
        }
        
        // Fallback: Store the game type information in the state data
        if (!success) {
            // Get current state data or create new map
            Map<String, Object> currentStateData = pasState.getStateData();
            Map<String, Object> updatedStateData = (currentStateData != null) ? 
                new HashMap<>(currentStateData) : new HashMap<>();
            
            // Store type information as strings in state data map
            updatedStateData.put("game_type_name", utilsGameType.name());
            updatedStateData.put("game_type_ordinal", utilsGameType.ordinal());
            
            // Update the state data
            pasState.setStateData(updatedStateData);
            System.err.println("Using fallback method to store GameType information in state data");
        }
        
        // Set screenshot if available - use converter
        if (state.getScreenshot() != null) {
            utils.Bitmap utilsBitmap = models.BitmapConverter.toUtilsBitmap(state.getScreenshot());
            pasState.setCurrentScreenshot(utilsBitmap);
        }
        
        return pasState;
    }
    
    /**
     * Convert a PredictiveActionSystem.GameState to a models.GameState
     * 
     * @param pasState The PredictiveActionSystem.GameState to convert
     * @return The equivalent models.GameState
     */
    public static models.GameState toModelGameState(models.PredictiveActionSystem.GameState pasState) {
        if (pasState == null) {
            return null;
        }
        
        // Create a new models.GameState
        models.GameState state = new models.GameState();
        
        // Set game ID as metadata
        state.setMetadataValue("game_id", pasState.getGameId());
        
        // Set game type - get utils.GameType from PAS and convert to models.GameType
        models.GameType modelsGameType = null;
        
        if (pasState.getGameType() != null) {
            // Use GameTypeConverter to convert from utils.GameType to models.GameType
            // Get the GameType from PAS GameState, which might be utils.GameType or possibly models.GameType
            utils.GameType utilsType = null;
            try {
                // Use Object to handle any type
                Object gameType = pasState.getGameType();
                if (gameType instanceof utils.GameType) {
                    utilsType = (utils.GameType) gameType;
                } else if (gameType instanceof models.GameType) {
                    // If it's already a models.GameType, set it directly and skip conversion
                    modelsGameType = (models.GameType) gameType;
                }
            } catch (Exception e) {
                // In case of error, use UNKNOWN
                modelsGameType = models.GameType.UNKNOWN;
            }
            // Convert utilsType to modelsGameType only if needed
            if (modelsGameType == null && utilsType != null) {
                modelsGameType = utils.GameTypeConverter.toModelsGameType(utilsType);
            } else if (modelsGameType == null) {
                // Default to UNKNOWN if we couldn't get either type
                modelsGameType = models.GameType.UNKNOWN;
            }
        } else {
            modelsGameType = models.GameType.UNKNOWN;
        }
        
        state.setGameType(modelsGameType);
        
        // Set screenshot if available - use converter
        if (pasState.getCurrentScreenshot() != null) {
            android.graphics.Bitmap androidBitmap = models.BitmapConverter.toAndroidBitmap(pasState.getCurrentScreenshot());
            state.setScreenshot(androidBitmap);
        }
        
        // Set other data from state data
        Map<String, Object> stateData = pasState.getStateData();
        if (stateData != null) {
            if (stateData.containsKey("screen_type")) {
                state.setScreenType((String) stateData.get("screen_type"));
            }
            
            if (stateData.containsKey("timestamp")) {
                Object timestampObj = stateData.get("timestamp");
                if (timestampObj instanceof Long) {
                    state.setTimestamp((Long) timestampObj);
                } else if (timestampObj != null) {
                    try {
                        state.setTimestamp(Long.parseLong(timestampObj.toString()));
                    } catch (NumberFormatException e) {
                        // Use current time if parsing fails
                        state.setTimestamp(System.currentTimeMillis());
                    }
                }
            }
            
            if (stateData.containsKey("frame_id")) {
                Object frameIdObj = stateData.get("frame_id");
                if (frameIdObj instanceof Integer) {
                    state.setFrameId((Integer) frameIdObj);
                } else if (frameIdObj != null) {
                    try {
                        state.setFrameId(Integer.parseInt(frameIdObj.toString()));
                    } catch (NumberFormatException e) {
                        // Use 0 if parsing fails
                        state.setFrameId(0);
                    }
                }
            }
            
            // Copy remaining data to metadata
            Map<String, Object> metadata = new HashMap<>();
            for (Map.Entry<String, Object> entry : stateData.entrySet()) {
                String key = entry.getKey();
                if (!key.equals("screen_type") && !key.equals("timestamp") && !key.equals("frame_id")) {
                    metadata.put(key, entry.getValue());
                }
            }
            state.setMetadata(metadata);
        }
        
        return state;
    }
    
    /**
     * Convert any type of game state to a models.GameState
     * 
     * @param state The game state to convert (could be models.GameState or PredictiveActionSystem.GameState)
     * @return The equivalent models.GameState
     */
    public static models.GameState ensureModelGameState(Object state) {
        if (state == null) {
            return null;
        }
        
        if (state instanceof models.GameState) {
            return (models.GameState) state;
        }
        
        if (state instanceof models.PredictiveActionSystem.GameState) {
            return toModelGameState((models.PredictiveActionSystem.GameState) state);
        }
        
        // For any other type, use GameStateHelper to create a new state
        return GameStateHelper.getInstance().toGameState(state);
    }
    
    /**
     * Convert any type of game state to a PredictiveActionSystem.GameState
     * 
     * @param state The game state to convert (could be models.GameState or PredictiveActionSystem.GameState)
     * @return The equivalent PredictiveActionSystem.GameState
     */
    public static models.PredictiveActionSystem.GameState ensurePasGameState(Object state) {
        if (state == null) {
            return null;
        }
        
        if (state instanceof models.PredictiveActionSystem.GameState) {
            return (models.PredictiveActionSystem.GameState) state;
        }
        
        if (state instanceof models.GameState) {
            return toPasGameState((models.GameState) state);
        }
        
        // For any other type, first convert to models.GameState using helper, then convert to PAS.GameState
        models.GameState modelState = GameStateHelper.getInstance().toGameState(state);
        return toPasGameState(modelState);
    }
}