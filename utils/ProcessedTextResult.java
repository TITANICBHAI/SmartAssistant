package utils;

import java.util.HashMap;
import java.util.Map;
import java.util.List;
import java.util.ArrayList;

/**
 * Class representing the result of natural language processing on text input.
 * Contains information about the intent, entities, and sentiment of the processed text.
 */
public class ProcessedTextResult {
    private String originalText;
    private String intent;
    private Map<String, Object> entities;
    private String sentiment;
    private float confidence;
    private Map<String, Object> metadata;

    /**
     * Create a new ProcessedText
     * @param originalText The original text input
     */
    public ProcessedTextResult(String originalText) {
        this.originalText = originalText;
        this.entities = new HashMap<>();
        this.metadata = new HashMap<>();
        this.intent = "unknown";
        this.sentiment = "neutral";
        this.confidence = 0.0f;
    }

    /**
     * Create a new ProcessedText with intent and entities
     * @param originalText The original text input
     * @param intent The detected intent
     * @param entities The detected entities
     */
    public ProcessedTextResult(String originalText, String intent, Map<String, Object> entities) {
        this(originalText);
        this.intent = intent;
        if (entities != null) {
            this.entities.putAll(entities);
        }
    }

    /**
     * Get the original text
     * @return The original text
     */
    public String getOriginalText() {
        return originalText;
    }

    /**
     * Set the original text
     * @param originalText The original text
     */
    public void setOriginalText(String originalText) {
        this.originalText = originalText;
    }

    /**
     * Get the intent
     * @return The intent
     */
    public String getIntent() {
        return intent;
    }

    /**
     * Set the intent
     * @param intent The intent
     */
    public void setIntent(String intent) {
        this.intent = intent;
    }

    /**
     * Get the entities
     * @return The entities
     */
    public Map<String, Object> getEntities() {
        return new HashMap<>(entities);
    }

    /**
     * Add an entity
     * @param key The entity key
     * @param value The entity value
     */
    public void addEntity(String key, Object value) {
        this.entities.put(key, value);
    }

    /**
     * Get the sentiment
     * @return The sentiment
     */
    public String getSentiment() {
        return sentiment;
    }

    /**
     * Set the sentiment
     * @param sentiment The sentiment
     */
    public void setSentiment(String sentiment) {
        this.sentiment = sentiment;
    }

    /**
     * Get the confidence
     * @return The confidence
     */
    public float getConfidence() {
        return confidence;
    }

    /**
     * Set the confidence
     * @param confidence The confidence
     */
    public void setConfidence(float confidence) {
        this.confidence = confidence;
    }

    /**
     * Get metadata
     * @return The metadata
     */
    public Map<String, Object> getMetadata() {
        return new HashMap<>(metadata);
    }

    /**
     * Add metadata
     * @param key The metadata key
     * @param value The metadata value
     */
    public void addMetadata(String key, Object value) {
        this.metadata.put(key, value);
    }

    /**
     * Convert to map
     * @return Map representation
     */
    public Map<String, Object> toMap() {
        Map<String, Object> map = new HashMap<>();
        map.put("text", originalText);
        map.put("intent", intent);
        map.put("entities", new HashMap<>(entities));
        map.put("sentiment", sentiment);
        map.put("confidence", confidence);
        map.put("metadata", new HashMap<>(metadata));
        return map;
    }

    @Override
    public String toString() {
        return "ProcessedTextResult{" +
                "text='" + originalText + '\'' +
                ", intent='" + intent + '\'' +
                ", entities=" + entities +
                ", sentiment='" + sentiment + '\'' +
                ", confidence=" + confidence +
                '}';
    }
}