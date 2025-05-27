package utils;

import models.ElementType;

/**
 * UIElementTypeConstants
 * This class provides constants for UI element types that can be used across different type systems
 */
public final class UIElementTypeConstants {
    private UIElementTypeConstants() {
        // Private constructor to prevent instantiation
    }
    
    // Common type names as strings
    public static final String BUTTON = "button";
    public static final String TEXT = "text";
    public static final String IMAGE = "image";
    public static final String INPUT = "input";
    public static final String INPUT_FIELD = "input_field";
    public static final String CONTAINER = "container";
    public static final String SCROLL_VIEW = "scroll_view";
    public static final String LIST_ITEM = "list_item";
    public static final String CHECKBOX = "checkbox";
    public static final String RADIO_BUTTON = "radio_button";
    public static final String TOGGLE = "toggle";
    public static final String SLIDER = "slider";
    public static final String PROGRESS_BAR = "progress_bar";
    public static final String ICON = "icon";
    public static final String UNKNOWN = "unknown";
    public static final String DROPDOWN = "dropdown";
    public static final String LABEL = "label";
    public static final String LINK = "link";
    public static final String OPTION = "option";
    public static final String RADIO = "radio";
    public static final String SWITCH = "switch";
    public static final String SELECT = "select";
    public static final String TEXTAREA = "textarea";
    public static final String VIDEO = "video";
    public static final String AUDIO = "audio";
    public static final String SECTION = "section";
    public static final String PANEL = "panel";
    public static final String DIALOG = "dialog";
    public static final String TOAST = "toast";
    public static final String MENU = "menu";
    public static final String MENU_ITEM = "menu_item";
    public static final String TAB = "tab";
    public static final String HEADER = "header";
    public static final String FOOTER = "footer";
    public static final String DIVIDER = "divider";
    
    /**
     * Convert a string type to a models.ElementType
     * @param typeStr The type string
     * @return The models.ElementType
     */
    public static ElementType toModelElementType(String typeStr) {
        if (typeStr == null || typeStr.isEmpty()) {
            return ElementType.UNKNOWN;
        }
        
        String normalizedType = typeStr.trim().toLowerCase();
        
        switch (normalizedType) {
            case BUTTON: return ElementType.BUTTON;
            case TEXT: return ElementType.TEXT;
            case IMAGE: return ElementType.IMAGE;
            case INPUT:
            case INPUT_FIELD: return ElementType.INPUT_FIELD;
            case CONTAINER: return ElementType.CONTAINER;
            case SCROLL_VIEW: return ElementType.SCROLL_VIEW;
            case LIST_ITEM: return ElementType.LIST_ITEM;
            case CHECKBOX: return ElementType.CHECKBOX;
            case RADIO_BUTTON:
            case RADIO: return ElementType.RADIO_BUTTON;
            case TOGGLE: return ElementType.TOGGLE;
            case SLIDER: return ElementType.SLIDER;
            case PROGRESS_BAR: return ElementType.PROGRESS_BAR;
            case ICON: return ElementType.ICON;
            case DROPDOWN: return ElementType.DROPDOWN;
            case LABEL: return ElementType.LABEL;
            case LINK: return ElementType.LINK;
            case OPTION: return ElementType.OPTION;
            case SELECT: return ElementType.SELECT;
            case TEXTAREA: return ElementType.TEXTAREA;
            case VIDEO: return ElementType.VIDEO;
            case AUDIO: return ElementType.AUDIO;
            case SECTION: return ElementType.SECTION;
            case PANEL: return ElementType.PANEL;
            case DIALOG: return ElementType.DIALOG;
            case TOAST: return ElementType.TOAST;
            case MENU: return ElementType.MENU;
            case MENU_ITEM: return ElementType.MENU_ITEM;
            default: return ElementType.UNKNOWN;
        }
    }
    
