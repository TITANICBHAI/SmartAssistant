package utils;

import java.util.Map;
import java.util.HashMap;

/**
 * Contains information about an application.
 * This is a compatibility class for Android's ApplicationInfo class.
 */
public class ApplicationInfo {
    private String packageName;
    private String name;
    private String version;
    private String appCategory;
    private boolean isGame;
    private GameType gameType;
    private Map<String, Object> metadata;
    
    /**
     * Create a new ApplicationInfo instance
     */
    public ApplicationInfo() {
        this.metadata = new HashMap<>();
        this.isGame = false;
        this.gameType = GameType.UNKNOWN;
    }
    
    /**
     * Create a new ApplicationInfo instance with the specified package name
     * @param packageName The package name
     */
    public ApplicationInfo(String packageName) {
        this();
        this.packageName = packageName;
    }
    
    /**
     * Create a new ApplicationInfo instance with the specified package name and name
     * @param packageName The package name
     * @param name The application name
     */
    public ApplicationInfo(String packageName, String name) {
        this(packageName);
        this.name = name;
    }
    
    /**
     * Get the package name
     * @return The package name
     */
    public String getPackageName() {
        return packageName;
    }
    
    /**
     * Set the package name
     * @param packageName The package name
     */
    public void setPackageName(String packageName) {
        this.packageName = packageName;
    }
    
    /**
     * Get the application name
     * @return The application name
     */
    public String getName() {
        return name;
    }
    
    /**
     * Set the application name
     * @param name The application name
     */
    public void setName(String name) {
        this.name = name;
    }
    
    /**
     * Get the application label (same as name, but matches Android's naming)
     * @return The application label
     */
    public String getLabel() {
        return name;
    }
    
    /**
     * Get the application version
     * @return The application version
     */
    public String getVersion() {
        return version;
    }
    
    /**
     * Set the application version
     * @param version The application version
     */
    public void setVersion(String version) {
        this.version = version;
    }
    
    /**
     * Get the application category
     * @return The application category
     */
    public String getAppCategory() {
        return appCategory;
    }
    
    /**
     * Set the application category
     * @param appCategory The application category
     */
    public void setAppCategory(String appCategory) {
        this.appCategory = appCategory;
    }
    
    /**
     * Check if the application is a game
     * @return True if the application is a game, false otherwise
     */
    public boolean isGame() {
        return isGame;
    }
    
    /**
     * Set whether the application is a game
     * @param isGame True if the application is a game, false otherwise
     */
    public void setIsGame(boolean isGame) {
        this.isGame = isGame;
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
        this.isGame = (gameType != GameType.UNKNOWN);
    }
    
    /**
     * Get a metadata value
     * @param key The metadata key
     * @return The metadata value, or null if not found
     */
    public Object getMetadata(String key) {
        return metadata.get(key);
    }
    
    /**
     * Set a metadata value
     * @param key The metadata key
     * @param value The metadata value
     */
    public void setMetadata(String key, Object value) {
        metadata.put(key, value);
    }
    
    /**
     * Get all metadata
     * @return A map of all metadata
     */
    public Map<String, Object> getAllMetadata() {
        return new HashMap<>(metadata);
    }
    
    /**
     * Remove a metadata value
     * @param key The metadata key
     * @return The removed value, or null if not found
     */
    public Object removeMetadata(String key) {
        return metadata.remove(key);
    }
    
    /**
     * Clear all metadata
     */
    public void clearMetadata() {
        metadata.clear();
    }
    
    @Override
    public String toString() {
        return "ApplicationInfo{" +
                "packageName='" + packageName + '\'' +
                ", name='" + name + '\'' +
                ", version='" + version + '\'' +
                ", isGame=" + isGame +
                ", gameType=" + gameType +
                '}';
    }
    
    /**
     * Create a standardized string representation of this application info
     * @return A standardized string representation
     */
    public String toStandardizedElement() {
        StringBuilder sb = new StringBuilder();
        sb.append("package=").append(packageName != null ? packageName : "").append(";");
        sb.append("name=").append(name != null ? name : "").append(";");
        sb.append("version=").append(version != null ? version : "").append(";");
        sb.append("category=").append(appCategory != null ? appCategory : "").append(";");
        sb.append("isGame=").append(isGame).append(";");
        sb.append("gameType=").append(gameType != null ? gameType.name() : "UNKNOWN");
        return sb.toString();
    }
    
    /**
     * Create an ApplicationInfo instance from a standardized string representation
     * @param standardized The standardized string representation
     * @return The ApplicationInfo instance, or null if the string is invalid
     */
    public static ApplicationInfo fromStandardizedElement(String standardized) {
        if (standardized == null || standardized.isEmpty()) {
            return null;
        }
        
        ApplicationInfo info = new ApplicationInfo();
        String[] parts = standardized.split(";");
        
        for (String part : parts) {
            String[] keyValue = part.split("=", 2);
            if (keyValue.length != 2) {
                continue;
            }
            
            String key = keyValue[0].trim();
            String value = keyValue[1].trim();
            
            switch (key) {
                case "package":
                    info.setPackageName(value);
                    break;
                case "name":
                    info.setName(value);
                    break;
                case "version":
                    info.setVersion(value);
                    break;
                case "category":
                    info.setAppCategory(value);
                    break;
                case "isGame":
                    info.setIsGame(Boolean.parseBoolean(value));
                    break;
                case "gameType":
                    try {
                        info.setGameType(GameType.valueOf(value));
                    } catch (IllegalArgumentException e) {
                        info.setGameType(GameType.UNKNOWN);
                    }
                    break;
            }
        }
        
        return info;
    }
}