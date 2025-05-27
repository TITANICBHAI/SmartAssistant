package utils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Constants used for NLP integration
 * Provides standardized constants for natural language processing
 */
public class NLPIntegrationHelperConstants {
    
    // Intent types - what the user wants to do
    public static final String INTENT_UNKNOWN = "unknown";
    public static final String INTENT_CLICK = "click";
    public static final String INTENT_SWIPE = "swipe";
    public static final String INTENT_TYPE = "type";
    public static final String INTENT_SCROLL = "scroll";
    public static final String INTENT_NAVIGATE = "navigate";
    public static final String INTENT_SEARCH = "search";
    public static final String INTENT_SELECT = "select";
    public static final String INTENT_OPEN = "open";
    public static final String INTENT_CLOSE = "close";
    public static final String INTENT_SAVE = "save";
    public static final String INTENT_DELETE = "delete";
    public static final String INTENT_SHARE = "share";
    public static final String INTENT_TAKE = "take"; // e.g., take photo
    public static final String INTENT_PLAY = "play";
    public static final String INTENT_PAUSE = "pause";
    public static final String INTENT_STOP = "stop";
    public static final String INTENT_LONG_PRESS = "long_press";
    public static final String INTENT_BACK = "back";
    public static final String INTENT_HOME = "home";
    public static final String INTENT_ANALYZE = "analyze";
    
    // Entity types - what the user is referring to
    public static final String ENTITY_BUTTON = "button";
    public static final String ENTITY_TEXT = "text";
    public static final String ENTITY_IMAGE = "image";
    public static final String ENTITY_INPUT = "input";
    public static final String ENTITY_ELEMENT = "element";
    public static final String ENTITY_SCREEN = "screen";
    public static final String ENTITY_MENU = "menu";
    public static final String ENTITY_ITEM = "item";
    public static final String ENTITY_LINK = "link";
    public static final String ENTITY_FILE = "file";
    public static final String ENTITY_LOCATION = "location";
    public static final String ENTITY_TIME = "time";
    public static final String ENTITY_DATE = "date";
    public static final String ENTITY_PERSON = "person";
    public static final String ENTITY_ORGANIZATION = "organization";
    
    // Direction entities
    public static final String ENTITY_DIRECTION = "direction";
    public static final String DIRECTION_UP = "up";
    public static final String DIRECTION_DOWN = "down";
    public static final String DIRECTION_LEFT = "left";
    public static final String DIRECTION_RIGHT = "right";
    
    // Confidence thresholds
    public static final float CONFIDENCE_HIGH = 0.8f;
    public static final float CONFIDENCE_MEDIUM = 0.5f;
    public static final float CONFIDENCE_LOW = 0.3f;
    
    // Action types
    public static final String ACTION_TYPE_UI = "ui_action";
    public static final String ACTION_TYPE_SYSTEM = "system_action";
    public static final String ACTION_TYPE_DATA = "data_action";
    public static final String ACTION_TYPE_NAVIGATION = "navigation_action";
    
    // Error codes
    public static final int ERROR_INVALID_INPUT = 1001;
    public static final int ERROR_UNSUPPORTED_ACTION = 1002;
    public static final int ERROR_ELEMENT_NOT_FOUND = 1003;
    public static final int ERROR_ELEMENT_DISABLED = 1004;
    public static final int ERROR_AMBIGUOUS_REFERENCE = 1005;
    public static final int ERROR_PERMISSION_DENIED = 1006;
    public static final int ERROR_TIMEOUT = 1007;
    public static final int ERROR_INTERNAL = 1008;
    public static final int ERROR_CONNECTION = 1009;
    
    // Status messages
    public static final String STATUS_INITIALIZING = "Initializing";
    public static final String STATUS_PROCESSING = "Processing";
    public static final String STATUS_ANALYZING = "Analyzing";
    public static final String STATUS_EXECUTING = "Executing";
    public static final String STATUS_COMPLETED = "Completed";
    public static final String STATUS_FAILED = "Failed";
    
    // Language processing modes
    public static final String MODE_STRICT = "strict";
    public static final String MODE_RELAXED = "relaxed";
    public static final String MODE_ADAPTIVE = "adaptive";
    
    // Feature flags
    public static final String FEATURE_MULTI_STEP = "multi_step_actions";
    public static final String FEATURE_CONTEXT_AWARE = "context_awareness";
    public static final String FEATURE_DISAMBIGUATION = "disambiguation";
    public static final String FEATURE_LEARNING = "adaptive_learning";
    public static final String FEATURE_OFFLINE = "offline_processing";
    
    private NLPIntegrationHelperConstants() {
        // Private constructor to prevent instantiation
    }
    
    /**
     * Class for processed text results
     * Contains the extracted intent, entities, and confidence from NLP processing
     */
    public static class ProcessedText {
        private String intent;
        private List<Map<String, Object>> entities;
        private float confidence;
        private String rawText;
        private Map<String, Object> metadata;
        
