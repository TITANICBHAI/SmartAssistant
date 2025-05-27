package com.aiassistant.models;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Represents the state of a game for AI analysis and control
 */
public class GameState {
    // Screen analysis results
    private Map<String, Object> screenAnalysisResults = new HashMap<>();
    // Game identification
    private String gameId;
    private String gameName;
    private String gameType;
    
    // Game state
    private boolean isRunning;
    private GameScreenType currentScreen;
    private Map<String, Object> gameSpecificState = new HashMap<>();
    
    // Game entities
    private List<GameObject> gameObjects = new ArrayList<>();
    private GameObject playerObject;
    private List<GameObject> enemies = new ArrayList<>();
    private List<GameObject> collectibles = new ArrayList<>();
    
    // Game metrics
    private int score;
    private int level;
    private float gameTime;
    private Map<String, Float> gameMetrics = new HashMap<>();
    
    // Using the separate GameScreenType enum from models package
    
    /**
     * Default constructor
     */
    public GameState() {
    }
    
    /**
     * Constructor with game ID
     */
    public GameState(String gameId) {
        this.gameId = gameId;
    }
    
    /**
     * Get game ID
     */
    public String getGameId() {
        return gameId;
    }
    
    /**
     * Set game ID
     */
    public void setGameId(String gameId) {
        this.gameId = gameId;
    }
    
    /**
     * Get game name
     */
    public String getGameName() {
        return gameName;
    }
    
    /**
     * Set game name
     */
    public void setGameName(String gameName) {
        this.gameName = gameName;
    }
    
    /**
     * Get game type
     */
    public String getGameType() {
        return gameType;
    }
    
    /**
     * Set game type
     */
    public void setGameType(String gameType) {
        this.gameType = gameType;
    }
    
    /**
     * Check if game is running
     */
    public boolean isRunning() {
        return isRunning;
    }
    
    /**
     * Set if game is running
     */
    public void setRunning(boolean running) {
        isRunning = running;
    }
    
    /**
     * Get current game screen type
     */
    public GameScreenType getCurrentScreen() {
        return currentScreen;
    }
    
    /**
     * Set current game screen type
     */
    public void setCurrentScreen(GameScreenType currentScreen) {
        this.currentScreen = currentScreen;
    }
    
    /**
     * Get game-specific state data
     */
    public Map<String, Object> getGameSpecificState() {
        return gameSpecificState;
    }
    
    /**
     * Set game-specific state data
     */
    public void setGameSpecificState(Map<String, Object> gameSpecificState) {
        this.gameSpecificState = gameSpecificState;
    }
    
    /**
     * Get specific game state value
     */
    public Object getGameStateValue(String key) {
        return gameSpecificState.get(key);
    }
    
    /**
     * Set specific game state value
     */
    public void setGameStateValue(String key, Object value) {
        this.gameSpecificState.put(key, value);
    }
    
    /**
     * Get all game objects
     */
    public List<GameObject> getGameObjects() {
        return gameObjects;
    }
    
    /**
     * Set all game objects
     */
    public void setGameObjects(List<GameObject> gameObjects) {
        this.gameObjects = gameObjects;
    }
    
    /**
     * Add a game object
     */
    public void addGameObject(GameObject gameObject) {
        this.gameObjects.add(gameObject);
        
        // Categorize the object
        if (gameObject.isPlayer()) {
            this.playerObject = gameObject;
        } else if (gameObject.isEnemy()) {
            this.enemies.add(gameObject);
        } else if (gameObject.isCollectible()) {
            this.collectibles.add(gameObject);
        }
    }
    
    /**
     * Get player object
     */
    public GameObject getPlayerObject() {
        return playerObject;
    }
    
    /**
     * Set player object
     */
    public void setPlayerObject(GameObject playerObject) {
        this.playerObject = playerObject;
    }
    
    /**
     * Get enemy objects
     */
    public List<GameObject> getEnemies() {
        return enemies;
    }
    
    /**
     * Set enemy objects
     */
    public void setEnemies(List<GameObject> enemies) {
        this.enemies = enemies;
    }
    
    /**
     * Get collectible objects
     */
    public List<GameObject> getCollectibles() {
        return collectibles;
    }
    
    /**
     * Set collectible objects
     */
    public void setCollectibles(List<GameObject> collectibles) {
        this.collectibles = collectibles;
    }
    
    /**
     * Get game score
     */
    public int getScore() {
        return score;
    }
    
    /**
     * Set game score
     */
    public void setScore(int score) {
        this.score = score;
    }
    
    /**
     * Get game level
     */
    public int getLevel() {
        return level;
    }
    
    /**
     * Set game level
     */
    public void setLevel(int level) {
        this.level = level;
    }
    
    /**
     * Get game time
     */
    public float getGameTime() {
        return gameTime;
    }
    
    /**
     * Set game time
     */
    public void setGameTime(float gameTime) {
        this.gameTime = gameTime;
    }
    
    /**
     * Get game metrics
     */
    public Map<String, Float> getGameMetrics() {
        return gameMetrics;
    }
    
