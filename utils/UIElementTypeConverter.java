package utils;

import models.ElementType;

/**
 * Utility class to convert between different element type representations
 */
public class UIElementTypeConverter {

    // Define constants for compatibility with the rest of the codebase
    public static final String PROGRESS = "PROGRESS";
    public static final String TOOLBAR = "TOOLBAR";
    public static final String SCROLLVIEW = "SCROLLVIEW";
    public static final String HEADER = "HEADER";
    public static final String FOOTER = "FOOTER";
    public static final String DIVIDER = "DIVIDER";
    public static final String SWITCH = "SWITCH";
    
    /**
     * Convert a utils ElementType to a models ElementType
     * 
     * @param type The type to convert
     * @return The converted type
     */
    public static ElementType toModelElementType(String typeStr) {
        if (typeStr == null || typeStr.isEmpty()) {
            return ElementType.UNKNOWN;
        }
        
        try {
            // Try to map based on the string value
            String upperType = typeStr.toUpperCase();
            
            if (upperType.contains("BUTTON")) {
                return ElementType.BUTTON;
            } else if (upperType.contains("TEXT")) {
                return ElementType.TEXT;
            } else if (upperType.contains("IMAGE")) {
                return ElementType.IMAGE;
            } else if (upperType.contains("CONTAINER") || upperType.contains("LAYOUT")) {
                return ElementType.CONTAINER;
            } else if (upperType.contains("INPUT")) {
                return ElementType.INPUT;
            } else if (upperType.contains("CHECKBOX")) {
                return ElementType.CHECKBOX;
            } else if (upperType.contains("RADIO")) {
                return ElementType.RADIO_BUTTON;
            } else if (upperType.contains("SWITCH")) {
                return ElementType.TOGGLE;
            } else if (upperType.contains("SLIDER")) {
                return ElementType.SLIDER;
            } else if (upperType.contains("SCROLL")) {
                return ElementType.SCROLL_VIEW;
            } else if (upperType.contains("LIST")) {
                return ElementType.LIST;
            } else if (upperType.contains("GRID")) {
                return ElementType.GRID;
            } else if (upperType.contains("CARD")) {
                return ElementType.CARD;
            } else if (upperType.contains("DIALOG")) {
                return ElementType.DIALOG;
            } else if (upperType.contains("MENU")) {
                return ElementType.MENU;
            } else if (upperType.contains("TAB")) {
                return ElementType.TAB;
            } else if (upperType.contains("ICON")) {
                return ElementType.ICON;
            } else if (upperType.contains("PROGRESS")) {
                return ElementType.PROGRESS_BAR;
            } else if (upperType.contains("TOOLBAR")) {
                return ElementType.NAVIGATION;
            } else if (upperType.contains("NAV")) {
                return ElementType.NAVIGATION;
            } else if (upperType.contains("HEADER")) {
                // Handle as a generic container if not available in models.ElementType
                return ElementType.CONTAINER;
            } else if (upperType.contains("FOOTER")) {
                // Handle as a generic container if not available in models.ElementType
                return ElementType.CONTAINER;
            } else if (upperType.contains("DIVIDER")) {
                // Handle as a generic UI element if not available in models.ElementType
                return ElementType.UNKNOWN;
            }
            
            return ElementType.UNKNOWN;
            
        } catch (Exception e) {
            return ElementType.UNKNOWN;
        }
    }
    
    /**
     * Convert any object to a models ElementType
     * 
     * @param type The type object to convert
     * @return The converted type
     */
    public static ElementType toModelElementType(Object type) {
        if (type == null) {
            return ElementType.UNKNOWN;
        }
        
        if (type instanceof ElementType) {
            return (ElementType) type;
        } else if (type instanceof String) {
            return toModelElementType((String) type);
        } else if (type instanceof utils.UIElement.ElementType) {
            return toModelElementType(type.toString());
        } else if (type instanceof models.StandardizedUIElementType) {
            return ((models.StandardizedUIElementType) type).toElementType();
        } else {
            return toModelElementType(type.toString());
        }
    }
    
