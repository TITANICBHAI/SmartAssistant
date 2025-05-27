package com.aiassistant.models;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import android.graphics.drawable.Drawable;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import com.aiassistant.core.AIController.GameType;

/**
 * Model class representing information about an application
 */
public class AppInfo {
    private final String packageName;
    private final String appName;
    private final Drawable appIcon;
    private int usageCount;
    private float confidenceScore;
    private boolean learned;
    private final Map<String, Object> metadata;
    
    /**
     * Create a new AppInfo
     * 
     * @param packageName Package name of the app
     * @param appName Display name of the app
     * @param appIcon App icon drawable
     * @param usageCount Number of times the app has been used
     * @param confidenceScore Confidence score for the app (0.0-1.0)
     * @param learned Whether learning data exists for this app
     * @param metadata Additional metadata
     */
    public AppInfo(
            @NonNull String packageName,
            @NonNull String appName,
            @Nullable Drawable appIcon,
            int usageCount,
            float confidenceScore,
            boolean learned,
            @Nullable Map<String, Object> metadata) {
        this.packageName = packageName;
        this.appName = appName;
        this.appIcon = appIcon;
        this.usageCount = Math.max(0, usageCount);
        this.confidenceScore = Math.max(0.0f, Math.min(1.0f, confidenceScore));
        this.learned = learned;
        this.metadata = metadata != null ? new HashMap<>(metadata) : new HashMap<>();
    }
    
    /**
     * Create a simple AppInfo with default values
     * 
     * @param packageName Package name of the app
     * @param appName Display name of the app
     * @param appIcon App icon drawable
     */
    public AppInfo(
            @NonNull String packageName,
            @NonNull String appName,
            @Nullable Drawable appIcon) {
        this(packageName, appName, appIcon, 0, 0.0f, false, null);
    }

    @NonNull
    public String getPackageName() {
        return packageName;
    }

    @NonNull
    public String getAppName() {
        return appName;
    }

    @Nullable
    public Drawable getAppIcon() {
        return appIcon;
    }

    public int getUsageCount() {
        return usageCount;
    }

    public void setUsageCount(int usageCount) {
        this.usageCount = Math.max(0, usageCount);
    }

    public void incrementUsageCount() {
        this.usageCount++;
    }

    public float getConfidenceScore() {
        return confidenceScore;
    }

    public void setConfidenceScore(float confidenceScore) {
        this.confidenceScore = Math.max(0.0f, Math.min(1.0f, confidenceScore));
    }

    public boolean isLearned() {
        return learned;
    }

    public void setLearned(boolean learned) {
        this.learned = learned;
    }
    
    /**
     * Get the game type of this app
     * 
     * @return Game type or GameType.UNKNOWN if not a game
     */
    @NonNull
    public GameType getGameType() {
        // First check if we have a stored game type in metadata
        String storedType = getMetadataString("game_type", null);
        if (storedType != null) {
            return GameType.fromString(storedType);
        }
        
        // Otherwise determine from package name
        return GameType.fromPackageName(packageName);
    }
    
    /**
     * Set the game type for this app
     * 
     * @param gameType Game type
     */
    public void setGameType(GameType gameType) {
        if (gameType != null) {
            updateMetadata("game_type", gameType.getValue());
        }
    }

    @NonNull
    public Map<String, Object> getMetadata() {
        return new HashMap<>(metadata);
    }
    
    /**
     * Get a specific metadata value
     * 
     * @param key Metadata key
     * @return Metadata value or null if not found
     */
    @Nullable
    public Object getMetadataValue(String key) {
        return metadata.get(key);
    }
    
    /**
     * Get a specific metadata value as a string
     * 
     * @param key Metadata key
     * @param defaultValue Default value if metadata is missing or not a string
     * @return Metadata value as string
     */
    public String getMetadataString(String key, String defaultValue) {
        Object value = metadata.get(key);
        return value instanceof String ? (String) value : defaultValue;
    }
    
