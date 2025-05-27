package utils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Helper class for NLP integration.
 */
public class NLPIntegrationHelper {
    private static NLPIntegrationHelper instance;
    private NaturalLanguageProcessor nlp;
    private Map<String, Object> contextData;
    
    /**
     * Private constructor for singleton pattern.
     */
    private NLPIntegrationHelper() {
        this.nlp = NaturalLanguageProcessor.getInstance();
        this.contextData = new HashMap<>();
    }
    
    /**
     * Get singleton instance.
     * 
     * @return NLPIntegrationHelper instance
     */
    public static synchronized NLPIntegrationHelper getInstance() {
        if (instance == null) {
            instance = new NLPIntegrationHelper();
        }
        return instance;
    }
    
    /**
     * Get singleton instance with context.
     * 
     * @param context Application context
     * @return NLPIntegrationHelper instance
     */
    public static synchronized NLPIntegrationHelper getInstance(Context context) {
        if (instance == null) {
            instance = new NLPIntegrationHelper();
            instance.nlp = NaturalLanguageProcessor.getInstance(context);
        }
        return instance;
    }
    
    /**
     * Process user input text.
     * 
     * @param text User input
     * @return Processed result
     */
    public NaturalLanguageProcessor.ProcessedText processUserInput(String text) {
        if (text == null || text.isEmpty()) {
            return new NaturalLanguageProcessor.ProcessedText();
        }
        
        // Process the text
        NaturalLanguageProcessor.ProcessedText result = nlp.processText(text);
        
        // Update context with the latest processed text
        contextData.put("last_text", text);
        contextData.put("last_intent", result.getPrimaryIntent());
        
        // Store entities in context
        if (result.getEntities() != null && !result.getEntities().isEmpty()) {
            for (Map.Entry<String, List<String>> entry : result.getEntities().entrySet()) {
                contextData.put("entity_" + entry.getKey(), entry.getValue());
            }
        }
        
        return result;
    }
    
    /**
     * Check if text has specific intent.
     * 
     * @param text Text to analyze
     * @param intent Intent to look for
     * @return True if intent is found
     */
    public boolean hasIntent(String text, String intent) {
        if (text == null || text.isEmpty() || intent == null || intent.isEmpty()) {
            return false;
        }
        return nlp.hasIntent(text, intent);
    }
    
    /**
     * Extract entities from text.
     * 
     * @param text Text to analyze
     * @return Map of entity types to values
     */
    public Map<String, List<String>> extractEntities(String text) {
        if (text == null || text.isEmpty()) {
            return new HashMap<>();
        }
        return nlp.extractEntities(text);
    }
    
    /**
     * Generate response to user input.
     * 
     * @param userText User input
     * @return Generated response
     */
    public String generateResponse(String userText) {
        if (userText == null || userText.isEmpty()) {
            return "";
        }
        return nlp.generateResponse(userText, contextData);
    }
    
    /**
     * Get the context data.
     * 
     * @return Map of context data
     */
    public Map<String, Object> getContextData() {
        return new HashMap<>(contextData);
    }
    
    /**
     * Add data to context.
     * 
     * @param key Context key
     * @param value Context value
     */
    public void addToContext(String key, Object value) {
        if (key != null && !key.isEmpty()) {
            contextData.put(key, value);
        }
    }
    
    /**
     * Clear the context data.
     */
    public void clearContext() {
        contextData.clear();
    }
    
    /**
     * Get the primary intent from text.
     * 
     * @param text Text to analyze
     * @return Primary intent
     */
    public String getPrimaryIntent(String text) {
        if (text == null || text.isEmpty()) {
            return "";
        }
        
        NaturalLanguageProcessor.ProcessedText processed = nlp.processText(text);
        return processed.getPrimaryIntent();
    }
    
    /**
     * Extract keywords from text.
     * 
     * @param text Text to analyze
     * @return List of keywords
     */
    public List<String> extractKeywords(String text) {
        if (text == null || text.isEmpty()) {
            return new ArrayList<>();
        }
        return nlp.extractKeywords(text);
    }
    
    /**
     * Classify text into categories.
     * 
     * @param text Text to classify
     * @return Map of category to confidence
     */
    public Map<String, Float> classifyText(String text) {
        if (text == null || text.isEmpty()) {
            return new HashMap<>();
        }
        return nlp.classifyText(text);
    }
    
    /**
     * Process a game-related command.
     * 
     * @param command Command text
     * @return Processed action or null if not recognized
     */
    public String processGameCommand(String command) {
        if (command == null || command.isEmpty()) {
            return null;
        }
        
        // Process the command
        NaturalLanguageProcessor.ProcessedText processed = nlp.processText(command);
        
        // If there's a recognized intent, convert it to an action
        if (processed.getPrimaryIntent() != null && !processed.getPrimaryIntent().isEmpty()) {
            switch (processed.getPrimaryIntent()) {
                case "move":
                    return "MOVE";
                case "attack":
                    return "ATTACK";
                case "defend":
                    return "DEFEND";
                case "use_item":
                    return "USE_ITEM";
                default:
                    return null;
            }
        }
        
        return null;
    }
    
    /**
     * Analyze pattern in text.
     * 
     * @param text Text to analyze
     * @return Analysis result
     */
    public Map<String, Object> analyzePattern(String text) {
        Map<String, Object> result = new HashMap<>();
        
        if (text == null || text.isEmpty()) {
            return result;
        }
        
        // Extract entities
        Map<String, List<String>> entities = nlp.extractEntities(text);
        result.put("entities", entities);
        
        // Classify text
        Map<String, Float> categories = nlp.classifyText(text);
        result.put("categories", categories);
        
        // Extract keywords
        List<String> keywords = nlp.extractKeywords(text);
        result.put("keywords", keywords);
        
        return result;
    }
    
    /**
     * Get the NLP processor.
     * 
     * @return NLP processor
     */
    public NaturalLanguageProcessor getNlp() {
        return nlp;
    }
    
    /**
     * Set the NLP processor.
     * 
     * @param nlp NLP processor
     */
    public void setNlp(NaturalLanguageProcessor nlp) {
        this.nlp = nlp;
    }
}