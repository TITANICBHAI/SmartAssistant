package models;

import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.ArrayList;

/**
 * Interface for game-specific package management functionality
 * This was moved from GameContext.PackageManager to fix class naming issues
 */
public interface GamePackageManager {
    /**
     * Get a package info by package name
     * 
     * @param packageName The package name
     * @return The GameApplicationInfo or null if not found
     */
    GameApplicationInfo getPackageInfo(String packageName);
    
    /**
     * Check if a package is installed
     * 
     * @param packageName The package name
     * @return True if the package is installed
     */
    boolean isPackageInstalled(String packageName);
    
    /**
     * Get all installed packages
     * 
     * @return List of all installed packages
     */
    List<GameApplicationInfo> getInstalledPackages();
    
    /**
     * Default implementation of GamePackageManager
     */
    public static class DefaultPackageManager implements GamePackageManager {
        private final Map<String, GameApplicationInfo> packages;
        
        /**
         * Create a new package manager
         */
        public DefaultPackageManager() {
            this.packages = new HashMap<>();
        }
        
        /**
         * Create a new package manager with initial packages
         * 
         * @param packages Map of package name to GameApplicationInfo
         */
        public DefaultPackageManager(Map<String, GameApplicationInfo> packages) {
            this.packages = new HashMap<>(packages);
        }
        
        /**
         * Add a package
         * 
         * @param info The application info
         */
        public void addPackage(GameApplicationInfo info) {
            if (info != null) {
                packages.put(info.getPackageName(), info);
            }
        }
        
        /**
         * Remove a package
         * 
         * @param packageName The package name
         * @return The removed GameApplicationInfo or null if not found
         */
        public GameApplicationInfo removePackage(String packageName) {
            return packages.remove(packageName);
        }
        
        @Override
        public GameApplicationInfo getPackageInfo(String packageName) {
            return packages.get(packageName);
        }
        
        @Override
        public boolean isPackageInstalled(String packageName) {
            return packages.containsKey(packageName);
        }
        
        @Override
        public List<GameApplicationInfo> getInstalledPackages() {
            return new ArrayList<>(packages.values());
        }
    }
}