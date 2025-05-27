package utils;

import models.GameTrainer;
import models.GameTrainer.ActionRecommendation;
import android.content.Context;

import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.ArrayList;

/**
 * Helper class for GameTrainer
 * Provides compatibility methods for working with GameTrainer implementations
 */
public class GameTrainerHelper {
    
    /**
     * Public wrapper for GameTrainer's private TrainingSession class
     * Used to provide compatibility with the internal TrainingSession type
     */
    public static class TrainingSession {
        private Object internalSession;
        private String sessionId;
        
        /**
         * Create wrapper from internal session object
         * 
         * @param internalSession The internal TrainingSession object
         */
        public TrainingSession(Object internalSession) {
            this.internalSession = internalSession;
            
            // Try to extract session ID
            try {
                this.sessionId = (String) internalSession.getClass()
                    .getMethod("getSessionId")
                    .invoke(internalSession);
            } catch (Exception ex) {
                this.sessionId = "unknown_session";
            }
        }
        
        /**
         * Get the session ID
         * 
         * @return Session ID
         */
        public String getSessionId() {
            return sessionId;
        }
        
        /**
         * Get the internal session object
         * 
         * @return Internal session object
         */
        public Object getInternalSession() {
            return internalSession;
        }
    }

    /**
     * Get instance of GameTrainer with utils.Context
     * 
     * @param context Context object
     * @return GameTrainer instance
     */
    public static GameTrainer getInstance(utils.Context context) {
        try {
            // First try with Context parameter
            return (GameTrainer) GameTrainer.class
                .getMethod("getInstance", android.content.Context.class)
                .invoke(null, context);
        } catch (Exception e) {
            try {
                // Try no-arg getInstance
                return (GameTrainer) GameTrainer.class
                    .getMethod("getInstance")
                    .invoke(null);
            } catch (Exception ex) {
                // Create a new instance directly as fallback
                try {
                    return GameTrainer.class.getConstructor(android.content.Context.class).newInstance(context);
                } catch (Exception exc) {
                    try {
                        return GameTrainer.class.newInstance();
                    } catch (Exception excp) {
                        System.err.println("Failed to create GameTrainer: " + excp.getMessage());
                        return null;
                    }
                }
            }
        }
    }
    
    /**
     * Get instance of GameTrainer with Android context
     * 
     * @param androidContext Android context
     * @return GameTrainer instance
     */
    public static GameTrainer getInstance(android.content.Context androidContext) {
        if (androidContext == null) {
            return null;
        }
        
        try {
            // Try direct getInstance with Android context
            return (GameTrainer) GameTrainer.class
                .getMethod("getInstance", android.content.Context.class)
                .invoke(null, androidContext);
        } catch (Exception e) {
            // If direct method fails, try with a different approach
            try {
                // Try creating with default constructor and then initializing
                GameTrainer trainer = GameTrainer.class.newInstance();
                
                // Call initialize with context
                trainer.getClass().getMethod("initialize", android.content.Context.class)
                      .invoke(trainer, androidContext);
                
                return trainer;
            } catch (Exception ex) {
                System.err.println("Failed to get GameTrainer instance: " + ex.getMessage());
                return null;
            }
        }
    }
    
    /**
     * Start the game trainer
     * 
     * @param trainer GameTrainer instance
     */
    public static void start(GameTrainer trainer) {
        if (trainer == null) {
            return;
        }
        
        try {
            // Try to call start method
            trainer.getClass().getMethod("start").invoke(trainer);
        } catch (Exception e) {
            try {
                // Try alternative method names
                trainer.getClass().getMethod("initialize").invoke(trainer);
            } catch (Exception ex) {
                System.err.println("Failed to start GameTrainer: " + ex.getMessage());
            }
        }
    }
    
