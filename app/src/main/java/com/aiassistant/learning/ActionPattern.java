package com.aiassistant.learning;

import java.io.Serializable;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Represents a learned action pattern with context conditions
 */
public class ActionPattern implements Serializable {
    private static final long serialVersionUID = 1L;
    
    private String actionType;
    private Map<String, Object> contextPattern;
    private Map<String, Float> contextKeyWeights;
    private float confidence;
    private int occurrences;
    private int successfulOccurrences;
    private long lastUsed;
    private long firstObserved;
    
    /**
     * Create a new action pattern
     */
    public ActionPattern(String actionType, Map<String, Object> context) {
        this.actionType = actionType;
        this.contextPattern = new HashMap<>(context);
        this.contextKeyWeights = new HashMap<>();
        this.confidence = 0.5f;  // Initial confidence
        this.occurrences = 1;
        this.successfulOccurrences = 0;
        this.lastUsed = System.currentTimeMillis();
        this.firstObserved = System.currentTimeMillis();
        
        // Initialize context key weights
        for (String key : context.keySet()) {
            contextKeyWeights.put(key, 0.5f);  // Initial weight
        }
    }
    
    /**
     * Calculate similarity between this pattern and a new action
     */
    public float calculateSimilarity(String actionType, Map<String, Object> context) {
        // If action type doesn't match, return 0
        if (!this.actionType.equals(actionType)) {
            return 0.0f;
        }
        
        // Calculate context similarity
        return calculateContextSimilarity(context);
    }
    
    /**
     * Calculate context similarity
     */
    private float calculateContextSimilarity(Map<String, Object> context) {
        if (context == null || context.isEmpty() || contextPattern.isEmpty()) {
            return 0.0f;
        }
        
        float totalWeight = 0.0f;
        float matchedWeight = 0.0f;
        
        // Find common keys
        Set<String> commonKeys = new HashSet<>(contextPattern.keySet());
        commonKeys.retainAll(context.keySet());
        
        // Calculate weighted match for common keys
        for (String key : commonKeys) {
            float keyWeight = contextKeyWeights.getOrDefault(key, 0.5f);
            totalWeight += keyWeight;
            
            // Compare values
            Object patternValue = contextPattern.get(key);
            Object contextValue = context.get(key);
            
            if (patternValue != null && contextValue != null) {
                if (patternValue.equals(contextValue)) {
                    matchedWeight += keyWeight;
                } else if (patternValue instanceof String && contextValue instanceof String) {
                    // For strings, check if one contains the other
                    String patternStr = (String) patternValue;
                    String contextStr = (String) contextValue;
                    
                    if (patternStr.contains(contextStr) || contextStr.contains(patternStr)) {
                        matchedWeight += keyWeight * 0.7f;  // Partial match
                    }
                } else if (patternValue instanceof Number && contextValue instanceof Number) {
                    // For numbers, check if they're close
                    double patternNum = ((Number) patternValue).doubleValue();
                    double contextNum = ((Number) contextValue).doubleValue();
                    double diff = Math.abs(patternNum - contextNum);
                    double max = Math.max(Math.abs(patternNum), Math.abs(contextNum));
                    
                    if (max > 0 && diff / max < 0.1) {
                        matchedWeight += keyWeight * 0.9f;  // Close match
                    }
                }
            }
        }
        
        // Include missing keys in total weight
        for (String key : contextPattern.keySet()) {
            if (!commonKeys.contains(key)) {
                totalWeight += contextKeyWeights.getOrDefault(key, 0.5f) * 0.5f;  // Half weight for missing keys
            }
        }
        
        // If no total weight, return 0
        if (totalWeight <= 0.0f) {
            return 0.0f;
        }
        
        return matchedWeight / totalWeight;
    }
    
