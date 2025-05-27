package com.aiassistant.models;

import androidx.annotation.NonNull;

/**
 * Standardized enumeration of UI element types
 */
public enum StandardizedUIElementType {
    // Basic UI elements
    BUTTON("button"),
    TEXT("text"),
    IMAGE("image"),
    ICON("icon"),
    EDIT_TEXT("edit_text"),
    TEXT_FIELD("text_field"),
    CHECKBOX("checkbox"),
    RADIO_BUTTON("radio_button"),
    TOGGLE("toggle"),
    SWITCH("switch"),
    
    // Layout elements
    CONTAINER("container"),
    CARD("card"),
    LIST_ITEM("list_item"),
    GRID_ITEM("grid_item"),
    DIVIDER("divider"),
    
    // Navigation elements
    NAVIGATION_BAR("navigation_bar"),
    NAVIGATION_DRAWER("navigation_drawer"),
    TAB("tab"),
    TOOLBAR("toolbar"),
    MENU("menu"),
    MENU_ITEM("menu_item"),
    
    // Input controls
    SLIDER("slider"),
    PROGRESS_BAR("progress_bar"),
    SPINNER("spinner"),
    DROPDOWN("dropdown"),
    PICKER("picker"),
    DATE_PICKER("date_picker"),
    TIME_PICKER("time_picker"),
    
    // Advanced elements
    RECYCLER_VIEW("recycler_view"),
    WEB_VIEW("web_view"),
    MAP_VIEW("map_view"),
    VIDEO_VIEW("video_view"),
    BOTTOM_SHEET("bottom_sheet"),
    FLOATING_ACTION_BUTTON("floating_action_button"),
    
    // Other elements
    DIALOG("dialog"),
    TOAST("toast"),
    SNACKBAR("snackbar"),
    NOTIFICATION("notification"),
    BADGE("badge"),
    CHIP("chip"),
    
    // Unknown or custom elements
    UNKNOWN("unknown"),
    CUSTOM("custom");
    
    private final String value;
    
    StandardizedUIElementType(String value) {
        this.value = value;
    }
    
    @NonNull
    public String getValue() {
        return value;
    }
    
    /**
     * Convert a string value to a UI element type
     * 
     * @param value String value
     * @return UI element type or UNKNOWN if not found
     */
    @NonNull
    public static StandardizedUIElementType fromString(String value) {
        if (value == null) {
            return UNKNOWN;
        }
        
        String lowerValue = value.toLowerCase();
        
        for (StandardizedUIElementType type : values()) {
            if (type.value.equals(lowerValue)) {
                return type;
            }
        }
        
        // Try to match by name if not found by value
        try {
            return valueOf(value.toUpperCase());
        } catch (IllegalArgumentException e) {
            return UNKNOWN;
        }
    }
    
    /**
     * Get the display name of the UI element type
     * 
     * @return Display name
     */
    @NonNull
    public String getDisplayName() {
        String name = name().toLowerCase();
        StringBuilder result = new StringBuilder();
        
        boolean capitalizeNext = true;
        for (int i = 0; i < name.length(); i++) {
            char c = name.charAt(i);
            
            if (c == '_') {
                result.append(' ');
                capitalizeNext = true;
            } else {
                result.append(capitalizeNext ? Character.toUpperCase(c) : c);
                capitalizeNext = false;
            }
        }
        
        return result.toString();
    }
    
    @Override
    public String toString() {
        return value;
    }
}