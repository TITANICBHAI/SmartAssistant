package models;

import java.util.HashMap;
import java.util.Map;

/**
 * Standardized representation of game types across the application.
 * This provides a common way to represent game types, regardless of source.
 */
public class StandardizedGameType {
    private GameType type;
    private String name;
    private String description;
    private Map<String, Object> metadata;
    
    /**
     * Enumeration of standard game types
     */
    public enum GameType {
        PUZZLE,           // Puzzle games like match-3, block puzzle, etc.
        CARD,             // Card games like solitaire, poker, etc.
        BOARD,            // Board games like chess, checkers, etc.
        ARCADE,           // Arcade games like platformers, shooters, etc.
        WORD,             // Word games like crosswords, word search, etc.
        STRATEGY,         // Strategy games like tower defense, RTS, etc.
        SIMULATION,       // Simulation games like city builders, life sims, etc.
        RPG,              // Role-playing games with character progression
        ADVENTURE,        // Adventure games focused on exploration and story
        ACTION,           // Action games with reflex-based gameplay
        SPORTS,           // Sports games like football, basketball, etc.
        RACING,           // Racing games
        EDUCATIONAL,      // Educational games for learning
        CASUAL,           // Casual games with simple mechanics
        ACTION_ADVENTURE, // Action-adventure hybrid games
        FIGHTING,         // Fighting and combat games
        SHOOTER,          // First-person or third-person shooters
        PLATFORMER,       // Platform jumping games
        PUBG_MOBILE,      // PUBG Mobile battle royale game
        FREE_FIRE,        // Free Fire battle royale game
        POKEMON_UNITE,    // Pokemon Unite MOBA game
        MOBA,             // Multiplayer Online Battle Arena games
        CLASH_OF_CLANS,   // Clash of Clans strategy game
        OTHER,            // Other types of applications or games
        UNKNOWN;          // Unknown or unclassified game type
        
        /**
         * Convert to string representation
         */
        @Override
        public String toString() {
            return name().toLowerCase();
        }
        
        /**
         * Get a descriptive name for this game type
         * 
         * @return Descriptive name
         */
        public String getDisplayName() {
            switch (this) {
                case PUZZLE:
                    return "Puzzle Game";
                case CARD:
                    return "Card Game";
                case BOARD:
                    return "Board Game";
                case ARCADE:
                    return "Arcade Game";
                case WORD:
                    return "Word Game";
                case STRATEGY:
                    return "Strategy Game";
                case SIMULATION:
                    return "Simulation Game";
                case RPG:
                    return "Role-Playing Game";
                case ADVENTURE:
                    return "Adventure Game";
                case ACTION:
                    return "Action Game";
                case SPORTS:
                    return "Sports Game";
                case RACING:
                    return "Racing Game";
                case EDUCATIONAL:
                    return "Educational Game";
                case CASUAL:
                    return "Casual Game";
                case ACTION_ADVENTURE:
                    return "Action-Adventure Game";
                case FIGHTING:
                    return "Fighting Game";
                case SHOOTER:
                    return "Shooter Game";
                case PLATFORMER:
                    return "Platformer Game";
                case PUBG_MOBILE:
                    return "PUBG Mobile";
                case FREE_FIRE:
                    return "Free Fire";
                case POKEMON_UNITE:
                    return "Pokemon Unite";
                case MOBA:
                    return "MOBA Game";
                case CLASH_OF_CLANS:
                    return "Clash of Clans";
                case OTHER:
                    return "Other Application";
                default:
                    return "Unknown Game";
            }
        }
    }
    
    /**
     * Create a new StandardizedGameType with the specified type
     * 
     * @param type Game type
     */
    public StandardizedGameType(GameType type) {
        this.type = type;
        this.name = type.getDisplayName();
        this.description = "";
        this.metadata = new HashMap<>();
    }
    
