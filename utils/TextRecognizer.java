package utils;

import java.util.ArrayList;
import java.util.List;

/**
 * Class for recognizing text in images.
 */
public class TextRecognizer {
    private Context context;
    private boolean isInitialized;
    
    /**
     * Constructor
     */
    public TextRecognizer() {
        this.isInitialized = false;
    }
    
    /**
     * Initialize the recognizer
     * 
     * @param context Application context
     */
    public void initialize(Context context) {
        if (isInitialized) {
            return;
        }
        
        this.context = context;
        
        // In a real app, this would initialize an OCR or ML model
        // For this mock version, we'll just set initialized to true
        
        isInitialized = true;
    }
    
    /**
     * Recognize text in an image
     * 
     * @param image Image to analyze
     * @return List of recognized text regions
     */
    public List<RecognizedText> recognizeText(Bitmap image) {
        if (!isInitialized || image == null) {
            return new ArrayList<>();
        }
        
        // In a real app, this would run OCR on the image
        // For this mock version, we'll just create some simulated detections
        
        List<RecognizedText> results = new ArrayList<>();
        
        // Full dimensions
        int width = image.getWidth();
        int height = image.getHeight();
        
        // Score/points text at top of screen
        results.add(new RecognizedText(
            "SCORE: 1250",
            RectHelper.toAndroidRect(new utils.Rect(width - 200, 30, width - 40, 70)),
            0.95f
        ));
        
        // Lives counter
        results.add(new RecognizedText(
            "LIVES: 3",
            RectHelper.toAndroidRect(new utils.Rect(40, 30, 150, 70)),
            0.92f
        ));
        
        // Game level
        results.add(new RecognizedText(
            "LEVEL 5",
            RectHelper.toAndroidRect(new utils.Rect(width/2 - 60, 30, width/2 + 60, 70)),
            0.93f
        ));
        
        // Button text
        results.add(new RecognizedText(
            "ATTACK",
            RectHelper.toAndroidRect(new utils.Rect(width/2 - 60, height - 120, width/2 + 60, height - 80)),
            0.88f
        ));
        
        // Menu button
        results.add(new RecognizedText(
            "MENU",
            RectHelper.toAndroidRect(new utils.Rect(width - 100, 40, width - 40, 80)),
            0.85f
        ));
        
        // Inventory label
        results.add(new RecognizedText(
            "ITEMS",
            RectHelper.toAndroidRect(new utils.Rect(width - 120, height - 120, width - 50, height - 80)),
            0.84f
        ));
        
        // Player name
        results.add(new RecognizedText(
            "PLAYER_1",
            RectHelper.toAndroidRect(new utils.Rect(50, height/2 - 70, 150, height/2 - 30)),
            0.87f
        ));
        
        return results;
    }
}