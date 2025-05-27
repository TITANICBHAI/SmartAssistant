package com.aiassistant.learning;

import android.content.Context;
import android.util.Log;
import android.view.accessibility.AccessibilityNodeInfo;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Main learning manager for the AI
 * This class handles learning patterns from user interactions and content
 */
public class LearningManager {
    private static final String TAG = "LearningManager";
    
    // Singleton instance
    private static LearningManager instance;
    
    // Data directory
    private File dataDir;
    
    // Pattern storage
    private Map<String, List<ActionPattern>> appActionPatterns;
    private Map<String, List<SequencePattern>> actionSequences;
    private Map<String, Map<String, Float>> contextScores;
    private Map<String, ConceptModel> conceptModels;
    
    // Context variables
    private Map<String, Object> globalContext;
    
    // Learning rates
    private float patternLearningRate = 0.1f;
    private float conceptLearningRate = 0.05f;
    
    // Minimum confidence threshold for suggestions
    private float minimumConfidence = 0.6f;
    
    /**
     * Get singleton instance
     */
    public static synchronized LearningManager getInstance(Context context) {
        if (instance == null) {
            instance = new LearningManager(context);
        }
        return instance;
    }
    
    /**
     * Private constructor for singleton
     */
    private LearningManager(Context context) {
        Log.d(TAG, "Initializing LearningManager");
        
        // Set up data directory
        dataDir = new File(context.getFilesDir(), "learning_data");
        if (!dataDir.exists()) {
            dataDir.mkdirs();
        }
        
        // Initialize storage
        appActionPatterns = new ConcurrentHashMap<>();
        actionSequences = new ConcurrentHashMap<>();
        contextScores = new ConcurrentHashMap<>();
        conceptModels = new ConcurrentHashMap<>();
        
        // Initialize context
        globalContext = new ConcurrentHashMap<>();
        
        // Load saved data
        loadData();
    }
    
    /**
     * Learn from user action in app
     */
    public void learnFromUserAction(String actionType, Map<String, Object> context, AccessibilityNodeInfo node) {
        if (context == null || !context.containsKey("package_name")) {
            Log.e(TAG, "Cannot learn from action without package name");
            return;
        }
        
        String packageName = context.get("package_name").toString();
        
        // Create or get app patterns
        List<ActionPattern> patterns = appActionPatterns.computeIfAbsent(packageName, k -> new ArrayList<>());
        
        // Check if this action matches existing patterns
        boolean patternUpdated = false;
        
        for (ActionPattern pattern : patterns) {
            float similarity = pattern.calculateSimilarity(actionType, context);
            
            if (similarity > 0.8f) {
                // Update existing pattern
                pattern.update(actionType, context, patternLearningRate);
                patternUpdated = true;
                break;
            }
        }
        
        // Create new pattern if needed
        if (!patternUpdated) {
            patterns.add(new ActionPattern(actionType, context));
        }
        
        // Learn sequences if we have previous actions
        learnSequences(packageName, actionType, context);
        
        // Extract concepts from node and context
        learnConcepts(packageName, actionType, context, node);
        
        // Update context scores
        updateContextScores(packageName, context);
        
        // Save data periodically
        saveDataAsync();
    }
    
    /**
     * Process YouTube content for learning
     */
    public void processYoutubeContent(String videoId, String title, String content) {
        Log.d(TAG, "Processing YouTube content: " + title);
        
        // Extract concepts from the title and content
        List<String> concepts = extractConcepts(title + " " + content);
        
        // Create or update concept models
        for (String concept : concepts) {
            ConceptModel model = conceptModels.computeIfAbsent(concept, k -> new ConceptModel(concept));
            model.addReference(videoId, title, 1.0f);
        }
        
        // Extract potential action sequences from content
        List<SequencePattern> sequences = extractSequencesFromContent(content);
        
        // Store sequences
        String youtubeDomain = "youtube_" + videoId;
        actionSequences.put(youtubeDomain, sequences);
        
        // Save data
        saveDataAsync();
    }
    
    /**
     * Get suggested actions for a package
     */
    public List<ActionSuggestion> getSuggestedActions(String packageName, Map<String, Object> currentContext) {
        List<ActionSuggestion> suggestions = new ArrayList<>();
        
        // Get patterns for this package
        List<ActionPattern> patterns = appActionPatterns.getOrDefault(packageName, new ArrayList<>());
        
        // Find matching patterns
        for (ActionPattern pattern : patterns) {
            float contextMatch = pattern.calculateContextMatch(currentContext);
            
            if (contextMatch > minimumConfidence) {
                suggestions.add(new ActionSuggestion(
                        pattern.getActionType(),
                        pattern.generateParameters(currentContext),
                        contextMatch,
                        pattern.getLastUsed(),
                        pattern.getSuccessRate()
                ));
            }
        }
        
        // Sort by confidence
        suggestions.sort((a, b) -> Float.compare(b.getConfidence(), a.getConfidence()));
        
        return suggestions;
    }
    