    /**
     * Update pattern with new context
     */
    public void update(String actionType, Map<String, Object> context, float learningRate) {
        if (!this.actionType.equals(actionType)) {
            return;
        }
        
        occurrences++;
        lastUsed = System.currentTimeMillis();
        
        // Update context pattern - merge new context but don't replace existing values
        // instead, gradually shift towards new values
        for (Map.Entry<String, Object> entry : context.entrySet()) {
            String key = entry.getKey();
            Object newValue = entry.getValue();
            
            // Update key weight (increase if key is observed again)
            float currentWeight = contextKeyWeights.getOrDefault(key, 0.5f);
            contextKeyWeights.put(key, Math.min(1.0f, currentWeight + learningRate * 0.2f));
            
            // If existing value, potentially update it
            if (contextPattern.containsKey(key)) {
                Object currentValue = contextPattern.get(key);
                
                // Update value if it's the same type but different
                if (currentValue != null && newValue != null && 
                    currentValue.getClass() == newValue.getClass() && 
                    !currentValue.equals(newValue)) {
                    
                    // For numbers, average them
                    if (currentValue instanceof Number && newValue instanceof Number) {
                        double currentNum = ((Number) currentValue).doubleValue();
                        double newNum = ((Number) newValue).doubleValue();
                        double updatedNum = currentNum * (1 - learningRate) + newNum * learningRate;
                        
                        if (currentValue instanceof Integer) {
                            contextPattern.put(key, (int) Math.round(updatedNum));
                        } else if (currentValue instanceof Long) {
                            contextPattern.put(key, Math.round(updatedNum));
                        } else if (currentValue instanceof Float) {
                            contextPattern.put(key, (float) updatedNum);
                        } else if (currentValue instanceof Double) {
                            contextPattern.put(key, updatedNum);
                        }
                    }
                    // For other types, replace with some probability based on learning rate
                    else if (Math.random() < learningRate) {
                        contextPattern.put(key, newValue);
                    }
                }
            } 
            // If new key, add it
            else {
                contextPattern.put(key, newValue);
            }
        }
        
        // Decrease weight for keys not present in this context
        Set<String> missingKeys = new HashSet<>(contextPattern.keySet());
        missingKeys.removeAll(context.keySet());
        
        for (String key : missingKeys) {
            float currentWeight = contextKeyWeights.getOrDefault(key, 0.5f);
            float newWeight = currentWeight * (1 - learningRate * 0.2f);
            
            if (newWeight < 0.1f) {
                // If weight becomes too low, remove the key
                contextKeyWeights.remove(key);
                contextPattern.remove(key);
            } else {
                contextKeyWeights.put(key, newWeight);
            }
        }
    }
    
    /**
     * Calculate match between this pattern and current context
     */
    public float calculateContextMatch(Map<String, Object> currentContext) {
        return calculateContextSimilarity(currentContext);
    }
    
    /**
     * Generate parameters for action execution
     */
    public Map<String, Object> generateParameters(Map<String, Object> currentContext) {
        Map<String, Object> params = new HashMap<>();
        
        // Start with our pattern context
        params.putAll(contextPattern);
        
        // Update with current context values for known keys
        for (String key : params.keySet()) {
            if (currentContext.containsKey(key)) {
                params.put(key, currentContext.get(key));
            }
        }
        
        return params;
    }
    
    /**
     * Record successful execution
     */
    public void recordSuccess() {
        successfulOccurrences++;
        updateConfidence();
    }
    
    /**
     * Record failed execution
     */
    public void recordFailure() {
        updateConfidence();
    }
    
    /**
     * Update confidence based on success rate
     */
    private void updateConfidence() {
        if (occurrences > 0) {
            float successRate = (float) successfulOccurrences / occurrences;
            confidence = 0.3f * confidence + 0.7f * successRate;
        }
    }
    
    // Getters and setters
    
    public String getActionType() {
        return actionType;
    }
    
    public Map<String, Object> getContextPattern() {
        return new HashMap<>(contextPattern);
    }
    
    public float getConfidence() {
        return confidence;
    }
    
    public int getOccurrences() {
        return occurrences;
    }
    
    public long getLastUsed() {
        return lastUsed;
    }
    
    public long getFirstObserved() {
        return firstObserved;
    }
    
    public float getSuccessRate() {
        return occurrences > 0 ? (float) successfulOccurrences / occurrences : 0.0f;
    }
    
    @Override
    public String toString() {
        return "ActionPattern{" +
                "actionType='" + actionType + '\'' +
                ", contextKeys=" + contextPattern.keySet() +
                ", confidence=" + confidence +
                ", occurrences=" + occurrences +
                '}';
    }
}