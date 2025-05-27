package utils;

import java.util.Map;
import java.util.HashMap;
import java.util.ArrayList;
import java.util.List;
import utils.LogHelper;

/**
 * Helper class for getting information about applications.
 * This class provides functionality for retrieving and managing application information.
 */
public class AppInfoHelper {
    private static final String TAG = "AppInfoHelper";
    private static AppInfoHelper instance;
    
    private PackageManager packageManager;
    private Map<String, ApplicationInfo> appInfoCache;
    
    /**
     * Get the singleton instance of AppInfoHelper.
     * @return The singleton instance
     */
    public static AppInfoHelper getInstance() {
        if (instance == null) {
            instance = new AppInfoHelper();
        }
        return instance;
    }
    
    /**
     * Private constructor to enforce singleton pattern
     */
    private AppInfoHelper() {
        this.packageManager = new DefaultPackageManager();
        this.appInfoCache = new HashMap<>();
    }
    
    /**
     * Set a custom package manager
     * @param packageManager The package manager to use
     */
    public void setPackageManager(PackageManager packageManager) {
        if (packageManager != null) {
            this.packageManager = packageManager;
        }
    }
    
    /**
     * Get the package manager
     * @return The package manager
     */
    public PackageManager getPackageManager() {
        return packageManager;
    }
    
    /**
     * Get the application name for a package
     * @param packageName The package name
     * @return The application name, or the package name if not found
     */
    public String getAppName(String packageName) {
        if (packageName == null || packageName.isEmpty()) {
            return "";
        }
        
        try {
            ApplicationInfo ai = packageManager.getApplicationInfo(packageName, 0);
            return ai.getName(); // Use name instead of label
        } catch (PackageManager.NameNotFoundException e) {
            LogHelper.e(TAG, "Package not found: " + packageName, e);
            return packageName;
        }
    }
    
    /**
     * Get the application information for a package
     * @param packageName The package name
     * @return The ApplicationInfo, or null if not found
     */
    public ApplicationInfo getAppInfo(String packageName) {
        if (packageName == null || packageName.isEmpty()) {
            return null;
        }
        
        // Check cache first
        if (appInfoCache.containsKey(packageName)) {
            return appInfoCache.get(packageName);
        }
        
        try {
            ApplicationInfo ai = packageManager.getApplicationInfo(packageName, 0);
            
            // Cache the result
            appInfoCache.put(packageName, ai);
            
            return ai;
        } catch (PackageManager.NameNotFoundException e) {
            LogHelper.e(TAG, "Package not found: " + packageName, e);
            return null;
        }
    }
    
    /**
     * Check if an application is a game
     * @param packageName The package name
     * @return True if the application is a game, false otherwise
     */
    public boolean isGame(String packageName) {
        if (packageName == null || packageName.isEmpty()) {
            return false;
        }
        
        try {
            ApplicationInfo ai = packageManager.getApplicationInfo(packageName, 0);
            
            // Check if this is a game
            if (ai.isGame()) {
                return true;
            }
            
            // If not explicitly marked as a game, check the package name
            GameType gameType = GameType.fromPackageName(packageName);
            return gameType != GameType.UNKNOWN;
        } catch (PackageManager.NameNotFoundException e) {
            LogHelper.e(TAG, "Package not found: " + packageName, e);
            return false;
        }
    }
    
    /**
     * Get all information about an application as a map
     * @param packageName The package name
     * @return A map of information, or an empty map if not found
     */
    public Map<String, Object> getAppInfoMap(String packageName) {
        Map<String, Object> info = new HashMap<>();
        
        if (packageName == null || packageName.isEmpty()) {
            return info;
        }
        
        try {
            ApplicationInfo ai = packageManager.getApplicationInfo(packageName, 0);
            
            // Add basic information
            info.put("packageName", packageName);
            info.put("appName", ai.getName());
            info.put("isGame", ai.isGame());
            
            // Add game type information
            GameType gameType = ai.getGameType();
            info.put("gameType", gameType.name());
            info.put("gameTypeDisplay", gameType.getDisplayName());
            
            // Add version information
            String version = packageManager.getPackageVersion(packageName);
            if (version != null) {
                info.put("version", version);
            }
            
            // Add all metadata
            info.putAll(ai.getAllMetadata());
            
        } catch (PackageManager.NameNotFoundException e) {
            LogHelper.e(TAG, "Package not found: " + packageName, e);
        }
        
        return info;
    }
    
    /**
     * Get a list of installed applications
     * @return A list of ApplicationInfo objects
     */
    public List<ApplicationInfo> getInstalledApps() {
        List<ApplicationInfo> apps = new ArrayList<>();
        
        String[] packages = packageManager.getInstalledPackages();
        for (String packageName : packages) {
            ApplicationInfo appInfo = getAppInfo(packageName);
            if (appInfo != null) {
                apps.add(appInfo);
            }
        }
        
        return apps;
    }
    
