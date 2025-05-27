package com.aiassistant.models;

import android.graphics.Rect;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

/**
 * Standardized UI element representation that combines detection and model aspects
 */
public class StandardizedUIElement {
    private final String elementId;
    private final StandardizedUIElementType elementType;
    private final Rect bounds;
    private final String text;
    private final String contentDescription;
    private final boolean clickable;
    private final boolean focusable;
    private final boolean visible;
    private final boolean enabled;
    private final float confidence;
    private final Map<String, Object> attributes;
    private final long timestamp;
    
    /**
     * Create a new StandardizedUIElement
     * 
     * @param elementId Unique identifier for this element
     * @param elementType Type of UI element
     * @param bounds Bounds of the element on screen
     * @param text Text of the element
     * @param contentDescription Content description of the element
     * @param clickable Whether the element is clickable
     * @param focusable Whether the element is focusable
     * @param visible Whether the element is visible
     * @param enabled Whether the element is enabled
     * @param confidence Confidence score for detected elements (0.0-1.0)
     * @param attributes Additional attributes of the element
     * @param timestamp Timestamp when this element was created/detected
     */
    public StandardizedUIElement(
            @NonNull String elementId,
            @NonNull StandardizedUIElementType elementType,
            @NonNull Rect bounds,
            @Nullable String text,
            @Nullable String contentDescription,
            boolean clickable,
            boolean focusable,
            boolean visible,
            boolean enabled,
            float confidence,
            @Nullable Map<String, Object> attributes,
            long timestamp) {
        this.elementId = elementId;
        this.elementType = elementType;
        this.bounds = new Rect(bounds);
        this.text = text != null ? text : "";
        this.contentDescription = contentDescription != null ? contentDescription : "";
        this.clickable = clickable;
        this.focusable = focusable;
        this.visible = visible;
        this.enabled = enabled;
        this.confidence = Math.max(0.0f, Math.min(1.0f, confidence));
        this.attributes = attributes != null ? new HashMap<>(attributes) : new HashMap<>();
        this.timestamp = timestamp;
    }
    
    @NonNull
    public String getElementId() {
        return elementId;
    }
    
    /**
     * Get the element ID (alias for getElementId for API compatibility)
     * 
     * @return Element ID
     */
    @NonNull
    public String getId() {
        return elementId;
    }
    
    @NonNull
    public StandardizedUIElementType getElementType() {
        return elementType;
    }
    
    /**
     * Get the element type as a string
     * 
     * @return Type string
     */
    @NonNull
    public String getTypeString() {
        return elementType.getValue();
    }
    
    /**
     * Get the element type as a string (alias for getTypeString for API compatibility)
     * 
     * @return Type string
     */
    @NonNull
    public String getType() {
        return elementType.getValue();
    }
    
    @NonNull
    public Rect getBounds() {
        return new Rect(bounds);
    }
    
    @NonNull
    public String getText() {
        return text;
    }
    
    @NonNull
    public String getContentDescription() {
        return contentDescription;
    }
    
    public boolean isClickable() {
        return clickable;
    }
    
    public boolean isFocusable() {
        return focusable;
    }
    
    public boolean isVisible() {
        return visible;
    }
    
    public boolean isEnabled() {
        return enabled;
    }
    
    public float getConfidence() {
        return confidence;
    }
    
    @NonNull
    public Map<String, Object> getAttributes() {
        return new HashMap<>(attributes);
    }
    
    public long getTimestamp() {
        return timestamp;
    }
    
    /**
     * Get the width of the element
     * 
     * @return Width in pixels
     */
    public int getWidth() {
        return bounds.width();
    }
    
    /**
     * Get the height of the element
     * 
     * @return Height in pixels
     */
    public int getHeight() {
        return bounds.height();
    }
    
    /**
     * Get the center X coordinate of the element
     * 
     * @return Center X coordinate
     */
    public int getCenterX() {
        return bounds.centerX();
    }
    