    /**
     * Create a new StandardizedGameType with the specified type and metadata
     * 
     * @param type Game type
     * @param name Custom name
     * @param description Custom description
     * @param metadata Additional metadata
     */
    public StandardizedGameType(GameType type, String name, String description, Map<String, Object> metadata) {
        this.type = type;
        this.name = name != null ? name : type.getDisplayName();
        this.description = description != null ? description : "";
        this.metadata = metadata != null ? new HashMap<>(metadata) : new HashMap<>();
    }
    
    /**
     * Get the game type
     * 
     * @return Game type
     */
    public GameType getType() {
        return type;
    }
    
    /**
     * Set the game type
     * 
     * @param type New game type
     */
    public void setType(GameType type) {
        this.type = type;
    }
    
    /**
     * Get the name
     * 
     * @return Name
     */
    public String getName() {
        return name;
    }
    
    /**
     * Set the name
     * 
     * @param name New name
     */
    public void setName(String name) {
        this.name = name;
    }
    
    /**
     * Get the description
     * 
     * @return Description
     */
    public String getDescription() {
        return description;
    }
    
    /**
     * Set the description
     * 
     * @param description New description
     */
    public void setDescription(String description) {
        this.description = description;
    }
    
    /**
     * Get the metadata
     * 
     * @return Metadata map
     */
    public Map<String, Object> getMetadata() {
        return metadata;
    }
    
