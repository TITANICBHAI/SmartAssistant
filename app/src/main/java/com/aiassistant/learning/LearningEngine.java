package com.aiassistant.learning;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Advanced learning engine that adapts to user behavior and game patterns
 * This class provides adaptive learning capabilities for the AI assistant
 */
public class LearningEngine {
    private static final String TAG = "LearningEngine";
    
    // Learning mode
    public enum LearningMode {
        PASSIVE,     // Only observe and learn, don't act
        ACTIVE,      // Learn and suggest actions
        AUTONOMOUS   // Learn and act autonomously
    }
    
    // Pattern confidence levels
    public enum ConfidenceLevel {
        VERY_LOW,   // Just starting to learn, < 20% confidence
        LOW,        // Emerging pattern, 20-40% confidence
        MEDIUM,     // Established pattern, 40-60% confidence
        HIGH,       // Strong pattern, 60-80% confidence
        VERY_HIGH   // Extremely reliable pattern, > 80% confidence
    }
    
    // Learning sources
    public enum LearningSource {
        USER_ACTION,    // Learned from user actions
        OBSERVATION,    // Observed behavior
        SYSTEM_EVENT,   // System events
        AUTOMATED_TEST, // Automated testing
        SYNTHETIC,      // Synthetic data
        CROSS_APP,      // Cross-application pattern
        GAME_SPECIFIC,  // Game-specific pattern
        IMPORTED        // Imported from external source
    }
    
    // Learning parameters
    private static final int MAX_PATTERNS = 1000;
    private static final int MAX_HISTORY = 500;
    private static final double MINIMUM_PATTERN_CONFIDENCE = 0.2; // 20% minimum confidence
    private static final double ACTION_SUCCESS_REWARD = 0.1; // Reward for successful action
    private static final double ACTION_FAILURE_PENALTY = 0.15; // Penalty for failed action
    
    // Core learning state
    private final Context context;
    private LearningMode currentMode = LearningMode.PASSIVE;
    private boolean initialized = false;
    private boolean lowPowerMode = false;
    
    // Pattern storage and history
    private final Map<String, LearningPattern> patterns = new ConcurrentHashMap<>();
    private final List<Map<String, Object>> actionHistory = new CopyOnWriteArrayList<>();
    private final Map<String, Integer> patternUsageCounts = new ConcurrentHashMap<>();
    private SharedPreferences preferences;
    
    // Learning stats
    private int totalObservations = 0;
    private int totalSuccessfulPredictions = 0;
    private int totalActions = 0;
    private double overallConfidence = 0.0;
    private long learningStartTime;
    
    /**
     * Create a new learning engine
     */
    public LearningEngine(Context context) {
        this.context = context;
        
        // Initialize
        initialize();
    }
    
    /**
     * Initialize the learning engine
     */
    private void initialize() {
        try {
            // Load preferences
            preferences = context.getSharedPreferences("learning_engine", Context.MODE_PRIVATE);
            
            // Load previous patterns if available
            loadPatterns();
            
            // Set initialization timestamp
            learningStartTime = System.currentTimeMillis();
            
            // Set initialized flag
            initialized = true;
            
            Log.d(TAG, "Learning engine initialized with " + patterns.size() + " patterns");
        } catch (Exception e) {
            Log.e(TAG, "Error initializing learning engine: " + e.getMessage(), e);
            initialized = false;
        }
    }
    
    /**
     * Set learning mode
     */
    public void setLearningMode(LearningMode mode) {
        if (this.currentMode != mode) {
            Log.d(TAG, "Changing learning mode from " + this.currentMode + " to " + mode);
            this.currentMode = mode;
        }
    }
    
    /**
     * Get current learning mode
     */
    public LearningMode getLearningMode() {
        return currentMode;
    }
    
    /**
     * Set low power mode
     */
    public void setLowPowerMode(boolean enabled) {
        this.lowPowerMode = enabled;
    }
    