    /**
     * Set game metrics
     */
    public void setGameMetrics(Map<String, Float> gameMetrics) {
        this.gameMetrics = gameMetrics;
    }
    
    /**
     * Update a specific game metric
     */
    public void updateGameMetric(String metricName, float value) {
        this.gameMetrics.put(metricName, value);
    }
    
    /**
     * Clear all game objects
     */
    public void clearGameObjects() {
        this.gameObjects.clear();
        this.enemies.clear();
        this.collectibles.clear();
        this.playerObject = null;
    }
    
    /**
     * Get screen analysis results
     * 
     * @return Map of screen analysis results
     */
    public Map<String, Object> getScreenAnalysisResults() {
        return screenAnalysisResults;
    }
    
    /**
     * Set screen analysis results
     * 
     * @param results Map of screen analysis results
     */
    public void setScreenAnalysisResults(Map<String, Object> results) {
        this.screenAnalysisResults = results != null ? results : new HashMap<>();
    }
    
    /**
     * Update the game state based on screen analysis
     */
    public void updateFromScreenAnalysis() {
        if (screenAnalysisResults.isEmpty()) {
            return;
        }
        
        // Extract relevant information from screen analysis
        // and update the game state accordingly
        
        // Example: Update game objects based on detected elements
        if (screenAnalysisResults.containsKey("detectedObjects")) {
            Object detectedObjects = screenAnalysisResults.get("detectedObjects");
            if (detectedObjects instanceof List) {
                // Process detected objects...
            }
        }
        
        // Example: Update game state based on screen text
        if (screenAnalysisResults.containsKey("screenText")) {
            Object screenText = screenAnalysisResults.get("screenText");
            if (screenText instanceof String) {
                // Parse screen text...
            }
        }
        
        // Example: Update score if detected
        if (screenAnalysisResults.containsKey("score")) {
            Object scoreObj = screenAnalysisResults.get("score");
            if (scoreObj instanceof Number) {
                this.score = ((Number) scoreObj).intValue();
            } else if (scoreObj instanceof String) {
                try {
                    this.score = Integer.parseInt((String) scoreObj);
                } catch (NumberFormatException e) {
                    // Failed to parse score
                }
            }
        }
    }
    
    /**
     * Inner class representing game objects
     */
    public static class GameObject {
        private String id;
        private String type;
        private float[] position = new float[3]; // x, y, z
        private float[] size = new float[2]; // width, height
        private float rotation;
        private String state;
        private boolean isPlayer;
        private boolean isEnemy;
        private boolean isCollectible;
        private boolean isObstacle;
        private Map<String, Object> properties = new HashMap<>();
        
        public GameObject() {
        }
        
        public GameObject(String id, String type) {
            this.id = id;
            this.type = type;
        }
        
        public String getId() {
            return id;
        }
        
        public void setId(String id) {
            this.id = id;
        }
        
        public String getType() {
            return type;
        }
        
        public void setType(String type) {
            this.type = type;
        }
        
        public float[] getPosition() {
            return position;
        }
        
        public void setPosition(float[] position) {
            this.position = position;
        }
        
        public float[] getSize() {
            return size;
        }
        
        public void setSize(float[] size) {
            this.size = size;
        }
        
        public float getRotation() {
            return rotation;
        }
        
        public void setRotation(float rotation) {
            this.rotation = rotation;
        }
        
        public String getState() {
            return state;
        }
        
        public void setState(String state) {
            this.state = state;
        }
        
        public boolean isPlayer() {
            return isPlayer;
        }
        
        public void setPlayer(boolean player) {
            isPlayer = player;
        }
        
        public boolean isEnemy() {
            return isEnemy;
        }
        
        public void setEnemy(boolean enemy) {
            isEnemy = enemy;
        }
        
        public boolean isCollectible() {
            return isCollectible;
        }
        
        public void setCollectible(boolean collectible) {
            isCollectible = collectible;
        }
        
        public boolean isObstacle() {
            return isObstacle;
        }
        
        public void setObstacle(boolean obstacle) {
            isObstacle = obstacle;
        }
        
        public Map<String, Object> getProperties() {
            return properties;
        }
        
        public void setProperties(Map<String, Object> properties) {
            this.properties = properties;
        }
        
        public void addProperty(String key, Object value) {
            this.properties.put(key, value);
        }
        
        public Object getProperty(String key) {
            return this.properties.get(key);
        }
        
        /**
         * Calculate distance to another game object
         */
        public float distanceTo(GameObject other) {
            float dx = this.position[0] - other.position[0];
            float dy = this.position[1] - other.position[1];
            return (float) Math.sqrt(dx * dx + dy * dy);
        }
        
        /**
         * Check if this object collides with another
         */
        public boolean collidesWith(GameObject other) {
            // Simple AABB collision detection
            return (
                this.position[0] < other.position[0] + other.size[0] &&
                this.position[0] + this.size[0] > other.position[0] &&
                this.position[1] < other.position[1] + other.size[1] &&
                this.position[1] + this.size[1] > other.position[1]
            );
        }
    }
}