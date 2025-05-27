package models;

/**
 * ApplicationInfo class for the GameContext interface.
 * This defines application metadata used in the game context.
 */
public class GameContext$ApplicationInfo {
    private String packageName;
    private String processName;
    private String className;
    private String permission;
    private String taskAffinity;
    private String sourceDir;
    private String publicSourceDir;
    private String dataDir;
    private int descriptionRes;
    private int theme;
    private int flags;
    private int uid;
    private boolean enabled;

    /**
     * Default constructor.
     */
    public GameContext$ApplicationInfo() {
        packageName = "";
        processName = "";
        className = "";
        permission = "";
        taskAffinity = "";
        sourceDir = "";
        publicSourceDir = "";
        dataDir = "";
        descriptionRes = 0;
        theme = 0;
        flags = 0;
        uid = 0;
        enabled = true;
    }

    /**
     * Constructor with package name.
     * 
     * @param packageName The package name
     */
    public GameContext$ApplicationInfo(String packageName) {
        this();
        this.packageName = packageName;
    }

    /**
     * Copy constructor.
     * 
     * @param other ApplicationInfo to copy from
     */
    public GameContext$ApplicationInfo(GameContext$ApplicationInfo other) {
        if (other != null) {
            this.packageName = other.packageName;
            this.processName = other.processName;
            this.className = other.className;
            this.permission = other.permission;
            this.taskAffinity = other.taskAffinity;
            this.sourceDir = other.sourceDir;
            this.publicSourceDir = other.publicSourceDir;
            this.dataDir = other.dataDir;
            this.descriptionRes = other.descriptionRes;
            this.theme = other.theme;
            this.flags = other.flags;
            this.uid = other.uid;
            this.enabled = other.enabled;
        } else {
            // Initialize with default values
            this.packageName = "";
            this.processName = "";
            this.className = "";
            this.permission = "";
            this.taskAffinity = "";
            this.sourceDir = "";
            this.publicSourceDir = "";
            this.dataDir = "";
            this.descriptionRes = 0;
            this.theme = 0;
            this.flags = 0;
            this.uid = 0;
            this.enabled = true;
        }
    }

    /**
     * Get the package name.
     * 
     * @return Package name
     */
    public String getPackageName() {
        return packageName;
    }

    /**
     * Set the package name.
     * 
     * @param packageName Package name
     */
    public void setPackageName(String packageName) {
        this.packageName = packageName;
    }

    /**
     * Get the process name.
     * 
     * @return Process name
     */
    public String getProcessName() {
        return processName;
    }

    /**
     * Set the process name.
     * 
     * @param processName Process name
     */
    public void setProcessName(String processName) {
        this.processName = processName;
    }

    /**
     * Get the class name.
     * 
     * @return Class name
     */
    public String getClassName() {
        return className;
    }

    /**
     * Set the class name.
     * 
     * @param className Class name
     */
    public void setClassName(String className) {
        this.className = className;
    }

    /**
     * Get the permission.
     * 
     * @return Permission
     */
    public String getPermission() {
        return permission;
    }

    /**
     * Set the permission.
     * 
     * @param permission Permission
     */
    public void setPermission(String permission) {
        this.permission = permission;
    }

    /**
     * Get the task affinity.
     * 
     * @return Task affinity
     */
    public String getTaskAffinity() {
        return taskAffinity;
    }

    /**
     * Set the task affinity.
     * 
     * @param taskAffinity Task affinity
     */
    public void setTaskAffinity(String taskAffinity) {
        this.taskAffinity = taskAffinity;
    }

    /**
     * Get the source directory.
     * 
     * @return Source directory
     */
    public String getSourceDir() {
        return sourceDir;
    }

    /**
     * Set the source directory.
     * 
     * @param sourceDir Source directory
     */
    public void setSourceDir(String sourceDir) {
        this.sourceDir = sourceDir;
    }

    /**
     * Get the public source directory.
     * 
     * @return Public source directory
     */
    public String getPublicSourceDir() {
        return publicSourceDir;
    }

    /**
     * Set the public source directory.
     * 
     * @param publicSourceDir Public source directory
     */
    public void setPublicSourceDir(String publicSourceDir) {
        this.publicSourceDir = publicSourceDir;
    }

    /**
     * Get the data directory.
     * 
     * @return Data directory
     */
    public String getDataDir() {
        return dataDir;
    }

    /**
     * Set the data directory.
     * 
     * @param dataDir Data directory
     */
    public void setDataDir(String dataDir) {
        this.dataDir = dataDir;
    }

    /**
     * Get the description resource ID.
     * 
     * @return Description resource ID
     */
    public int getDescriptionRes() {
        return descriptionRes;
    }

    /**
     * Set the description resource ID.
     * 
     * @param descriptionRes Description resource ID
     */
    public void setDescriptionRes(int descriptionRes) {
        this.descriptionRes = descriptionRes;
    }

    /**
     * Get the theme.
     * 
     * @return Theme
     */
    public int getTheme() {
        return theme;
    }

    /**
     * Set the theme.
     * 
     * @param theme Theme
     */
    public void setTheme(int theme) {
        this.theme = theme;
    }

    /**
     * Get the flags.
     * 
     * @return Flags
     */
    public int getFlags() {
        return flags;
    }

    /**
     * Set the flags.
     * 
     * @param flags Flags
     */
    public void setFlags(int flags) {
        this.flags = flags;
    }

    /**
     * Get the UID.
     * 
     * @return UID
     */
    public int getUid() {
        return uid;
    }

    /**
     * Set the UID.
     * 
     * @param uid UID
     */
    public void setUid(int uid) {
        this.uid = uid;
    }

    /**
     * Check if the application is enabled.
     * 
     * @return True if enabled
     */
    public boolean isEnabled() {
        return enabled;
    }

    /**
     * Set if the application is enabled.
     * 
     * @param enabled Enabled flag
     */
    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    @Override
    public String toString() {
        return "ApplicationInfo{" +
                "packageName='" + packageName + '\'' +
                ", className='" + className + '\'' +
                ", enabled=" + enabled +
                '}';
    }
    
    /**
     * Convert to a standard ApplicationInfo
     * 
     * @return A new ApplicationInfo instance
     */
    public models.ApplicationInfo toApplicationInfo() {
        models.ApplicationInfo.DefaultApplicationInfo appInfo = 
            new models.ApplicationInfo.DefaultApplicationInfo(packageName, className, processName, 1);
        return appInfo;
    }
    
    /**
     * Create from a standard ApplicationInfo
     * 
     * @param info The source ApplicationInfo
     * @return A new GameContext$ApplicationInfo
     */
    public static GameContext$ApplicationInfo fromApplicationInfo(models.ApplicationInfo info) {
        if (info == null) {
            return null;
        }
        
        GameContext$ApplicationInfo appInfo = new GameContext$ApplicationInfo();
        appInfo.setPackageName(info.getPackageName());
        appInfo.setClassName(info.getApplicationName());
        appInfo.setProcessName(info.getVersionName());
        return appInfo;
    }
}