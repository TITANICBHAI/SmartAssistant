package utils;

import android.util.Log;
import java.lang.reflect.Method;
import java.lang.reflect.Field;

/**
 * Helper for resolving ambiguous ML-related classes
 */
public class MLClassResolver {
    private static final String TAG = "MLClassResolver";
    
    /**
     * Get the GameRuleUnderstanding class
     * @param preferredPackage The preferred package
     * @return The GameRuleUnderstanding class
     */
    public static Class<?> getGameRuleUnderstandingClass(String preferredPackage) {
        try {
            // Try the preferred package first
            try {
                return Class.forName(preferredPackage + ".GameRuleUnderstanding");
            } catch (Exception e) {
                // Class doesn't exist in the preferred package
            }
            
            // Try ml package
            try {
                return Class.forName("com.aiassistant.ml.GameRuleUnderstanding");
            } catch (Exception e) {
                // Class doesn't exist in ml package
            }
            
            // Try models package
            try {
                return Class.forName("com.aiassistant.models.GameRuleUnderstanding");
            } catch (Exception e) {
                // Class doesn't exist in models package
            }
            
            Log.e(TAG, "Failed to find GameRuleUnderstanding class");
            return null;
            
        } catch (Exception e) {
            Log.e(TAG, "Error getting GameRuleUnderstanding class: " + e.getMessage());
            return null;
        }
    }
    
    /**
     * Get the DeepRLModel class
     * @param preferredPackage The preferred package
     * @return The DeepRLModel class
     */
    public static Class<?> getDeepRLModelClass(String preferredPackage) {
        try {
            // Try the preferred package first
            try {
                return Class.forName(preferredPackage + ".DeepRLModel");
            } catch (Exception e) {
                // Class doesn't exist in the preferred package
            }
            
            // Try ml package
            try {
                return Class.forName("com.aiassistant.ml.DeepRLModel");
            } catch (Exception e) {
                // Class doesn't exist in ml package
            }
            
            // Try models package
            try {
                return Class.forName("com.aiassistant.models.DeepRLModel");
            } catch (Exception e) {
                // Class doesn't exist in models package
            }
            
            Log.e(TAG, "Failed to find DeepRLModel class");
            return null;
            
        } catch (Exception e) {
            Log.e(TAG, "Error getting DeepRLModel class: " + e.getMessage());
            return null;
        }
    }
    
    /**
     * Get the GameTrainer class
     * @param preferredPackage The preferred package
     * @return The GameTrainer class
     */
    public static Class<?> getGameTrainerClass(String preferredPackage) {
        try {
            // Try the preferred package first
            try {
                return Class.forName(preferredPackage + ".GameTrainer");
            } catch (Exception e) {
                // Class doesn't exist in the preferred package
            }
            
            // Try ml package
            try {
                return Class.forName("com.aiassistant.ml.GameTrainer");
            } catch (Exception e) {
                // Class doesn't exist in ml package
            }
            
            // Try models package
            try {
                return Class.forName("com.aiassistant.models.GameTrainer");
            } catch (Exception e) {
                // Class doesn't exist in models package
            }
            
            Log.e(TAG, "Failed to find GameTrainer class");
            return null;
            
        } catch (Exception e) {
            Log.e(TAG, "Error getting GameTrainer class: " + e.getMessage());
            return null;
        }
    }
    
    /**
     * Get the RuleExtractionSystem class
     * @param preferredPackage The preferred package
     * @return The RuleExtractionSystem class
     */
    public static Class<?> getRuleExtractionSystemClass(String preferredPackage) {
        try {
            // Try the preferred package first
            try {
                return Class.forName(preferredPackage + ".RuleExtractionSystem");
            } catch (Exception e) {
                // Class doesn't exist in the preferred package
            }
            
            // Try ml package
            try {
                return Class.forName("com.aiassistant.ml.RuleExtractionSystem");
            } catch (Exception e) {
                // Class doesn't exist in ml package
            }
            
            // Try models package
            try {
                return Class.forName("com.aiassistant.models.RuleExtractionSystem");
            } catch (Exception e) {
                // Class doesn't exist in models package
            }
            
            Log.e(TAG, "Failed to find RuleExtractionSystem class");
            return null;
            
        } catch (Exception e) {
            Log.e(TAG, "Error getting RuleExtractionSystem class: " + e.getMessage());
            return null;
        }
    }
    
