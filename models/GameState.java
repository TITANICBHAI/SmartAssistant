package models;

import java.util.HashMap;
import java.util.Map;
import java.util.ArrayList;
import java.util.List;
import android.graphics.Bitmap;

/**
 * Represents the current state of a game.
 */
public class GameState {
    private Map<String, Object> stateData;
    private Map<models.ElementType, Integer> elementCounts;
    private List<models.UIElementInterface> activeElements;
    private utils.GameType gameType;
    private String screenType;
    private Bitmap screenshot;
    private Map<String, Object> metadata;
    private long timestamp;
    private int frameId;
    
    /**
     * Constructor
     */
    public GameState() {
        stateData = new HashMap<>();
        elementCounts = new HashMap<>();
        activeElements = new ArrayList<>();
        metadata = new HashMap<>();
        gameType = utils.GameType.UNKNOWN;
        screenType = "unknown";
        timestamp = System.currentTimeMillis();
        frameId = 0;
    }
    
    /**
     * Get state data
     * 
     * @return Map of state data
     */
    public Map<String, Object> getStateData() {
        return stateData;
    }
    
    /**
     * Set state data
     * 
     * @param stateData The state data to set
     */
    public void setStateData(Map<String, Object> stateData) {
        this.stateData = stateData != null ? new HashMap<>(stateData) : new HashMap<>();
    }
    
    /**
     * Get a state data value
     * 
     * @param key The key to get data for
     * @return The data value or null if not found
     */
    public Object getStateDataValue(String key) {
        return stateData.get(key);
    }
    
    /**
     * Set a state data value
     * 
     * @param key The key to set data for
     * @param value The data value to set
     */
    public void setStateDataValue(String key, Object value) {
        if (key != null) {
            stateData.put(key, value);
        }
    }
    
    /**
     * Get element counts
     * 
     * @return Map of element type to count
     */
    public Map<models.ElementType, Integer> getElementCounts() {
        return elementCounts;
    }
    
    /**
     * Set element counts
     * 
     * @param elementCounts The element counts to set
     */
    public void setElementCounts(Map<models.ElementType, Integer> elementCounts) {
        this.elementCounts = elementCounts != null ? new HashMap<>(elementCounts) : new HashMap<>();
    }
    
    /**
     * Get the count for a specific element type
     * 
     * @param type The element type
     * @return The count for the element type, or 0 if not found
     */
    public int getElementCount(models.ElementType type) {
        return elementCounts.getOrDefault(type, 0);
    }
    
    /**
     * Set the count for a specific element type
     * 
     * @param type The element type
     * @param count The count to set
     */
    public void setElementCount(models.ElementType type, int count) {
        if (type != null) {
            elementCounts.put(type, count);
        }
    }
    
    /**
     * Get active elements
     * 
     * @return List of active elements
     */
    public List<models.UIElementInterface> getActiveElements() {
        return activeElements;
    }
    
    /**
     * Set active elements
     * 
     * @param activeElements The active elements to set
     */
    public void setActiveElements(List<models.UIElementInterface> activeElements) {
        this.activeElements = activeElements != null ? new ArrayList<>(activeElements) : new ArrayList<>();
    }
    
    /**
     * Add an active element
     * 
     * @param element The element to add
     */
    public void addActiveElement(models.UIElementInterface element) {
        if (element != null) {
            activeElements.add(element);
        }
    }
    
    /**
     * Remove an active element
     * 
     * @param element The element to remove
     * @return True if the element was removed, false otherwise
     */
    public boolean removeActiveElement(models.UIElementInterface element) {
        return activeElements.remove(element);
    }
    
    /**
     * Clear active elements
     */
    public void clearActiveElements() {
        activeElements.clear();
    }
    
    /**
     * Get the game type
     * 
     * @return The game type
     */
    public utils.GameType getGameType() {
        return gameType;
    }
    
    /**
     * Set the game type
     * 
     * @param gameType The game type to set
     */
    public void setGameType(utils.GameType gameType) {
        this.gameType = gameType != null ? gameType : utils.GameType.UNKNOWN;
    }
    
    /**
     * Set the game type using a models.GameType
     * This provides compatibility with models package GameType
     * 
     * @param gameType The models.GameType to set
     */
    public void setGameType(models.GameType gameType) {
        if (gameType == null) {
            this.gameType = utils.GameType.UNKNOWN;
            return;
        }
        
        // Convert models.GameType to utils.GameType
        try {
            this.gameType = utils.GameType.valueOf(gameType.name());
        } catch (IllegalArgumentException e) {
            // Default to UNKNOWN if conversion fails
            this.gameType = utils.GameType.UNKNOWN;
        }
    }
    
    /**
     * Get the screen type
     * 
     * @return The screen type
     */
    public String getScreenType() {
        return screenType;
    }
    
    /**
     * Set the screen type
     * 
     * @param screenType The screen type to set
     */
    public void setScreenType(String screenType) {
        this.screenType = screenType != null ? screenType : "unknown";
    }
    
    /**
     * Get the screenshot
     * 
     * @return The screenshot bitmap
     */
    public Bitmap getScreenshot() {
        return screenshot;
    }
    
    /**
     * Set the screenshot
     * 
     * @param screenshot The screenshot bitmap to set
     */
    public void setScreenshot(Bitmap screenshot) {
        this.screenshot = screenshot;
    }
    
    /**
     * Get the metadata
     * 
     * @return The metadata map
     */
    public Map<String, Object> getMetadata() {
        return metadata;
    }
    
