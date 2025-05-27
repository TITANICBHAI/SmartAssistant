package com.aiassistant.models;

import androidx.annotation.NonNull;
import utils.UIElementConverter;

/**
 * Enumeration of UI element types for the models system
 */
public enum UIElementType {
    BUTTON,
    TEXT,
    IMAGE,
    CHECKBOX,
    TOGGLE,
    RADIO_BUTTON,
    SWITCH,
    SLIDER,
    PROGRESS_BAR,
    SPINNER,
    DROPDOWN,
    EDIT_TEXT,
    SEEK_BAR,
    SCROLL_VIEW,
    LIST_ITEM,
    MENU_ITEM,
    NAVIGATION_BUTTON,
    TAB,
    ICON,
    DIALOG,
    CONTAINER,
    UNKNOWN;

    /**
     * Convert to StandardizedUIElementType
     * 
     * @return StandardizedUIElementType equivalent
     */
    @NonNull
    public StandardizedUIElementType toStandardized() {
        try {
            return StandardizedUIElementType.valueOf(this.name());
        } catch (IllegalArgumentException e) {
            return StandardizedUIElementType.UNKNOWN;
        }
    }
    
    /**
     * Create UIElementType from StandardizedUIElementType
     * 
     * @param standardized StandardizedUIElementType to convert
     * @return UIElementType equivalent
     */
    @NonNull
    public static UIElementType fromStandardized(@NonNull StandardizedUIElementType standardized) {
        try {
            return UIElementType.valueOf(standardized.name());
        } catch (IllegalArgumentException e) {
            return UIElementType.UNKNOWN;
        }
    }
    
    /**
     * Get UIElementType from string representation
     * 
     * @param typeString String representation of element type
     * @return UIElementType (defaults to UNKNOWN if no match)
     */
    @NonNull
    public static UIElementType fromString(String typeString) {
        if (typeString == null) {
            return UNKNOWN;
        }
        
        StandardizedUIElementType standardized = StandardizedUIElementType.fromString(typeString);
        return fromStandardized(standardized);
    }
}