    /**
     * Get a specific metadata value as an integer
     * 
     * @param key Metadata key
     * @param defaultValue Default value if metadata is missing or not an integer
     * @return Metadata value as integer
     */
    public int getMetadataInt(String key, int defaultValue) {
        Object value = metadata.get(key);
        if (value instanceof Number) {
            return ((Number) value).intValue();
        } else if (value instanceof String) {
            try {
                return Integer.parseInt((String) value);
            } catch (NumberFormatException e) {
                return defaultValue;
            }
        }
        return defaultValue;
    }
    
    /**
     * Get a specific metadata value as a boolean
     * 
     * @param key Metadata key
     * @param defaultValue Default value if metadata is missing or not a boolean
     * @return Metadata value as boolean
     */
    public boolean getMetadataBoolean(String key, boolean defaultValue) {
        Object value = metadata.get(key);
        if (value instanceof Boolean) {
            return (Boolean) value;
        } else if (value instanceof String) {
            return Boolean.parseBoolean((String) value);
        }
        return defaultValue;
    }
    
    /**
     * Update a metadata value
     * 
     * @param key Metadata key
     * @param value Metadata value
     */
    public void updateMetadata(String key, Object value) {
        metadata.put(key, value);
    }
    
    /**
     * Remove a metadata value
     * 
     * @param key Metadata key
     */
    public void removeMetadata(String key) {
        metadata.remove(key);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        AppInfo appInfo = (AppInfo) o;
        return packageName.equals(appInfo.packageName);
    }

    @Override
    public int hashCode() {
        return Objects.hash(packageName);
    }

    @Override
    public String toString() {
        return "AppInfo{" +
                "packageName='" + packageName + '\'' +
                ", appName='" + appName + '\'' +
                ", usageCount=" + usageCount +
                ", confidenceScore=" + confidenceScore +
                ", learned=" + learned +
                '}';
    }
    
    /**
     * Builder class for AppInfo
     */
    public static class Builder {
        private String packageName;
        private String appName;
        private Drawable appIcon;
        private int usageCount = 0;
        private float confidenceScore = 0.0f;
        private boolean learned = false;
        private Map<String, Object> metadata = new HashMap<>();
        
        /**
         * Create a builder with required elements
         * 
         * @param packageName Package name of the app
         * @param appName Display name of the app
         */
        public Builder(@NonNull String packageName, @NonNull String appName) {
            this.packageName = packageName;
            this.appName = appName;
        }
        
        /**
         * Set the app icon
         * 
         * @param appIcon App icon drawable
         * @return This Builder instance
         */
        @NonNull
        public Builder setAppIcon(@Nullable Drawable appIcon) {
            this.appIcon = appIcon;
            return this;
        }
        
        /**
         * Set the usage count
         * 
         * @param usageCount Usage count
         * @return This Builder instance
         */
        @NonNull
        public Builder setUsageCount(int usageCount) {
            this.usageCount = Math.max(0, usageCount);
            return this;
        }
        
        /**
         * Set the confidence score
         * 
         * @param confidenceScore Confidence score (0.0-1.0)
         * @return This Builder instance
         */
        @NonNull
        public Builder setConfidenceScore(float confidenceScore) {
            this.confidenceScore = Math.max(0.0f, Math.min(1.0f, confidenceScore));
            return this;
        }
        
        /**
         * Set whether learning data exists
         * 
         * @param learned Whether learning data exists
         * @return This Builder instance
         */
        @NonNull
        public Builder setLearned(boolean learned) {
            this.learned = learned;
            return this;
        }
        
        /**
         * Add a metadata value
         * 
         * @param key Metadata key
         * @param value Metadata value
         * @return This Builder instance
         */
        @NonNull
        public Builder addMetadata(@NonNull String key, @Nullable Object value) {
            this.metadata.put(key, value);
            return this;
        }
        
        /**
         * Add all metadata from a map
         * 
         * @param meta Map of metadata to add
         * @return This Builder instance
         */
        @NonNull
        public Builder addMetadata(@Nullable Map<String, Object> meta) {
            if (meta != null) {
                this.metadata.putAll(meta);
            }
            return this;
        }
        
        /**
         * Build the AppInfo
         * 
         * @return AppInfo instance
         */
        @NonNull
        public AppInfo build() {
            return new AppInfo(
                    packageName, appName, appIcon,
                    usageCount, confidenceScore, learned, metadata);
        }
    }
}