package models;

import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.ArrayList;

/**
 * Interface for package management functionality
 */
public interface PackageManager {
    /**
     * Get a package info by package name
     * 
     * @param packageName The package name
     * @return The ApplicationInfo or null if not found
     */
    ApplicationInfo getPackageInfo(String packageName);
    
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
    List<ApplicationInfo> getInstalledPackages();
    
    /**
     * Default implementation of PackageManager
     */
    public static class DefaultPackageManager implements PackageManager {
        private final Map<String, ApplicationInfo> packages;
        
        /**
         * Create a new package manager
         */
        public DefaultPackageManager() {
            this.packages = new HashMap<>();
        }
        
        /**
         * Create a new package manager with initial packages
         * 
         * @param packages Map of package name to ApplicationInfo
         */
        public DefaultPackageManager(Map<String, ApplicationInfo> packages) {
            this.packages = new HashMap<>(packages);
        }
        
        /**
         * Add a package
         * 
         * @param info The application info
         */
        public void addPackage(ApplicationInfo info) {
            if (info != null) {
                packages.put(info.getPackageName(), info);
            }
        }
        
        /**
         * Remove a package
         * 
         * @param packageName The package name
         * @return The removed ApplicationInfo or null if not found
         */
        public ApplicationInfo removePackage(String packageName) {
            return packages.remove(packageName);
        }
        
        @Override
        public ApplicationInfo getPackageInfo(String packageName) {
            return packages.get(packageName);
        }
        
        @Override
        public boolean isPackageInstalled(String packageName) {
            return packages.containsKey(packageName);
        }
        
        @Override
        public List<ApplicationInfo> getInstalledPackages() {
            return new ArrayList<>(packages.values());
        }
    }
}