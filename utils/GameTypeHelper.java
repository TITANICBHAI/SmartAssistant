package utils;

import models.GameType;

/**
 * Helper class for working with GameType objects.
 * Provides utility methods to convert strings to GameType objects and vice versa.
 */
public class GameTypeHelper {
    /**
     * Convert a string to a GameType
     * @param typeName The type name to convert
     * @return The corresponding GameType, or UNKNOWN if not found
     */
    public static GameType fromString(String typeName) {
        if (typeName == null || typeName.isEmpty()) {
            return GameType.UNKNOWN;
        }
        
        try {
            return GameType.valueOf(typeName.toUpperCase());
        } catch (IllegalArgumentException e) {
            // Try to match by name ignoring case
            for (GameType type : GameType.values()) {
                if (type.toString().equalsIgnoreCase(typeName)) {
                    return type;
                }
            }
            return GameType.UNKNOWN;
        }
    }
    
    /**
     * Check if a game type is action-oriented
     * @param gameType The game type to check
     * @return True if the game type is action-oriented
     */
    public static boolean isActionOriented(GameType gameType) {
        if (gameType == null) {
            return false;
        }
        
        switch (gameType) {
            case ACTION:
            case ACTION_ADVENTURE:
            case FIGHTING:
            case SHOOTER:
            case PLATFORMER:
            case RACING:
                return true;
            default:
                return false;
        }
    }
    
    /**
     * Check if a game type is thinking-oriented
     * @param gameType The game type to check
     * @return True if the game type is thinking-oriented
     */
    public static boolean isThinkingOriented(GameType gameType) {
        if (gameType == null) {
            return false;
        }
        
        switch (gameType) {
            case PUZZLE:
            case STRATEGY:
            case CARD:
            case BOARD:
            case RPG:
            case SIMULATION:
                return true;
            default:
                return false;
        }
    }
    
    /**
     * Get the display name for a game type
     * @param gameType The game type
     * @return The display name, or "Unknown" if null
     */
    public static String getDisplayName(GameType gameType) {
        if (gameType == null) {
            return "Unknown";
        }
        
        String name = gameType.toString();
        
        // Convert SNAKE_CASE to Title Case
        StringBuilder displayName = new StringBuilder();
        String[] words = name.split("_");
        
        for (String word : words) {
            if (!word.isEmpty()) {
                displayName.append(word.substring(0, 1).toUpperCase());
                if (word.length() > 1) {
                    displayName.append(word.substring(1).toLowerCase());
                }
                displayName.append(" ");
            }
        }
        
        return displayName.toString().trim();
    }
}