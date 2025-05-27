package models;

/**
 * Types of UI elements used in the models package.
 */
public enum ElementType {
    /**
     * Unknown element type.
     */
    UNKNOWN,
    
    /**
     * A button element.
     */
    BUTTON,
    
    /**
     * A text element.
     */
    TEXT,
    
    /**
     * An image element.
     */
    IMAGE,
    
    /**
     * A checkbox element.
     */
    CHECKBOX,
    
    /**
     * A radio button element.
     */
    RADIO_BUTTON,
    
    /**
     * A toggle element.
     */
    TOGGLE,
    
    /**
     * A text field or input element.
     */
    TEXT_FIELD,
    
    /**
     * A slider element.
     */
    SLIDER,
    
    /**
     * A spinner element.
     */
    SPINNER,
    
    /**
     * A progress bar element.
     */
    PROGRESS_BAR,
    
    /**
     * A menu element.
     */
    MENU,
    
    /**
     * A dialog element.
     */
    DIALOG,
    
    /**
     * A card element.
     */
    CARD,
    
    /**
     * An icon element.
     */
    ICON,
    
    /**
     * A container element.
     */
    CONTAINER,
    
    /**
     * A list element.
     */
    LIST,
    
    /**
     * A grid element.
     */
    GRID,
    
    /**
     * A game object.
     */
    GAME_OBJECT,
    
    /**
     * A player element in a game.
     */
    PLAYER,
    
    /**
     * An enemy element in a game.
     */
    ENEMY,
    
    /**
     * A collectible element in a game.
     */
    COLLECTIBLE,
    
    /**
     * A power-up element in a game.
     */
    POWERUP,
    
    /**
     * An obstacle element in a game.
     */
    OBSTACLE,
    
    /**
     * A platform element in a game.
     */
    PLATFORM,
    
    /**
     * A background element.
     */
    BACKGROUND,
    
    /**
     * A foreground element.
     */
    FOREGROUND,
    
    /**
     * An input field element.
     */
    INPUT_FIELD,
    
    /**
     * Input element (alias for INPUT_FIELD for compatibility).
     */
    INPUT,
    
    /**
     * A scroll view element.
     */
    SCROLL_VIEW,
    
    /**
     * A list item element.
     */
    LIST_ITEM,
    
    /**
     * A label element.
     */
    LABEL,
    
    /**
     * A link element.
     */
    LINK,
    
    /**
     * An option element.
     */
    OPTION,
    
    /**
     * A select element.
     */
    SELECT,
    
    /**
     * A textarea element.
     */
    TEXTAREA,
    
    /**
     * A dropdown element.
     */
    DROPDOWN,
    
    /**
     * A tab element.
     */
    TAB,
    
    /**
     * A navigation element.
     */
    NAVIGATION,
    
    /**
     * A video element.
     */
    VIDEO,
    
    /**
     * An audio element.
     */
    AUDIO,
    
    /**
     * A section element.
     */
    SECTION,
    
    /**
     * A panel element.
     */
    PANEL,
    
    /**
     * A toast element.
     */
    TOAST,
    
    /**
     * A menu item element.
     */
    MENU_ITEM;
    
