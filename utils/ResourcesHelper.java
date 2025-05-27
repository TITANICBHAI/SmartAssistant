package utils;

import java.util.HashMap;
import java.util.Map;

/**
 * Helper class for handling Android resources across different package contexts.
 * This provides a way to access string and color resources when not running
 * in an actual Android environment.
 */
public class ResourcesHelper {
    // Simulate Android R.string resources
    public static class string {
        public static final int scheduled_for = 1000;
        public static final int at_time = 1001;
        public static final int game_completed = 1002;
        public static final int game_started = 1003;
        public static final int game_paused = 1004;
        public static final int game_resumed = 1005;
        public static final int game_failed = 1006;
        public static final int task_created = 1007;
        public static final int task_updated = 1008;
        public static final int task_deleted = 1009;
        public static final int task_completed = 1010;
        public static final int task_failed = 1011;
        public static final int every_day = 1012;
        public static final int every_weekday = 1013;
        public static final int every_weekend = 1014;
        public static final int on_days = 1015;
        public static final int notify_minutes_before = 1016;
        public static final int error = 1017;
        public static final int success = 1018;
        public static final int warning = 1019;
        public static final int info = 1020;
        public static final int app_name = 1021;
        public static final int action_settings = 1022;
    }
    
    // Simulate Android R.color resources
    public static class color {
        public static final int colorPrimary = 2000;
        public static final int colorPrimaryDark = 2001;
        public static final int colorAccent = 2002;
        public static final int textColorPrimary = 2003;
        public static final int textColorSecondary = 2004;
        public static final int colorRed = 2005;
        public static final int colorGreen = 2006;
        public static final int colorBlue = 2007;
        public static final int colorYellow = 2008;
        public static final int colorOrange = 2009;
        public static final int colorPurple = 2010;
        public static final int colorGray = 2011;
        public static final int colorLightGray = 2012;
        
        // Status colors
        public static final int status_active = 2100;
        public static final int status_scheduled = 2101;
        public static final int status_completed = 2102;
        public static final int status_cancelled = 2103;
        public static final int status_pending = 2104;
        public static final int status_running = 2105;
        public static final int status_failed = 2106;
        public static final int status_paused = 2107;
        public static final int status_canceled = 2108;
        public static final int default_status_color = 2109;
        
        // Priority colors
        public static final int priority_high = 2200;
        public static final int priority_medium = 2201;
        public static final int priority_low = 2202;
        public static final int priority_critical = 2203;
        public static final int default_priority_color = 2204;
        
        // Trigger type colors
        public static final int trigger_manual = 2300;
        public static final int trigger_scheduled = 2301;
        public static final int trigger_recurring = 2302;
        public static final int trigger_event = 2303;
        public static final int trigger_condition = 2304;
        public static final int default_trigger_color = 2305;
    }
    
    // String resource map
    private static final Map<Integer, String> stringResourceMap = new HashMap<>();
    
    // Color resource map
    private static final Map<Integer, Integer> colorResourceMap = new HashMap<>();
    
