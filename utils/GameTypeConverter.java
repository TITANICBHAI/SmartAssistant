package utils;

/**
 * Utility class for converting between different types of GameType enums.
 */
public class GameTypeConverter {
    // Constants for game types not in the enum
    private static final String PUBG_MOBILE = "PUBG_MOBILE";
    private static final String FREE_FIRE = "FREE_FIRE";
    private static final String POKEMON_UNITE = "POKEMON_UNITE";
    private static final String CLASH_OF_CLANS = "CLASH_OF_CLANS";
    private static final String MOBA = "MOBA";
    
    /**
     * Convert a string to utils.GameType.
     * 
     * @param typeStr The type string
     * @return The utils.GameType
     */
    public static GameType fromString(String typeStr) {
        return GameType.fromString(typeStr);
    }
    
    /**
     * Convert a string to models.GameType.
     * 
     * @param typeStr The type string
     * @return The models.GameType
     */
    public static models.GameType toModelsGameType(String typeStr) {
        return models.GameType.fromString(typeStr);
    }
    
    /**
     * Convert from package name to models.GameType.
     * 
     * @param packageName The package name
     * @return The models.GameType
     */
    public static models.GameType packageNameToModelsGameType(String packageName) {
        if (packageName == null || packageName.isEmpty()) {
            return models.GameType.UNKNOWN;
        }
        
        // Special handling for common games
        if (packageName.contains("pubg")) {
            return models.GameType.SHOOTER;
        } else if (packageName.contains("freefire") || packageName.contains("free.fire")) {
            return models.GameType.SHOOTER;
        } else if (packageName.contains("pokemon") && packageName.contains("unite")) {
            return models.GameType.MOBA;
        } else if (packageName.contains("clash") && packageName.contains("clans")) {
            return models.GameType.STRATEGY;
        } else if (packageName.contains("mobilelegen") || packageName.contains("aov")) {
            return models.GameType.MOBA;
        } else if (packageName.contains("minecraft")) {
            return models.GameType.ADVENTURE;
        } else if (packageName.contains("candy") || packageName.contains("match3")) {
            return models.GameType.MATCH3;
        } else if (packageName.contains("asphalt") || packageName.contains("racing")) {
            return models.GameType.RACING;
        }
        
        // Convert via built-in method
        GameType utilType = GameType.fromPackageName(packageName);
        return utilType.toModelsGameType();
    }
    
    /**
     * Convert from utils.GameType to models.GameType.
     * 
     * @param type The utils.GameType
     * @return The equivalent models.GameType
     */
    public static models.GameType toModelsGameType(GameType type) {
        if (type == null) {
            return models.GameType.UNKNOWN;
        }
        
        switch (type) {
            case UNKNOWN:
                return models.GameType.UNKNOWN;
            case PUZZLE:
                return models.GameType.PUZZLE;
            case WORD:
                return models.GameType.WORD;
            case ARCADE:
                return models.GameType.ARCADE;
            case RACING:
                return models.GameType.RACING;
            case PLATFORM:
                return models.GameType.PLATFORM;
            case ACTION:
                return models.GameType.ACTION;
            case ADVENTURE:
                return models.GameType.ADVENTURE;
            case RPG:
                return models.GameType.RPG;
            case STRATEGY:
                return models.GameType.STRATEGY;
            case SIMULATION:
                return models.GameType.SIMULATION;
            case SPORTS:
                return models.GameType.SPORTS;
            case FIGHTING:
                return models.GameType.FIGHTING;
            case CARD:
                return models.GameType.CARD;
            case BOARD:
                return models.GameType.BOARD;
            case EDUCATIONAL:
                return models.GameType.EDUCATIONAL;
            case MUSIC:
                return models.GameType.valueOf("MUSIC");
            case TRIVIA:
                return models.GameType.valueOf("TRIVIA");
            case CASINO:
                return models.GameType.valueOf("CASINO");
            case MATCH3:
                return models.GameType.valueOf("MATCH3");
            case IDLE:
                return models.GameType.valueOf("IDLE");
            case RUNNER:
                return models.GameType.valueOf("RUNNER");
            case TOWER_DEFENSE:
                return models.GameType.valueOf("TOWER_DEFENSE");
            case SHOOTER:
                return models.GameType.SHOOTER;
            case CASUAL:
                return models.GameType.CASUAL;
            default:
                try {
                    return models.GameType.valueOf(type.name());
                } catch (IllegalArgumentException e) {
                    return models.GameType.UNKNOWN;
                }
        }
    }
    
    /**
     * Convert from models.GameType to utils.GameType.
     * 
     * @param type The models.GameType
     * @return The equivalent utils.GameType
     */
    public static GameType fromModelsGameType(models.GameType type) {
        if (type == null) {
            return GameType.UNKNOWN;
        }
        
        switch (type) {
            case UNKNOWN:
                return GameType.UNKNOWN;
            case PUZZLE:
                return GameType.PUZZLE;
            case WORD:
                return GameType.WORD;
            case ARCADE:
                return GameType.ARCADE;
            case RACING:
                return GameType.RACING;
            case PLATFORM:
                return GameType.PLATFORM;
            case ACTION:
                return GameType.ACTION;
            case ADVENTURE:
                return GameType.ADVENTURE;
            case RPG:
                return GameType.RPG;
            case STRATEGY:
                return GameType.STRATEGY;
            case SIMULATION:
                return GameType.SIMULATION;
            case SPORTS:
                return GameType.SPORTS;
            case FIGHTING:
                return GameType.FIGHTING;
            case CARD:
                return GameType.CARD;
            case BOARD:
                return GameType.BOARD;
            case EDUCATIONAL:
                return GameType.EDUCATIONAL;
            case MUSIC:
                return GameType.valueOf("MUSIC");
            case TRIVIA:
                return GameType.valueOf("TRIVIA");
            case CASINO:
                return GameType.valueOf("CASINO");
            case MATCH3:
                return GameType.valueOf("MATCH3");
            case IDLE:
                return GameType.valueOf("IDLE");
            case RUNNER:
                return GameType.valueOf("RUNNER");
            case TOWER_DEFENSE:
                return GameType.valueOf("TOWER_DEFENSE");
            case SHOOTER:
                return GameType.SHOOTER;
            case CASUAL:
                return GameType.CASUAL;
            case PUBG_MOBILE:
                return GameType.SHOOTER;
            case FREE_FIRE:
                return GameType.SHOOTER;
            case POKEMON_UNITE:
                return GameType.STRATEGY;
            case MOBA:
                return GameType.STRATEGY;
            case CLASH_OF_CLANS:
                return GameType.STRATEGY;
            default:
                try {
                    return GameType.valueOf(type.name());
                } catch (IllegalArgumentException e) {
                    return GameType.UNKNOWN;
                }
        }
    }
}