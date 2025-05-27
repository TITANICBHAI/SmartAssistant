package utils;

/**
 * Mock implementation of Android's R class
 * This provides resource IDs that match those in ResourcesHelper
 */
public final class R {
    // Private constructor to prevent instantiation
    private R() {}
    
    public static final class string {
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
        
        // Private constructor to prevent instantiation
        private string() {}
    }
    
    public static final class color {
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
        
        // Private constructor to prevent instantiation
        private color() {}
    }
    
    public static final class id {
        // Layout IDs (3000-3999)
        public static final int activity_main = 3000;
        public static final int fragment_home = 3001;
        public static final int fragment_tasks = 3002;
        public static final int fragment_settings = 3003;
        
        // View IDs (4000-4999)
        public static final int recycler_view = 4000;
        public static final int text_view_title = 4001;
        public static final int text_view_description = 4002;
        public static final int button_add = 4003;
        public static final int button_cancel = 4004;
        
        // Private constructor to prevent instantiation
        private id() {}
    }
    
    public static final class layout {
        public static final int activity_main = 5000;
        public static final int fragment_home = 5001;
        public static final int fragment_tasks = 5002;
        public static final int fragment_settings = 5003;
        public static final int item_task = 5004;
        public static final int dialog_add_task = 5005;
        
        // Private constructor to prevent instantiation
        private layout() {}
    }
    
    public static final class drawable {
        public static final int ic_launcher = 6000;
        public static final int ic_add = 6001;
        public static final int ic_settings = 6002;
        public static final int ic_home = 6003;
        public static final int ic_tasks = 6004;
        
        // Private constructor to prevent instantiation
        private drawable() {}
    }
    
    public static final class menu {
        public static final int menu_main = 7000;
        public static final int menu_tasks = 7001;
        
        // Private constructor to prevent instantiation
        private menu() {}
    }
}