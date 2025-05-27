package utils;

/**
 * Enum for the types of games.
 */
public enum GameType {
    UNKNOWN("UNKNOWN"),
    PUZZLE("PUZZLE"),
    WORD("WORD"),
    ARCADE("ARCADE"),
    RACING("RACING"),
    PLATFORM("PLATFORM"),
    ACTION("ACTION"),
    ADVENTURE("ADVENTURE"),
    RPG("RPG"),
    STRATEGY("STRATEGY"),
    SIMULATION("SIMULATION"),
    SPORTS("SPORTS"),
    FIGHTING("FIGHTING"),
    CARD("CARD"),
    BOARD("BOARD"),
    EDUCATIONAL("EDUCATIONAL"),
    MUSIC("MUSIC"),
    TRIVIA("TRIVIA"),
    CASINO("CASINO"),
    MATCH3("MATCH3"),
    IDLE("IDLE"),
    RUNNER("RUNNER"),
    TOWER_DEFENSE("TOWER_DEFENSE"),
    SHOOTER("SHOOTER"),
    CASUAL("CASUAL"),
    OTHER("OTHER"),
    // Special game types for specific games
    PUBG_MOBILE("PUBG_MOBILE"),
    FREE_FIRE("FREE_FIRE"),
    POKEMON_UNITE("POKEMON_UNITE"),
    MOBA("MOBA"),
    CLASH_OF_CLANS("CLASH_OF_CLANS"),
    ACTION_ADVENTURE("ACTION_ADVENTURE"),
    PLATFORMER("PLATFORMER");
    
    private final String typeName;
    
    /**
     * Constructor.
     * 
     * @param typeName The type name
     */
    GameType(String typeName) {
        this.typeName = typeName;
    }
    
    /**
     * Get the type name.
     * 
     * @return The type name
     */
    public String getTypeName() {
        return typeName;
    }
    
    /**
     * Convert from package name to GameType.
     * 
     * @param packageName The package name
     * @return The GameType
     */
    public static GameType fromPackageName(String packageName) {
        if (packageName == null || packageName.isEmpty()) {
            return UNKNOWN;
        }
        
        if (packageName.contains("puzzle")) {
            return PUZZLE;
        } else if (packageName.contains("word") || packageName.contains("crossword") || packageName.contains("scrabble")) {
            return WORD;
        } else if (packageName.contains("arcade")) {
            return ARCADE;
        } else if (packageName.contains("racing") || packageName.contains("race") || packageName.contains("speed")) {
            return RACING;
        } else if (packageName.contains("platform") || packageName.contains("jump")) {
            return PLATFORM;
        } else if (packageName.contains("action")) {
            return ACTION;
        } else if (packageName.contains("adventure") || packageName.contains("quest")) {
            return ADVENTURE;
        } else if (packageName.contains("rpg") || packageName.contains("role")) {
            return RPG;
        } else if (packageName.contains("strategy") || packageName.contains("tactics")) {
            return STRATEGY;
        } else if (packageName.contains("simulation") || packageName.contains("sim")) {
            return SIMULATION;
        } else if (packageName.contains("sports") || packageName.contains("football") || packageName.contains("soccer") || packageName.contains("basketball")) {
            return SPORTS;
        } else if (packageName.contains("fighting") || packageName.contains("combat") || packageName.contains("wrestler")) {
            return FIGHTING;
        } else if (packageName.contains("card") || packageName.contains("poker") || packageName.contains("solitaire")) {
            return CARD;
        } else if (packageName.contains("board") || packageName.contains("chess") || packageName.contains("checkers")) {
            return BOARD;
        } else if (packageName.contains("education") || packageName.contains("learn") || packageName.contains("teach")) {
            return EDUCATIONAL;
        } else if (packageName.contains("music") || packageName.contains("rhythm") || packageName.contains("beat")) {
            return MUSIC;
        } else if (packageName.contains("trivia") || packageName.contains("quiz")) {
            return TRIVIA;
        } else if (packageName.contains("casino") || packageName.contains("slot") || packageName.contains("gamble")) {
            return CASINO;
        } else if (packageName.contains("match3") || packageName.contains("candy") || packageName.contains("jewel") || packageName.contains("gem")) {
            return MATCH3;
        } else if (packageName.contains("idle") || packageName.contains("clicker")) {
            return IDLE;
        } else if (packageName.contains("runner") || packageName.contains("endless") || packageName.contains("subway") || packageName.contains("temple")) {
            return RUNNER;
        } else if (packageName.contains("tower") && packageName.contains("defense")) {
            return TOWER_DEFENSE;
        } else if (packageName.contains("shooter") || packageName.contains("shoot") || packageName.contains("fps")) {
            return SHOOTER;
        } else if (packageName.contains("casual")) {
            return CASUAL;
        }
        
        return UNKNOWN;
    }
    