    /**
     * Get suggested action sequences
     */
    public List<SequencePattern> getSuggestedSequences(String packageName, Map<String, Object> currentContext) {
        List<SequencePattern> suggestions = new ArrayList<>();
        
        // Get sequences for this package
        List<SequencePattern> sequences = actionSequences.getOrDefault(packageName, new ArrayList<>());
        
        // Find matching sequences
        for (SequencePattern sequence : sequences) {
            float contextMatch = sequence.calculateContextMatch(currentContext);
            
            if (contextMatch > minimumConfidence) {
                suggestions.add(sequence);
            }
        }
        
        // Sort by confidence
        suggestions.sort((a, b) -> Float.compare(b.getConfidence(), a.getConfidence()));
        
        return suggestions;
    }
    
    /**
     * Update global context
     */
    public void updateGlobalContext(String key, Object value) {
        globalContext.put(key, value);
    }
    
    /**
     * Get global context
     */
    public Map<String, Object> getGlobalContext() {
        return new HashMap<>(globalContext);
    }
    
    /**
     * Set minimum confidence threshold
     */
    public void setMinimumConfidence(float confidence) {
        this.minimumConfidence = Math.max(0.1f, Math.min(0.9f, confidence));
    }
    
    /**
     * Set pattern learning rate
     */
    public void setPatternLearningRate(float rate) {
        this.patternLearningRate = Math.max(0.01f, Math.min(0.5f, rate));
    }
    
    /**
     * Set concept learning rate
     */
    public void setConceptLearningRate(float rate) {
        this.conceptLearningRate = Math.max(0.01f, Math.min(0.5f, rate));
    }
    
    /**
     * Learn action sequences
     */
    private void learnSequences(String packageName, String actionType, Map<String, Object> context) {
        // Get sequences for this package
        List<SequencePattern> sequences = actionSequences.computeIfAbsent(packageName, k -> new ArrayList<>());
        
        // Check if this action matches the next step in any sequence
        boolean sequenceUpdated = false;
        
        for (SequencePattern sequence : sequences) {
            if (sequence.isIncomplete()) {
                float match = sequence.matchNextAction(actionType, context);
                
                if (match > 0.7f) {
                    // Update existing sequence
                    sequence.addAction(actionType, context);
                    sequenceUpdated = true;
                    break;
                }
            }
        }
        
        // Create new sequence if needed
        if (!sequenceUpdated) {
            SequencePattern newSequence = new SequencePattern(packageName);
            newSequence.addAction(actionType, context);
            sequences.add(newSequence);
        }
    }
    
    /**
     * Learn concepts from node and context
     */
    private void learnConcepts(String packageName, String actionType, Map<String, Object> context, AccessibilityNodeInfo node) {
        if (node == null) return;
        
        // Extract text from node
        String nodeText = node.getText() != null ? node.getText().toString() : "";
        String nodeDescription = node.getContentDescription() != null ? 
                               node.getContentDescription().toString() : "";
        
        // Combine texts
        String combinedText = nodeText + " " + nodeDescription;
        
        // Extract concepts
        List<String> concepts = extractConcepts(combinedText);
        
        // Update concept models
        for (String concept : concepts) {
            ConceptModel model = conceptModels.computeIfAbsent(concept, k -> new ConceptModel(concept));
            model.addReference(packageName, actionType, conceptLearningRate);
            model.updateContext(context);
        }
    }
    
    /**
     * Extract concepts from text
     */
    private List<String> extractConcepts(String text) {
        List<String> concepts = new ArrayList<>();
        
        // Simple keyword extraction (real implementation would use NLP)
        // This is a very simplified implementation
        if (text == null || text.isEmpty()) {
            return concepts;
        }
        
        // Split by common delimiters
        String[] words = text.split("[ ,.;:!?\\n\\t\\r]+");
        
        // Filter out common words and short words
        for (String word : words) {
            if (word.length() > 3 && !isCommonWord(word)) {
                concepts.add(word.toLowerCase());
            }
        }
        
        return concepts;
    }
    
    /**
     * Check if a word is common (stop word)
     */
    private boolean isCommonWord(String word) {
        // List of common English stop words
        String[] stopWords = {
            "the", "and", "that", "have", "for", "not", "with", "you", "this",
            "but", "his", "from", "they", "say", "her", "she", "will", "one",
            "all", "would", "there", "their", "what", "out", "about", "who",
            "get", "which", "when", "make", "can", "like", "time", "just", "him"
        };
        
        word = word.toLowerCase();
        
        for (String stopWord : stopWords) {
            if (word.equals(stopWord)) {
                return true;
            }
        }
        
        return false;
    }
    
