package com.aiassistant.models;

import java.util.List;
import java.util.Map;
import android.graphics.Bitmap;

/**
 * Class representing the state of a game.
 */
public class GameState {
    private GameType gameType;
    private UIElement[] uiElements;
    private Bitmap screenshot;
    private Map<String, Object> metadata;
    private int lives;
    private int score;
    
    /**
     * Create a new GameState
     */
    public GameState() {
        // Default constructor
    }
    
    /**
     * Create a new GameState
     * @param gameType The game type
     * @param uiElements The UI elements
     * @param screenshot The screenshot
     */
    public GameState(GameType gameType, UIElement[] uiElements, Bitmap screenshot) {
        this.gameType = gameType;
        this.uiElements = uiElements;
        this.screenshot = screenshot;
    }
    
    /**
     * Get the game type
     * @return The game type
     */
    public GameType getGameType() {
        return gameType;
    }
    
    /**
     * Set the game type
     * @param gameType The game type
     */
    public void setGameType(GameType gameType) {
        this.gameType = gameType;
    }
    
    /**
     * Get the UI elements
     * @return The UI elements
     */
    public UIElement[] getUIElements() {
        return uiElements;
    }
    
    /**
     * Set the UI elements
     * @param uiElements The UI elements
     */
    public void setUIElements(UIElement[] uiElements) {
        this.uiElements = uiElements;
    }
    
    /**
     * Get the screenshot
     * @return The screenshot
     */
    public Bitmap getScreenshot() {
        return screenshot;
    }
    
    /**
     * Set the screenshot
     * @param screenshot The screenshot
     */
    public void setScreenshot(Bitmap screenshot) {
        this.screenshot = screenshot;
    }
    
    /**
     * Get the metadata
     * @return The metadata
     */
    public Map<String, Object> getMetadata() {
        return metadata;
    }
    
    /**
     * Set the metadata
     * @param metadata The metadata
     */
    public void setMetadata(Map<String, Object> metadata) {
        this.metadata = metadata;
    }
    
    /**
     * Get the lives
     * @return The lives
     */
    public int getLives() {
        return lives;
    }
    
    /**
     * Set the lives
     * @param lives The lives
     */
    public void setLives(int lives) {
        this.lives = lives;
    }
    
    /**
     * Get the score
     * @return The score
     */
    public int getScore() {
        return score;
    }
    
    /**
     * Set the score
     * @param score The score
     */
    public void setScore(int score) {
        this.score = score;
    }
}