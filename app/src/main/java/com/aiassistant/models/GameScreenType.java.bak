package com.aiassistant.models;

/**
 * Enum representing different types of screens in a game
 */
public enum GameScreenType {
    MAIN_MENU("main_menu"),
    LOADING("loading"),
    GAMEPLAY("gameplay"),
    PAUSE_MENU("pause_menu"),
    SETTINGS("settings"),
    LEVEL_SELECT("level_select"),
    STORE("store"),
    RESULTS("results"),
    GAME_OVER("game_over"),
    CHARACTER_SELECT("character_select"),
    TUTORIAL("tutorial"),
    CUTSCENE("cutscene"),
    UNKNOWN("unknown");
    
    private final String value;
    
    GameScreenType(String value) {
        this.value = value;
    }
    
    public String getValue() {
        return value;
    }
    
    /**
     * Convert a string to GameScreenType
     * 
     * @param value String value
     * @return GameScreenType or UNKNOWN if not found
     */
    public static GameScreenType fromString(String value) {
        if (value == null) {
            return UNKNOWN;
        }
        
        for (GameScreenType type : values()) {
            if (type.value.equalsIgnoreCase(value)) {
                return type;
            }
        }
        
        return UNKNOWN;
    }
}