    /**
     * Process screen analysis results
     */
    public void processScreenAnalysis(Map<String, Object> results) {
        if (!initialized) {
            return;
        }
        
        try {
            // Increment observation counter
            totalObservations++;
            
            // Extract content type
            Map<String, Object> contentResult = (Map<String, Object>) results.get("content");
            String contentType = contentResult != null ? (String) contentResult.get("content_type") : "unknown";
            
            // Process based on content type
            switch (contentType) {
                case "game":
                    processGameContent(results);
                    break;
                    
                case "social":
                case "messaging":
                    processSocialContent(results);
                    break;
                    
                case "browser":
                case "video":
                    processMediaContent(results);
                    break;
                    
                default:
                    processGenericContent(results);
                    break;
            }
            
            // Update overall confidence
            updateOverallConfidence();
            
            // Add to history
            addToHistory(results);
            
        } catch (Exception e) {
            Log.e(TAG, "Error processing screen analysis: " + e.getMessage(), e);
        }
    }
    
    /**
     * Process game-specific content
     */
    private void processGameContent(Map<String, Object> results) {
        // Extract enemies data
        List<Map<String, Object>> enemies = (List<Map<String, Object>>) results.get("enemies");
        
        if (enemies != null && !enemies.isEmpty()) {
            // Learn enemy patterns
            for (Map<String, Object> enemy : enemies) {
                String enemyId = (String) enemy.get("id");
                double threat = (double) enemy.getOrDefault("threat", 0.0);
                
                // Create pattern key
                String patternKey = "game_enemy_threat_" + (threat > 0.7 ? "high" : threat > 0.4 ? "medium" : "low");
                
                // Get or create pattern
                LearningPattern pattern = getOrCreatePattern(patternKey);
                
                // Update pattern
                pattern.observations++;
                pattern.confidence = calculatePatternConfidence(pattern);
                
                // Learn enemy position patterns
                Map<String, Integer> bounds = (Map<String, Integer>) enemy.get("bounds");
                if (bounds != null) {
                    int centerX = (bounds.get("left") + bounds.get("right")) / 2;
                    int centerY = (bounds.get("top") + bounds.get("bottom")) / 2;
                    
                    // Is enemy in center region?
                    boolean inCenterRegion = centerX > 400 && centerX < 680 && centerY > 300 && centerY < 780;
                    
                    String positionPatternKey = "game_enemy_position_" + (inCenterRegion ? "center" : "peripheral");
                    LearningPattern positionPattern = getOrCreatePattern(positionPatternKey);
                    positionPattern.observations++;
                    positionPattern.confidence = calculatePatternConfidence(positionPattern);
                }
            }
            
            // Learn highest threat enemy targeting pattern
            Map<String, Object> highestThreatEnemy = (Map<String, Object>) results.get("highest_threat_enemy");
            if (highestThreatEnemy != null) {
                String patternKey = "game_target_highest_threat";
                LearningPattern pattern = getOrCreatePattern(patternKey);
                pattern.observations++;
                pattern.confidence = calculatePatternConfidence(pattern);
            }
        }
    }
    
    /**
     * Process social/messaging content
     */
    private void processSocialContent(Map<String, Object> results) {
        // Extract text
        List<Map<String, Object>> textElements = (List<Map<String, Object>>) results.get("text");
        
        if (textElements != null && !textElements.isEmpty()) {
            // Look for common social patterns
            for (Map<String, Object> text : textElements) {
                String textContent = (String) text.get("text");
                
                if (textContent != null) {
                    String lowercaseText = textContent.toLowerCase();
                    
                    // Check for message patterns
                    if (lowercaseText.contains("message") || lowercaseText.contains("chat") || 
                        lowercaseText.contains("send") || lowercaseText.contains("reply")) {
                        
                        String patternKey = "social_message_interaction";
                        LearningPattern pattern = getOrCreatePattern(patternKey);
                        pattern.observations++;
                        pattern.confidence = calculatePatternConfidence(pattern);
                    }
                    
                    // Check for notification patterns
                    if (lowercaseText.contains("notification") || lowercaseText.contains("alert") || 
                        lowercaseText.contains("new") || lowercaseText.contains("unread")) {
                        
                        String patternKey = "social_notification_pattern";
                        LearningPattern pattern = getOrCreatePattern(patternKey);
                        pattern.observations++;
                        pattern.confidence = calculatePatternConfidence(pattern);
                    }
                }
            }
        }
    }
    