    /**
     * Convert a models ElementType to a utils ElementType
     * 
     * @param type The type to convert
     * @return The converted type
     */
    public static utils.ElementType toUtilsElementType(ElementType type) {
        if (type == null) {
            return utils.ElementType.UNKNOWN;
        }
        
        switch (type) {
            case BUTTON:
                return utils.ElementType.BUTTON;
            case TEXT:
                return utils.ElementType.TEXT;
            case IMAGE:
                return utils.ElementType.IMAGE;
            case CONTAINER:
                return utils.ElementType.CONTAINER;
            case INPUT:
            case INPUT_FIELD:
                return utils.ElementType.INPUT;
            case CHECKBOX:
                return utils.ElementType.CHECKBOX;
            case RADIO_BUTTON:
                return utils.ElementType.RADIO;
            case TOGGLE:
                return utils.ElementType.TOGGLE;
            case SLIDER:
                return utils.ElementType.SLIDER;
            case SCROLL_VIEW:
                return utils.ElementType.SCROLL_VIEW;
            case LIST:
            case LIST_ITEM:
                return utils.ElementType.LIST;
            case GRID:
                return utils.ElementType.GRID;
            case CARD:
                return utils.ElementType.CARD;
            case DIALOG:
                return utils.ElementType.DIALOG;
            case MENU:
            case MENU_ITEM:
                return utils.ElementType.MENU;
            case TAB:
                return utils.ElementType.TAB;
            case ICON:
                return utils.ElementType.ICON;
            case PROGRESS_BAR:
                return utils.ElementType.PROGRESS;
            case NAVIGATION:
                return utils.ElementType.NAVIGATION;
            case GAME_OBJECT:
                // Game-specific UI elements don't have direct equivalents in utils
                return utils.ElementType.GAME_OBJECT;
            case PLAYER:
                // For player elements, use a specific type
                return utils.ElementType.PLAYER;
            case ENEMY:
                // Map game enemy to an appropriate type
                return utils.ElementType.ENEMY;
            case COLLECTIBLE:
                // Map collectible game items
                return utils.ElementType.COLLECTIBLE;
            case POWERUP:
                // Map power-up items
                return utils.ElementType.POWERUP;
            case OBSTACLE:
                // Map obstacles
                return utils.ElementType.OBSTACLE;
            case PLATFORM:
                // Map platforms
                return utils.ElementType.PLATFORM;
            case BACKGROUND:
                // Map background elements
                return utils.ElementType.BACKGROUND;
            case FOREGROUND:
                // Map foreground elements
                return utils.ElementType.FOREGROUND;
            case LABEL:
                return utils.ElementType.LABEL;
            case LINK:
                return utils.ElementType.LINK;
            case OPTION:
                return utils.ElementType.OPTION;
            case SELECT:
                return utils.ElementType.SELECT;
            case TEXTAREA:
                return utils.ElementType.TEXTAREA;
            case DROPDOWN:
                return utils.ElementType.DROPDOWN;
            case VIDEO:
                // Map video
                return utils.ElementType.VIDEO;
            case AUDIO:
                // Map audio
                return utils.ElementType.AUDIO;
            case SECTION:
                // Map section
                return utils.ElementType.SECTION;
            case PANEL:
                // Map panel
                return utils.ElementType.PANEL;
            case TOAST:
                // Map toast notifications
                return utils.ElementType.NOTIFICATION;
            case SPINNER:
                // Map spinner to progress
                return utils.ElementType.PROGRESSINDICATOR;
            default:
                // Handle special cases from string constants
                String typeName = type.name();
                if (typeName.equals("TOOLBAR")) {
                    return utils.ElementType.TOOLBAR;
                } else if (typeName.equals("HEADER")) {
                    return utils.ElementType.HEADER;
                } else if (typeName.equals("FOOTER")) {
                    return utils.ElementType.FOOTER;
                } else if (typeName.equals("DIVIDER")) {
                    return utils.ElementType.DIVIDER;
                }
                return utils.ElementType.UNKNOWN;
        }
    }
    
    /**
     * Convert any object to a utils ElementType
     * 
     * @param type The type object to convert
     * @return The converted type
     */
    public static utils.ElementType toUtilsElementType(Object type) {
        if (type == null) {
            return utils.ElementType.UNKNOWN;
        }
        
        if (type instanceof utils.ElementType) {
            return (utils.ElementType) type;
        } else if (type instanceof ElementType) {
            return toUtilsElementType((ElementType) type);
        } else if (type instanceof String) {
            try {
                return utils.ElementType.valueOf((String) type);
            } catch (IllegalArgumentException e) {
                return utils.ElementType.UNKNOWN;
            }
        } else if (type instanceof models.StandardizedUIElementType) {
            return ((models.StandardizedUIElementType) type).toUtilsElementType();
        } else {
            try {
                return utils.ElementType.valueOf(type.toString());
            } catch (Exception e) {
                return utils.ElementType.UNKNOWN;
            }
        }
    }
}