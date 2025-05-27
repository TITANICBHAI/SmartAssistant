package utils;

import java.util.HashMap;
import java.util.Map;

/**
 * Represents the state of a game at a particular point in time.
 * This class is compatible with the utils package and serves as a bridge
 * between the models.GameState and other game state representations.
 */
public class GameState {
    private String gameId;
    private GameType gameType = GameType.UNKNOWN;
    private Bitmap currentScreenshot;
    private Map<String, Object> stateData = new HashMap<>();
    private long timestamp = System.currentTimeMillis();
    private int frameId = 0;
    private String screenType;

    /**
     * Create a new empty game state
     */
    public GameState() {
        this("unknown");
    }

    /**
     * Create a new game state with the specified ID
     * 
     * @param gameId The ID of the game
     */
    public GameState(String gameId) {
        this.gameId = gameId;
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
     * Set the game ID
     * 
     * @param gameId The new game ID
     */
    public void setGameId(String gameId) {
        this.gameId = gameId;
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

    /**
     * Get the current screenshot
     * 
     * @return The current screenshot as a Bitmap
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
     * Set the current screenshot - alias for setCurrentScreenshot method
     * For compatibility with different screenshot APIs
     * 
     * @param screenshot The new screenshot as a Bitmap
     */
    public void setScreenshot(Bitmap screenshot) {
        setCurrentScreenshot(screenshot);
    }
    
    /**
     * Set the current screenshot - handles android.graphics.Bitmap conversion
     * 
     * @param screenshot The new screenshot as an android.graphics.Bitmap
     */
    public void setScreenshot(android.graphics.Bitmap screenshot) {
        // Convert android.graphics.Bitmap to utils.Bitmap
        if (screenshot != null) {
            utils.Bitmap utilsBitmap = models.BitmapConverter.toUtilsBitmap(screenshot);
            setCurrentScreenshot(utilsBitmap);
        } else {
            setCurrentScreenshot(null);
        }
    }

    /**
     * Get the state data
     * 
     * @return The state data
     */
    public Map<String, Object> getStateData() {
        return stateData;
    }

    /**
     * Set the state data
     * 
     * @param stateData The new state data
     */
    public void setStateData(Map<String, Object> stateData) {
        if (stateData != null) {
            this.stateData = stateData;
        } else {
            this.stateData = new HashMap<>();
        }
    }

    /**
     * Update the state with the specified context data
     * 
     * @param contextData The context data to update with
     */
    public void updateState(Map<String, Object> contextData) {
        if (contextData != null) {
            stateData.putAll(contextData);
        }
    }

    /**
     * Get the timestamp
     * 
     * @return The timestamp
     */
    public long getTimestamp() {
        return timestamp;
    }

    /**
     * Set the timestamp
     * 
     * @param timestamp The new timestamp
     */
    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }

    /**
     * Get the frame ID
     * 
     * @return The frame ID
     */
    public int getFrameId() {
        return frameId;
    }

    /**
     * Set the frame ID
     * 
     * @param frameId The new frame ID
     */
    public void setFrameId(int frameId) {
        this.frameId = frameId;
    }

    /**
     * Get the screen type
     * 
     * @return The screen type
     */
    public String getScreenType() {
        return screenType;
    }

    /**
     * Set the screen type
     * 
     * @param screenType The new screen type
     */
    public void setScreenType(String screenType) {
        this.screenType = screenType;
    }
}