    /**
     * Stop the game trainer
     * 
     * @param trainer GameTrainer instance
     */
    public static void stop(GameTrainer trainer) {
        if (trainer == null) {
            return;
        }
        
        try {
            // Try to call stop method
            trainer.getClass().getMethod("stop").invoke(trainer);
        } catch (Exception e) {
            try {
                // Try alternative method names
                trainer.getClass().getMethod("shutdown").invoke(trainer);
            } catch (Exception ex) {
                System.err.println("Failed to stop GameTrainer: " + ex.getMessage());
            }
        }
    }
    
    /**
     * Set the game type for the trainer
     * 
     * @param trainer GameTrainer instance
     * @param gameType String representing the game type
     */
    public static void setGameType(GameTrainer trainer, String gameType) {
        if (trainer == null || gameType == null) {
            return;
        }
        
        try {
            // Try to call setGameType method
            trainer.getClass().getMethod("setGameType", String.class).invoke(trainer, gameType);
        } catch (Exception e) {
            try {
                // Try alternative method names
                trainer.getClass().getMethod("configureForGame", String.class).invoke(trainer, gameType);
            } catch (Exception ex) {
                System.err.println("Failed to set game type: " + ex.getMessage());
            }
        }
    }
    
    /**
     * Start a new training session
     * 
     * @param trainer GameTrainer instance
     * @param sessionId Session identifier
     * @return Training session object
     */
    public static TrainingSession startTrainingSession(GameTrainer trainer, String sessionId) {
        if (trainer == null || sessionId == null) {
            return null;
        }
        
        try {
            // Try to call startTrainingSession method
            Object internalSession = trainer.getClass()
                .getMethod("startTrainingSession", String.class)
                .invoke(trainer, sessionId);
                
            if (internalSession != null) {
                return new TrainingSession(internalSession);
            }
            return null;
        } catch (Exception e) {
            try {
                // Try alternative method names
                Object internalSession = trainer.getClass()
                    .getMethod("beginSession", String.class)
                    .invoke(trainer, sessionId);
                    
                if (internalSession != null) {
                    return new TrainingSession(internalSession);
                }
                return null;
            } catch (Exception ex) {
                System.err.println("Failed to start training session: " + ex.getMessage());
                return null;
            }
        }
    }
    
    /**
     * End a training session
     * 
     * @param trainer GameTrainer instance
     * @param sessionId Session identifier
     * @return Success status
     */
    public static boolean endTrainingSession(GameTrainer trainer, String sessionId) {
        if (trainer == null || sessionId == null) {
            return false;
        }
        
        try {
            // Try to call endTrainingSession method
            return (boolean) trainer.getClass()
                .getMethod("endTrainingSession", String.class)
                .invoke(trainer, sessionId);
        } catch (Exception e) {
            try {
                // Try alternative method names
                return (boolean) trainer.getClass()
                    .getMethod("endSession", String.class)
                    .invoke(trainer, sessionId);
            } catch (Exception ex) {
                System.err.println("Failed to end training session: " + ex.getMessage());
                return false;
            }
        }
    }
    
    /**
     * Record a training example
     * 
     * @param trainer GameTrainer instance
     * @param sessionId Session identifier
     * @param state Current state
     * @param action Action taken
     * @param reward Reward received
     * @param nextState Resulting state
     * @return Success status
     */
    public static boolean recordExample(GameTrainer trainer,
                                     String sessionId,
                                     Map<String, Object> state,
                                     String action,
                                     double reward,
                                     Map<String, Object> nextState) {
        if (trainer == null || sessionId == null || state == null || action == null || nextState == null) {
            return false;
        }
        
        try {
            // Try to call recordExample method
            return (boolean) trainer.getClass()
                .getMethod("recordExample", String.class, Map.class, String.class, double.class, Map.class)
                .invoke(trainer, sessionId, state, action, reward, nextState);
        } catch (Exception e) {
            try {
                // Try alternative method names
                return (boolean) trainer.getClass()
                    .getMethod("recordTrainingExample", String.class, Map.class, String.class, double.class, Map.class)
                    .invoke(trainer, sessionId, state, action, reward, nextState);
            } catch (Exception ex) {
                System.err.println("Failed to record example: " + ex.getMessage());
                return false;
            }
        }
    }
    