    /**
     * Convert a string type to a utils.UIElement.ElementType
     * @param typeStr The type string
     * @return The utils.UIElement.ElementType
     */
    public static UIElement.ElementType toUtilsElementType(String typeStr) {
        if (typeStr == null || typeStr.isEmpty()) {
            return UIElement.ElementType.UNKNOWN;
        }
        
        String normalizedType = typeStr.trim().toLowerCase();
        
        switch (normalizedType) {
            case BUTTON: return UIElement.ElementType.BUTTON;
            case TEXT: return UIElement.ElementType.TEXT;
            case IMAGE: return UIElement.ElementType.IMAGE;
            case INPUT:
            case INPUT_FIELD: return UIElement.ElementType.INPUT;
            case CONTAINER: return UIElement.ElementType.CONTAINER;
            case SCROLL_VIEW: return UIElement.ElementType.SCROLLVIEW;
            case LIST_ITEM: return UIElement.ElementType.LIST;
            case CHECKBOX: return UIElement.ElementType.CHECKBOX;
            case RADIO_BUTTON:
            case RADIO: return UIElement.ElementType.RADIO_BUTTON;
            case TOGGLE: return UIElement.ElementType.TOGGLE;
            case SLIDER: return UIElement.ElementType.SLIDER;
            case PROGRESS_BAR: return UIElement.ElementType.PROGRESS;
            case ICON: return UIElement.ElementType.ICON;
            default: return UIElement.ElementType.UNKNOWN;
        }
    }
    
    /**
     * Convert a models.ElementType to a string
     * @param type The models.ElementType
     * @return The type string
     */
    public static String toString(ElementType type) {
        if (type == null) {
            return UNKNOWN;
        }
        
        switch (type) {
            case BUTTON: return BUTTON;
            case TEXT: return TEXT;
            case IMAGE: return IMAGE;
            case INPUT: return INPUT_FIELD;
            case CONTAINER: return CONTAINER;
            case SCROLL_VIEW: return SCROLL_VIEW;
            case LIST_ITEM: return LIST_ITEM;
            case CHECKBOX: return CHECKBOX;
            case RADIO_BUTTON: return RADIO_BUTTON;
            case TOGGLE: return TOGGLE;
            case SLIDER: return SLIDER;
            case PROGRESS_BAR: return PROGRESS_BAR;
            case ICON: return ICON;
            case DROPDOWN: return DROPDOWN;
            case LABEL: return LABEL;
            case LINK: return LINK;
            case OPTION: return OPTION;
            case SELECT: return SELECT;
            case TEXTAREA: return TEXTAREA;
            case VIDEO: return VIDEO;
            case AUDIO: return AUDIO;
            case SECTION: return SECTION;
            case PANEL: return PANEL;
            case DIALOG: return DIALOG;
            case TOAST: return TOAST;
            case MENU: return MENU;
            case MENU_ITEM: return MENU_ITEM;
            default: return UNKNOWN;
        }
    }
    
    /**
     * Convert a utils.UIElement.ElementType to a string
     * @param type The utils.UIElement.ElementType
     * @return The type string
     */
    public static String toString(UIElement.ElementType type) {
        if (type == null) {
            return UNKNOWN;
        }
        
        switch (type) {
            case BUTTON: return BUTTON;
            case TEXT: return TEXT;
            case IMAGE: return IMAGE;
            case INPUT: return INPUT_FIELD;
            case CONTAINER: return CONTAINER;
            case SCROLLVIEW: return SCROLL_VIEW;
            case LIST: return LIST_ITEM;
            case CHECKBOX: return CHECKBOX;
            case RADIO_BUTTON: return RADIO_BUTTON;
            case TOGGLE: return TOGGLE;
            case SLIDER: return SLIDER;
            case PROGRESS: return PROGRESS_BAR;
            case ICON: return ICON;
            case DIALOG: return DIALOG;
            case MENU: return MENU;
            case TOOLBAR: return "toolbar";
            case CARD: return "card";
            case NAVIGATION: return "navigation";
            case HEADER: return HEADER;
            case FOOTER: return FOOTER;
            case GAME_CONTROL: return "game_control";
            default: return UNKNOWN;
        }
    }
}