    /**
     * Process media content
     */
    private void processMediaContent(Map<String, Object> results) {
        // Extract text and objects
        List<Map<String, Object>> textElements = (List<Map<String, Object>>) results.get("text");
        List<Map<String, Object>> objects = (List<Map<String, Object>>) results.get("objects");
        
        // Text-based patterns
        if (textElements != null && !textElements.isEmpty()) {
            for (Map<String, Object> text : textElements) {
                String textContent = (String) text.get("text");
                
                if (textContent != null) {
                    String lowercaseText = textContent.toLowerCase();
                    
                    // Media control patterns
                    if (lowercaseText.contains("play") || lowercaseText.contains("pause") || 
                        lowercaseText.contains("stop") || lowercaseText.contains("next") || 
                        lowercaseText.contains("previous")) {
                        
                        String patternKey = "media_control_pattern";
                        LearningPattern pattern = getOrCreatePattern(patternKey);
                        pattern.observations++;
                        pattern.confidence = calculatePatternConfidence(pattern);
                    }
                }
            }
        }
        
        // Object-based patterns
        if (objects != null && !objects.isEmpty()) {
            for (Map<String, Object> obj : objects) {
                String objectClass = (String) obj.get("class");
                
                if (objectClass != null) {
                    // Media playing patterns
                    if (objectClass.equals("tv") || objectClass.equals("monitor") || 
                        objectClass.equals("screen") || objectClass.equals("laptop")) {
                        
                        String patternKey = "media_viewing_pattern";
                        LearningPattern pattern = getOrCreatePattern(patternKey);
                        pattern.observations++;
                        pattern.confidence = calculatePatternConfidence(pattern);
                    }
                }
            }
        }
    }
    
    /**
     * Process generic content
     */
    private void processGenericContent(Map<String, Object> results) {
        // Extract text features and objects
        Map<String, Object> textFeatures = (Map<String, Object>) results.get("text_features");
        List<Map<String, Object>> objects = (List<Map<String, Object>>) results.get("objects");
        
        // UI element patterns
        if (textFeatures != null) {
            boolean hasButton = (boolean) textFeatures.getOrDefault("has_button", false);
            boolean hasMenu = (boolean) textFeatures.getOrDefault("has_menu", false);
            
            if (hasButton) {
                String patternKey = "ui_button_interaction";
                LearningPattern pattern = getOrCreatePattern(patternKey);
                pattern.observations++;
                pattern.confidence = calculatePatternConfidence(pattern);
            }
            
            if (hasMenu) {
                String patternKey = "ui_menu_navigation";
                LearningPattern pattern = getOrCreatePattern(patternKey);
                pattern.observations++;
                pattern.confidence = calculatePatternConfidence(pattern);
            }
        }
        
        // Object recognition patterns
        if (objects != null && !objects.isEmpty()) {
            // Count object types
            Map<String, Integer> objectCounts = new HashMap<>();
            
            for (Map<String, Object> obj : objects) {
                String objectClass = (String) obj.get("class");
                
                if (objectClass != null) {
                    objectCounts.put(objectClass, objectCounts.getOrDefault(objectClass, 0) + 1);
                }
            }
            
            // Learn patterns based on common objects
            for (Map.Entry<String, Integer> entry : objectCounts.entrySet()) {
                if (entry.getValue() >= 2) { // Only learn if multiple instances
                    String patternKey = "object_multiple_" + entry.getKey();
                    LearningPattern pattern = getOrCreatePattern(patternKey);
                    pattern.observations++;
                    pattern.confidence = calculatePatternConfidence(pattern);
                }
            }
        }
    }
    