    /**
     * Get recommended actions for a state
     * 
     * @param trainer GameTrainer instance
     * @param state Current state
     * @return List of action recommendations
     */
    public static List<ActionRecommendation> getRecommendedActions(GameTrainer trainer, Map<String, Object> state) {
        if (trainer == null || state == null) {
            return new ArrayList<>();
        }
        
        try {
            // Try to call getRecommendedActions method
            @SuppressWarnings("unchecked")
            List<ActionRecommendation> recommendations = (List<ActionRecommendation>) trainer.getClass()
                .getMethod("getRecommendedActions", Map.class)
                .invoke(trainer, state);
            
            return recommendations != null ? recommendations : new ArrayList<>();
        } catch (Exception e) {
            try {
                // Try alternative method names
                @SuppressWarnings("unchecked")
                List<ActionRecommendation> recommendations = (List<ActionRecommendation>) trainer.getClass()
                    .getMethod("recommendActions", Map.class)
                    .invoke(trainer, state);
                
                return recommendations != null ? recommendations : new ArrayList<>();
            } catch (Exception ex) {
                System.err.println("Failed to get recommended actions: " + ex.getMessage());
                return new ArrayList<>();
            }
        }
    }
    
    /**
     * Save the trained model
     * 
     * @param trainer GameTrainer instance
     * @param filepath Path to save the model
     * @return Success status
     */
    public static boolean saveModel(GameTrainer trainer, String filepath) {
        if (trainer == null || filepath == null) {
            return false;
        }
        
        try {
            // Try to call saveModel method
            return (boolean) trainer.getClass()
                .getMethod("saveModel", String.class)
                .invoke(trainer, filepath);
        } catch (Exception e) {
            try {
                // Try alternative method names
                return (boolean) trainer.getClass()
                    .getMethod("save", String.class)
                    .invoke(trainer, filepath);
            } catch (Exception ex) {
                System.err.println("Failed to save model: " + ex.getMessage());
                return false;
            }
        }
    }
    
    /**
     * Load a trained model
     * 
     * @param trainer GameTrainer instance
     * @param filepath Path to load the model from
     * @return Success status
     */
    public static boolean loadModel(GameTrainer trainer, String filepath) {
        if (trainer == null || filepath == null) {
            return false;
        }
        
        try {
            // Try to call loadModel method
            return (boolean) trainer.getClass()
                .getMethod("loadModel", String.class)
                .invoke(trainer, filepath);
        } catch (Exception e) {
            try {
                // Try alternative method names
                return (boolean) trainer.getClass()
                    .getMethod("load", String.class)
                    .invoke(trainer, filepath);
            } catch (Exception ex) {
                System.err.println("Failed to load model: " + ex.getMessage());
                return false;
            }
        }
    }
    
    /**
     * Get training statistics
     * 
     * @param trainer GameTrainer instance
     * @return Map of statistics
     */
    public static Map<String, Object> getStats(GameTrainer trainer) {
        if (trainer == null) {
            return new HashMap<>();
        }
        
        try {
            // Try to call getStats method
            @SuppressWarnings("unchecked")
            Map<String, Object> stats = (Map<String, Object>) trainer.getClass()
                .getMethod("getStats")
                .invoke(trainer);
            
            return stats != null ? stats : new HashMap<>();
        } catch (Exception e) {
            try {
                // Try alternative method names
                @SuppressWarnings("unchecked")
                Map<String, Object> stats = (Map<String, Object>) trainer.getClass()
                    .getMethod("getStatistics")
                    .invoke(trainer);
                
                return stats != null ? stats : new HashMap<>();
            } catch (Exception ex) {
                System.err.println("Failed to get stats: " + ex.getMessage());
                return new HashMap<>();
            }
        }
    }
}