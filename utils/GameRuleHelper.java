package utils;

import models.GameRuleUnderstanding.GameRule;
import java.util.Map;
import java.util.HashMap;

/**
 * Helper class for GameRule
 * Provides compatibility methods and access for GameRule objects
 */
public class GameRuleHelper {
    
    /**
     * Get the name of a game rule
     * 
     * @param rule GameRule object
     * @return Rule name or null if not available
     */
    public static String getName(GameRule rule) {
        if (rule == null) {
            return null;
        }
        
        try {
            // Try direct method call
            return (String) rule.getClass().getMethod("getName").invoke(rule);
        } catch (Exception e) {
            try {
                // Try alternative method names
                return (String) rule.getClass().getMethod("getRuleName").invoke(rule);
            } catch (Exception ex) {
                try {
                    // Try to access name field directly
                    java.lang.reflect.Field nameField = rule.getClass().getDeclaredField("name");
                    nameField.setAccessible(true);
                    return (String) nameField.get(rule);
                } catch (Exception exc) {
                    try {
                        // Try to access id field
                        java.lang.reflect.Field idField = rule.getClass().getDeclaredField("id");
                        idField.setAccessible(true);
                        return (String) idField.get(rule);
                    } catch (Exception excp) {
                        System.err.println("Failed to get rule name: " + excp.getMessage());
                        return "Unknown Rule";
                    }
                }
            }
        }
    }
    
    /**
     * Get the description of a game rule
     * 
     * @param rule GameRule object
     * @return Rule description or null if not available
     */
    public static String getDescription(GameRule rule) {
        if (rule == null) {
            return null;
        }
        
        try {
            // Try direct method call
            return (String) rule.getClass().getMethod("getDescription").invoke(rule);
        } catch (Exception e) {
            try {
                // Try alternative method names
                return (String) rule.getClass().getMethod("getRuleDescription").invoke(rule);
            } catch (Exception ex) {
                try {
                    // Try to access description field directly
                    java.lang.reflect.Field descField = rule.getClass().getDeclaredField("description");
                    descField.setAccessible(true);
                    return (String) descField.get(rule);
                } catch (Exception exc) {
                    System.err.println("Failed to get rule description: " + exc.getMessage());
                    return null;
                }
            }
        }
    }
    
    /**
     * Get the priority of a game rule
     * 
     * @param rule GameRule object
     * @return Rule priority or 0 if not available
     */
    public static int getPriority(GameRule rule) {
        if (rule == null) {
            return 0;
        }
        
        try {
            // Try direct method call
            return (int) rule.getClass().getMethod("getPriority").invoke(rule);
        } catch (Exception e) {
            try {
                // Try alternative method names
                return (int) rule.getClass().getMethod("getRulePriority").invoke(rule);
            } catch (Exception ex) {
                try {
                    // Try to access priority field directly
                    java.lang.reflect.Field priorityField = rule.getClass().getDeclaredField("priority");
                    priorityField.setAccessible(true);
                    return (int) priorityField.get(rule);
                } catch (Exception exc) {
                    System.err.println("Failed to get rule priority: " + exc.getMessage());
                    return 0;
                }
            }
        }
    }
    
    /**
     * Get the conditions of a game rule
     * 
     * @param rule GameRule object
     * @return Map of rule conditions or empty map if not available
     */
    public static Map<String, Object> getConditions(GameRule rule) {
        if (rule == null) {
            return new HashMap<>();
        }
        
        try {
            // Try direct method call
            @SuppressWarnings("unchecked")
            Map<String, Object> conditions = (Map<String, Object>) rule.getClass().getMethod("getConditions").invoke(rule);
            return conditions != null ? conditions : new HashMap<>();
        } catch (Exception e) {
            try {
                // Try alternative method names
                @SuppressWarnings("unchecked")
                Map<String, Object> conditions = (Map<String, Object>) rule.getClass().getMethod("getRuleConditions").invoke(rule);
                return conditions != null ? conditions : new HashMap<>();
            } catch (Exception ex) {
                try {
                    // Try to access conditions field directly
                    java.lang.reflect.Field conditionsField = rule.getClass().getDeclaredField("conditions");
                    conditionsField.setAccessible(true);
                    @SuppressWarnings("unchecked")
                    Map<String, Object> conditions = (Map<String, Object>) conditionsField.get(rule);
                    return conditions != null ? conditions : new HashMap<>();
                } catch (Exception exc) {
                    System.err.println("Failed to get rule conditions: " + exc.getMessage());
                    return new HashMap<>();
                }
            }
        }
    }
    
    /**
     * Check if a rule applies to a given state
     * 
     * @param rule GameRule object
     * @param state State to check against
     * @return True if rule applies
     */
    public static boolean applies(GameRule rule, Map<String, Object> state) {
        if (rule == null || state == null) {
            return false;
        }
        
        try {
            // Try direct method call
            return (boolean) rule.getClass().getMethod("applies", Map.class).invoke(rule, state);
        } catch (Exception e) {
            try {
                // Try alternative method names
                return (boolean) rule.getClass().getMethod("isApplicable", Map.class).invoke(rule, state);
            } catch (Exception ex) {
                try {
                    // Try alternative method names
                    return (boolean) rule.getClass().getMethod("matches", Map.class).invoke(rule, state);
                } catch (Exception exc) {
                    System.err.println("Failed to check if rule applies: " + exc.getMessage());
                    
                    // Manual check using conditions
                    try {
                        Map<String, Object> conditions = getConditions(rule);
                        for (Map.Entry<String, Object> entry : conditions.entrySet()) {
                            String key = entry.getKey();
                            Object expectedValue = entry.getValue();
                            
                            if (!state.containsKey(key) || !state.get(key).equals(expectedValue)) {
                                return false;
                            }
                        }
                        return !conditions.isEmpty();
                    } catch (Exception excp) {
                        return false;
                    }
                }
            }
        }
    }
}