    /**
     * Get the center Y coordinate of the element
     * 
     * @return Center Y coordinate
     */
    public int getCenterY() {
        return bounds.centerY();
    }
    
    /**
     * Get an attribute value
     * 
     * @param key Attribute key
     * @return Attribute value or null if not found
     */
    @Nullable
    public Object getAttribute(String key) {
        return attributes.get(key);
    }
    
    /**
     * Get an attribute value with a default
     * 
     * @param key Attribute key
     * @param defaultValue Default value if attribute not found
     * @param <T> Expected attribute type
     * @return Attribute value or default if not found
     */
    @SuppressWarnings("unchecked")
    public <T> T getAttribute(String key, T defaultValue) {
        Object value = attributes.get(key);
        if (value == null) {
            return defaultValue;
        }
        
        try {
            return (T) value;
        } catch (ClassCastException e) {
            return defaultValue;
        }
    }
    
    /**
     * Check if the element has an attribute
     * 
     * @param key Attribute key
     * @return Whether the element has the attribute
     */
    public boolean hasAttribute(String key) {
        return attributes.containsKey(key);
    }
    
    /**
     * Check if the element contains the given point
     * 
     * @param x X coordinate
     * @param y Y coordinate
     * @return Whether the element contains the point
     */
    public boolean containsPoint(int x, int y) {
        return bounds.contains(x, y);
    }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        StandardizedUIElement that = (StandardizedUIElement) o;
        return clickable == that.clickable &&
                focusable == that.focusable &&
                visible == that.visible &&
                enabled == that.enabled &&
                Float.compare(that.confidence, confidence) == 0 &&
                elementId.equals(that.elementId) &&
                elementType == that.elementType &&
                bounds.equals(that.bounds) &&
                text.equals(that.text) &&
                contentDescription.equals(that.contentDescription);
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(elementId, elementType, bounds, text, contentDescription,
                clickable, focusable, visible, enabled, confidence);
    }
    
    @Override
    public String toString() {
        return "StandardizedUIElement{" +
                "id='" + elementId + '\'' +
                ", type=" + elementType +
                ", text='" + text + '\'' +
                ", bounds=" + bounds +
                ", confidence=" + confidence +
                '}';
    }
    
    /**
     * Builder for StandardizedUIElement
     */
    public static class Builder {
        private String elementId;
        private StandardizedUIElementType elementType;
        private Rect bounds;
        private String text;
        private String contentDescription;
        private boolean clickable = true;
        private boolean focusable = true;
        private boolean visible = true;
        private boolean enabled = true;
        private float confidence = 1.0f;
        private Map<String, Object> attributes = new HashMap<>();
        private long timestamp = System.currentTimeMillis();
        
        /**
         * Create a builder with required parameters
         * 
         * @param elementType Type of UI element
         * @param bounds Bounds of the element on screen
         */
        public Builder(@NonNull StandardizedUIElementType elementType, @NonNull Rect bounds) {
            this.elementId = UUID.randomUUID().toString();
            this.elementType = elementType;
            this.bounds = new Rect(bounds);
            this.text = "";
            this.contentDescription = "";
        }
        
        /**
         * Create a builder with an existing element ID
         * 
         * @param elementId Element ID
         * @param elementType Type of UI element
         * @param bounds Bounds of the element on screen
         */
        public Builder(@NonNull String elementId, @NonNull StandardizedUIElementType elementType, @NonNull Rect bounds) {
            this.elementId = elementId;
            this.elementType = elementType;
            this.bounds = new Rect(bounds);
            this.text = "";
            this.contentDescription = "";
        }
        
        /**
         * Set the element ID
         * 
         * @param elementId Element ID
         * @return This builder
         */
        @NonNull
        public Builder setElementId(@NonNull String elementId) {
            this.elementId = elementId;
            return this;
        }
        
        /**
         * Set the element type
         * 
         * @param elementType Element type
         * @return This builder
         */
        @NonNull
        public Builder setElementType(@NonNull StandardizedUIElementType elementType) {
            this.elementType = elementType;
            return this;
        }
        
        /**
         * Set the element bounds
         * 
         * @param bounds Element bounds
         * @return This builder
         */
        @NonNull
        public Builder setBounds(@NonNull Rect bounds) {
            this.bounds = new Rect(bounds);
            return this;
        }
        
        /**
         * Set the element bounds using coordinates
         * 
         * @param left Left coordinate
         * @param top Top coordinate
         * @param right Right coordinate
         * @param bottom Bottom coordinate
         * @return This builder
         */
        @NonNull
        public Builder setBounds(int left, int top, int right, int bottom) {
            this.bounds = new Rect(left, top, right, bottom);
            return this;
        }
        
        /**
         * Set the element text
         * 
         * @param text Element text
         * @return This builder
         */
        @NonNull
        public Builder setText(@Nullable String text) {
            this.text = text != null ? text : "";
            return this;
        }
        
        /**
         * Set the element content description
         * 
         * @param contentDescription Element content description
         * @return This builder
         */
        @NonNull
        public Builder setContentDescription(@Nullable String contentDescription) {
            this.contentDescription = contentDescription != null ? contentDescription : "";
            return this;
        }
        
        /**
         * Set whether the element is clickable
         * 
         * @param clickable Whether the element is clickable
         * @return This builder
         */
        @NonNull
        public Builder setClickable(boolean clickable) {
            this.clickable = clickable;
            return this;
        }
        
        /**
         * Set whether the element is focusable
         * 
         * @param focusable Whether the element is focusable
         * @return This builder
         */
        @NonNull
        public Builder setFocusable(boolean focusable) {
            this.focusable = focusable;
            return this;
        }
        
        /**
         * Set whether the element is visible
         * 
         * @param visible Whether the element is visible
         * @return This builder
         */
        @NonNull
        public Builder setVisible(boolean visible) {
            this.visible = visible;
            return this;
        }
        
        /**
         * Set whether the element is enabled
         * 
         * @param enabled Whether the element is enabled
         * @return This builder
         */
        @NonNull
        public Builder setEnabled(boolean enabled) {
            this.enabled = enabled;
            return this;
        }
        
        /**
         * Set the confidence score for detected elements
         * 
         * @param confidence Confidence score (0.0-1.0)
         * @return This builder
         */
        @NonNull
        public Builder setConfidence(float confidence) {
            this.confidence = Math.max(0.0f, Math.min(1.0f, confidence));
            return this;
        }
        
        /**
         * Add an attribute
         * 
         * @param key Attribute key
         * @param value Attribute value
         * @return This builder
         */
        @NonNull
        public Builder addAttribute(@NonNull String key, @Nullable Object value) {
            this.attributes.put(key, value);
            return this;
        }
        
        /**
         * Add multiple attributes
         * 
         * @param attributes Attributes to add
         * @return This builder
         */
        @NonNull
        public Builder addAttributes(@Nullable Map<String, Object> attributes) {
            if (attributes != null) {
                this.attributes.putAll(attributes);
            }
            return this;
        }
        
        /**
         * Set the timestamp
         * 
         * @param timestamp Timestamp
         * @return This builder
         */
        @NonNull
        public Builder setTimestamp(long timestamp) {
            this.timestamp = timestamp;
            return this;
        }
        
        /**
         * Build the StandardizedUIElement
         * 
         * @return StandardizedUIElement
         */
        @NonNull
        public StandardizedUIElement build() {
            return new StandardizedUIElement(
                    elementId,
                    elementType,
                    bounds,
                    text,
                    contentDescription,
                    clickable,
                    focusable,
                    visible,
                    enabled,
                    confidence,
                    attributes,
                    timestamp
            );
        }
    }
}