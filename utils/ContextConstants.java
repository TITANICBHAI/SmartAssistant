package utils;

/**
 * Constants for Context interface similar to those in Android Context
 */
public class ContextConstants {
    // Common service names
    public static final String CLIPBOARD_SERVICE = "clipboard";
    public static final String ACCESSIBILITY_SERVICE = "accessibility";
    public static final String NOTIFICATION_SERVICE = "notification";
    public static final String ACTIVITY_SERVICE = "activity";
    public static final String WINDOW_SERVICE = "window";
    public static final String LAYOUT_INFLATER_SERVICE = "layout_inflater";
    public static final String POWER_SERVICE = "power";
    public static final String ALARM_SERVICE = "alarm";
    public static final String CONNECTIVITY_SERVICE = "connectivity";
    
    // Feature flags
    public static final int FEATURE_WINDOW_TRANSPARENCY = 1001;
    public static final int FEATURE_NO_TITLE = 1002;
    public static final int FEATURE_INDETERMINATE_PROGRESS = 1003;
    public static final int FEATURE_PROGRESS = 1004;
    
    // Accessibility flags
    public static final int ACCESSIBILITY_FOCUS_INPUT = 2;
    public static final int ACCESSIBILITY_FOCUS_ACCESSIBILITY = 1;
    
    // Accessibility action constants
    public static final int ACTION_SET_TEXT = 2001;
    public static final int ACTION_PASTE = 2002;
    public static final int GLOBAL_ACTION_BACK = 1001;
    public static final int GLOBAL_ACTION_HOME = 1002;
    
    // Accessibility node argument keys
    public static final String ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE = "ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE";
}