    /**
     * Convert from string to GameType.
     * 
     * @param typeStr The type string
     * @return The GameType
     */
    public static GameType fromString(String typeStr) {
        if (typeStr == null || typeStr.isEmpty()) {
            return UNKNOWN;
        }
        
        String normalized = typeStr.toUpperCase().trim().replace(' ', '_');
        
        for (GameType type : values()) {
            if (type.typeName.equalsIgnoreCase(normalized) || type.name().equalsIgnoreCase(normalized)) {
                return type;
            }
        }
        
        // Handle specific game names
        if (normalized.contains("PUBG") || normalized.equals("PUBG_MOBILE")) {
            return SHOOTER;
        } else if (normalized.contains("FREE_FIRE") || normalized.equals("FREE_FIRE")) {
            return SHOOTER;
        } else if (normalized.contains("POKEMON") && normalized.contains("UNITE")) {
            return STRATEGY;
        } else if (normalized.contains("CLASH") && normalized.contains("CLANS")) {
            return STRATEGY;
        } else if (normalized.contains("MOBA")) {
            return STRATEGY;
        }
        
        // Try to check for substrings if there's no exact match
        if (normalized.contains("PUZZLE")) {
            return PUZZLE;
        } else if (normalized.contains("WORD")) {
            return WORD;
        } else if (normalized.contains("ARCADE")) {
            return ARCADE;
        } else if (normalized.contains("RACING") || normalized.contains("RACE")) {
            return RACING;
        } else if (normalized.contains("PLATFORM")) {
            return PLATFORM;
        } else if (normalized.contains("ACTION")) {
            return ACTION;
        } else if (normalized.contains("ADVENTURE")) {
            return ADVENTURE;
        } else if (normalized.contains("RPG") || normalized.contains("ROLE")) {
            return RPG;
        } else if (normalized.contains("STRATEGY")) {
            return STRATEGY;
        } else if (normalized.contains("SIMULATION")) {
            return SIMULATION;
        } else if (normalized.contains("SPORTS")) {
            return SPORTS;
        } else if (normalized.contains("FIGHTING")) {
            return FIGHTING;
        } else if (normalized.contains("CARD")) {
            return CARD;
        } else if (normalized.contains("BOARD")) {
            return BOARD;
        } else if (normalized.contains("EDUCATION")) {
            return EDUCATIONAL;
        } else if (normalized.contains("MUSIC")) {
            return MUSIC;
        } else if (normalized.contains("TRIVIA")) {
            return TRIVIA;
        } else if (normalized.contains("CASINO")) {
            return CASINO;
        } else if (normalized.contains("MATCH3") || normalized.contains("MATCH_3")) {
            return MATCH3;
        } else if (normalized.contains("IDLE")) {
            return IDLE;
        } else if (normalized.contains("RUNNER")) {
            return RUNNER;
        } else if (normalized.contains("TOWER") && normalized.contains("DEFENSE")) {
            return TOWER_DEFENSE;
        } else if (normalized.contains("SHOOTER") || normalized.contains("FPS")) {
            return SHOOTER;
        } else if (normalized.contains("CASUAL")) {
            return CASUAL;
        }
        
        return UNKNOWN;
    }
    
    /**
     * Convert GameType to models.GameType.
     * 
     * @return The models.GameType
     */
    public models.GameType toModelsGameType() {
        return GameTypeConverter.toModelsGameType(this);
    }
    
    /**
     * Get a human-readable display name for this game type.
     * 
     * @return A formatted display name for this game type
     */
    public String getDisplayName() {
        // Convert enum constant to a readable format
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
            case ACTION_ADVENTURE:
                return "Action-Adventure Game";
            case FIGHTING:
                return "Fighting Game";
            case SHOOTER:
                return "Shooter Game";
            case PLATFORMER:
            case PLATFORM:
                return "Platformer Game";
            case MUSIC:
                return "Music & Rhythm Game";
            case TRIVIA:
                return "Trivia Game";
            case CASINO:
                return "Casino Game";
            case MATCH3:
                return "Match-3 Puzzle Game";
            case IDLE:
                return "Idle Game";
            case RUNNER:
                return "Runner Game";
            case TOWER_DEFENSE:
                return "Tower Defense Game";
            case OTHER:
                return "Other Game";
            case UNKNOWN:
            default:
                return "Unknown Game Type";
        }
    }
}