    /**
     * Record action result for learning
     */
    public void recordActionResult(String actionType, Map<String, Object> actionParams, boolean success) {
        if (!initialized) {
            return;
        }
        
        try {
            // Increment action counter
            totalActions++;
            
            // Create pattern key based on action type
            String patternKey = "action_" + actionType.toLowerCase();
            
            // Get or create pattern
            LearningPattern pattern = getOrCreatePattern(patternKey);
            
            // Update pattern based on success
            pattern.observations++;
            
            if (success) {
                pattern.successCount++;
                pattern.confidence += ACTION_SUCCESS_REWARD;
            } else {
                pattern.confidence -= ACTION_FAILURE_PENALTY;
            }
            
            // Ensure confidence is within bounds
            pattern.confidence = Math.max(0.0, Math.min(1.0, pattern.confidence));
            
            // Track usage
            patternUsageCounts.put(patternKey, patternUsageCounts.getOrDefault(patternKey, 0) + 1);
            
            // If successful, increment counter
            if (success) {
                totalSuccessfulPredictions++;
            }
            
            // Create action record
            Map<String, Object> actionRecord = new HashMap<>();
            actionRecord.put("type", actionType);
            actionRecord.put("params", actionParams);
            actionRecord.put("success", success);
            actionRecord.put("timestamp", System.currentTimeMillis());
            actionRecord.put("pattern_key", patternKey);
            actionRecord.put("pattern_confidence", pattern.confidence);
            
            // Add to history
            addToHistory(actionRecord);
            
            // Update overall confidence
            updateOverallConfidence();
            
        } catch (Exception e) {
            Log.e(TAG, "Error recording action result: " + e.getMessage(), e);
        }
    }
    
    /**
     * Check if a pattern should be applied in current context
     */
    public boolean shouldApplyPattern(String patternKey, Map<String, Object> currentContext) {
        if (!initialized) {
            return false;
        }
        
        // Get pattern
        LearningPattern pattern = patterns.get(patternKey);
        
        if (pattern == null) {
            return false;
        }
        
        // Check if confidence is high enough
        if (pattern.confidence < MINIMUM_PATTERN_CONFIDENCE) {
            return false;
        }
        
        // Check if we're in appropriate mode
        switch (currentMode) {
            case PASSIVE:
                return false; // Never apply patterns in passive mode
                
            case ACTIVE:
                // Only apply high confidence patterns
                return pattern.confidence >= 0.6;
                
            case AUTONOMOUS:
                // Apply patterns with medium or higher confidence
                return pattern.confidence >= 0.4;
                
            default:
                return false;
        }
    }
    
    /**
     * Get recommended action for current context
     */
    public Map<String, Object> getRecommendedAction(Map<String, Object> currentContext) {
        if (!initialized) {
            return null;
        }
        
        // Find best pattern match
        String bestPatternKey = null;
        double bestConfidence = MINIMUM_PATTERN_CONFIDENCE;
        
        for (Map.Entry<String, LearningPattern> entry : patterns.entrySet()) {
            LearningPattern pattern = entry.getValue();
            
            if (pattern.confidence > bestConfidence && matchesContext(pattern, currentContext)) {
                bestPatternKey = entry.getKey();
                bestConfidence = pattern.confidence;
            }
        }
        
        // If no pattern found, return null
        if (bestPatternKey == null) {
            return null;
        }
        
        // Get pattern
        LearningPattern bestPattern = patterns.get(bestPatternKey);
        
        // Create recommendation
        Map<String, Object> recommendation = new HashMap<>();
        recommendation.put("pattern_key", bestPatternKey);
        recommendation.put("confidence", bestPattern.confidence);
        recommendation.put("observations", bestPattern.observations);
        recommendation.put("success_rate", bestPattern.getSuccessRate());
        
        // Get action details based on pattern key
        String[] parts = bestPatternKey.split("_", 2);
        if (parts.length > 1) {
            if (parts[0].equals("action")) {
                recommendation.put("action_type", parts[1]);
            } else if (parts[0].equals("game")) {
                recommendation.put("action_type", "game_action");
                recommendation.put("action_subtype", parts[1]);
            } else if (parts[0].equals("ui")) {
                recommendation.put("action_type", "ui_action");
                recommendation.put("action_subtype", parts[1]);
            }
        }
        
        return recommendation;
    }
    
    /**
     * Check if pattern matches current context
     */
    private boolean matchesContext(LearningPattern pattern, Map<String, Object> currentContext) {
        // In a full implementation, this would check context features
        // For this simplified version, we'll just return true
        return true;
    }
    
