package models;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * PackageManager class for the GameContext interface.
 * This provides package information and management for the game context.
 */
public class GameContext$PackageManager {
    private Map<String, GameContext$ApplicationInfo> installedPackages;

    /**
     * Default constructor.
     */
    public GameContext$PackageManager() {
        installedPackages = new HashMap<>();
    }

    /**
     * Get information about an installed package.
     * 
     * @param packageName The package name
     * @return The ApplicationInfo or null if not found
     */
    public GameContext$ApplicationInfo getApplicationInfo(String packageName) {
        return installedPackages.get(packageName);
    }

    /**
     * Get all installed packages.
     * 
     * @return List of ApplicationInfo objects
     */
    public List<GameContext$ApplicationInfo> getInstalledApplications() {
        return new ArrayList<>(installedPackages.values());
    }

    /**
     * Get a list of installed packages.
     * 
     * @param flags Flags to filter the list
     * @return List of ApplicationInfo objects
     */
    public List<GameContext$ApplicationInfo> getInstalledApplications(int flags) {
        // Flags are ignored in this mock implementation
        return getInstalledApplications();
    }

    /**
     * Check if a package is installed.
     * 
     * @param packageName The package name
     * @return True if the package is installed
     */
    public boolean isPackageInstalled(String packageName) {
        return installedPackages.containsKey(packageName);
    }

    /**
     * Install a package.
     * 
     * @param applicationInfo The ApplicationInfo to install
     * @return True if installation succeeded
     */
    public boolean installPackage(GameContext$ApplicationInfo applicationInfo) {
        if (applicationInfo != null && applicationInfo.getPackageName() != null && !applicationInfo.getPackageName().isEmpty()) {
            installedPackages.put(applicationInfo.getPackageName(), applicationInfo);
            return true;
        }
        return false;
    }

    /**
     * Uninstall a package.
     * 
     * @param packageName The package name
     * @return True if uninstallation succeeded
     */
    public boolean uninstallPackage(String packageName) {
        if (packageName != null && !packageName.isEmpty() && installedPackages.containsKey(packageName)) {
            installedPackages.remove(packageName);
            return true;
        }
        return false;
    }

    /**
     * Get the package name for a given component.
     * 
     * @param component The component name (class name)
     * @return The package name or null if not found
     */
    public String getPackageNameForComponent(String component) {
        if (component == null || component.isEmpty()) {
            return null;
        }

        for (GameContext$ApplicationInfo info : installedPackages.values()) {
            if (component.equals(info.getClassName())) {
                return info.getPackageName();
            }
        }
        return null;
    }

    /**
     * Get packages with a given permission.
     * 
     * @param permission The permission
     * @return List of package names with the permission
     */
    public List<String> getPackagesWithPermission(String permission) {
        List<String> result = new ArrayList<>();
        
        if (permission == null || permission.isEmpty()) {
            return result;
        }

        for (GameContext$ApplicationInfo info : installedPackages.values()) {
            if (permission.equals(info.getPermission())) {
                result.add(info.getPackageName());
            }
        }
        return result;
    }

    /**
     * Check if a component exists.
     * 
     * @param component The component name (class name)
     * @return True if the component exists
     */
    public boolean componentExists(String component) {
        return getPackageNameForComponent(component) != null;
    }
}