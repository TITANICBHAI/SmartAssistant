package models;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.lang.reflect.Method;
import android.graphics.Rect;
import utils.UIElement;

/**
 * Analyzer for game scenes, extracting UI elements and game state information.
 */
public class GameSceneAnalyzer {
    private List<models.UIElementInterface> detectedElements;
    private GameState gameState;
    private Map<String, Object> sceneData;
    
    /**
     * Constructor
     */
    public GameSceneAnalyzer() {
        detectedElements = new ArrayList<>();
        gameState = new GameState();
        sceneData = new HashMap<>();
        setupDefaultColors();
    }
    
    /**
     * Setup default color mappings for different game types
     */
    private void setupDefaultColors() {
        // Map of game type to enemy colors (RGB values)
        // Use a type-safe enum map for known types
        Map<String, int[]> enemyColors = new HashMap<>();
        
        // Add default enemy color mappings for different game types
        // We use string keys to avoid direct dependency on utils.AIController.GameType
        // Get the enum constants through the proxy class
        enemyColors.put("PUBG_MOBILE", new int[] {
            0xFF0000, // Red
            0xFF3300, // Red-orange
            0xFF6600  // Orange-red
        });
        
        enemyColors.put("FREE_FIRE", new int[] {
            0xFF0000, // Red
            0xFFFF00, // Yellow
            0xFF00FF  // Magenta
        });
        
        enemyColors.put("CLASH_OF_CLANS", new int[] {
            0xFF0000, // Red
            0x990000  // Dark red
        });
        
        enemyColors.put("POKEMON_UNITE", new int[] {
            0xFF0000, // Red
            0x0000FF  // Blue
        });
        
        enemyColors.put("MOBA", new int[] {
            0xFF0000, // Red
            0x0000FF, // Blue
            0xFF00FF  // Magenta
        });
        
        // Store in scene data
        sceneData.put("enemyColors", enemyColors);
    }
    
    /**
     * Analyze a screenshot and extract UI elements
     * 
     * @param data The image data to analyze
     * @return List of detected UI elements
     */
    public List<models.UIElementInterface> analyzeScene(byte[] data) {
        // Mock implementation
        detectedElements.clear();
        // In a real implementation, this would use computer vision to detect elements
        return detectedElements;
    }
    
    /**
     * Get UI elements of a specific type
     * 
     * @param type The element type to filter by
     * @return List of UI elements of the specified type
     */
    public List<models.UIElementInterface> getElementsByType(ElementType type) {
        List<models.UIElementInterface> result = new ArrayList<>();
        for (models.UIElementInterface element : detectedElements) {
            if (element.getType() != null && 
                element.getType().equalsIgnoreCase(type.name())) {
                result.add(element);
            }
        }
        return result;
    }
    
    /**
     * Get UI elements by a string type name
     * 
     * @param typeName The element type name
     * @return List of UI elements of the specified type
     */
    public List<models.UIElementInterface> getElementsByTypeName(String typeName) {
        List<models.UIElementInterface> result = new ArrayList<>();
        for (models.UIElementInterface element : detectedElements) {
            if (element.getType() != null && element.getType().equalsIgnoreCase(typeName)) {
                result.add(element);
            }
        }
        return result;
    }
    
    /**
     * Get UI elements containing specific text
     * 
     * @param text The text to search for
     * @return List of UI elements containing the specified text
     */
    public List<models.UIElementInterface> getElementsByText(String text) {
        List<models.UIElementInterface> result = new ArrayList<>();
        for (models.UIElementInterface element : detectedElements) {
            if (element.getText() != null && element.getText().contains(text)) {
                result.add(element);
            }
        }
        return result;
    }
    
    /**
     * Get UI elements at a specific position
     * 
     * @param x X coordinate
     * @param y Y coordinate
     * @return List of UI elements at the specified position
     */
    public List<models.UIElementInterface> getElementsAtPosition(int x, int y) {
        List<models.UIElementInterface> result = new ArrayList<>();
        for (models.UIElementInterface element : detectedElements) {
            if (element.contains(x, y)) {
                result.add(element);
            }
        }
        return result;
    }
    
    /**
     * Get UI elements in a specific region
     * 
     * @param rect The region to search in
     * @return List of UI elements in the specified region
     */
    public List<models.UIElementInterface> getElementsInRegion(android.graphics.Rect rect) {
        List<models.UIElementInterface> result = new ArrayList<>();
        for (models.UIElementInterface element : detectedElements) {
            // Get the bounds as utils.Rect and convert if needed
            android.graphics.Rect elementBounds = null;
            if (element.getRectBounds() == null) {
                // Skip if null bounds
                continue;
            }
            
            if (element.getRectBounds() instanceof utils.Rect) {
                // Convert utils.Rect to android.graphics.Rect
                elementBounds = RectConverter.toAndroidRect((utils.Rect) element.getRectBounds());
            } else {
                // For any other type, try to convert through RectConverter
                try {
                    elementBounds = RectConverter.toAndroidRect(element.getRectBounds());
                } catch (Exception e) {
                    // Skip if conversion failed
                    continue;
                }
            }
            
            if (elementBounds != null) {
                // Use RectConverter to check for intersection
                if (RectConverter.intersects(elementBounds, rect)) {
                    result.add(element);
                }
            }
        }
        return result;
    }
    
