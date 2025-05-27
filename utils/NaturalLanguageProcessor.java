package utils;

import java.util.List;
import java.util.Map;

/**
 * Interface for natural language processing capabilities.
 */
public interface NaturalLanguageProcessor {
    // Intent constants
    String INTENT_CLICK = "click";
    String INTENT_SWIPE = "swipe";
    String INTENT_TYPE = "type";
    String INTENT_SEARCH = "search";
    String INTENT_NAVIGATE = "navigate";
    String INTENT_HELP = "help";
    
    // Entity constants
    String ENTITY_BUTTON = "button";
    String ENTITY_ELEMENT = "element";
    String ENTITY_TEXT = "text";
    String ENTITY_DIRECTION = "direction";
    String ENTITY_LOCATION = "location";
    String ENTITY_NUMBER = "number";
    String ENTITY_DATE = "date";
    String ENTITY_TIME = "time";
    
    /**
     * Get a singleton instance of the NLP processor.
     * 
     * @return NLP processor instance
     */
    static NaturalLanguageProcessor getInstance() {
        return new DefaultNaturalLanguageProcessor();
    }
    
    /**
     * Get a singleton instance with context.
     * 
     * @param context Application context
     * @return NLP processor instance
     */
    static NaturalLanguageProcessor getInstance(Context context) {
        return new DefaultNaturalLanguageProcessor(context);
    }
    
    /**
     * Process text to extract intents and entities.
     * 
     * @param text Text to process
     * @return Processed result
     */
    ProcessedText processText(String text);
    
    /**
     * Extract keywords from text.
     * 
     * @param text Text to analyze
     * @return List of keywords
     */
    List<String> extractKeywords(String text);
    
    /**
     * Classify text into categories.
     * 
     * @param text Text to classify
     * @return Map of category -> confidence
     */
    Map<String, Float> classifyText(String text);
    
    /**
     * Check if text contains a specific intent.
     * 
     * @param text Text to check
     * @param intent Intent to look for
     * @return True if intent is present
     */
    boolean hasIntent(String text, String intent);
    
    /**
     * Extract entities from text.
     * 
     * @param text Text to analyze
     * @return Map of entity type -> entity value
     */
    Map<String, List<String>> extractEntities(String text);
    
    /**
     * Generate a response to user text.
     * 
     * @param userText User input text
     * @param context Conversation context
     * @return Generated response
     */
    String generateResponse(String userText, Map<String, Object> context);
    
    /**
     * Container for processed text results.
     */
    public static class ProcessedText {
        private String originalText;
        private Map<String, Float> intents;
        private Map<String, List<String>> entities;
        private String primaryIntent;
        private float intentConfidence;
        
        /**
         * Default constructor.
         */
        public ProcessedText() {
        }
        
        /**
         * Get the original text.
         * 
         * @return Original text
         */
        public String getOriginalText() {
            return originalText;
        }
        
        /**
         * Set the original text.
         * 
         * @param originalText Original text
         */
        public void setOriginalText(String originalText) {
            this.originalText = originalText;
        }
        
        /**
         * Get detected intents.
         * 
         * @return Map of intent -> confidence
         */
        public Map<String, Float> getIntents() {
            return intents;
        }
        
        /**
         * Set detected intents.
         * 
         * @param intents Map of intent -> confidence
         */
        public void setIntents(Map<String, Float> intents) {
            this.intents = intents;
        }
        
        /**
         * Get extracted entities.
         * 
         * @return Map of entity type -> entity values
         */
        public Map<String, List<String>> getEntities() {
            return entities;
        }
        
        /**
         * Set extracted entities.
         * 
         * @param entities Map of entity type -> entity values
         */
        public void setEntities(Map<String, List<String>> entities) {
            this.entities = entities;
        }
        
        /**
         * Get primary intent.
         * 
         * @return Primary intent
         */
        public String getPrimaryIntent() {
            return primaryIntent;
        }
        
        /**
         * Get the primary intent (alias for getPrimaryIntent for backward compatibility).
         * 
         * @return Primary intent
         */
        public String getIntent() {
            return getPrimaryIntent();
        }
        
        /**
         * Set primary intent.
         * 
         * @param primaryIntent Primary intent
         */
        public void setPrimaryIntent(String primaryIntent) {
            this.primaryIntent = primaryIntent;
        }
        
        /**
         * Get confidence in primary intent.
         * 
         * @return Confidence (0.0-1.0)
         */
        public float getIntentConfidence() {
            return intentConfidence;
        }
        
        /**
         * Set confidence in primary intent.
         * 
         * @param intentConfidence Confidence (0.0-1.0)
         */
        public void setIntentConfidence(float intentConfidence) {
            this.intentConfidence = intentConfidence;
        }
    }
    
    /**
     * Default implementation of NaturalLanguageProcessor.
     */
    public static class DefaultNaturalLanguageProcessor implements NaturalLanguageProcessor {
        private Context context;
        
        /**
         * Default constructor.
         */
        public DefaultNaturalLanguageProcessor() {
        }
        
        /**
         * Constructor with context.
         * 
         * @param context Application context
         */
        public DefaultNaturalLanguageProcessor(Context context) {
            this.context = context;
        }
        
        @Override
        public ProcessedText processText(String text) {
            // Implementation would go here
            return new ProcessedText();
        }
        
        @Override
        public List<String> extractKeywords(String text) {
            // Implementation would go here
            return new java.util.ArrayList<>();
        }
        
        @Override
        public Map<String, Float> classifyText(String text) {
            // Implementation would go here
            return new java.util.HashMap<>();
        }
        
        @Override
        public boolean hasIntent(String text, String intent) {
            // Implementation would go here
            return false;
        }
        
        @Override
        public Map<String, List<String>> extractEntities(String text) {
            // Implementation would go here
            return new java.util.HashMap<>();
        }
        
        @Override
        public String generateResponse(String userText, Map<String, Object> context) {
            // Implementation would go here
            return "";
        }
    }
}