    /**
     * Set the metadata
     * 
     * @param metadata New metadata map
     */
    public void setMetadata(Map<String, Object> metadata) {
        this.metadata = metadata != null ? new HashMap<>(metadata) : new HashMap<>();
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
     * Check if this is a puzzle game
     * 
     * @return True if this is a puzzle game
     */
    public boolean isPuzzleGame() {
        return type == GameType.PUZZLE;
    }
    
    /**
     * Check if this is a card game
     * 
     * @return True if this is a card game
     */
    public boolean isCardGame() {
        return type == GameType.CARD;
    }
    
    /**
     * Check if this is a board game
     * 
     * @return True if this is a board game
     */
    public boolean isBoardGame() {
        return type == GameType.BOARD;
    }
    
    /**
     * Check if this is a casual game
     * 
     * @return True if this is a casual game
     */
    public boolean isCasualGame() {
        return type == GameType.CASUAL || type == GameType.PUZZLE;
    }
    
    /**
     * Check if this is an action-oriented game
     * 
     * @return True if this is an action-oriented game
     */
    public boolean isActionOriented() {
        return type == GameType.ACTION || 
               type == GameType.ACTION_ADVENTURE || 
               type == GameType.FIGHTING || 
               type == GameType.SHOOTER || 
               type == GameType.PLATFORMER || 
               type == GameType.RACING;
    }
    
    /**
     * Check if this is a thinking-oriented game
     * 
     * @return True if this is a thinking-oriented game
     */
    public boolean isThinkingOriented() {
        return type == GameType.PUZZLE || 
               type == GameType.STRATEGY || 
               type == GameType.CARD || 
               type == GameType.BOARD || 
               type == GameType.RPG || 
               type == GameType.SIMULATION;
    }
    
    /**
     * Convert to a map representation
     * 
     * @return Map containing all game type data
     */
    public Map<String, Object> toMap() {
        Map<String, Object> result = new HashMap<>();
        result.put("type", type.toString());
        result.put("name", name);
        result.put("description", description);
        result.put("metadata", metadata);
        return result;
    }
    
    /**
     * Create a StandardizedGameType from a map
     * 
     * @param data Map containing game type data
     * @return Standardized game type
     */
    public static StandardizedGameType fromMap(Map<String, Object> data) {
        if (data == null) {
            return new StandardizedGameType(GameType.UNKNOWN);
        }
        
        String typeStr = (String) data.getOrDefault("type", "unknown");
        GameType type;
        try {
            type = GameType.valueOf(typeStr.toUpperCase());
        } catch (IllegalArgumentException e) {
            type = GameType.UNKNOWN;
        }
        
        String name = (String) data.getOrDefault("name", null);
        String description = (String) data.getOrDefault("description", null);
        Map<String, Object> metadata = (Map<String, Object>) data.getOrDefault("metadata", null);
        
        return new StandardizedGameType(type, name, description, metadata);
    }
    
    /**
     * Convert from a string representation to a GameType
     * 
     * @param typeStr String representation of game type
     * @return Corresponding GameType, or UNKNOWN if not recognized
     */
    public static GameType fromString(String typeStr) {
        if (typeStr == null || typeStr.isEmpty()) {
            return GameType.UNKNOWN;
        }
        
        try {
            return GameType.valueOf(typeStr.toUpperCase());
        } catch (IllegalArgumentException e) {
            // Try some common aliases
            if (typeStr.equalsIgnoreCase("match3") || 
                typeStr.equalsIgnoreCase("match-3") ||
                typeStr.equalsIgnoreCase("matching")) {
                return GameType.PUZZLE;
            } else if (typeStr.equalsIgnoreCase("cards") ||
                       typeStr.equalsIgnoreCase("cardgame")) {
                return GameType.CARD;
            } else if (typeStr.equalsIgnoreCase("boardgame") ||
                       typeStr.equalsIgnoreCase("chess") ||
                       typeStr.equalsIgnoreCase("checkers")) {
                return GameType.BOARD;
            } else if (typeStr.equalsIgnoreCase("arcade")) {
                return GameType.ARCADE;
            } else if (typeStr.equalsIgnoreCase("platformer")) {
                return GameType.PLATFORMER;
            } else if (typeStr.equalsIgnoreCase("shooter") ||
                       typeStr.equalsIgnoreCase("fps") ||
                       typeStr.equalsIgnoreCase("tps")) {
                return GameType.SHOOTER;
            } else if (typeStr.equalsIgnoreCase("wordgame") ||
                       typeStr.equalsIgnoreCase("crossword") ||
                       typeStr.equalsIgnoreCase("scrabble")) {
                return GameType.WORD;
            } else if (typeStr.equalsIgnoreCase("action_adventure") || 
                       typeStr.equalsIgnoreCase("action-adventure")) {
                return GameType.ACTION_ADVENTURE;
            } else if (typeStr.equalsIgnoreCase("fighting") ||
                       typeStr.equalsIgnoreCase("combat")) {
                return GameType.FIGHTING;
            } else if (typeStr.equalsIgnoreCase("pubg") ||
                       typeStr.equalsIgnoreCase("pubgmobile")) {
                return GameType.PUBG_MOBILE;
            } else if (typeStr.equalsIgnoreCase("freefire") ||
                       typeStr.equalsIgnoreCase("free_fire")) {
                return GameType.FREE_FIRE;
            } else if (typeStr.equalsIgnoreCase("pokemon") ||
                       typeStr.equalsIgnoreCase("pokemon_unite") ||
                       typeStr.equalsIgnoreCase("pokemonunite")) {
                return GameType.POKEMON_UNITE;
            } else if (typeStr.equalsIgnoreCase("moba")) {
                return GameType.MOBA;
            } else if (typeStr.equalsIgnoreCase("clash") ||
                       typeStr.equalsIgnoreCase("clashofclans") ||
                       typeStr.equalsIgnoreCase("clash_of_clans")) {
                return GameType.CLASH_OF_CLANS;
            } else if (typeStr.equalsIgnoreCase("other")) {
                return GameType.OTHER;
            }
            
            return GameType.UNKNOWN;
        }
    }
    
    /**
     * Convert from utils.GameType to StandardizedGameType
     * 
     * @param gameType The utils.GameType to convert
     * @return Standardized game type
     */
    public static StandardizedGameType fromUtilsGameType(utils.GameType gameType) {
        if (gameType == null) {
            return new StandardizedGameType(GameType.UNKNOWN);
        }
        
        // Try to find a direct mapping by name
        String typeName = gameType.name();
        try {
            GameType stdType = GameType.valueOf(typeName);
            return new StandardizedGameType(stdType);
        } catch (IllegalArgumentException e) {
            // If no direct mapping, map based on some known types
            if (gameType == utils.GameType.PUZZLE) {
                return new StandardizedGameType(GameType.PUZZLE);
            } else if (gameType == utils.GameType.CARD) {
                return new StandardizedGameType(GameType.CARD);
            } else if (gameType == utils.GameType.BOARD) {
                return new StandardizedGameType(GameType.BOARD);
            } else if (gameType == utils.GameType.ARCADE) {
                return new StandardizedGameType(GameType.ARCADE);
            } else if (gameType == utils.GameType.WORD) {
                return new StandardizedGameType(GameType.WORD);
            } else if (gameType == utils.GameType.STRATEGY) {
                return new StandardizedGameType(GameType.STRATEGY);
            } else if (gameType == utils.GameType.SIMULATION) {
                return new StandardizedGameType(GameType.SIMULATION);
            } else if (gameType == utils.GameType.RPG) {
                return new StandardizedGameType(GameType.RPG);
            } else if (gameType == utils.GameType.ADVENTURE) {
                return new StandardizedGameType(GameType.ADVENTURE);
            } else if (gameType == utils.GameType.ACTION) {
                return new StandardizedGameType(GameType.ACTION);
            } else if (gameType == utils.GameType.SPORTS) {
                return new StandardizedGameType(GameType.SPORTS);
            } else if (gameType == utils.GameType.RACING) {
                return new StandardizedGameType(GameType.RACING);
            } else if (gameType == utils.GameType.EDUCATIONAL) {
                return new StandardizedGameType(GameType.EDUCATIONAL);
            } else if (gameType == utils.GameType.CASUAL) {
                return new StandardizedGameType(GameType.CASUAL);
            } else if (gameType == utils.GameType.PUBG_MOBILE) {
                return new StandardizedGameType(GameType.PUBG_MOBILE);
            } else if (gameType == utils.GameType.FREE_FIRE) {
                return new StandardizedGameType(GameType.FREE_FIRE);
            } else if (gameType == utils.GameType.POKEMON_UNITE) {
                return new StandardizedGameType(GameType.POKEMON_UNITE);
            } else if (gameType == utils.GameType.MOBA) {
                return new StandardizedGameType(GameType.MOBA);
            } else if (gameType == utils.GameType.CLASH_OF_CLANS) {
                return new StandardizedGameType(GameType.CLASH_OF_CLANS);
            } else if (gameType == utils.GameType.ACTION_ADVENTURE) {
                return new StandardizedGameType(GameType.ACTION_ADVENTURE);
            } else if (gameType == utils.GameType.FIGHTING) {
                return new StandardizedGameType(GameType.FIGHTING);
            } else if (gameType == utils.GameType.SHOOTER) {
                return new StandardizedGameType(GameType.SHOOTER);
            } else if (gameType == utils.GameType.PLATFORMER) {
                return new StandardizedGameType(GameType.PLATFORMER);
            } else if (gameType == utils.GameType.OTHER) {
                return new StandardizedGameType(GameType.OTHER);
            } else {
                return new StandardizedGameType(GameType.UNKNOWN);
            }
        }
    }
    
    /**
     * Convert to utils.GameType
     * 
     * @return Equivalent utils.GameType
     */
    public utils.GameType toUtilsGameType() {
        String typeName = type.name();
        try {
            return utils.GameType.valueOf(typeName);
        } catch (IllegalArgumentException e) {
            return utils.GameType.UNKNOWN;
        }
    }
    
    /**
     * Get the display name for this game type
     * 
     * @return Display name
     */
    public String getDisplayName() {
        return name;
    }
    
    @Override
    public String toString() {
        return "GameType{type=" + type + ", name='" + name + "'}";
    }
}