    /**
     * Set the metadata
     * 
     * @param metadata The metadata map to set
     */
    public void setMetadata(Map<String, Object> metadata) {
        this.metadata = metadata != null ? new HashMap<>(metadata) : new HashMap<>();
    }
    
    /**
     * Get a metadata value
     * 
     * @param key The key to get metadata for
     * @return The metadata value or null if not found
     */
    public Object getMetadataValue(String key) {
        return metadata.get(key);
    }
    
    /**
     * Set a metadata value
     * 
     * @param key The key to set metadata for
     * @param value The metadata value to set
     */
    public void setMetadataValue(String key, Object value) {
        if (key != null) {
            metadata.put(key, value);
        }
    }
    
    /**
     * Get the timestamp
     * 
     * @return The timestamp in milliseconds
     */
    public long getTimestamp() {
        return timestamp;
    }
    
    /**
     * Set the timestamp
     * 
     * @param timestamp The timestamp in milliseconds to set
     */
    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }
    
    /**
     * Set the timestamp
     * 
     * @param timestamp The timestamp in Long object form
     */
    public void setTimestamp(Long timestamp) {
        this.timestamp = timestamp != null ? timestamp : System.currentTimeMillis();
    }
    
    /**
     * Get the frame ID
     * 
     * @return The frame ID
     */
    public int getFrameId() {
        return frameId;
    }
    
    /**
     * Set the frame ID
     * 
     * @param frameId The frame ID to set
     */
    public void setFrameId(int frameId) {
        this.frameId = frameId;
    }
    
    /**
     * Set the frame ID
     * 
     * @param frameId The frame ID in Integer object form
     */
    public void setFrameId(Integer frameId) {
        this.frameId = frameId != null ? frameId : 0;
    }
    
    /**
     * Update the game state from an analysis map
     * 
     * @param analysisData The analysis data map
     */
    public void updateFromAnalysis(Map<String, Object> analysisData) {
        if (analysisData == null) {
            return;
        }
        
        // Update metadata with analysis data
        for (Map.Entry<String, Object> entry : analysisData.entrySet()) {
            setMetadataValue(entry.getKey(), entry.getValue());
        }
        
        // Extract and set specific data fields if present
        if (analysisData.containsKey("screen_type")) {
            setScreenType((String) analysisData.get("screen_type"));
        }
        
        if (analysisData.containsKey("game_type")) {
            Object gameTypeObj = analysisData.get("game_type");
            if (gameTypeObj instanceof utils.GameType) {
                setGameType((utils.GameType) gameTypeObj);
            } else if (gameTypeObj instanceof String) {
                setGameType(utils.GameType.fromString((String) gameTypeObj));
            }
        }
        
        if (analysisData.containsKey("timestamp")) {
            Object timestampObj = analysisData.get("timestamp");
            if (timestampObj instanceof Long) {
                setTimestamp((Long) timestampObj);
            } else if (timestampObj instanceof String) {
                try {
                    setTimestamp(Long.parseLong((String) timestampObj));
                } catch (NumberFormatException e) {
                    // Ignore and use default
                }
            }
        }
        
        if (analysisData.containsKey("frame_id")) {
            Object frameIdObj = analysisData.get("frame_id");
            if (frameIdObj instanceof Integer) {
                setFrameId((Integer) frameIdObj);
            } else if (frameIdObj instanceof String) {
                try {
                    setFrameId(Integer.parseInt((String) frameIdObj));
                } catch (NumberFormatException e) {
                    // Ignore and use default
                }
            }
        }
    }
    
    /**
     * Get a string representation of the game state
     * 
     * @return String representation
     */
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder("GameState:\n");
        
        sb.append("Game Type: ").append(gameType).append("\n");
        sb.append("Screen Type: ").append(screenType).append("\n");
        sb.append("Frame ID: ").append(frameId).append("\n");
        sb.append("Timestamp: ").append(timestamp).append("\n");
        sb.append("Screenshot: ").append(screenshot != null ? "Present" : "None").append("\n");
        
        sb.append("Element counts: ").append(elementCounts.size()).append("\n");
        for (Map.Entry<models.ElementType, Integer> entry : elementCounts.entrySet()) {
            sb.append("  ").append(entry.getKey()).append(": ").append(entry.getValue()).append("\n");
        }
        
        sb.append("Active elements: ").append(activeElements.size()).append("\n");
        for (models.UIElementInterface element : activeElements) {
            sb.append("  ").append(element).append("\n");
        }
        
        sb.append("State data: ").append(stateData.size()).append(" entries\n");
        for (Map.Entry<String, Object> entry : stateData.entrySet()) {
            sb.append("  ").append(entry.getKey()).append(": ");
            Object value = entry.getValue();
            if (value instanceof Map) {
                sb.append("{").append(((Map<?, ?>) value).size()).append(" entries}");
            } else if (value instanceof List) {
                sb.append("[").append(((List<?>) value).size()).append(" items]");
            } else {
                sb.append(value);
            }
            sb.append("\n");
        }
        
        sb.append("Metadata: ").append(metadata.size()).append(" entries\n");
        for (Map.Entry<String, Object> entry : metadata.entrySet()) {
            sb.append("  ").append(entry.getKey()).append(": ");
            Object value = entry.getValue();
            if (value instanceof Map) {
                sb.append("{").append(((Map<?, ?>) value).size()).append(" entries}");
            } else if (value instanceof List) {
                sb.append("[").append(((List<?>) value).size()).append(" items]");
            } else {
                sb.append(value);
            }
            sb.append("\n");
        }
        
        return sb.toString();
    }
}