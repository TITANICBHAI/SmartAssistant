package models;

/**
 * Standardized UI element types for cross-platform compatibility.
 */
public enum StandardizedUIElementType {
    BUTTON,
    TEXT,
    IMAGE,
    INPUT,
    CHECKBOX,
    CONTAINER,
    LIST,
    CARD,
    ICON,
    SLIDER,
    SCROLLBAR,
    PROGRESS_BAR,
    INPUT_FIELD,
    UNKNOWN;
    
    /**
     * Convert a string to StandardizedUIElementType.
     * 
     * @param typeStr Type string
     * @return StandardizedUIElementType
     */
    public static StandardizedUIElementType fromString(String typeStr) {
        if (typeStr == null || typeStr.isEmpty()) {
            return UNKNOWN;
        }
        
        switch (typeStr.toUpperCase()) {
            case "BUTTON":
                return BUTTON;
            case "TEXT":
                return TEXT;
            case "IMAGE":
                return IMAGE;
            case "INPUT":
                return INPUT;
            case "CHECKBOX":
                return CHECKBOX;
            case "CONTAINER":
                return CONTAINER;
            case "LIST":
                return LIST;
            case "CARD":
                return CARD;
            case "ICON":
                return ICON;
            case "SLIDER":
                return SLIDER;
            case "SCROLLBAR":
                return SCROLLBAR;
            case "PROGRESS_BAR":
                return PROGRESS_BAR;
            case "INPUT_FIELD":
                return INPUT_FIELD;
            default:
                return UNKNOWN;
        }
    }
    
    /**
     * Convert StandardizedUIElementType to a platform-specific type.
     * 
     * @param platformName Platform name (e.g., "android", "web")
     * @return Platform-specific type string
     */
    public String toPlatformType(String platformName) {
        if (platformName == null || platformName.isEmpty()) {
            return toString();
        }
        
        switch (platformName.toLowerCase()) {
            case "android":
                return toAndroidType();
            case "web":
                return toWebType();
            default:
                return toString();
        }
    }
    
    /**
     * Convert to Android-specific type.
     * 
     * @return Android type string
     */
    private String toAndroidType() {
        switch (this) {
            case BUTTON:
                return "android.widget.Button";
            case TEXT:
                return "android.widget.TextView";
            case IMAGE:
                return "android.widget.ImageView";
            case INPUT:
                return "android.widget.EditText";
            case CHECKBOX:
                return "android.widget.CheckBox";
            case CONTAINER:
                return "android.view.ViewGroup";
            case LIST:
                return "android.widget.ListView";
            case CARD:
                return "androidx.cardview.widget.CardView";
            case ICON:
                return "android.widget.ImageView";
            case SLIDER:
                return "android.widget.SeekBar";
            case SCROLLBAR:
                return "android.widget.ScrollView";
            case PROGRESS_BAR:
                return "android.widget.ProgressBar";
            case INPUT_FIELD:
                return "android.widget.EditText";
            case UNKNOWN:
            default:
                return "android.view.View";
        }
    }
    
    /**
     * Convert to web-specific type.
     * 
     * @return Web type string
     */
    private String toWebType() {
        switch (this) {
            case BUTTON:
                return "button";
            case TEXT:
                return "p";
            case IMAGE:
                return "img";
            case INPUT:
                return "input";
            case CHECKBOX:
                return "input[type=checkbox]";
            case CONTAINER:
                return "div";
            case LIST:
                return "ul";
            case CARD:
                return "div.card";
            case ICON:
                return "i";
            case SLIDER:
                return "input[type=range]";
            case SCROLLBAR:
                return "div.scrollbar";
            case PROGRESS_BAR:
                return "progress";
            case INPUT_FIELD:
                return "input[type=text]";
            case UNKNOWN:
            default:
                return "div";
        }
    }
    
    /**
     * Convert this StandardizedUIElementType to a models.ElementType.
     * 
     * @return The corresponding ElementType
     */
    public ElementType toElementType() {
        switch (this) {
            case BUTTON:
                return ElementType.BUTTON;
            case TEXT:
                return ElementType.TEXT;
            case IMAGE:
                return ElementType.IMAGE;
            case INPUT:
            case INPUT_FIELD:
                return ElementType.INPUT_FIELD;
            case CHECKBOX:
                return ElementType.CHECKBOX;
            case CONTAINER:
                return ElementType.CONTAINER;
            case LIST:
                return ElementType.LIST_ITEM;
            case CARD:
                return ElementType.CARD;
            case ICON:
                return ElementType.ICON;
            case SLIDER:
                return ElementType.SLIDER;
            case SCROLLBAR:
                return ElementType.SCROLL_VIEW;
            case PROGRESS_BAR:
                return ElementType.PROGRESS_BAR;
            case UNKNOWN:
            default:
                return ElementType.UNKNOWN;
        }
    }
    
    /**
     * Convert this StandardizedUIElementType directly to a utils.ElementType.
     * This is a convenience method that avoids going through models.ElementType first.
     * 
     * @return The corresponding utils.ElementType
     */
    public utils.ElementType toUtilsElementType() {
        switch (this) {
            case BUTTON:
                return utils.ElementType.BUTTON;
            case TEXT:
                return utils.ElementType.TEXT;
            case IMAGE:
                return utils.ElementType.IMAGE;
            case INPUT:
            case INPUT_FIELD:
                return utils.ElementType.INPUT;
            case CHECKBOX:
                return utils.ElementType.CHECKBOX;
            case CONTAINER:
                return utils.ElementType.CONTAINER;
            case LIST:
                return utils.ElementType.LIST;
            case CARD:
                return utils.ElementType.CARD;
            case ICON:
                return utils.ElementType.ICON;
            case SLIDER:
                return utils.ElementType.SLIDER;
            case SCROLLBAR:
                return utils.ElementType.SCROLL_VIEW;
            case PROGRESS_BAR:
                return utils.ElementType.PROGRESS;
            case UNKNOWN:
            default:
                return utils.ElementType.UNKNOWN;
        }
    }
    
    /**
     * Convert from utils.ElementType to StandardizedUIElementType.
     * 
     * @param type The utils.ElementType to convert
     * @return The corresponding StandardizedUIElementType
     */
    public static StandardizedUIElementType fromUtilsElementType(utils.ElementType type) {
        if (type == null) {
            return UNKNOWN;
        }
        
        switch (type) {
            case BUTTON:
                return BUTTON;
            case TEXT:
                return TEXT;
            case IMAGE:
                return IMAGE;
            case INPUT:
            case INPUT_FIELD:
                return INPUT;
            case CHECKBOX:
                return CHECKBOX;
            case CONTAINER:
                return CONTAINER;
            case LIST:
                return LIST;
            case CARD:
                return CARD;
            case ICON:
                return ICON;
            case SLIDER:
                return SLIDER;
            case SCROLL_VIEW:
                return SCROLLBAR;
            case PROGRESS:
            case PROGRESS_BAR:
                return PROGRESS_BAR;
            case TOOLBAR:
                return CONTAINER; // Best match
            case HEADER:
                return CONTAINER; // Best match
            case FOOTER:
                return CONTAINER; // Best match
            case TOGGLE:
                return BUTTON; // Best match
            case MENU:
                return CONTAINER; // Best match
            case TAB:
                return BUTTON; // Best match
            case NAVIGATION:
                return CONTAINER; // Best match
            case UNKNOWN:
            default:
                return UNKNOWN;
        }
    }
}