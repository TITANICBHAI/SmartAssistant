package com.aiassistant.models;

/**
 * Standardized game type enum
 * Consolidates multiple GameType implementations across the codebase
 */
public enum StandardizedGameType {
    // General game categories
    ACTION("action"),
    ADVENTURE("adventure"),
    PUZZLE("puzzle"),
    ROLE_PLAYING("role_playing"),
    STRATEGY("strategy"),
    SIMULATION("simulation"),
    SPORTS("sports"),
    RACING("racing"),
    CASUAL("casual"),
    
    // Specific game types
    PUBG_MOBILE("pubg_mobile"),
    FREE_FIRE("free_fire"),
    CLASH_OF_CLANS("clash_of_clans"),
    POKEMON_UNITE("pokemon_unite"),
    MOBA("moba"),
    RPG("rpg"),
    FPS("fps"),
    
    // Default
    UNKNOWN("unknown");
    
    private final String value;
    
    StandardizedGameType(String value) {
        this.value = value;
    }
    
    public String getValue() {
        return value;
    }
    
    /**
     * Find game type by string value
     * 
     * @param value String value to search for
     * @return Matching game type or UNKNOWN if not found
     */
    public static StandardizedGameType fromString(String value) {
        if (value == null || value.isEmpty()) {
            return UNKNOWN;
        }
        
        for (StandardizedGameType type : values()) {
            if (type.value.equalsIgnoreCase(value)) {
                return type;
            }
        }
        
        return UNKNOWN;
    }
    
    /**
     * Check if this game type is an FPS game
     * 
     * @return True if FPS game
     */
    public boolean isFpsGame() {
        return this == FPS || this == PUBG_MOBILE || this == FREE_FIRE;
    }
    
    /**
     * Check if this game type is a MOBA game
     * 
     * @return True if MOBA game
     */
    public boolean isMobaGame() {
        return this == MOBA || this == POKEMON_UNITE;
    }
    
    /**
     * Check if this game type is a strategy game
     * 
     * @return True if strategy game
     */
    public boolean isStrategyGame() {
        return this == STRATEGY || this == CLASH_OF_CLANS;
    }
    
    @Override
    public String toString() {
        return value;
    }
}