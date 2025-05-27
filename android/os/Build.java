package android.os;

/**
 * Mock implementation of Android Build class for development outside of Android.
 * This class provides access to information about the build environment.
 */
public class Build {
    /**
     * The name of the underlying board, like "goldfish".
     */
    public static final String BOARD = "mock_board";
    
    /**
     * The consumer-visible brand with which the product/hardware will be associated.
     */
    public static final String BRAND = "mock";
    
    /**
     * The name of the industrial design.
     */
    public static final String DEVICE = "mock_device";
    
    /**
     * A build ID string meant for displaying to the user.
     */
    public static final String DISPLAY = "Mock-Build";
    
    /**
     * A string that uniquely identifies this build.
     */
    public static final String FINGERPRINT = "mock/mock_device/mock:11/MOCK/12345:user/release-keys";
    
    /**
     * The name of the hardware (from the kernel command line or /proc).
     */
    public static final String HARDWARE = "mock_hardware";
    
    /**
     * Either a changelist number, or a label like "M4-rc20".
     */
    public static final String ID = "MOCK";
    
    /**
     * The manufacturer of the product/hardware.
     */
    public static final String MANUFACTURER = "Mock Manufacturer";
    
    /**
     * The end-user-visible name for the end product.
     */
    public static final String MODEL = "Mock Model";
    
    /**
     * The name of the overall product.
     */
    public static final String PRODUCT = "mock_product";
    
    /**
     * The system image series number.
     */
    public static final String SERIAL = "mock12345";
    
    /**
     * A hardware serial number, if available.
     */
    public static final String HARDWARE_SERIAL = "HW-MOCK-12345";
    
    /**
     * Various version strings
     */
    public static class VERSION {
        /**
         * The current development codename, or the string "REL" if this is a release build.
         */
        public static final String CODENAME = "REL";
        
        /**
         * The internal value used by the underlying source control to represent this build.
         */
        public static final String INCREMENTAL = "12345";
        
        /**
         * The user-visible version string.
         */
        public static final String RELEASE = "11";
        
        /**
         * The user-visible SDK version of the framework.
         */
        public static final int SDK_INT = 30;
        
        /**
         * The base OS build the product is based on.
         */
        public static final String BASE_OS = "";
        
        /**
         * The current security patch level.
         */
        public static final String SECURITY_PATCH = "2023-01-01";
        
        /**
         * The user-visible security patch level.
         */
        public static final String SECURITY_LEVEL = "1";
    }
    
    /**
     * The type of build, like "user" or "eng".
     */
    public static final String TYPE = "user";
    
    /**
     * Various CPU architecture constants
     */
    public static class CPU_ABI {
        public static final String ARM = "armeabi-v7a";
        public static final String ARM64 = "arm64-v8a";
        public static final String X86 = "x86";
        public static final String X86_64 = "x86_64";
    }
    
    /**
     * The CPU architecture of the current device
     */
    public static final String CPU_ABI = "x86_64";
    
    /**
     * The secondary CPU architecture of the current device
     */
    public static final String CPU_ABI2 = "x86";
    
    /**
     * Whether this build was for an emulator
     */
    public static final boolean IS_EMULATOR = false;
    
    /**
     * Whether this build was for a virtual device
     */
    public static final boolean IS_VIRTUAL_DEVICE = true;
    
    /**
     * Information about the current Android version
     */
    public static class VERSION_CODES {
        /**
         * Android 4.1, 4.1.1
         */
        public static final int JELLY_BEAN = 16;
        
        /**
         * Android 4.2, 4.2.2
         */
        public static final int JELLY_BEAN_MR1 = 17;
        
        /**
         * Android 4.3
         */
        public static final int JELLY_BEAN_MR2 = 18;
        
        /**
         * Android 4.4
         */
        public static final int KITKAT = 19;
        
        /**
         * Android 4.4W
         */
        public static final int KITKAT_WATCH = 20;
        
        /**
         * Android 5.0
         */
        public static final int LOLLIPOP = 21;
        
        /**
         * Android 5.1
         */
        public static final int LOLLIPOP_MR1 = 22;
        
        /**
         * Android 6.0
         */
        public static final int M = 23;
        
        /**
         * Android 7.0
         */
        public static final int N = 24;
        
        /**
         * Android 7.1
         */
        public static final int N_MR1 = 25;
        
        /**
         * Android 8.0
         */
        public static final int O = 26;
        
        /**
         * Android 8.1
         */
        public static final int O_MR1 = 27;
        
        /**
         * Android 9.0
         */
        public static final int P = 28;
        
        /**
         * Android 10.0
         */
        public static final int Q = 29;
        
        /**
         * Android 11.0
         */
        public static final int R = 30;
        
        /**
         * Android 12.0
         */
        public static final int S = 31;
        
        /**
         * Android 12L
         */
        public static final int S_V2 = 32;
        
        /**
         * Android 13.0
         */
        public static final int TIRAMISU = 33;
    }
}