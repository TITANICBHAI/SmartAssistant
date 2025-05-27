package models;

import android.graphics.Bitmap;
import java.util.HashMap;
import java.util.Map;

/**
 * Information about an application.
 * This includes metadata about the app such as package name, app name, and game type.
 */
public class AppInfo {
    private String packageName;
    private String appName;
    private String version;
    private Bitmap icon;
    private GameType gameType;
    private StandardizedGameType standardizedGameType;
    private boolean isGame;
    private Map<String, Object> metadata;
    
    /**
     * Create a new AppInfo
     */
    public AppInfo() {
        this.packageName = "";
        this.appName = "";
        this.version = "";
        this.icon = null;
        this.gameType = GameType.UNKNOWN;
        this.standardizedGameType = new StandardizedGameType(StandardizedGameType.GameType.UNKNOWN);
        this.isGame = false;
        this.metadata = new HashMap<>();
    }
    
    /**
     * Create a new AppInfo with basic information
     * 
     * @param packageName Package name
     * @param appName App name
     * @param version Version string
     */
    public AppInfo(String packageName, String appName, String version) {
        this.packageName = packageName;
        this.appName = appName;
        this.version = version;
        this.icon = null;
        this.gameType = determineGameTypeFromPackage(packageName);
        this.standardizedGameType = convertGameTypeToStandardized(this.gameType);
        this.isGame = (this.gameType != GameType.UNKNOWN);
        this.metadata = new HashMap<>();
    }
    
    /**
     * Create a new AppInfo with comprehensive information
     * 
     * @param packageName Package name
     * @param appName App name
     * @param version Version string
     * @param icon App icon
     * @param gameType Game type
     * @param metadata Additional metadata
     */
    public AppInfo(String packageName, String appName, String version, Bitmap icon, 
                   GameType gameType, Map<String, Object> metadata) {
        this.packageName = packageName;
        this.appName = appName;
        this.version = version;
        this.icon = icon;
        this.gameType = gameType;
        this.standardizedGameType = convertGameTypeToStandardized(gameType);
        this.isGame = (gameType != GameType.UNKNOWN);
        this.metadata = metadata != null ? new HashMap<>(metadata) : new HashMap<>();
    }
    
    /**
     * Get the package name
     * 
     * @return Package name
     */
    public String getPackageName() {
        return packageName;
    }
    
    /**
     * Set the package name
     * 
     * @param packageName New package name
     */
    public void setPackageName(String packageName) {
        this.packageName = packageName;
        this.gameType = determineGameTypeFromPackage(packageName);
        this.standardizedGameType = convertGameTypeToStandardized(this.gameType);
        this.isGame = (this.gameType != GameType.UNKNOWN);
    }
    
    /**
     * Get the app name
     * 
     * @return App name
     */
    public String getAppName() {
        return appName;
    }
    
    /**
     * Set the app name
     * 
     * @param appName New app name
     */
    public void setAppName(String appName) {
        this.appName = appName;
    }
    
    /**
     * Get the version
     * 
     * @return Version string
     */
    public String getVersion() {
        return version;
    }
    
    /**
     * Set the version
     * 
     * @param version New version string
     */
    public void setVersion(String version) {
        this.version = version;
    }
    
    /**
     * Get the icon
     * 
     * @return App icon
     */
    public Bitmap getIcon() {
        return icon;
    }
    
    /**
     * Set the icon
     * 
     * @param icon New app icon
     */
    public void setIcon(Bitmap icon) {
        this.icon = icon;
    }
    
    /**
     * Get the game type
     * 
     * @return Game type
     */
    public GameType getGameType() {
        return gameType;
    }
    
    /**
     * Set the game type
     * 
     * @param gameType New game type
     */
    public void setGameType(GameType gameType) {
        this.gameType = gameType;
        this.standardizedGameType = convertGameTypeToStandardized(gameType);
        this.isGame = (gameType != GameType.UNKNOWN);
    }
    
    /**
     * Get the standardized game type
     * 
     * @return Standardized game type
     */
    public StandardizedGameType getStandardizedGameType() {
        return standardizedGameType;
    }
    
    /**
     * Set the standardized game type
     * 
     * @param standardizedGameType New standardized game type
     */
    public void setStandardizedGameType(StandardizedGameType standardizedGameType) {
        this.standardizedGameType = standardizedGameType;
        this.gameType = convertStandardizedToGameType(standardizedGameType);
        this.isGame = (standardizedGameType.getType() != StandardizedGameType.GameType.UNKNOWN);
    }
    
    /**
     * Check if this is a game
     * 
     * @return True if this is a game
     */
    public boolean isGame() {
        return isGame;
    }
    
    /**
     * Set whether this is a game
     * 
     * @param isGame True if this is a game
     */
    public void setIsGame(boolean isGame) {
        this.isGame = isGame;
        
        if (!isGame && this.gameType != GameType.UNKNOWN) {
            this.gameType = GameType.UNKNOWN;
            this.standardizedGameType = new StandardizedGameType(StandardizedGameType.GameType.UNKNOWN);
        }
    }
    
    /**
     * Get metadata
     * 
     * @return Metadata map
     */
    public Map<String, Object> getMetadata() {
        return metadata;
    }
    
    /**
     * Set metadata
     * 
     * @param metadata New metadata map
     */
    public void setMetadata(Map<String, Object> metadata) {
        this.metadata = new HashMap<>(metadata);
    }
    
    /**
     * Get a metadata value
     * 
     * @param key Metadata key
     * @return Metadata value, or null if not found
     */
    public Object getMetadataValue(String key) {
        return metadata.get(key);
    }
    
    /**
     * Set a metadata value
     * 
     * @param key Metadata key
     * @param value Metadata value
     */
    public void setMetadataValue(String key, Object value) {
        metadata.put(key, value);
    }
    
