package utils;

/**
 * Helper class for converting between different GameType implementations.
 */
public class AIControllerGameTypeHelper {
    private static final String TAG = "AIControllerGameTypeHelper";
    private static boolean initialized = false;
    
    /**
     * Initialize the helper class
     */
    public static void initialize() {
        if (!initialized) {
            System.out.println("Initializing AIControllerGameTypeHelper");
            initialized = true;
        }
    }
    
    /**
     * Get PUBG_MOBILE game type
     * @return PUBG_MOBILE game type
     */
    public static utils.GameType getPUBG_MOBILE() {
        return utils.GameType.PUBG_MOBILE;
    }
    
    /**
     * Get FREE_FIRE game type
     * @return FREE_FIRE game type
     */
    public static utils.GameType getFREE_FIRE() {
        return utils.GameType.FREE_FIRE;
    }
    
    /**
     * Get POKEMON_UNITE game type
     * @return POKEMON_UNITE game type
     */
    public static utils.GameType getPOKEMON_UNITE() {
        return utils.GameType.POKEMON_UNITE;
    }
    
    /**
     * Get MOBA game type
     * @return MOBA game type
     */
    public static utils.GameType getMOBA() {
        return utils.GameType.MOBA;
    }
    
    /**
     * Convert from model GameType to AIController GameType
     * @param gameType The model GameType
     * @return The AIController GameType or null if conversion failed
     */
    public static utils.GameType fromModelGameType(models.GameType gameType) {
        if (gameType == null) {
            return null;
        }
        
        try {
            String gameTypeName = gameType.name();
            return utils.GameType.valueOf(gameTypeName);
        } catch (Exception e) {
            System.err.println("Error converting model GameType to AIController GameType: " + e.getMessage());
            return null;
        }
    }
    
    /**
     * Convert from AIController GameType to model GameType
     * @param gameType The AIController GameType
     * @return The model GameType or null if conversion failed
     */
    public static models.GameType toModelGameType(utils.GameType gameType) {
        if (gameType == null) {
            return null;
        }
        
        try {
            String gameTypeName = gameType.name();
            return models.GameType.valueOf(gameTypeName);
        } catch (Exception e) {
            System.err.println("Error converting AIController GameType to model GameType: " + e.getMessage());
            return null;
        }
    }
    
    /**
     * Convert from GameType name to AIController GameType
     * @param gameTypeName The GameType name
     * @return The AIController GameType or null if conversion failed
     */
    public static utils.GameType fromString(String gameTypeName) {
        if (gameTypeName == null || gameTypeName.isEmpty()) {
            return null;
        }
        
        try {
            return utils.GameType.valueOf(gameTypeName.toUpperCase());
        } catch (Exception e) {
            System.err.println("Error converting string to AIController GameType: " + e.getMessage());
            return null;
        }
    }
}