    /**
     * Get a list of installed games
     * @return A list of ApplicationInfo objects for games
     */
    public List<ApplicationInfo> getInstalledGames() {
        List<ApplicationInfo> games = new ArrayList<>();
        
        String[] packages = packageManager.getInstalledPackages();
        for (String packageName : packages) {
            ApplicationInfo appInfo = getAppInfo(packageName);
            if (appInfo != null && appInfo.isGame()) {
                games.add(appInfo);
            }
        }
        
        return games;
    }
    
    /**
     * Clear the application information cache
     */
    public void clearCache() {
        appInfoCache.clear();
    }
    
    /**
     * Get the game type for a package
     * @param packageName The package name
     * @return The GameType, or UNKNOWN if not found or not a game
     */
    public GameType getGameType(String packageName) {
        ApplicationInfo appInfo = getAppInfo(packageName);
        if (appInfo == null) {
            return GameType.UNKNOWN;
        }
        
        GameType direct = appInfo.getGameType();
        if (direct != GameType.UNKNOWN) {
            return direct;
        }
        
        // If not directly specified, infer from package name
        return GameType.fromPackageName(packageName);
    }
    
    /**
     * Abstract class for package management functionality
     */
    public static abstract class PackageManager {
        /**
         * Exception thrown when a package is not found
         */
        public static class NameNotFoundException extends Exception {
            public NameNotFoundException(String message) {
                super(message);
            }
        }
        
        /**
         * Get application info for a package
         * 
         * @param packageName The package name
         * @return The application info
         * @throws NameNotFoundException If the package is not found
         */
        public abstract ApplicationInfo getApplicationInfo(String packageName)
            throws NameNotFoundException;
        
        /**
         * Get application info for a package with flags
         * 
         * @param packageName The package name
         * @param flags Additional flags
         * @return The application info
         * @throws NameNotFoundException If the package is not found
         */
        public abstract ApplicationInfo getApplicationInfo(String packageName, int flags)
            throws NameNotFoundException;
        
        /**
         * Check if a package is installed
         * 
         * @param packageName The package name
         * @return True if the package is installed
         */
        public abstract boolean isPackageInstalled(String packageName);
        
        /**
         * Get the version of a package
         * 
         * @param packageName The package name
         * @return The version string
         * @throws NameNotFoundException If the package is not found
         */
        public abstract String getPackageVersion(String packageName)
            throws NameNotFoundException;
        
        /**
         * Get a list of installed packages
         * 
         * @return Array of package names
         */
        public abstract String[] getInstalledPackages();
    }
    
    /**
     * A default implementation of PackageManager that uses a dummy internal database.
     */
    private class DefaultPackageManager extends PackageManager {
        private Map<String, ApplicationInfo> packages;
        
        /**
         * Create a new DefaultPackageManager
         */
        public DefaultPackageManager() {
            packages = new HashMap<>();
            
            // Add some dummy packages for testing
            addDummyPackage("com.example.game", "Example Game", true, GameType.ACTION);
            addDummyPackage("com.example.app", "Example App", false, GameType.UNKNOWN);
        }
        
        /**
         * Add a dummy package for testing
         * @param packageName The package name
         * @param name The application name
         * @param isGame Whether the application is a game
         * @param gameType The game type
         */
        private void addDummyPackage(String packageName, String name, boolean isGame, GameType gameType) {
            ApplicationInfo info = new ApplicationInfo(packageName, name);
            info.setIsGame(isGame);
            info.setGameType(gameType);
            info.setVersion("1.0.0");
            packages.put(packageName, info);
        }
        
        @Override
        public ApplicationInfo getApplicationInfo(String packageName) throws PackageManager.NameNotFoundException {
            if (!packages.containsKey(packageName)) {
                throw new PackageManager.NameNotFoundException("Package not found: " + packageName);
            }
            return packages.get(packageName);
        }
        
        @Override
        public ApplicationInfo getApplicationInfo(String packageName, int flags) throws PackageManager.NameNotFoundException {
            // Flags are ignored in this implementation
            return getApplicationInfo(packageName);
        }
        
        @Override
        public boolean isPackageInstalled(String packageName) {
            return packages.containsKey(packageName);
        }
        
        @Override
        public String getPackageVersion(String packageName) throws PackageManager.NameNotFoundException {
            ApplicationInfo info = getApplicationInfo(packageName);
            return info.getVersion();
        }
        
        @Override
        public String[] getInstalledPackages() {
            return packages.keySet().toArray(new String[0]);
        }
    }
}