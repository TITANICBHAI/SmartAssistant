package utils;

import models.RuleExtractionSystem;
import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.ArrayList;

/**
 * Helper class for RuleExtractionSystem
 * Provides compatibility methods for working with RuleExtractionSystem implementations
 */
public class RuleExtractionSystemHelper {

    /**
     * Get instance of RuleExtractionSystem with utils.Context
     * 
     * @param context Context object
     * @return RuleExtractionSystem instance
     */
    public static RuleExtractionSystem getInstance(Context context) {
        try {
            // First try with Context parameter
            return (RuleExtractionSystem) RuleExtractionSystem.class
                .getMethod("getInstance", Context.class)
                .invoke(null, context);
        } catch (Exception e) {
            try {
                // Try no-arg getInstance
                return (RuleExtractionSystem) RuleExtractionSystem.class
                    .getMethod("getInstance")
                    .invoke(null);
            } catch (Exception ex) {
                // Create a new instance directly as fallback
                try {
                    return RuleExtractionSystem.class.getConstructor(Context.class).newInstance(context);
                } catch (Exception exc) {
                    try {
                        return RuleExtractionSystem.class.newInstance();
                    } catch (Exception excp) {
                        System.err.println("Failed to create RuleExtractionSystem: " + excp.getMessage());
                        return null;
                    }
                }
            }
        }
    }
    
    /**
     * Get instance of RuleExtractionSystem with Android context
     * 
     * @param androidContext Android context
     * @return RuleExtractionSystem instance
     */
    public static RuleExtractionSystem getInstance(android.content.Context androidContext) {
        // Create utils.Context wrapper and use it
        Context utilsContext = ContextCompatHelper.fromAndroidContext(androidContext);
        return getInstance(utilsContext);
    }
    
    /**
     * Start the rule extraction system
     * 
     * @param system RuleExtractionSystem instance
     */
    public static void start(RuleExtractionSystem system) {
        if (system == null) {
            return;
        }
        
        try {
            // Try to call start method
            system.getClass().getMethod("start").invoke(system);
        } catch (Exception e) {
            try {
                // Try alternative method names
                system.getClass().getMethod("initialize").invoke(system);
            } catch (Exception ex) {
                System.err.println("Failed to start RuleExtractionSystem: " + ex.getMessage());
            }
        }
    }
    
    /**
     * Stop the rule extraction system
     * 
     * @param system RuleExtractionSystem instance
     */
    public static void stop(RuleExtractionSystem system) {
        if (system == null) {
            return;
        }
        
        try {
            // Try to call stop method
            system.getClass().getMethod("stop").invoke(system);
        } catch (Exception e) {
            try {
                // Try alternative method names
                system.getClass().getMethod("shutdown").invoke(system);
            } catch (Exception ex) {
                System.err.println("Failed to stop RuleExtractionSystem: " + ex.getMessage());
            }
        }
    }
    
    /**
     * Process an observation of the game state
     * 
     * @param system RuleExtractionSystem instance
     * @param state Current state data
     * @param lastAction Last action taken
     * @param reward Reward value
     */
    public static void processObservation(RuleExtractionSystem system, 
                                        Map<String, Object> state,
                                        String lastAction,
                                        float reward) {
        if (system == null || state == null) {
            return;
        }
        
        try {
            // Try to call processObservation method
            system.getClass()
                .getMethod("processObservation", Map.class, String.class, float.class)
                .invoke(system, state, lastAction, reward);
        } catch (Exception e) {
            try {
                // Try alternative method signatures
                system.getClass()
                    .getMethod("processState", Map.class, String.class, float.class)
                    .invoke(system, state, lastAction, reward);
            } catch (Exception ex) {
                System.err.println("Failed to process observation: " + ex.getMessage());
            }
        }
    }
    