        /**
         * Default constructor
         */
        public ProcessedText() {
            this.entities = new ArrayList<>();
            this.metadata = new HashMap<>();
            this.confidence = 0.0f;
            this.intent = INTENT_UNKNOWN;
        }
        
        /**
         * Get the intent
         * 
         * @return Intent string
         */
        public String getIntent() {
            return intent;
        }
        
        /**
         * Set the intent
         * 
         * @param intent Intent string
         */
        public void setIntent(String intent) {
            this.intent = intent;
        }
        
        /**
         * Get entities
         * 
         * @return List of entity maps
         */
        public List<Map<String, Object>> getEntities() {
            return entities;
        }
        
        /**
         * Set entities
         * 
         * @param entities List of entity maps
         */
        public void setEntities(List<Map<String, Object>> entities) {
            this.entities = entities != null ? entities : new ArrayList<>();
        }
        
        /**
         * Add an entity
         * 
         * @param type Entity type
         * @param value Entity value
         * @param confidence Entity confidence (0.0-1.0)
         */
        public void addEntity(String type, String value, float confidence) {
            Map<String, Object> entity = new HashMap<>();
            entity.put("type", type);
            entity.put("value", value);
            entity.put("confidence", confidence);
            entities.add(entity);
        }
        
        /**
         * Add an entity
         * 
         * @param entity Entity map
         */
        public void addEntity(Map<String, Object> entity) {
            if (entity != null) {
                entities.add(entity);
            }
        }
        
        /**
         * Get confidence score
         * 
         * @return Confidence score (0.0-1.0)
         */
        public float getConfidence() {
            return confidence;
        }
        
        /**
         * Set confidence score
         * 
         * @param confidence Confidence score (0.0-1.0)
         */
        public void setConfidence(float confidence) {
            this.confidence = Math.max(0.0f, Math.min(1.0f, confidence));
        }
        
        /**
         * Get raw text
         * 
         * @return Raw text
         */
        public String getRawText() {
            return rawText;
        }
        
        /**
         * Set raw text
         * 
         * @param rawText Raw text
         */
        public void setRawText(String rawText) {
            this.rawText = rawText;
        }
        
        /**
         * Get metadata
         * 
         * @return Metadata map
         */
        public Map<String, Object> getMetadata() {
            return metadata;
        }
        
        /**
         * Set metadata
         * 
         * @param metadata Metadata map
         */
        public void setMetadata(Map<String, Object> metadata) {
            this.metadata = metadata != null ? metadata : new HashMap<>();
        }
        
        /**
         * Get a metadata value
         * 
         * @param key Metadata key
         * @return Value or null
         */
        public Object getMetadataValue(String key) {
            return metadata.get(key);
        }
        
        /**
         * Set a metadata value
         * 
         * @param key Metadata key
         * @param value Metadata value
         */
        public void setMetadataValue(String key, Object value) {
            if (key != null) {
                if (value != null) {
                    metadata.put(key, value);
                } else {
                    metadata.remove(key);
                }
            }
        }
        
        /**
         * Check if this result has a specific intent
         * 
         * @param intentToCheck Intent to check
         * @return True if intent matches
         */
        public boolean hasIntent(String intentToCheck) {
            return intentToCheck != null && intentToCheck.equals(intent);
        }
        
        /**
         * Check if this result has a specific entity type
         * 
         * @param entityType Entity type to check
         * @return True if entity type is present
         */
        public boolean hasEntityType(String entityType) {
            if (entityType == null || entities.isEmpty()) {
                return false;
            }
            
            for (Map<String, Object> entity : entities) {
                if (entityType.equals(entity.get("type"))) {
                    return true;
                }
            }
            
            return false;
        }
        
        /**
         * Get entities of a specific type
         * 
         * @param entityType Entity type
         * @return List of matching entities
         */
        public List<Map<String, Object>> getEntitiesByType(String entityType) {
            List<Map<String, Object>> result = new ArrayList<>();
            
            if (entityType == null || entities.isEmpty()) {
                return result;
            }
            
            for (Map<String, Object> entity : entities) {
                if (entityType.equals(entity.get("type"))) {
                    result.add(entity);
                }
            }
            
            return result;
        }
        
        /**
         * Convert to a simple map representation
         * 
         * @return Map representation
         */
        public Map<String, Object> toMap() {
            Map<String, Object> result = new HashMap<>();
            result.put("intent", intent);
            result.put("entities", entities);
            result.put("confidence", confidence);
            if (rawText != null) {
                result.put("rawText", rawText);
            }
            if (!metadata.isEmpty()) {
                result.put("metadata", metadata);
            }
            return result;
        }
        
        @Override
        public String toString() {
            return "ProcessedText{" +
                    "intent='" + intent + '\'' +
                    ", entities=" + entities +
                    ", confidence=" + confidence +
                    ", rawText='" + rawText + '\'' +
                    ", metadata=" + metadata +
                    '}';
        }
    }
}