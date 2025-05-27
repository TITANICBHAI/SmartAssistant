package models;

/**
 * Enumeration of game types supported by the AI assistant.
 * Mirror of the utils.GameType enum to ensure compatibility.
 * This represents different categories of games that can be analyzed.
 */
public enum GameType {
    PUZZLE,        // Puzzle games like match-3, block puzzle, etc.
    CARD,          // Card games like solitaire, poker, etc.
    BOARD,         // Board games like chess, checkers, etc.
    ARCADE,        // Arcade games like platformers, shooters, etc.
    WORD,          // Word games like crosswords, word search, etc.
    STRATEGY,      // Strategy games like tower defense, RTS, etc.
    SIMULATION,    // Simulation games like city builders, life sims, etc.
    RPG,           // Role-playing games with character progression
    ADVENTURE,     // Adventure games focused on exploration and story
    ACTION,        // Action games with reflex-based gameplay
    SPORTS,        // Sports games like football, basketball, etc.
    RACING,        // Racing games
    EDUCATIONAL,   // Educational games for learning
    CASUAL,        // Casual games with simple mechanics
    PUBG_MOBILE,   // PUBG Mobile battle royale game
    FREE_FIRE,     // Free Fire battle royale game
    POKEMON_UNITE, // Pokemon Unite MOBA game
    MOBA,          // Multiplayer Online Battle Arena games
    CLASH_OF_CLANS,// Clash of Clans strategy game
    ACTION_ADVENTURE, // Action-adventure hybrid games
    FIGHTING,      // Fighting and combat games
    SHOOTER,       // First-person or third-person shooters
    PLATFORMER,    // Platform jumping games
    PLATFORM,      // Synonym for PLATFORMER
    MUSIC,         // Music and rhythm games
    TRIVIA,        // Trivia and quiz games
    CASINO,        // Casino and gambling games
    MATCH3,        // Match-3 puzzle games
    IDLE,          // Idle and clicker games
    RUNNER,        // Endless runner games
    TOWER_DEFENSE, // Tower defense strategy games
    OTHER,         // Other types of applications or games
    UNKNOWN;       // Unknown or unclassified game type
    
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
                return "Platformer Game";
            case OTHER:
                return "Other Application";
            default:
                return "Unknown Game";
        }
    }
    
    /**
     * Get a short description of this game type
     * 
     * @return Short description
     */
    public String getDescription() {
        switch (this) {
            case PUZZLE:
                return "Games that challenge logical thinking with patterns and problem-solving";
            case CARD:
                return "Games played with cards, either traditional or digital";
            case BOARD:
                return "Digital versions of traditional board games or similar";
            case ARCADE:
                return "Fast-paced games with simple controls and increasing difficulty";
            case WORD:
                return "Games focused on language, vocabulary, and word formation";
            case STRATEGY:
                return "Games that prioritize decision-making and planning";
            case SIMULATION:
                return "Games that simulate real-world systems or activities";
            case RPG:
                return "Games with character development, stats, and progression";
            case ADVENTURE:
                return "Story-driven games with exploration and puzzles";
            case ACTION:
                return "Games focused on reflexes, timing, and coordination";
            case SPORTS:
                return "Games simulating real-world sports and competitions";
            case RACING:
                return "Games focused on competitive vehicle racing";
            case EDUCATIONAL:
                return "Games designed primarily to teach or educate";
            case CASUAL:
                return "Simple games with minimal learning curve";
            case PUBG_MOBILE:
                return "Battle royale game where players fight to be the last one standing";
            case FREE_FIRE:
                return "Fast-paced battle royale game with unique character abilities";
            case POKEMON_UNITE:
                return "Team-based MOBA game featuring Pokemon characters";
            case MOBA:
                return "Multiplayer Online Battle Arena games with team-based combat";
            case CLASH_OF_CLANS:
                return "Strategic base-building and combat game";
            case ACTION_ADVENTURE:
                return "Games blending action gameplay with exploration and puzzle elements";
            case FIGHTING:
                return "Combat games focusing on one-on-one battles with special moves";
            case SHOOTER:
                return "Games where the primary gameplay involves shooting enemies";
            case PLATFORMER:
                return "Games involving running and jumping between platforms";
            case OTHER:
                return "Other applications or non-gaming software";
            default:
                return "Unclassified or unknown game type";
        }
    }
    
    /**
     * Check if this game type is a puzzle game
     * 
     * @return True if this is a puzzle game
     */
    public boolean isPuzzleGame() {
        return this == PUZZLE;
    }
    
    /**
     * Check if this game type is a card game
     * 
     * @return True if this is a card game
     */
    public boolean isCardGame() {
        return this == CARD;
    }
    
    /**
     * Check if this game type is a board game
     * 
     * @return True if this is a board game
     */
    public boolean isBoardGame() {
        return this == BOARD;
    }
    
    /**
     * Check if this game type is a casual game
     * 
     * @return True if this is a casual game
     */
    public boolean isCasualGame() {
        return this == CASUAL || this == PUZZLE;
    }
    
    /**
     * Check if this game type requires quick reflexes
     * 
     * @return True if this game type requires quick reflexes
     */
    public boolean requiresQuickReflexes() {
        return this == ACTION || this == ARCADE || this == SHOOTER || 
               this == PLATFORMER || this == RACING || this == FIGHTING ||
               this == PUBG_MOBILE || this == FREE_FIRE;
    }
    
    /**
     * Check if this game type is strategic
     * 
     * @return True if this is a strategic game
     */
    public boolean isStrategic() {
        return this == STRATEGY || this == BOARD || this == CLASH_OF_CLANS ||
               this == RPG || this == MOBA || this == POKEMON_UNITE;
    }
    
    /**
     * Convert from a string representation to a GameType
     * 
     * @param typeStr String representation of game type
     * @return Corresponding GameType, or UNKNOWN if not recognized
     */
    public static GameType fromString(String typeStr) {
        if (typeStr == null || typeStr.isEmpty()) {
            return UNKNOWN;
        }
        
        try {
            return valueOf(typeStr.toUpperCase());
        } catch (IllegalArgumentException e) {
            return UNKNOWN;
        }
    }
    
    /**
     * Determine the game type based on the package name.
     * This analyzes the package name to identify known games.
     * 
     * @param packageName The package name to analyze
     * @return The identified GameType, or UNKNOWN if not recognized
     */
    public static GameType fromPackageName(String packageName) {
        if (packageName == null || packageName.isEmpty()) {
            return UNKNOWN;
        }
        
        packageName = packageName.toLowerCase();
        
        // MOBA games
        if (packageName.contains("leagueoflegends") || 
            packageName.contains("mobilelegends") || 
            packageName.contains("dota2") || 
            packageName.contains("heroesofthestore") ||
            packageName.contains("arenofvalor") ||
            packageName.contains("vainglory")) {
            return MOBA;
        }
        
        // Battle Royale games
        if (packageName.contains("pubg") || 
            packageName.contains("playerunknownsbattlegrounds") || 
            packageName.contains("pubgmobile")) {
            return PUBG_MOBILE;
        }
        
        if (packageName.contains("freefire") || 
            packageName.contains("garena.freefire")) {
            return FREE_FIRE;
        }
        
        // Pokemon games
        if (packageName.contains("pokemon.unite") || 
            packageName.contains("pokemonunite")) {
            return POKEMON_UNITE;
        }
        
        // Strategy games
        if (packageName.contains("clashofclans") || 
            packageName.contains("supercell.clashofclans")) {
            return CLASH_OF_CLANS;
        }
        
        if (packageName.contains("clashroyale") || 
            packageName.contains("supercell.clashroyale") ||
            packageName.contains("riseofkingdoms") ||
            packageName.contains("boombeach") ||
            packageName.contains("agestrategy") ||
            packageName.contains("strategy")) {
            return STRATEGY;
        }
        
        // Puzzle games
        if (packageName.contains("puzzle") || 
            packageName.contains("match3") || 
            packageName.contains("candy") ||
            packageName.contains("block") ||
            packageName.contains("tetris") ||
            packageName.contains("wordscapes")) {
            return PUZZLE;
        }
        
        // Card games
        if (packageName.contains("card") || 
            packageName.contains("poker") ||
            packageName.contains("hearthstone") ||
            packageName.contains("solitaire")) {
            return CARD;
        }
        
        // Board games
        if (packageName.contains("chess") || 
            packageName.contains("checkers") ||
            packageName.contains("board") ||
            packageName.contains("backgammon") ||
            packageName.contains("scrabble") ||
            packageName.contains("monopoly")) {
            return BOARD;
        }
        
        // RPG games
        if (packageName.contains("rpg") || 
            packageName.contains("roleplay") ||
            packageName.contains("genshinimpact") ||
            packageName.contains("roblox")) {
            return RPG;
        }
        
        // Racing games
        if (packageName.contains("racing") || 
            packageName.contains("asphalt") ||
            packageName.contains("needforspeed") ||
            packageName.contains("mariocar") ||
            packageName.contains("realracing")) {
            return RACING;
        }
        
        // Sports games
        if (packageName.contains("sport") || 
            packageName.contains("fifa") ||
            packageName.contains("football") ||
            packageName.contains("basketball") ||
            packageName.contains("baseball") ||
            packageName.contains("soccer") ||
            packageName.contains("tennis") ||
            packageName.contains("golf")) {
            return SPORTS;
        }
        
        // Arcade games
        if (packageName.contains("arcade") ||
            packageName.contains("fruitninja") ||
            packageName.contains("jetpack") ||
            packageName.contains("templerun") ||
            packageName.contains("mario")) {
            return ARCADE;
        }
        
        // Educational games
        if (packageName.contains("education") ||
            packageName.contains("learn") ||
            packageName.contains("quiz") ||
            packageName.contains("trivia") ||
            packageName.contains("brain") ||
            packageName.contains("memory")) {
            return EDUCATIONAL;
        }
        
        // Adventure games
        if (packageName.contains("adventure") ||
            packageName.contains("explore") ||
            packageName.contains("quest") ||
            packageName.contains("minecraft")) {
            return ADVENTURE;
        }
        
        // Look for "game" in the package name to detect generic games
        if (packageName.contains("game") || 
            packageName.contains("casual") ||
            packageName.contains("play")) {
            return CASUAL;
        }
        
        return UNKNOWN;
    }
    
    /**
     * Convert a utils.GameType to models.GameType
     * 
     * @param utilsGameType The utils.GameType
     * @return The corresponding models.GameType
     */
    public static GameType fromUtilsGameType(utils.GameType utilsGameType) {
        if (utilsGameType == null) {
            return UNKNOWN;
        }
        
        try {
            return valueOf(utilsGameType.name());
        } catch (IllegalArgumentException e) {
            return UNKNOWN;
        }
    }
    
    /**
     * Convert to a utils.GameType
     * 
     * @return The corresponding utils.GameType
     */
    public utils.GameType toUtilsGameType() {
        try {
            return utils.GameType.valueOf(this.name());
        } catch (IllegalArgumentException e) {
            // Handle special cases that might not have direct mapping
            switch (this) {
                case PUBG_MOBILE:
                case FREE_FIRE:
                    return utils.GameType.SHOOTER;
                case POKEMON_UNITE:
                case MOBA:
                    return utils.GameType.STRATEGY;
                case CLASH_OF_CLANS:
                    return utils.GameType.STRATEGY;
                case PLATFORMER:
                    return utils.GameType.PLATFORM;
                case ACTION_ADVENTURE:
                    return utils.GameType.ACTION;
                default:
                    return utils.GameType.UNKNOWN;
            }
        }
    }
}