    /**
     * Get or create a learning pattern
     */
    private LearningPattern getOrCreatePattern(String patternKey) {
        // Check if pattern exists
        LearningPattern pattern = patterns.get(patternKey);
        
        // If not, create it
        if (pattern == null) {
            pattern = new LearningPattern();
            pattern.patternKey = patternKey;
            pattern.createdAt = System.currentTimeMillis();
            
            // Add to patterns map
            patterns.put(patternKey, pattern);
            
            // If we have too many patterns, remove least used ones
            if (patterns.size() > MAX_PATTERNS) {
                prunePatterns();
            }
        }
        
        // Update last used time
        pattern.lastUsedAt = System.currentTimeMillis();
        
        return pattern;
    }
    
    /**
     * Calculate pattern confidence
     */
    private double calculatePatternConfidence(LearningPattern pattern) {
        // Base confidence on number of observations, success rate, and age
        
        // Observation factor (more observations = higher confidence)
        double observationFactor = Math.min(1.0, pattern.observations / 10.0);
        
        // Success factor (higher success rate = higher confidence)
        double successFactor = pattern.getSuccessRate();
        
        // Recency factor (more recent = higher confidence)
        long ageMs = System.currentTimeMillis() - pattern.createdAt;
        double ageDays = ageMs / (1000.0 * 60 * 60 * 24);
        double recencyFactor = Math.max(0.0, 1.0 - (ageDays / 30.0)); // Decay over 30 days
        
        // Combined confidence
        double rawConfidence = (0.4 * observationFactor) + 
                               (0.4 * successFactor) + 
                               (0.2 * recencyFactor);
        
        // Ensure it's within bounds
        return Math.max(0.0, Math.min(1.0, rawConfidence));
    }
    
    /**
     * Update overall confidence
     */
    private void updateOverallConfidence() {
        // Calculate overall confidence based on success rate and pattern confidence
        double successRate = totalActions > 0 ? 
            (double) totalSuccessfulPredictions / totalActions : 0.0;
        
        // Calculate average pattern confidence
        double avgPatternConfidence = 0.0;
        int patternCount = 0;
        
        for (LearningPattern pattern : patterns.values()) {
            avgPatternConfidence += pattern.confidence;
            patternCount++;
        }
        
        avgPatternConfidence = patternCount > 0 ? avgPatternConfidence / patternCount : 0.0;
        
        // Overall confidence formula
        overallConfidence = (0.6 * successRate) + (0.4 * avgPatternConfidence);
    }
    
    /**
     * Remove least used patterns to stay under the limit
     */
    private void prunePatterns() {
        try {
            // Find least used patterns
            List<String> patternKeys = new ArrayList<>(patterns.keySet());
            
            // Sort by usage count (ascending)
            patternKeys.sort((k1, k2) -> {
                int count1 = patternUsageCounts.getOrDefault(k1, 0);
                int count2 = patternUsageCounts.getOrDefault(k2, 0);
                return Integer.compare(count1, count2);
            });
            
            // Remove oldest patterns to get back under limit
            int toRemove = patterns.size() - MAX_PATTERNS;
            
            for (int i = 0; i < toRemove && i < patternKeys.size(); i++) {
                String key = patternKeys.get(i);
                patterns.remove(key);
                patternUsageCounts.remove(key);
            }
            
        } catch (Exception e) {
            Log.e(TAG, "Error pruning patterns: " + e.getMessage(), e);
        }
    }
    
    /**
     * Add to action history
     */
    private void addToHistory(Map<String, Object> record) {
        // Add to history
        actionHistory.add(record);
        
        // Trim history if needed
        while (actionHistory.size() > MAX_HISTORY) {
            actionHistory.remove(0);
        }
    }
    
