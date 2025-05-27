package com.aiassistant.models;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * Model class representing a learning source for the AI system
 */
public class LearningSource {
    private final String id;
    private final String name;
    private final String description;
    private final String type;
    private final Map<String, Object> metadata;
    private int progress;
    private boolean enabled;
    private long lastUpdatedTimestamp;
    
    /**
     * Create a new LearningSource
     * 
     * @param id Unique identifier for this source
     * @param name Display name of the source
     * @param description Description of the source
     * @param type Type of learning source
     * @param metadata Additional metadata
     * @param progress Current learning progress (0-100)
     * @param enabled Whether this source is enabled
     * @param lastUpdatedTimestamp Last updated timestamp
     */
    public LearningSource(
            @NonNull String id,
            @NonNull String name,
            @NonNull String description,
            @NonNull String type,
            @Nullable Map<String, Object> metadata,
            int progress,
            boolean enabled,
            long lastUpdatedTimestamp) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.type = type;
        this.metadata = metadata != null ? new HashMap<>(metadata) : new HashMap<>();
        this.progress = Math.max(0, Math.min(100, progress));
        this.enabled = enabled;
        this.lastUpdatedTimestamp = lastUpdatedTimestamp;
    }
    
    /**
     * Create a simple LearningSource with default values
     * 
     * @param id Unique identifier
     * @param name Display name
     * @param type Type of learning source
     */
    public LearningSource(
            @NonNull String id,
            @NonNull String name,
            @NonNull String type) {
        this(id, name, "", type, null, 0, true, System.currentTimeMillis());
    }

    @NonNull
    public String getId() {
        return id;
    }

    @NonNull
    public String getName() {
        return name;
    }

    @NonNull
    public String getDescription() {
        return description;
    }

    @NonNull
    public String getType() {
        return type;
    }

    @NonNull
    public Map<String, Object> getMetadata() {
        return new HashMap<>(metadata);
    }

    public int getProgress() {
        return progress;
    }

    public void setProgress(int progress) {
        this.progress = Math.max(0, Math.min(100, progress));
        this.lastUpdatedTimestamp = System.currentTimeMillis();
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
        this.lastUpdatedTimestamp = System.currentTimeMillis();
    }

    public long getLastUpdatedTimestamp() {
        return lastUpdatedTimestamp;
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
        this.lastUpdatedTimestamp = System.currentTimeMillis();
    }
    
    /**
     * Remove a metadata value
     * 
     * @param key Metadata key
     */
    public void removeMetadata(String key) {
        metadata.remove(key);
        this.lastUpdatedTimestamp = System.currentTimeMillis();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        LearningSource that = (LearningSource) o;
        return progress == that.progress &&
                enabled == that.enabled &&
                id.equals(that.id) &&
                name.equals(that.name) &&
                Objects.equals(description, that.description) &&
                type.equals(that.type);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, name, description, type, progress, enabled);
    }

    @Override
    public String toString() {
        return "LearningSource{" +
                "id='" + id + '\'' +
                ", name='" + name + '\'' +
                ", type='" + type + '\'' +
                ", progress=" + progress +
                ", enabled=" + enabled +
                '}';
    }
    
    /**
     * Builder class for LearningSource
     */
    public static class Builder {
        private String id;
        private String name;
        private String description = "";
        private String type;
        private Map<String, Object> metadata = new HashMap<>();
        private int progress = 0;
        private boolean enabled = true;
        private long lastUpdatedTimestamp = System.currentTimeMillis();
        
        /**
         * Create a builder with required elements
         * 
         * @param id Unique identifier
         * @param name Display name
         * @param type Type of learning source
         */
        public Builder(@NonNull String id, @NonNull String name, @NonNull String type) {
            this.id = id;
            this.name = name;
            this.type = type;
        }
        
        /**
         * Set the description
         * 
         * @param description Description
         * @return This Builder instance
         */
        @NonNull
        public Builder setDescription(@Nullable String description) {
            this.description = description != null ? description : "";
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
         * Set the progress
         * 
         * @param progress Progress value (0-100)
         * @return This Builder instance
         */
        @NonNull
        public Builder setProgress(int progress) {
            this.progress = Math.max(0, Math.min(100, progress));
            return this;
        }
        
        /**
         * Set whether this source is enabled
         * 
         * @param enabled Whether this source is enabled
         * @return This Builder instance
         */
        @NonNull
        public Builder setEnabled(boolean enabled) {
            this.enabled = enabled;
            return this;
        }
        
        /**
         * Set the last updated timestamp
         * 
         * @param timestamp Last updated timestamp
         * @return This Builder instance
         */
        @NonNull
        public Builder setLastUpdatedTimestamp(long timestamp) {
            this.lastUpdatedTimestamp = timestamp;
            return this;
        }
        
        /**
         * Build the LearningSource
         * 
         * @return LearningSource instance
         */
        @NonNull
        public LearningSource build() {
            return new LearningSource(
                    id, name, description, type, metadata,
                    progress, enabled, lastUpdatedTimestamp);
        }
    }
}