    static {
        // Initialize string resources
        stringResourceMap.put(string.scheduled_for, "Scheduled for");
        stringResourceMap.put(string.at_time, "at");
        stringResourceMap.put(string.game_completed, "Game completed");
        stringResourceMap.put(string.game_started, "Game started");
        stringResourceMap.put(string.game_paused, "Game paused");
        stringResourceMap.put(string.game_resumed, "Game resumed");
        stringResourceMap.put(string.game_failed, "Game failed");
        stringResourceMap.put(string.task_created, "Task created");
        stringResourceMap.put(string.task_updated, "Task updated");
        stringResourceMap.put(string.task_deleted, "Task deleted");
        stringResourceMap.put(string.task_completed, "Task completed");
        stringResourceMap.put(string.task_failed, "Task failed");
        stringResourceMap.put(string.every_day, "Every day");
        stringResourceMap.put(string.every_weekday, "Every weekday");
        stringResourceMap.put(string.every_weekend, "Every weekend");
        stringResourceMap.put(string.on_days, "On days");
        stringResourceMap.put(string.notify_minutes_before, "Notify %d minutes before");
        stringResourceMap.put(string.error, "Error");
        stringResourceMap.put(string.success, "Success");
        stringResourceMap.put(string.warning, "Warning");
        stringResourceMap.put(string.info, "Information");
        stringResourceMap.put(string.app_name, "AI Assistant");
        stringResourceMap.put(string.action_settings, "Settings");
        
        // Initialize color resources
        colorResourceMap.put(color.colorPrimary, 0xFF3F51B5);        // Indigo
        colorResourceMap.put(color.colorPrimaryDark, 0xFF303F9F);    // Dark Indigo
        colorResourceMap.put(color.colorAccent, 0xFFFF4081);         // Pink
        colorResourceMap.put(color.textColorPrimary, 0xDE000000);    // 87% Black
        colorResourceMap.put(color.textColorSecondary, 0x99000000);  // 60% Black
        colorResourceMap.put(color.colorRed, 0xFFF44336);            // Red
        colorResourceMap.put(color.colorGreen, 0xFF4CAF50);          // Green
        colorResourceMap.put(color.colorBlue, 0xFF2196F3);           // Blue
        colorResourceMap.put(color.colorYellow, 0xFFFFEB3B);         // Yellow
        colorResourceMap.put(color.colorOrange, 0xFFFF9800);         // Orange
        colorResourceMap.put(color.colorPurple, 0xFF9C27B0);         // Purple
        colorResourceMap.put(color.colorGray, 0xFF9E9E9E);           // Gray
        colorResourceMap.put(color.colorLightGray, 0xFFE0E0E0);      // Light Gray
        
        // Status colors
        colorResourceMap.put(color.status_active, 0xFF4CAF50);       // Green
        colorResourceMap.put(color.status_scheduled, 0xFF2196F3);    // Blue
        colorResourceMap.put(color.status_completed, 0xFF9E9E9E);    // Gray
        colorResourceMap.put(color.status_cancelled, 0xFFF44336);    // Red
        colorResourceMap.put(color.status_pending, 0xFFFF9800);      // Orange
        colorResourceMap.put(color.status_running, 0xFF4CAF50);      // Green
        colorResourceMap.put(color.status_failed, 0xFFF44336);       // Red
        colorResourceMap.put(color.status_paused, 0xFF9C27B0);       // Purple
        colorResourceMap.put(color.status_canceled, 0xFFF44336);     // Red
        colorResourceMap.put(color.default_status_color, 0xFF9E9E9E); // Gray
        
        // Priority colors
        colorResourceMap.put(color.priority_high, 0xFFF44336);       // Red
        colorResourceMap.put(color.priority_medium, 0xFFFF9800);     // Orange
        colorResourceMap.put(color.priority_low, 0xFF4CAF50);        // Green
        colorResourceMap.put(color.priority_critical, 0xFF8E24AA);   // Dark Purple
        colorResourceMap.put(color.default_priority_color, 0xFFFF9800); // Orange
        
        // Trigger type colors
        colorResourceMap.put(color.trigger_manual, 0xFF9E9E9E);      // Gray
        colorResourceMap.put(color.trigger_scheduled, 0xFF2196F3);   // Blue
        colorResourceMap.put(color.trigger_recurring, 0xFF4CAF50);   // Green
        colorResourceMap.put(color.trigger_event, 0xFFFF9800);       // Orange
        colorResourceMap.put(color.trigger_condition, 0xFF9C27B0);   // Purple
        colorResourceMap.put(color.default_trigger_color, 0xFF9E9E9E); // Gray
    }
    
    /**
     * Get a string resource by ID
     * 
     * @param resId Resource ID
     * @return String value
     */
    public static String getString(int resId) {
        return stringResourceMap.getOrDefault(resId, "");
    }
    
    /**
     * Get a formatted string resource by ID
     * 
     * @param resId Resource ID
     * @param args Format arguments
     * @return Formatted string value
     */
    public static String getString(int resId, Object... args) {
        String template = getString(resId);
        try {
            return String.format(template, args);
        } catch (Exception e) {
            return template;
        }
    }
    
    /**
     * Get a color resource by ID
     * 
     * @param resId Resource ID
     * @return Color value (ARGB integer)
     */
    public static int getColor(int resId) {
        return colorResourceMap.getOrDefault(resId, 0xFF000000);  // Default to black
    }
    
    /**
     * Set a string resource value
     * 
     * @param resId Resource ID
     * @param value String value
     */
    public static void setString(int resId, String value) {
        stringResourceMap.put(resId, value);
    }
    
    /**
     * Set a color resource value
     * 
     * @param resId Resource ID
     * @param value Color value (ARGB integer)
     */
    public static void setColor(int resId, int value) {
        colorResourceMap.put(resId, value);
    }
}