    /**
     * Determine game type from package name
     * 
     * @param packageName Package name
     * @return Detected game type
     */
    private GameType determineGameTypeFromPackage(String packageName) {
        if (packageName == null || packageName.isEmpty()) {
            return GameType.UNKNOWN;
        }
        
        // Check for known game developers/publishers
        if (packageName.startsWith("com.king.") || 
            packageName.contains(".puzzle") || 
            packageName.contains("match3")) {
            return GameType.PUZZLE;
        } else if (packageName.contains(".card") || 
                   packageName.contains("poker") || 
                   packageName.contains("solitaire")) {
            return GameType.CARD;
        } else if (packageName.contains(".board") || 
                   packageName.contains("chess") || 
                   packageName.contains("checkers")) {
            return GameType.BOARD;
        } else if (packageName.contains(".arcade") || 
                   packageName.contains("shooter") || 
                   packageName.contains("platform")) {
            return GameType.ARCADE;
        } else if (packageName.contains(".word") || 
                   packageName.contains("crossword") || 
                   packageName.contains("scrabble")) {
            return GameType.WORD;
        } else if (packageName.contains(".strategy") || 
                   packageName.contains("tower") || 
                   packageName.contains("defense")) {
            return GameType.STRATEGY;
        } else if (packageName.contains(".simulation") || 
                   packageName.contains("simulator") || 
                   packageName.contains("tycoon")) {
            return GameType.SIMULATION;
        } else if (packageName.contains(".rpg") || 
                   packageName.contains("role")) {
            return GameType.RPG;
        } else if (packageName.contains(".adventure") || 
                   packageName.contains("quest")) {
            return GameType.ADVENTURE;
        } else if (packageName.contains(".action")) {
            return GameType.ACTION;
        } else if (packageName.contains(".sports") || 
                   packageName.contains("football") || 
                   packageName.contains("basketball")) {
            return GameType.SPORTS;
        } else if (packageName.contains(".racing") || 
                   packageName.contains("race") || 
                   packageName.contains("car")) {
            return GameType.RACING;
        } else if (packageName.contains(".casual")) {
            return GameType.CASUAL;
        } else if (packageName.contains(".game") || 
                   packageName.contains("games")) {
            // Generic game, type unknown
            return GameType.UNKNOWN;
        }
        
        // No clear game type detected
        return GameType.UNKNOWN;
    }
    
    /**
     * Convert from GameType to StandardizedGameType
     * 
     * @param gameType Game type to convert
     * @return Standardized game type
     */
    private StandardizedGameType convertGameTypeToStandardized(GameType gameType) {
        StandardizedGameType.GameType standardizedType;
        
        switch (gameType) {
            case PUZZLE:
                standardizedType = StandardizedGameType.GameType.PUZZLE;
                break;
            case CARD:
                standardizedType = StandardizedGameType.GameType.CARD;
                break;
            case BOARD:
                standardizedType = StandardizedGameType.GameType.BOARD;
                break;
            case ARCADE:
                standardizedType = StandardizedGameType.GameType.ARCADE;
                break;
            case WORD:
                standardizedType = StandardizedGameType.GameType.WORD;
                break;
            case STRATEGY:
                standardizedType = StandardizedGameType.GameType.STRATEGY;
                break;
            case SIMULATION:
                standardizedType = StandardizedGameType.GameType.SIMULATION;
                break;
            case RPG:
                standardizedType = StandardizedGameType.GameType.RPG;
                break;
            case ADVENTURE:
                standardizedType = StandardizedGameType.GameType.ADVENTURE;
                break;
            case ACTION:
                standardizedType = StandardizedGameType.GameType.ACTION;
                break;
            case SPORTS:
                standardizedType = StandardizedGameType.GameType.SPORTS;
                break;
            case RACING:
                standardizedType = StandardizedGameType.GameType.RACING;
                break;
            case EDUCATIONAL:
                standardizedType = StandardizedGameType.GameType.EDUCATIONAL;
                break;
            case CASUAL:
                standardizedType = StandardizedGameType.GameType.CASUAL;
                break;
            default:
                standardizedType = StandardizedGameType.GameType.UNKNOWN;
                break;
        }
        
        return new StandardizedGameType(standardizedType);
    }
    
    /**
     * Convert from StandardizedGameType to GameType
     * 
     * @param standardizedGameType Standardized game type to convert
     * @return Game type
     */
    private GameType convertStandardizedToGameType(StandardizedGameType standardizedGameType) {
        if (standardizedGameType == null) {
            return GameType.UNKNOWN;
        }
        
        switch (standardizedGameType.getType()) {
            case PUZZLE:
                return GameType.PUZZLE;
            case CARD:
                return GameType.CARD;
            case BOARD:
                return GameType.BOARD;
            case ARCADE:
                return GameType.ARCADE;
            case WORD:
                return GameType.WORD;
            case STRATEGY:
                return GameType.STRATEGY;
            case SIMULATION:
                return GameType.SIMULATION;
            case RPG:
                return GameType.RPG;
            case ADVENTURE:
                return GameType.ADVENTURE;
            case ACTION:
                return GameType.ACTION;
            case SPORTS:
                return GameType.SPORTS;
            case RACING:
                return GameType.RACING;
            case EDUCATIONAL:
                return GameType.EDUCATIONAL;
            case CASUAL:
                return GameType.CASUAL;
            default:
                return GameType.UNKNOWN;
        }
    }
    
    /**
     * Convert to string representation
     */
    @Override
    public String toString() {
        return "AppInfo{packageName='" + packageName + "', appName='" + appName + "', " +
               "gameType=" + gameType + ", isGame=" + isGame + "}";
    }
}