package utils;

/**
 * Utility class for converting between different ElementType implementations
 */
public class ElementTypeConverter {
    /**
     * Convert a utils.ElementType to a models.ElementType
     * 
     * @param type The utils.ElementType
     * @return The equivalent models.ElementType
     */
    public static models.ElementType toModelsElementType(utils.ElementType type) {
        if (type == null) {
            return models.ElementType.UNKNOWN;
        }
        
        try {
            // Try to match by name first
            return models.ElementType.valueOf(type.name());
        } catch (IllegalArgumentException e) {
            // Handle special cases that don't match by name
            if (type == utils.ElementType.RADIO || type == utils.ElementType.RADIO_BUTTON) {
                return models.ElementType.RADIO_BUTTON;
            } else if (type == utils.ElementType.PROGRESS || type == utils.ElementType.PROGRESS_BAR) {
                return models.ElementType.PROGRESS_BAR;
            } else if (type == utils.ElementType.INPUT || type == utils.ElementType.TEXT_FIELD || 
                       type == utils.ElementType.INPUT_FIELD) {
                return models.ElementType.TEXT_FIELD;
            } else if (type == utils.ElementType.SCROLL_VIEW || type == utils.ElementType.SCROLLVIEW) {
                return models.ElementType.SCROLL_VIEW;
            } else if (type == utils.ElementType.GAME_CONTROL) {
                return models.ElementType.GAME_OBJECT;
            } else if (type == utils.ElementType.NAVIGATION_ITEM) {
                return models.ElementType.MENU_ITEM;
            } else if (type == utils.ElementType.FOOTER) {
                return models.ElementType.FOREGROUND;
            } else {
                // Default fallback
                return models.ElementType.UNKNOWN;
            }
        }
    }
    
    /**
     * Convert a models.ElementType to a utils.ElementType
     * 
     * @param type The models.ElementType
     * @return The equivalent utils.ElementType
     */
    public static utils.ElementType toUtilsElementType(models.ElementType type) {
        if (type == null) {
            return utils.ElementType.UNKNOWN;
        }
        
        try {
            // Try to match by name first
            return utils.ElementType.valueOf(type.name());
        } catch (IllegalArgumentException e) {
            // Handle special cases that don't match by name
            if (type == models.ElementType.RADIO_BUTTON) {
                return utils.ElementType.RADIO;
            } else if (type == models.ElementType.PROGRESS_BAR) {
                return utils.ElementType.PROGRESS;
            } else if (type == models.ElementType.TEXT_FIELD || 
                       type == models.ElementType.INPUT || 
                       type == models.ElementType.INPUT_FIELD) {
                return utils.ElementType.INPUT;
            } else if (type == models.ElementType.SCROLL_VIEW) {
                return utils.ElementType.SCROLL_VIEW;
            } else if (type == models.ElementType.GAME_OBJECT) {
                return utils.ElementType.GAME_CONTROL;
            } else if (type == models.ElementType.MENU_ITEM) {
                return utils.ElementType.NAVIGATION_ITEM;
            } else if (type == models.ElementType.FOREGROUND) {
                return utils.ElementType.FOOTER;
            } else if (type == models.ElementType.PLAYER || 
                       type == models.ElementType.ENEMY ||
                       type == models.ElementType.COLLECTIBLE ||
                       type == models.ElementType.POWERUP ||
                       type == models.ElementType.OBSTACLE ||
                       type == models.ElementType.PLATFORM) {
                return utils.ElementType.GAME_CONTROL;
            } else {
                // Default fallback
                return utils.ElementType.UNKNOWN;
            }
        }
    }
    
    /**
     * Convert a string type name to a utils.ElementType
     * 
     * @param typeName The type name
     * @return The utils.ElementType or UNKNOWN if not found
     */
    public static utils.ElementType toUtilsElementType(String typeName) {
        if (typeName == null || typeName.isEmpty()) {
            return utils.ElementType.UNKNOWN;
        }
        
        try {
            return utils.ElementType.valueOf(typeName.toUpperCase());
        } catch (IllegalArgumentException e) {
            return utils.ElementType.UNKNOWN;
        }
    }
    
    /**
     * Convert a string type name to a models.ElementType
     * 
     * @param typeName The type name
     * @return The models.ElementType or UNKNOWN if not found
     */
    public static models.ElementType toModelsElementType(String typeName) {
        if (typeName == null || typeName.isEmpty()) {
            return models.ElementType.UNKNOWN;
        }
        
        try {
            return models.ElementType.valueOf(typeName.toUpperCase());
        } catch (IllegalArgumentException e) {
            return models.ElementType.UNKNOWN;
        }
    }
}