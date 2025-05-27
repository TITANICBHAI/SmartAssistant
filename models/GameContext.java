package models;

import utils.Context;
import java.util.List;
import java.util.Map;
import java.util.ArrayList;

/**
 * Interface for game-specific context
 * Extends the basic Context with game-specific functionality
 * Note: This interface has been refactored to use standalone class definitions
 * instead of nested interfaces to address compilation issues
 */
public interface GameContext extends Context {
    /**
     * A wrapper for application info that implements the standalone GameApplicationInfo interface
     * This class has been refactored to avoid nested interface definition issues
     */
    public static class GameApplicationInfoImpl implements GameApplicationInfo {
        private final String packageName;
        private final String appName;
        private final String versionName;
        private final int versionCode;
        private final Map<String, Object> metadata;
        
        /**
         * Create a new GameApplicationInfoImpl
         * 
         * @param packageName The package name
         * @param appName The application name
         * @param versionName The version name
         * @param versionCode The version code
         * @param metadata Additional metadata
         */
        public GameApplicationInfoImpl(String packageName, String appName, String versionName, 
                              int versionCode, Map<String, Object> metadata) {
            this.packageName = packageName;
            this.appName = appName;
            this.versionName = versionName;
            this.versionCode = versionCode;
            this.metadata = metadata;
        }
        
        /**
         * Create a new GameApplicationInfoImpl with default version code and no metadata
         * 
         * @param packageName The package name
         * @param appName The application name
         * @param versionName The version name
         */
        public GameApplicationInfoImpl(String packageName, String appName, String versionName) {
            this(packageName, appName, versionName, 1, null);
        }
        
        @Override
        public String getPackageName() {
            return packageName;
        }
        
        @Override
        public String getAppName() {
            return appName;
        }
        
        @Override
        public String getVersionName() {
            return versionName;
        }
        
        @Override
        public int getVersionCode() {
            return versionCode;
        }
        
        @Override
        public Map<String, Object> getMetadata() {
            return metadata;
        }
        
        /**
         * Convert to standard ApplicationInfo
         * 
         * @return A new ApplicationInfo instance
         */
        public ApplicationInfo toApplicationInfo() {
            models.ApplicationInfo.DefaultApplicationInfo appInfo = 
                new models.ApplicationInfo.DefaultApplicationInfo(packageName, appName, versionName, versionCode);
            
            // Set the metadata after construction
            if (metadata != null) {
                appInfo.setAllMetadata(metadata);
            }
            
            return appInfo;
        }
        
        /**
         * Create from standard ApplicationInfo
         * 
         * @param info The source ApplicationInfo
         * @return A new GameApplicationInfoImpl instance or null if info is null
         */
        public static GameApplicationInfoImpl fromApplicationInfo(ApplicationInfo info) {
            if (info == null) {
                return null;
            }
            // Create a new instance with the basic information
            GameApplicationInfoImpl appInfo = new GameApplicationInfoImpl(
                info.getPackageName(),
                info.getApplicationName(),
                info.getVersionName(),
                info.getVersionCode(),
                info.getAllMetadata()
            );
            return appInfo;
        }
    }
    
    /**
     * A wrapper for package manager functionality that implements the GamePackageManager interface
     * This class has been refactored to avoid nested interface definition issues
     */
    public static class GamePackageManagerImpl implements GamePackageManager {
        private final Map<String, GameApplicationInfo> installedPackages;
        
        /**
         * Create a new GamePackageManagerImpl
         * 
         * @param installedPackages Map of installed packages
         */
        public GamePackageManagerImpl(Map<String, GameApplicationInfo> installedPackages) {
            this.installedPackages = installedPackages;
        }
        
        @Override
        public GameApplicationInfo getPackageInfo(String packageName) {
            return installedPackages != null ? installedPackages.get(packageName) : null;
        }
        
        @Override
        public boolean isPackageInstalled(String packageName) {
            return installedPackages != null && installedPackages.containsKey(packageName);
        }
        
        @Override
        public List<GameApplicationInfo> getInstalledPackages() {
            return installedPackages != null ? new ArrayList<>(installedPackages.values()) : new ArrayList<>();
        }
        
        /**
         * Get all installed packages as a map
         * 
         * @return Map of all installed packages
         */
        public Map<String, GameApplicationInfo> getInstalledPackagesMap() {
            return installedPackages;
        }
    }
    /**
     * Get the underlying Context object
     * 
     * @return The context object
     */
    Context getContext();
    /**
     * Get the game type
     * 
     * @return The type of game for this context
     */
    StandardizedGameType getGameType();
    
    /**
     * Set the game type
     * 
     * @param gameType The game type to set
     */
    void setGameType(StandardizedGameType gameType);
    
    /**
     * Get game state information
     * 
     * @return Map containing the current game state
     */
    Map<String, Object> getGameState();
    
    /**
     * Get detected UI elements
     * 
     * @return List of detected UI elements in the game
     */
    List<StandardizedUIElement> getUIElements();
    
    /**
     * Get detected game objects
     * 
     * @return List of detected game objects
     */
    List<Object> getGameObjects();
    
    /**
     * Get available actions
     * 
     * @return List of available actions in the current state
     */
    List<String> getAvailableActions();
    
    /**
     * Get the current player state
     * 
     * @return Map containing player state information
     */
    Map<String, Object> getPlayerState();
    
    /**
     * Get the current score or progress
     * 
     * @return Score or progress value
     */
    int getScore();
    
    /**
     * Get the current level or stage
     * 
     * @return Level or stage identifier
     */
    String getLevel();
    
    /**
     * Get the game difficulty
     * 
     * @return Difficulty level
     */
    int getDifficulty();
    
    /**
     * Check if the game is in progress
     * 
     * @return True if the game is currently active
     */
    boolean isGameInProgress();
    
    /**
     * Check if the game is paused
     * 
     * @return True if the game is paused
     */
    boolean isGamePaused();
    
    /**
     * Get game rules
     * 
     * @return Map describing the game rules
     */
    Map<String, Object> getGameRules();
    
    /**
     * Get recognized patterns
     * 
     * @return List of recognized patterns in the game
     */
    List<String> getRecognizedPatterns();
    
    /**
     * Get a configuration value by key
     * 
     * @param key The configuration key
     * @return The configuration value, or null if not found
     */
    Object getConfig(String key);
    
    /**
     * Set a configuration value
     * 
     * @param key The configuration key
     * @param value The configuration value
     */
    void setConfig(String key, Object value);
    
    /**
     * Get all configuration values
     * 
     * @return Map of all configurations
     */
    Map<String, Object> getAllConfig();
    
    /**
     * Get a configuration or setting by key
     * 
     * @param key The setting key
     * @param defaultValue Default value if setting not found
     * @return The setting value, or defaultValue if not found
     */
    default Object getSetting(String key, Object defaultValue) {
        Object value = getConfig(key);
        return value != null ? value : defaultValue;
    }
    
    /**
     * Set a configuration or setting value
     * 
     * @param key The setting key
     * @param value The setting value
     */
    default void setSetting(String key, Object value) {
        setConfig(key, value);
    }
    
    /**
     * Get all settings
     * 
     * @return Map of all settings
     */
    default Map<String, Object> getSettings() {
        return getAllConfig();
    }
}