    /**
     * Get the ScreenshotManager class
     * @param preferredPackage The preferred package
     * @return The ScreenshotManager class
     */
    public static Class<?> getScreenshotManagerClass(String preferredPackage) {
        try {
            // Try the preferred package first
            try {
                return Class.forName(preferredPackage + ".ScreenshotManager");
            } catch (Exception e) {
                // Class doesn't exist in the preferred package
            }
            
            // Try utils package
            try {
                return Class.forName("com.aiassistant.utils.ScreenshotManager");
            } catch (Exception e) {
                // Class doesn't exist in utils package
            }
            
            // Try models package
            try {
                return Class.forName("com.aiassistant.models.ScreenshotManager");
            } catch (Exception e) {
                // Class doesn't exist in models package
            }
            
            Log.e(TAG, "Failed to find ScreenshotManager class");
            return null;
            
        } catch (Exception e) {
            Log.e(TAG, "Error getting ScreenshotManager class: " + e.getMessage());
            return null;
        }
    }
    
    /**
     * Create a new instance of GameRuleUnderstanding
     * @param preferredPackage The preferred package
     * @return The new instance
     */
    public static Object createGameRuleUnderstanding(String preferredPackage) {
        try {
            Class<?> clazz = getGameRuleUnderstandingClass(preferredPackage);
            if (clazz != null) {
                return clazz.newInstance();
            }
            return null;
        } catch (Exception e) {
            Log.e(TAG, "Error creating GameRuleUnderstanding: " + e.getMessage());
            return null;
        }
    }
    
    /**
     * Create a new instance of DeepRLModel
     * @param preferredPackage The preferred package
     * @return The new instance
     */
    public static Object createDeepRLModel(String preferredPackage) {
        try {
            Class<?> clazz = getDeepRLModelClass(preferredPackage);
            if (clazz != null) {
                return clazz.newInstance();
            }
            return null;
        } catch (Exception e) {
            Log.e(TAG, "Error creating DeepRLModel: " + e.getMessage());
            return null;
        }
    }
    
    /**
     * Create a new instance of GameTrainer
     * @param preferredPackage The preferred package
     * @return The new instance
     */
    public static Object createGameTrainer(String preferredPackage) {
        try {
            Class<?> clazz = getGameTrainerClass(preferredPackage);
            if (clazz != null) {
                return clazz.newInstance();
            }
            return null;
        } catch (Exception e) {
            Log.e(TAG, "Error creating GameTrainer: " + e.getMessage());
            return null;
        }
    }
    
    /**
     * Create a new instance of RuleExtractionSystem
     * @param preferredPackage The preferred package
     * @return The new instance
     */
    public static Object createRuleExtractionSystem(String preferredPackage) {
        try {
            Class<?> clazz = getRuleExtractionSystemClass(preferredPackage);
            if (clazz != null) {
                return clazz.newInstance();
            }
            return null;
        } catch (Exception e) {
            Log.e(TAG, "Error creating RuleExtractionSystem: " + e.getMessage());
            return null;
        }
    }
    
    /**
     * Create a new instance of ScreenshotManager
     * @param preferredPackage The preferred package
     * @return The new instance
     */
    public static Object createScreenshotManager(String preferredPackage) {
        try {
            Class<?> clazz = getScreenshotManagerClass(preferredPackage);
            if (clazz != null) {
                return clazz.newInstance();
            }
            return null;
        } catch (Exception e) {
            Log.e(TAG, "Error creating ScreenshotManager: " + e.getMessage());
            return null;
        }
    }
    
    /**
     * Set the game type on an ML object
     * @param object The ML object
     * @param gameType The game type
     */
    public static void setGameType(Object object, Object gameType) {
        if (object == null || gameType == null) {
            return;
        }
        
        try {
            Method setGameTypeMethod = object.getClass().getMethod("setGameType", gameType.getClass());
            setGameTypeMethod.invoke(object, gameType);
        } catch (Exception e) {
            Log.e(TAG, "Error setting game type: " + e.getMessage());
        }
    }
    
    /**
     * Get a helper class instance based on the resolved class
     * @param resolvedClass The resolved class
     * @param interfaceClass The interface class that the helper should implement
     * @return The helper instance
     */
    public static Object getHelperForClass(Class<?> resolvedClass, Class<?> interfaceClass) {
        if (resolvedClass == null || interfaceClass == null) {
            return null;
        }
        
        try {
            String helperClassName = "utils." + resolvedClass.getSimpleName() + "Helper";
            Class<?> helperClass = Class.forName(helperClassName);
            
            if (interfaceClass.isAssignableFrom(helperClass)) {
                return helperClass.newInstance();
            }
            
            return null;
        } catch (Exception e) {
            Log.e(TAG, "Error getting helper for class: " + e.getMessage());
            return null;
        }
    }
}