    /**
     * Save current state
     */
    public boolean saveState() {
        if (!initialized) {
            return false;
        }
        
        try {
            // In a real implementation, this would serialize and save all patterns
            // For this simplified version, we'll just save some basic counts
            SharedPreferences.Editor editor = preferences.edit();
            
            // Save counts
            editor.putInt("total_observations", totalObservations);
            editor.putInt("total_actions", totalActions);
            editor.putInt("total_successful_predictions", totalSuccessfulPredictions);
            
            // Save pattern keys (limited to 100 to avoid exceeding limits)
            Set<String> keySet = new HashSet<>();
            int count = 0;
            
            for (String key : patterns.keySet()) {
                keySet.add(key);
                count++;
                
                if (count >= 100) {
                    break;
                }
            }
            
            editor.putStringSet("pattern_keys", keySet);
            
            // Save confidence
            editor.putFloat("overall_confidence", (float) overallConfidence);
            
            // Apply changes
            editor.apply();
            
            return true;
            
        } catch (Exception e) {
            Log.e(TAG, "Error saving learning state: " + e.getMessage(), e);
            return false;
        }
    }
    
    /**
     * Load saved patterns
     */
    private void loadPatterns() {
        try {
            // In a real implementation, this would deserialize all saved patterns
            // For this simplified version, we'll just load some basic counts
            
            totalObservations = preferences.getInt("total_observations", 0);
            totalActions = preferences.getInt("total_actions", 0);
            totalSuccessfulPredictions = preferences.getInt("total_successful_predictions", 0);
            overallConfidence = preferences.getFloat("overall_confidence", 0.0f);
            
            // Load pattern keys
            Set<String> keySet = preferences.getStringSet("pattern_keys", new HashSet<>());
            
            // Create empty patterns for each key
            for (String key : keySet) {
                LearningPattern pattern = new LearningPattern();
                pattern.patternKey = key;
                pattern.createdAt = System.currentTimeMillis() - 86400000; // 1 day ago
                pattern.observations = 5; // Default value
                pattern.confidence = 0.3; // Default confidence
                
                patterns.put(key, pattern);
            }
            
        } catch (Exception e) {
            Log.e(TAG, "Error loading learning state: " + e.getMessage(), e);
        }
    }
    
    /**
     * Get learning stats
     */
    public Map<String, Object> getPerformanceMetrics() {
        Map<String, Object> metrics = new HashMap<>();
        
        metrics.put("total_observations", totalObservations);
        metrics.put("total_actions", totalActions);
        metrics.put("total_successful_predictions", totalSuccessfulPredictions);
        metrics.put("pattern_count", patterns.size());
        metrics.put("overall_confidence", overallConfidence);
        
        if (totalActions > 0) {
            metrics.put("success_rate", (double) totalSuccessfulPredictions / totalActions);
        } else {
            metrics.put("success_rate", 0.0);
        }
        
        metrics.put("learning_mode", currentMode.toString());
        metrics.put("low_power_mode", lowPowerMode);
        metrics.put("uptime_ms", System.currentTimeMillis() - learningStartTime);
        
        return metrics;
    }
    
    /**
     * Get confidence level for a pattern
     */
    public ConfidenceLevel getConfidenceLevel(double confidence) {
        if (confidence < 0.2) {
            return ConfidenceLevel.VERY_LOW;
        } else if (confidence < 0.4) {
            return ConfidenceLevel.LOW;
        } else if (confidence < 0.6) {
            return ConfidenceLevel.MEDIUM;
        } else if (confidence < 0.8) {
            return ConfidenceLevel.HIGH;
        } else {
            return ConfidenceLevel.VERY_HIGH;
        }
    }
    
    /**
     * Reset learning engine
     */
    public void reset() {
        patterns.clear();
        patternUsageCounts.clear();
        actionHistory.clear();
        totalObservations = 0;
        totalActions = 0;
        totalSuccessfulPredictions = 0;
        overallConfidence = 0.0;
        learningStartTime = System.currentTimeMillis();
        
        // Save reset state
        saveState();
    }
    
    /**
     * Learning pattern class
     */
    private static class LearningPattern {
        String patternKey;
        int observations = 0;
        int successCount = 0;
        double confidence = 0.0;
        long createdAt;
        long lastUsedAt;
        LearningSource source = LearningSource.OBSERVATION;
        Map<String, Object> metadata = new HashMap<>();
        
        /**
         * Get success rate for this pattern
         */
        double getSuccessRate() {
            return observations > 0 ? (double) successCount / observations : 0.0;
        }
    }
}