    /**
     * Extract action sequences from content
     */
    private List<SequencePattern> extractSequencesFromContent(String content) {
        List<SequencePattern> sequences = new ArrayList<>();
        
        // Real implementation would use NLP to identify steps and sequences
        // This is a simplified placeholder
        
        // Just create a placeholder sequence
        SequencePattern sequence = new SequencePattern("youtube_content");
        sequences.add(sequence);
        
        return sequences;
    }
    
    /**
     * Update context scores based on user action
     */
    private void updateContextScores(String packageName, Map<String, Object> context) {
        Map<String, Float> scores = contextScores.computeIfAbsent(packageName, k -> new HashMap<>());
        
        // Update scores for context keys
        for (String key : context.keySet()) {
            float currentScore = scores.getOrDefault(key, 0.0f);
            // Increase the score
            scores.put(key, Math.min(1.0f, currentScore + 0.1f));
        }
        
        // Decay scores for unused context keys
        for (String key : new ArrayList<>(scores.keySet())) {
            if (!context.containsKey(key)) {
                float currentScore = scores.get(key);
                // Decay the score
                float newScore = currentScore * 0.95f;
                if (newScore < 0.01f) {
                    scores.remove(key);
                } else {
                    scores.put(key, newScore);
                }
            }
        }
    }
    
    /**
     * Save data asynchronously
     */
    private void saveDataAsync() {
        new Thread(this::saveData).start();
    }
    
    /**
     * Save all learning data
     */
    private synchronized void saveData() {
        try {
            // Save action patterns
            File patternsFile = new File(dataDir, "action_patterns.dat");
            try (ObjectOutputStream out = new ObjectOutputStream(new FileOutputStream(patternsFile))) {
                out.writeObject(appActionPatterns);
            }
            
            // Save sequence patterns
            File sequencesFile = new File(dataDir, "action_sequences.dat");
            try (ObjectOutputStream out = new ObjectOutputStream(new FileOutputStream(sequencesFile))) {
                out.writeObject(actionSequences);
            }
            
            // Save context scores
            File scoresFile = new File(dataDir, "context_scores.dat");
            try (ObjectOutputStream out = new ObjectOutputStream(new FileOutputStream(scoresFile))) {
                out.writeObject(contextScores);
            }
            
            // Save concept models
            File conceptsFile = new File(dataDir, "concept_models.dat");
            try (ObjectOutputStream out = new ObjectOutputStream(new FileOutputStream(conceptsFile))) {
                out.writeObject(conceptModels);
            }
            
            Log.d(TAG, "Learning data saved successfully");
        } catch (Exception e) {
            Log.e(TAG, "Error saving learning data: " + e.getMessage());
        }
    }
    
    /**
     * Load all learning data
     */
    @SuppressWarnings("unchecked")
    private synchronized void loadData() {
        try {
            // Load action patterns
            File patternsFile = new File(dataDir, "action_patterns.dat");
            if (patternsFile.exists()) {
                try (ObjectInputStream in = new ObjectInputStream(new FileInputStream(patternsFile))) {
                    appActionPatterns = (Map<String, List<ActionPattern>>) in.readObject();
                }
            }
            
            // Load sequence patterns
            File sequencesFile = new File(dataDir, "action_sequences.dat");
            if (sequencesFile.exists()) {
                try (ObjectInputStream in = new ObjectInputStream(new FileInputStream(sequencesFile))) {
                    actionSequences = (Map<String, List<SequencePattern>>) in.readObject();
                }
            }
            
            // Load context scores
            File scoresFile = new File(dataDir, "context_scores.dat");
            if (scoresFile.exists()) {
                try (ObjectInputStream in = new ObjectInputStream(new FileInputStream(scoresFile))) {
                    contextScores = (Map<String, Map<String, Float>>) in.readObject();
                }
            }
            
            // Load concept models
            File conceptsFile = new File(dataDir, "concept_models.dat");
            if (conceptsFile.exists()) {
                try (ObjectInputStream in = new ObjectInputStream(new FileInputStream(conceptsFile))) {
                    conceptModels = (Map<String, ConceptModel>) in.readObject();
                }
            }
            
            Log.d(TAG, "Learning data loaded successfully");
        } catch (Exception e) {
            Log.e(TAG, "Error loading learning data: " + e.getMessage());
            
            // Initialize empty data if loading fails
            appActionPatterns = new ConcurrentHashMap<>();
            actionSequences = new ConcurrentHashMap<>();
            contextScores = new ConcurrentHashMap<>();
            conceptModels = new ConcurrentHashMap<>();
        }
    }
}