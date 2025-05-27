package utils;

/**
 * Enum for the types of UI elements.
 */
public enum ElementType {
    UNKNOWN("UNKNOWN"),
    BUTTON("BUTTON"),
    TEXT("TEXT"),
    IMAGE("IMAGE"),
    CONTAINER("CONTAINER"),
    CHECKBOX("CHECKBOX"),
    TOGGLE("TOGGLE"),
    SLIDER("SLIDER"),
    LIST("LIST"),
    GRID("GRID"),
    CARD("CARD"),
    DIALOG("DIALOG"),
    MENU("MENU"),
    TAB("TAB"),
    ICON("ICON"),
    NAVIGATION("NAVIGATION"),
    NAVIGATION_ITEM("NAVIGATION_ITEM"),
    RADIO("RADIO"),
    RADIO_BUTTON("RADIO_BUTTON"),
    PROGRESS("PROGRESS"),
    PROGRESS_BAR("PROGRESS_BAR"),
    TOOLBAR("TOOLBAR"),
    HEADER("HEADER"),
    FOOTER("FOOTER"),
    SCROLL_VIEW("SCROLL_VIEW"),
    SCROLLVIEW("SCROLLVIEW"),
    INPUT("INPUT"),
    TEXT_FIELD("TEXT_FIELD"),
    INPUT_FIELD("INPUT_FIELD"),
    GAME_CONTROL("GAME_CONTROL"),
    // Game-specific element types
    GAME_OBJECT("GAME_OBJECT"),
    PLAYER("PLAYER"),
    ENEMY("ENEMY"),
    COLLECTIBLE("COLLECTIBLE"),
    POWERUP("POWERUP"),
    OBSTACLE("OBSTACLE"),
    PLATFORM("PLATFORM"),
    BACKGROUND("BACKGROUND"),
    FOREGROUND("FOREGROUND"),
    // Additional UI element types
    LABEL("LABEL"),
    LINK("LINK"),
    OPTION("OPTION"),
    SELECT("SELECT"),
    TEXTAREA("TEXTAREA"),
    DROPDOWN("DROPDOWN"),
    VIDEO("VIDEO"),
    AUDIO("AUDIO"),
    SECTION("SECTION"),
    PANEL("PANEL"),
    NOTIFICATION("NOTIFICATION"),
    PROGRESSINDICATOR("PROGRESSINDICATOR"),
    DIVIDER("DIVIDER");
    
    private final String typeName;
    
    /**
     * Constructor.
     * 
     * @param typeName The type name
     */
    ElementType(String typeName) {
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
     * Convert from string to ElementType.
     * 
     * @param typeStr The type string
     * @return The ElementType
     */
    public static ElementType fromString(String typeStr) {
        if (typeStr == null || typeStr.isEmpty()) {
            return UNKNOWN;
        }
        
        String normalized = typeStr.toUpperCase().trim().replace(' ', '_');
        
        for (ElementType type : values()) {
            if (type.typeName.equalsIgnoreCase(normalized) || type.name().equalsIgnoreCase(normalized)) {
                return type;
            }
        }
        
        // Try to check for substrings if there's no exact match
        if (normalized.contains("BUTTON")) {
            return BUTTON;
        } else if (normalized.contains("TEXT")) {
            return TEXT;
        } else if (normalized.contains("IMAGE")) {
            return IMAGE;
        } else if (normalized.contains("CONTAINER") || normalized.contains("VIEW") || normalized.contains("LAYOUT")) {
            return CONTAINER;
        } else if (normalized.contains("CHECK")) {
            return CHECKBOX;
        } else if (normalized.contains("TOGGLE") || normalized.contains("SWITCH")) {
            return TOGGLE;
        } else if (normalized.contains("SLIDER") || normalized.contains("SEEK")) {
            return SLIDER;
        } else if (normalized.contains("LIST")) {
            return LIST;
        } else if (normalized.contains("GRID")) {
            return GRID;
        } else if (normalized.contains("CARD")) {
            return CARD;
        } else if (normalized.contains("DIALOG") || normalized.contains("POPUP")) {
            return DIALOG;
        } else if (normalized.contains("MENU")) {
            return MENU;
        } else if (normalized.contains("TAB")) {
            return TAB;
        } else if (normalized.contains("ICON")) {
            return ICON;
        } else if (normalized.contains("NAVIGATION")) {
            return NAVIGATION;
        } else if (normalized.contains("RADIO")) {
            return RADIO;
        } else if (normalized.contains("PROGRESS")) {
            return PROGRESS;
        } else if (normalized.contains("TOOL") && normalized.contains("BAR")) {
            return TOOLBAR;
        } else if (normalized.contains("HEADER")) {
            return HEADER;
        } else if (normalized.contains("FOOTER")) {
            return FOOTER;
        } else if (normalized.contains("SCROLL")) {
            return SCROLL_VIEW;
        } else if (normalized.contains("INPUT") || normalized.contains("FIELD") || normalized.contains("EDIT")) {
            return INPUT;
        } else if (normalized.contains("GAME") && normalized.contains("CONTROL")) {
            return GAME_CONTROL;
        } else if (normalized.contains("GAME") && normalized.contains("OBJECT")) {
            return GAME_OBJECT;
        } else if (normalized.contains("PLAYER")) {
            return PLAYER;
        } else if (normalized.contains("ENEMY")) {
            return ENEMY;
        } else if (normalized.contains("COLLECTIBLE")) {
            return COLLECTIBLE;
        } else if (normalized.contains("POWERUP") || (normalized.contains("POWER") && normalized.contains("UP"))) {
            return POWERUP;
        } else if (normalized.contains("OBSTACLE")) {
            return OBSTACLE;
        } else if (normalized.contains("PLATFORM")) {
            return PLATFORM;
        } else if (normalized.contains("BACKGROUND")) {
            return BACKGROUND;
        } else if (normalized.contains("FOREGROUND")) {
            return FOREGROUND;
        } else if (normalized.contains("LABEL")) {
            return LABEL;
        } else if (normalized.contains("LINK")) {
            return LINK;
        } else if (normalized.contains("OPTION")) {
            return OPTION;
        } else if (normalized.contains("SELECT")) {
            return SELECT;
        } else if (normalized.contains("TEXTAREA")) {
            return TEXTAREA;
        } else if (normalized.contains("DROPDOWN")) {
            return DROPDOWN;
        } else if (normalized.contains("VIDEO")) {
            return VIDEO;
        } else if (normalized.contains("AUDIO")) {
            return AUDIO;
        } else if (normalized.contains("SECTION")) {
            return SECTION;
        } else if (normalized.contains("PANEL")) {
            return PANEL;
        } else if (normalized.contains("NOTIFICATION")) {
            return NOTIFICATION;
        } else if (normalized.contains("PROGRESSINDICATOR")) {
            return PROGRESSINDICATOR;
        } else if (normalized.contains("DIVIDER")) {
            return DIVIDER;
        }
        
        return UNKNOWN;
    }
    
    /**
     * Convert ElementType to models.ElementType.
     * 
     * @return The models.ElementType
     */
    public models.ElementType toModelsElementType() {
        return ElementTypeConverter.toModelsElementType(this);
    }
}