    /**
     * Get all detected UI elements
     * 
     * @return List of all detected UI elements
     */
    public List<models.UIElementInterface> getAllElements() {
        return new ArrayList<>(detectedElements);
    }
    
    /**
     * Update the game state based on the current scene
     * 
     * @return Updated game state
     */
    public GameState updateGameState() {
        if (gameState == null) {
            gameState = new GameState();
        }
        
        // Update game state based on detected elements
        // For example, count elements by type
        Map<models.ElementType, Integer> elementCounts = new HashMap<>();
        for (models.UIElementInterface element : detectedElements) {
            String typeStr = element.getType();
            if (typeStr != null) {
                models.ElementType type = ElementType.fromString(typeStr);
                elementCounts.put(type, elementCounts.getOrDefault(type, 0) + 1);
            }
        }
        
        gameState.setElementCounts(elementCounts);
        return gameState;
    }
    
    /**
     * Set the current game state
     * 
     * @param gameState The game state to set
     */
    public void setGameState(GameState gameState) {
        this.gameState = gameState;
    }
    
    /**
     * Get the current game state
     * 
     * @return The current game state
     */
    public GameState getGameState() {
        return gameState;
    }
    
    /**
     * Add a UI element to the detected elements list
     * 
     * @param element The element to add
     */
    public void addElement(models.UIElementInterface element) {
        if (element != null) {
            // element is already a models.UIElementInterface, so we can directly add it
            detectedElements.add(element);
        }
    }
    
    /**
     * Add a utils.UIElementInterface to the detected elements list
     * 
     * @param element The element to add
     */
    public void addElementFromUtils(utils.UIElementInterface element) {
        if (element != null) {
            // Wrap the utils.UIElementInterface in a UIElementWrapper
            UIElementWrapper wrapper = new UIElementWrapper(element);
            models.UIElementInterface interfaceElement = wrapper;
            detectedElements.add(interfaceElement);
        }
    }
    
    /**
     * Add a utils.UIElement to the detected elements list by wrapping it
     * 
     * @param element The element to add
     */
    public void addElement(UIElement element) {
        if (element != null) {
            // Handle different types of UIElement - might be utils.UIElement, utils.UIElementInterface, or a direct implementation
            if (element instanceof utils.UIElementInterface) {
                UIElementWrapper wrapper = new UIElementWrapper((utils.UIElementInterface)element);
                // Store the wrapper object in a model.UIElementInterface list
                models.UIElementInterface interfaceElement = wrapper;
                detectedElements.add(interfaceElement);
            } else {
                try {
                    // Create a generic wrapper
                    UIElementWrapper wrapper = new UIElementWrapper();
                    wrapper.setBounds(element.getBounds());
                    wrapper.setType(element.getType());
                    wrapper.setId(element.getId());
                    wrapper.setText(element.getText());
                    wrapper.setClickable(element.isClickable());
                    models.UIElementInterface interfaceElement = wrapper;
                    detectedElements.add(interfaceElement);
                } catch (Exception e) {
                    System.err.println("Error wrapping UIElement: " + e.getMessage());
                }
            }
        }
    }
    
    /**
     * Remove a UI element from the detected elements list
     * 
     * @param element The element to remove
     * @return True if the element was removed, false otherwise
     */
    public boolean removeElement(models.UIElementInterface element) {
        return detectedElements.remove(element);
    }
    
    /**
     * Clear all detected elements
     */
    public void clearElements() {
        detectedElements.clear();
    }
    
    /**
     * Get scene data
     * 
     * @return Map of scene data
     */
    public Map<String, Object> getSceneData() {
        return sceneData;
    }
    
    /**
     * Set scene data
     * 
     * @param sceneData The scene data to set
     */
    public void setSceneData(Map<String, Object> sceneData) {
        this.sceneData = sceneData != null ? sceneData : new HashMap<>();
    }
    
    /**
     * Get a scene data value
     * 
     * @param key The key to get data for
     * @return The data value or null if not found
     */
    public Object getSceneDataValue(String key) {
        return sceneData.get(key);
    }
    
    /**
     * Set a scene data value
     * 
     * @param key The key to set data for
     * @param value The data value to set
     */
    public void setSceneDataValue(String key, Object value) {
        if (key != null) {
            sceneData.put(key, value);
        }
    }
}