    /**
     * Convert from utils.ElementType to models.ElementType.
     * 
     * @param type The utils.ElementType to convert
     * @return The equivalent models.ElementType
     */
    public static ElementType fromUtilsElementType(utils.ElementType type) {
        if (type == null) {
            return ElementType.UNKNOWN;
        }
        
        switch (type) {
            case RADIO:
            case RADIO_BUTTON:
                return ElementType.RADIO_BUTTON;
            case PROGRESS:
            case PROGRESS_BAR:
                return ElementType.PROGRESS_BAR;
            case SCROLL_VIEW:
            case SCROLLVIEW:
                return ElementType.SCROLL_VIEW;
            case INPUT:
                return ElementType.TEXT_FIELD;
            case BUTTON:
                return ElementType.BUTTON;
            case TEXT:
                return ElementType.TEXT;
            case IMAGE:
                return ElementType.IMAGE;
            case CONTAINER:
                return ElementType.CONTAINER;
            case CHECKBOX:
                return ElementType.CHECKBOX;
            case TOGGLE:
                return ElementType.TOGGLE;
            case SLIDER:
                return ElementType.SLIDER;
            case LIST:
                return ElementType.LIST;
            case GRID:
                return ElementType.GRID;
            case CARD:
                return ElementType.CARD;
            case DIALOG:
                return ElementType.DIALOG;
            case MENU:
                return ElementType.MENU;
            case TAB:
                return ElementType.TAB;
            case ICON:
                return ElementType.ICON;
            case NAVIGATION:
                return ElementType.NAVIGATION;
            default:
                try {
                    return ElementType.valueOf(type.name());
                } catch (IllegalArgumentException e) {
                    return ElementType.UNKNOWN;
                }
        }
    }
    
    /**
     * Convert from string to ElementType.
     * 
     * @param typeStr String representation of the element type
     * @return The equivalent ElementType
     */
    public static ElementType fromString(String typeStr) {
        if (typeStr == null || typeStr.trim().isEmpty()) {
            return UNKNOWN;
        }
        
        try {
            return valueOf(typeStr.toUpperCase());
        } catch (IllegalArgumentException e) {
            // Try to match with more specific handling
            if (typeStr.equalsIgnoreCase("input") || typeStr.equalsIgnoreCase("edit_text")) {
                return TEXT_FIELD;
            } else if (typeStr.equalsIgnoreCase("radio") || typeStr.equalsIgnoreCase("radio_button")) {
                return RADIO_BUTTON;
            } else if (typeStr.equalsIgnoreCase("progress") || typeStr.equalsIgnoreCase("progress_bar")) {
                return PROGRESS_BAR;
            } else if (typeStr.equalsIgnoreCase("check") || typeStr.equalsIgnoreCase("check_box")) {
                return CHECKBOX;
            } else if (typeStr.equalsIgnoreCase("scroll") || typeStr.equalsIgnoreCase("scroll_view")) {
                return SCROLL_VIEW;
            }
            
            return UNKNOWN;
        }
    }
    
    /**
     * Convert from models.ElementType to utils.ElementType.
     * 
     * @return The equivalent utils.ElementType
     */
    public utils.ElementType toUtilsElementType() {
        switch (this) {
            case RADIO_BUTTON:
                return utils.ElementType.RADIO;
            case PROGRESS_BAR:
                return utils.ElementType.PROGRESS;
            case SCROLL_VIEW:
                return utils.ElementType.SCROLL_VIEW;
            case INPUT_FIELD:
            case INPUT:
            case TEXT_FIELD:
                return utils.ElementType.INPUT;
            case BUTTON:
                return utils.ElementType.BUTTON;
            case TEXT:
                return utils.ElementType.TEXT;
            case IMAGE:
                return utils.ElementType.IMAGE;
            case CONTAINER:
                return utils.ElementType.CONTAINER;
            case CHECKBOX:
                return utils.ElementType.CHECKBOX;
            case TOGGLE:
                return utils.ElementType.TOGGLE;
            case SLIDER:
                return utils.ElementType.SLIDER;
            case LIST:
                return utils.ElementType.LIST;
            case GRID:
                return utils.ElementType.GRID;
            case CARD:
                return utils.ElementType.CARD;
            case DIALOG:
                return utils.ElementType.DIALOG;
            case MENU:
                return utils.ElementType.MENU;
            case TAB:
                return utils.ElementType.TAB;
            case ICON:
                return utils.ElementType.ICON;
            case NAVIGATION:
                return utils.ElementType.NAVIGATION;
            default:
                try {
                    return utils.ElementType.valueOf(this.name());
                } catch (IllegalArgumentException e) {
                    return utils.ElementType.UNKNOWN;
                }
        }
    }
}