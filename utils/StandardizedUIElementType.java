package utils;

import models.ElementType;

/**
 * Standard UIElement type definitions for compatibility between different implementations.
 */
public enum StandardizedUIElementType {
    BUTTON,
    TEXT,
    IMAGE,
    ICON,
    INPUT_FIELD,
    CONTAINER,
    LIST,
    GRID,
    CHECKBOX,
    RADIO_BUTTON,
    TOGGLE,
    DROPDOWN,
    SLIDER,
    PROGRESS_BAR,
    SPINNER,
    NAVIGATION,
    TAB,
    MENU,
    DIALOG,
    CARD,
    GAME_OBJECT,
    PLAYER,
    ENEMY,
    COLLECTIBLE,
    POWERUP,
    OBSTACLE,
    PLATFORM,
    BACKGROUND,
    FOREGROUND,
    UNKNOWN;
    
    /**
     * Convert from ElementType to StandardizedUIElementType
     * @param elementType The element type
     * @return The standardized element type
     */
    public static StandardizedUIElementType fromElementType(ElementType elementType) {
        if (elementType == null) {
            return UNKNOWN;
        }
        
        try {
            return StandardizedUIElementType.valueOf(elementType.name());
        } catch (IllegalArgumentException e) {
            return UNKNOWN;
        }
    }
    
    /**
     * Convert to ElementType
     * @return The element type
     */
    public ElementType toElementType() {
        try {
            return ElementType.valueOf(this.name());
        } catch (IllegalArgumentException e) {
            return ElementType.UNKNOWN;
        }
    }
    
    /**
     * Convert from String to StandardizedUIElementType
     * @param typeString The type string
     * @return The standardized element type
     */
    public static StandardizedUIElementType fromString(String typeString) {
        if (typeString == null || typeString.isEmpty()) {
            return UNKNOWN;
        }
        
        try {
            return StandardizedUIElementType.valueOf(typeString.toUpperCase());
        } catch (IllegalArgumentException e) {
            // Try to intelligently map common UI element types
            String upperCase = typeString.toUpperCase();
            if (upperCase.contains("BUTTON")) {
                return BUTTON;
            } else if (upperCase.contains("TEXT") || upperCase.contains("LABEL")) {
                return TEXT;
            } else if (upperCase.contains("IMAGE")) {
                return IMAGE;
            } else if (upperCase.contains("ICON")) {
                return ICON;
            } else if (upperCase.contains("INPUT") || upperCase.contains("EDIT") || upperCase.contains("FIELD")) {
                return INPUT_FIELD;
            } else if (upperCase.contains("CONTAINER") || upperCase.contains("LAYOUT") || upperCase.contains("VIEW")) {
                return CONTAINER;
            } else if (upperCase.contains("LIST")) {
                return LIST;
            } else if (upperCase.contains("CHECKBOX")) {
                return CHECKBOX;
            } else if (upperCase.contains("RADIO")) {
                return RADIO_BUTTON;
            } else if (upperCase.contains("TOGGLE") || upperCase.contains("SWITCH")) {
                return TOGGLE;
            } else if (upperCase.contains("DROPDOWN") || upperCase.contains("SELECT")) {
                return DROPDOWN;
            } else if (upperCase.contains("SLIDER") || upperCase.contains("SEEKBAR")) {
                return SLIDER;
            } else if (upperCase.contains("PROGRESS")) {
                return PROGRESS_BAR;
            } else if (upperCase.contains("MENU")) {
                return MENU;
            } else if (upperCase.contains("DIALOG")) {
                return DIALOG;
            } else if (upperCase.contains("CARD")) {
                return CARD;
            } else if (upperCase.contains("NAVIGATION") || upperCase.contains("NAV")) {
                return NAVIGATION;
            } else if (upperCase.contains("TAB")) {
                return TAB;
            } else if (upperCase.contains("PLAYER")) {
                return PLAYER;
            } else if (upperCase.contains("ENEMY")) {
                return ENEMY;
            } else if (upperCase.contains("BACKGROUND")) {
                return BACKGROUND;
            }
            
            return UNKNOWN;
        }
    }
}