    /**
     * Find relevant rules for a specific state
     * 
     * @param system RuleExtractionSystem instance
     * @param state Current state data
     * @param context Context object (can be null)
     * @return List of relevant rules
     */
    public static List<?> findRelevantRules(RuleExtractionSystem system,
                                          Map<String, Object> state,
                                          Object context) {
        if (system == null || state == null) {
            return new ArrayList<>();
        }
        
        try {
            // Try to call findRelevantRules with context
            if (context != null) {
                return (List<?>) system.getClass()
                    .getMethod("findRelevantRules", Map.class, Object.class)
                    .invoke(system, state, context);
            } else {
                // Try without context
                return (List<?>) system.getClass()
                    .getMethod("findRelevantRules", Map.class)
                    .invoke(system, state);
            }
        } catch (Exception e) {
            try {
                // Try alternative method names
                if (context != null) {
                    return (List<?>) system.getClass()
                        .getMethod("getRulesForState", Map.class, Object.class)
                        .invoke(system, state, context);
                } else {
                    return (List<?>) system.getClass()
                        .getMethod("getRulesForState", Map.class)
                        .invoke(system, state);
                }
            } catch (Exception ex) {
                System.err.println("Failed to find relevant rules: " + ex.getMessage());
                return new ArrayList<>();
            }
        }
    }
    
    /**
     * Extract rules from a list of observations
     * 
     * @param system RuleExtractionSystem instance
     * @param observations List of observations
     * @return List of extracted rules
     */
    public static List<?> extractRules(RuleExtractionSystem system, List<?> observations) {
        if (system == null || observations == null || observations.isEmpty()) {
            return new ArrayList<>();
        }
        
        try {
            // Try to call extractRules method
            return (List<?>) system.getClass()
                .getMethod("extractRules", List.class)
                .invoke(system, observations);
        } catch (Exception e) {
            try {
                // Try alternative method names
                return (List<?>) system.getClass()
                    .getMethod("deriveRules", List.class)
                    .invoke(system, observations);
            } catch (Exception ex) {
                System.err.println("Failed to extract rules: " + ex.getMessage());
                return new ArrayList<>();
            }
        }
    }
    
    /**
     * Get statistics from the rule extraction system
     * 
     * @param system RuleExtractionSystem instance
     * @return Map of statistics
     */
    public static Map<String, Object> getStats(RuleExtractionSystem system) {
        if (system == null) {
            return new HashMap<>();
        }
        
        try {
            // Try to call getStats method
            return (Map<String, Object>) system.getClass()
                .getMethod("getStats")
                .invoke(system);
        } catch (Exception e) {
            try {
                // Try alternative method names
                return (Map<String, Object>) system.getClass()
                    .getMethod("getStatistics")
                    .invoke(system);
            } catch (Exception ex) {
                System.err.println("Failed to get stats: " + ex.getMessage());
                return new HashMap<>();
            }
        }
    }
    
    /**
     * Get all extracted rules
     * 
     * @param system RuleExtractionSystem instance
     * @return List of all rules
     */
    public static List<?> getAllRules(RuleExtractionSystem system) {
        if (system == null) {
            return new ArrayList<>();
        }
        
        try {
            // Try to call getAllRules method
            return (List<?>) system.getClass()
                .getMethod("getAllRules")
                .invoke(system);
        } catch (Exception e) {
            try {
                // Try alternative method names
                return (List<?>) system.getClass()
                    .getMethod("getRules")
                    .invoke(system);
            } catch (Exception ex) {
                System.err.println("Failed to get all rules: " + ex.getMessage());
                return new ArrayList<>();
            }
        }
    }
    
    /**
     * Clear all rules from the system
     * 
     * @param system RuleExtractionSystem instance
     * @return Success status
     */
    public static boolean clearRules(RuleExtractionSystem system) {
        if (system == null) {
            return false;
        }
        
        try {
            // Try to call clearRules method
            return (boolean) system.getClass()
                .getMethod("clearRules")
                .invoke(system);
        } catch (Exception e) {
            try {
                // Try alternative method names
                return (boolean) system.getClass()
                    .getMethod("resetRules")
                    .invoke(system);
            } catch (Exception ex) {
                System.err.println("Failed to clear rules: " + ex.getMessage());
                return false;
            }
        }
    }
}