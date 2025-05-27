package models;

/**
 * Proxy class to access utils.AIController.GameType enum values
 * This class provides a safe way to access the enum values without direct dependencies
 */
public class AIControllerGameTypeProxy {
    
    /**
     * Get the PUBG_MOBILE game type from AIController
     * 
     * @return The PUBG_MOBILE game type
     */
    public static utils.AIController.GameType getPUBG_MOBILE() {
        return utils.AIController.GameType.PUBG_MOBILE;
    }
    
    /**
     * Get the FREE_FIRE game type from AIController
     * 
     * @return The FREE_FIRE game type
     */
    public static utils.AIController.GameType getFREE_FIRE() {
        return utils.AIController.GameType.FREE_FIRE;
    }
    
    /**
     * Get the POKEMON_UNITE game type from AIController
     * 
     * @return The POKEMON_UNITE game type
     */
    public static utils.AIController.GameType getPOKEMON_UNITE() {
        return utils.AIController.GameType.POKEMON_UNITE;
    }
    
    /**
     * Get the MOBA game type from AIController
     * 
     * @return The MOBA game type
     */
    public static utils.AIController.GameType getMOBA() {
        return utils.AIController.GameType.MOBA;
    }
    
    /**
     * Get the CLASH_OF_CLANS game type from AIController
     * 
     * @return The CLASH_OF_CLANS game type
     */
    public static utils.AIController.GameType getCLASH_OF_CLANS() {
        return utils.AIController.GameType.CLASH_OF_CLANS;
    }
    
    /**
     * Converts a StandardizedGameType enum to AIController.GameType
     * 
     * @param type The StandardizedGameType enum value
     * @return The corresponding AIController.GameType
     */
    public static utils.AIController.GameType fromStandardizedGameType(StandardizedGameType.GameType type) {
        if (type == null) {
            return utils.AIController.GameType.UNKNOWN;
        }
        
        switch (type) {
            case PUBG_MOBILE:
                return utils.AIController.GameType.PUBG_MOBILE;
            case FREE_FIRE:
                return utils.AIController.GameType.FREE_FIRE;
            case POKEMON_UNITE:
                return utils.AIController.GameType.POKEMON_UNITE;
            case MOBA:
                return utils.AIController.GameType.MOBA;
            case CLASH_OF_CLANS:
                return utils.AIController.GameType.CLASH_OF_CLANS;
            case PUZZLE:
                return utils.AIController.GameType.PUZZLE;
            case CARD:
                return utils.AIController.GameType.CARD;
            case BOARD:
                return utils.AIController.GameType.BOARD;
            case ARCADE:
                return utils.AIController.GameType.ARCADE;
            case WORD:
                return utils.AIController.GameType.WORD;
            case STRATEGY:
                return utils.AIController.GameType.STRATEGY;
            case SIMULATION:
                return utils.AIController.GameType.SIMULATION;
            case RPG:
                return utils.AIController.GameType.RPG;
            case ADVENTURE:
                return utils.AIController.GameType.ADVENTURE;
            case ACTION:
                return utils.AIController.GameType.ACTION;
            case SPORTS:
                return utils.AIController.GameType.SPORTS;
            case RACING:
                return utils.AIController.GameType.RACING;
            case EDUCATIONAL:
                return utils.AIController.GameType.EDUCATIONAL;
            case CASUAL:
                return utils.AIController.GameType.CASUAL;
            case ACTION_ADVENTURE:
                return utils.AIController.GameType.ACTION;
            case FIGHTING:
                return utils.AIController.GameType.ACTION;
            case SHOOTER:
                return utils.AIController.GameType.FPS;
            case PLATFORMER:
                return utils.AIController.GameType.ACTION;
            case OTHER:
                return utils.AIController.GameType.OTHER;
            case UNKNOWN:
            default:
                return utils.AIController.GameType.UNKNOWN;
        }
    }
    
    /**
     * Converts an AIController.GameType to StandardizedGameType.GameType
     * 
     * @param type The AIController.GameType
     * @return The corresponding StandardizedGameType.GameType
     */
    public static StandardizedGameType.GameType toStandardizedGameType(utils.AIController.GameType type) {
        if (type == null) {
            return StandardizedGameType.GameType.UNKNOWN;
        }
        
        switch (type) {
            case PUBG_MOBILE:
                return StandardizedGameType.GameType.PUBG_MOBILE;
            case FREE_FIRE:
                return StandardizedGameType.GameType.FREE_FIRE;
            case POKEMON_UNITE:
                return StandardizedGameType.GameType.POKEMON_UNITE;
            case MOBA:
                return StandardizedGameType.GameType.MOBA;
            case CLASH_OF_CLANS:
                return StandardizedGameType.GameType.CLASH_OF_CLANS;
            case PUZZLE:
                return StandardizedGameType.GameType.PUZZLE;
            case CARD:
                return StandardizedGameType.GameType.CARD;
            case BOARD:
                return StandardizedGameType.GameType.BOARD;
            case ARCADE:
                return StandardizedGameType.GameType.ARCADE;
            case WORD:
                return StandardizedGameType.GameType.WORD;
            case STRATEGY:
                return StandardizedGameType.GameType.STRATEGY;
            case SIMULATION:
                return StandardizedGameType.GameType.SIMULATION;
            case RPG:
                return StandardizedGameType.GameType.RPG;
            case ADVENTURE:
                return StandardizedGameType.GameType.ADVENTURE;
            case ACTION:
                return StandardizedGameType.GameType.ACTION;
            case SPORTS:
                return StandardizedGameType.GameType.SPORTS;
            case RACING:
                return StandardizedGameType.GameType.RACING;
            case EDUCATIONAL:
                return StandardizedGameType.GameType.EDUCATIONAL;
            case CASUAL:
                return StandardizedGameType.GameType.CASUAL;
            case FPS:
                return StandardizedGameType.GameType.SHOOTER;
            case OTHER:
                return StandardizedGameType.GameType.OTHER;
            case UNKNOWN:
            default:
                return StandardizedGameType.GameType.UNKNOWN;
        }
    }
}