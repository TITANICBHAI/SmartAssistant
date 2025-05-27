package models;

/**
 * Utility class to convert between GameType implementations in different packages
 */
public class GameTypeConverter {
    
    /**
     * Convert a models.GameType to utils.GameType
     * 
     * @param gameType The models game type to convert
     * @return The equivalent utils game type
     */
    public static utils.GameType toUtilsGameType(GameType gameType) {
        if (gameType == null) {
            return null;
        }
        
        try {
            return utils.GameType.valueOf(gameType.name());
        } catch (IllegalArgumentException e) {
            // If the enum value doesn't exist in utils.GameType, return UNKNOWN
            return utils.GameType.UNKNOWN;
        }
    }
    
    /**
     * Convert a utils.GameType to models.GameType
     * 
     * @param gameType The utils game type to convert
     * @return The equivalent models game type
     */
    public static GameType fromUtilsGameType(utils.GameType gameType) {
        if (gameType == null) {
            return null;
        }
        
        try {
            return GameType.valueOf(gameType.name());
        } catch (IllegalArgumentException e) {
            // If the enum value doesn't exist in models.GameType, return UNKNOWN
            return GameType.UNKNOWN;
        }
    }
}