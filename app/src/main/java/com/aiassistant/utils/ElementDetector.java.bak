package com.aiassistant.utils;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.Rect;
import android.util.Log;

import org.tensorflow.lite.Interpreter;

import java.io.File;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Detects UI elements and text in screenshots
 */
public class ElementDetector {
    private static final String TAG = "ElementDetector";
    
    private Context context;
    private Interpreter objectDetectionModel;
    private Interpreter textRecognitionModel;
    private ExecutorService executorService;
    
    // Model parameters
    private static final int INPUT_WIDTH = 300;
    private static final int INPUT_HEIGHT = 300;
    private static final int MAX_DETECTIONS = 20;
    private static final float DETECTION_THRESHOLD = 0.5f;
    
    // Element types
    public enum ElementType {
        BUTTON,
        TEXT_FIELD,
        CHECKBOX,
        TOGGLE,
        RADIO_BUTTON,
        DROPDOWN,
        SLIDER,
        MENU_ITEM,
        IMAGE,
        TEXT,
        OTHER
    }
    
    /**
     * Create a new ElementDetector
     * 
     * @param context Application context
     */
    public ElementDetector(Context context) {
        this.context = context;
        this.executorService = Executors.newCachedThreadPool();
        
        // Initialize TensorFlow Lite models
        initializeModels();
    }
    
    /**
     * Initialize TensorFlow Lite models
     */
    private void initializeModels() {
        // Load models asynchronously
        executorService.execute(() -> {
            try {
                // Load object detection model
                File objectDetectionFile = new File(context.getFilesDir(), "element_detector.tflite");
                if (objectDetectionFile.exists()) {
                    objectDetectionModel = new Interpreter(objectDetectionFile);
                    Log.d(TAG, "Loaded object detection model");
                } else {
                    Log.w(TAG, "Object detection model file not found");
                    // In a real app, we would download or extract the model here
                }
                
                // Load text recognition model
                File textRecognitionFile = new File(context.getFilesDir(), "text_recognition.tflite");
                if (textRecognitionFile.exists()) {
                    textRecognitionModel = new Interpreter(textRecognitionFile);
                    Log.d(TAG, "Loaded text recognition model");
                } else {
                    Log.w(TAG, "Text recognition model file not found");
                    // In a real app, we would download or extract the model here
                }
            } catch (Exception e) {
                Log.e(TAG, "Error initializing models: " + e.getMessage());
            }
        });
    }
    
    /**
     * Detect UI elements in an image
     * 
     * @param image The image to analyze
     * @return Map of detected elements and their properties
     */
    public Map<String, Object> detectElements(Bitmap image) {
        Map<String, Object> results = new HashMap<>();
        
        try {
            // Detect UI elements using deep learning model
            if (objectDetectionModel != null) {
                results.putAll(detectElementsWithModel(image));
            } else {
                // Fall back to traditional methods if model not available
                results.putAll(detectElementsTraditional(image));
            }
            
            // Detect text in the image
            Map<String, Object> textResults = detectText(image);
            if (textResults != null && !textResults.isEmpty()) {
                results.put("text", textResults);
            }
            
            // Count elements by type
            Map<ElementType, Integer> elementCounts = countElementsByType(results);
            results.put("element_counts", elementCounts);
            
        } catch (Exception e) {
            Log.e(TAG, "Error detecting elements: " + e.getMessage());
        }
        
        return results;
    }
    
    /**
     * Detect UI elements using deep learning model
     */
    private Map<String, Object> detectElementsWithModel(Bitmap image) {
        Map<String, Object> results = new HashMap<>();
        List<Map<String, Object>> elements = new ArrayList<>();
        
        try {
            // Resize image for model input
            Bitmap resizedImage = Bitmap.createScaledBitmap(image, INPUT_WIDTH, INPUT_HEIGHT, true);
            
            // Prepare input buffer
            ByteBuffer inputBuffer = ByteBuffer.allocateDirect(INPUT_WIDTH * INPUT_HEIGHT * 3 * 4);
            inputBuffer.order(ByteOrder.nativeOrder());
            inputBuffer.rewind();
            
            // Fill input buffer with image data
            for (int y = 0; y < INPUT_HEIGHT; y++) {
                for (int x = 0; x < INPUT_WIDTH; x++) {
                    int pixel = resizedImage.getPixel(x, y);
                    inputBuffer.putFloat(((pixel >> 16) & 0xFF) / 255.0f); // R
                    inputBuffer.putFloat(((pixel >> 8) & 0xFF) / 255.0f);  // G
                    inputBuffer.putFloat((pixel & 0xFF) / 255.0f);         // B
                }
            }
            
            // Prepare output buffers
            float[][][] outputLocations = new float[1][MAX_DETECTIONS][4];
            float[][] outputClasses = new float[1][MAX_DETECTIONS];
            float[][] outputScores = new float[1][MAX_DETECTIONS];
            float[] numDetections = new float[1];
            
            Map<Integer, Object> outputs = new HashMap<>();
            outputs.put(0, outputLocations);
            outputs.put(1, outputClasses);
            outputs.put(2, outputScores);
            outputs.put(3, numDetections);
            
            // Run inference
            objectDetectionModel.runForMultipleInputsOutputs(new Object[]{inputBuffer}, outputs);
            
            // Process results
            int numDetected = (int) numDetections[0];
            
            // Scale factors for converting normalized coordinates to original image
            float scaleX = (float) image.getWidth() / INPUT_WIDTH;
            float scaleY = (float) image.getHeight() / INPUT_HEIGHT;
            
            for (int i = 0; i < numDetected; i++) {
                float score = outputScores[0][i];
                
                // Filter by confidence threshold
                if (score >= DETECTION_THRESHOLD) {
                    // Get class index
                    int classIndex = (int) outputClasses[0][i];
                    ElementType elementType = getElementTypeFromIndex(classIndex);
                    
                    // Get bounding box
                    float top = outputLocations[0][i][0] * scaleY;
                    float left = outputLocations[0][i][1] * scaleX;
                    float bottom = outputLocations[0][i][2] * scaleY;
                    float right = outputLocations[0][i][3] * scaleX;
                    
                    // Create element info
                    Map<String, Object> element = new HashMap<>();
                    element.put("type", elementType.name());
                    element.put("confidence", score);
                    element.put("bounds", new Rect((int) left, (int) top, (int) right, (int) bottom));
                    
                    // Add to results
                    elements.add(element);
                }
            }
            
            results.put("elements", elements);
            
        } catch (Exception e) {
            Log.e(TAG, "Error detecting elements with model: " + e.getMessage());
        }
        
        return results;
    }
    
    /**
     * Detect UI elements using traditional computer vision
     */
    private Map<String, Object> detectElementsTraditional(Bitmap image) {
        Map<String, Object> results = new HashMap<>();
        List<Map<String, Object>> elements = new ArrayList<>();
        
        try {
            // Detect potential buttons
            List<Map<String, Object>> buttons = detectButtons(image);
            elements.addAll(buttons);
            
            // Detect potential text fields
            List<Map<String, Object>> textFields = detectTextFields(image);
            elements.addAll(textFields);
            
            // Detect checkboxes
            List<Map<String, Object>> checkboxes = detectCheckboxes(image);
            elements.addAll(checkboxes);
            
            // Detect toggles
            List<Map<String, Object>> toggles = detectToggles(image);
            elements.addAll(toggles);
            
            results.put("elements", elements);
            
        } catch (Exception e) {
            Log.e(TAG, "Error detecting elements using traditional method: " + e.getMessage());
        }
        
        return results;
    }
    
    /**
     * Detect buttons using traditional image processing
     */
    private List<Map<String, Object>> detectButtons(Bitmap image) {
        List<Map<String, Object>> buttons = new ArrayList<>();
        
        // Get image dimensions
        int width = image.getWidth();
        int height = image.getHeight();
        
        // Find rectangular shapes with consistent color
        List<Rect> rectangles = findRectangles(image);
        
        for (Rect rect : rectangles) {
            // Check if rectangle has properties of a button
            if (isLikelyButton(image, rect)) {
                Map<String, Object> button = new HashMap<>();
                button.put("type", ElementType.BUTTON.name());
                button.put("confidence", 0.7f);
                button.put("bounds", rect);
                
                buttons.add(button);
            }
        }
        
        return buttons;
    }
    
    /**
     * Detect text fields using traditional image processing
     */
    private List<Map<String, Object>> detectTextFields(Bitmap image) {
        List<Map<String, Object>> textFields = new ArrayList<>();
        
        // Get image dimensions
        int width = image.getWidth();
        int height = image.getHeight();
        
        // Find rectangular shapes with consistent color
        List<Rect> rectangles = findRectangles(image);
        
        for (Rect rect : rectangles) {
            // Check if rectangle has properties of a text field
            if (isLikelyTextField(image, rect)) {
                Map<String, Object> textField = new HashMap<>();
                textField.put("type", ElementType.TEXT_FIELD.name());
                textField.put("confidence", 0.7f);
                textField.put("bounds", rect);
                
                textFields.add(textField);
            }
        }
        
        return textFields;
    }
    
    /**
     * Detect checkboxes using traditional image processing
     */
    private List<Map<String, Object>> detectCheckboxes(Bitmap image) {
        List<Map<String, Object>> checkboxes = new ArrayList<>();
        
        // Get image dimensions
        int width = image.getWidth();
        int height = image.getHeight();
        
        // Find small square shapes
        List<Rect> squares = findSquares(image);
        
        for (Rect rect : squares) {
            // Check if square has properties of a checkbox
            if (isLikelyCheckbox(image, rect)) {
                Map<String, Object> checkbox = new HashMap<>();
                checkbox.put("type", ElementType.CHECKBOX.name());
                checkbox.put("confidence", 0.7f);
                checkbox.put("bounds", rect);
                
                // Check if it's checked
                boolean isChecked = isCheckboxChecked(image, rect);
                checkbox.put("checked", isChecked);
                
                checkboxes.add(checkbox);
            }
        }
        
        return checkboxes;
    }
    
    /**
     * Detect toggles using traditional image processing
     */
    private List<Map<String, Object>> detectToggles(Bitmap image) {
        List<Map<String, Object>> toggles = new ArrayList<>();
        
        // Get image dimensions
        int width = image.getWidth();
        int height = image.getHeight();
        
        // Find small rectangular shapes with round corners
        List<Rect> toggleRects = findPotentialToggles(image);
        
        for (Rect rect : toggleRects) {
            // Check if it has properties of a toggle
            if (isLikelyToggle(image, rect)) {
                Map<String, Object> toggle = new HashMap<>();
                toggle.put("type", ElementType.TOGGLE.name());
                toggle.put("confidence", 0.7f);
                toggle.put("bounds", rect);
                
                // Check if it's on
                boolean isOn = isToggleOn(image, rect);
                toggle.put("on", isOn);
                
                toggles.add(toggle);
            }
        }
        
        return toggles;
    }
    
    /**
     * Find rectangular shapes in image
     */
    private List<Rect> findRectangles(Bitmap image) {
        List<Rect> rectangles = new ArrayList<>();
        
        // Image dimensions
        int width = image.getWidth();
        int height = image.getHeight();
        
        // Minimum rectangle size
        int minWidth = 50;
        int minHeight = 30;
        
        // Downsample factor for faster processing
        int stepSize = 10;
        
        // Keep track of processed areas
        boolean[][] processed = new boolean[width][height];
        
        // Scan image
        for (int y = 0; y < height; y += stepSize) {
            for (int x = 0; x < width; x += stepSize) {
                if (processed[x][y]) continue;
                
                // Get pixel color
                int pixel = image.getPixel(x, y);
                
                // Try to expand to rectangle
                Rect rect = expandToRectangle(image, x, y, pixel, processed);
                
                // Check size
                if (rect.width() >= minWidth && rect.height() >= minHeight) {
                    rectangles.add(rect);
                }
            }
        }
        
        return rectangles;
    }
    
    /**
     * Expand from a point to find a rectangle of similar color
     */
    private Rect expandToRectangle(Bitmap image, int startX, int startY, int startColor, boolean[][] processed) {
        int width = image.getWidth();
        int height = image.getHeight();
        
        // Color similarity threshold
        int colorThreshold = 30;
        
        // Find right bound
        int right = startX;
        while (right < width - 1) {
            int pixel = image.getPixel(right + 1, startY);
            if (!isSimilarColor(pixel, startColor, colorThreshold)) {
                break;
            }
            right++;
        }
        
        // Find bottom bound
        int bottom = startY;
        boolean uniform = true;
        
        while (bottom < height - 1 && uniform) {
            // Check if next row is uniform color
            for (int x = startX; x <= right; x++) {
                int pixel = image.getPixel(x, bottom + 1);
                if (!isSimilarColor(pixel, startColor, colorThreshold)) {
                    uniform = false;
                    break;
                }
            }
            
            if (uniform) {
                bottom++;
            }
        }
        
        // Mark area as processed
        for (int y = startY; y <= bottom; y++) {
            for (int x = startX; x <= right; x++) {
                if (x < width && y < height) {
                    processed[x][y] = true;
                }
            }
        }
        
        return new Rect(startX, startY, right, bottom);
    }
    
    /**
     * Find square shapes in image
     */
    private List<Rect> findSquares(Bitmap image) {
        List<Rect> squares = new ArrayList<>();
        
        // Get rectangles first
        List<Rect> rectangles = findRectangles(image);
        
        // Filter for squares
        for (Rect rect : rectangles) {
            int width = rect.width();
            int height = rect.height();
            
            // Check if square-ish (1:1 ratio approximately)
            float ratio = (float) width / height;
            if (ratio >= 0.8f && ratio <= 1.2f && width <= 100) {
                squares.add(rect);
            }
        }
        
        return squares;
    }
    
    /**
     * Find potential toggle shapes
     */
    private List<Rect> findPotentialToggles(Bitmap image) {
        List<Rect> toggles = new ArrayList<>();
        
        // Get rectangles first
        List<Rect> rectangles = findRectangles(image);
        
        // Filter for potential toggles (wide rectangles)
        for (Rect rect : rectangles) {
            int width = rect.width();
            int height = rect.height();
            
            // Check if toggle-like (2:1 ratio approximately)
            float ratio = (float) width / height;
            if (ratio >= 1.5f && ratio <= 3.0f && height <= 50) {
                toggles.add(rect);
            }
        }
        
        return toggles;
    }
    
    /**
     * Check if a rectangle likely represents a button
     */
    private boolean isLikelyButton(Bitmap image, Rect rect) {
        // Check size
        int width = rect.width();
        int height = rect.height();
        
        if (width < 50 || height < 30 || width > 500 || height > 200) {
            return false;
        }
        
        // Check for uniform color
        if (!hasUniformColor(image, rect)) {
            return false;
        }
        
        // Check for borders (many buttons have borders)
        if (hasBorder(image, rect)) {
            return true;
        }
        
        // Check for distinct background color compared to surroundings
        if (hasDistinctBackground(image, rect)) {
            return true;
        }
        
        return false;
    }
    
    /**
     * Check if a rectangle likely represents a text field
     */
    private boolean isLikelyTextField(Bitmap image, Rect rect) {
        // Check size
        int width = rect.width();
        int height = rect.height();
        
        if (width < 100 || height < 30 || height > 100) {
            return false;
        }
        
        // Width should be greater than height for text fields
        if (width <= height) {
            return false;
        }
        
        // Check for uniform light color
        if (!hasUniformLightColor(image, rect)) {
            return false;
        }
        
        // Check for borders (most text fields have borders)
        if (hasBorder(image, rect)) {
            return true;
        }
        
        return false;
    }
    
    /**
     * Check if a square likely represents a checkbox
     */
    private boolean isLikelyCheckbox(Bitmap image, Rect rect) {
        // Check size
        int size = Math.max(rect.width(), rect.height());
        
        if (size < 20 || size > 50) {
            return false;
        }
        
        // Check for border
        if (!hasBorder(image, rect)) {
            return false;
        }
        
        return true;
    }
    
    /**
     * Check if a rectangle likely represents a toggle switch
     */
    private boolean isLikelyToggle(Bitmap image, Rect rect) {
        // Check size
        int width = rect.width();
        int height = rect.height();
        
        if (width < 40 || height < 20 || width > 100 || height > 50) {
            return false;
        }
        
        // Width should be greater than height for toggles
        if (width <= height) {
            return false;
        }
        
        // Check for rounded corners
        if (!hasRoundedCorners(image, rect)) {
            return false;
        }
        
        return true;
    }
    
    /**
     * Check if area has uniform color
     */
    private boolean hasUniformColor(Bitmap image, Rect rect) {
        // Sample points within the rectangle
        int numSamples = 10;
        int colorThreshold = 30;
        
        // Get center color
        int centerX = rect.centerX();
        int centerY = rect.centerY();
        int centerColor = image.getPixel(centerX, centerY);
        
        // Check sample points
        for (int i = 0; i < numSamples; i++) {
            int x = rect.left + random.nextInt(rect.width());
            int y = rect.top + random.nextInt(rect.height());
            int pixel = image.getPixel(x, y);
            
            if (!isSimilarColor(pixel, centerColor, colorThreshold)) {
                return false;
            }
        }
        
        return true;
    }
    
    /**
     * Check if area has uniform light color
     */
    private boolean hasUniformLightColor(Bitmap image, Rect rect) {
        // First check if it's uniform
        if (!hasUniformColor(image, rect)) {
            return false;
        }
        
        // Check if color is light
        int centerX = rect.centerX();
        int centerY = rect.centerY();
        int centerColor = image.getPixel(centerX, centerY);
        
        return isLightColor(centerColor);
    }
    
    /**
     * Check if color is light
     */
    private boolean isLightColor(int color) {
        int r = Color.red(color);
        int g = Color.green(color);
        int b = Color.blue(color);
        
        // Calculate brightness
        float brightness = (r * 0.299f + g * 0.587f + b * 0.114f) / 255.0f;
        
        return brightness > 0.65f;
    }
    
    /**
     * Check if rectangle has a visible border
     */
    private boolean hasBorder(Bitmap image, Rect rect) {
        int width = rect.width();
        int height = rect.height();
        
        // Minimal size check
        if (width < 10 || height < 10) {
            return false;
        }
        
        // Sample points on each edge
        int numSamples = 5;
        int colorThreshold = 50;
        
        // Get center color
        int centerX = rect.centerX();
        int centerY = rect.centerY();
        int centerColor = image.getPixel(centerX, centerY);
        
        // Check top edge
        for (int i = 0; i < numSamples; i++) {
            int x = rect.left + (width * i) / numSamples;
            int pixel = image.getPixel(x, rect.top);
            
            if (!isSimilarColor(pixel, centerColor, colorThreshold)) {
                return true;
            }
        }
        
        // Check bottom edge
        for (int i = 0; i < numSamples; i++) {
            int x = rect.left + (width * i) / numSamples;
            int pixel = image.getPixel(x, rect.bottom);
            
            if (!isSimilarColor(pixel, centerColor, colorThreshold)) {
                return true;
            }
        }
        
        // Check left edge
        for (int i = 0; i < numSamples; i++) {
            int y = rect.top + (height * i) / numSamples;
            int pixel = image.getPixel(rect.left, y);
            
            if (!isSimilarColor(pixel, centerColor, colorThreshold)) {
                return true;
            }
        }
        
        // Check right edge
        for (int i = 0; i < numSamples; i++) {
            int y = rect.top + (height * i) / numSamples;
            int pixel = image.getPixel(rect.right, y);
            
            if (!isSimilarColor(pixel, centerColor, colorThreshold)) {
                return true;
            }
        }
        
        return false;
    }
    
    /**
     * Check if rectangle has a distinct background compared to surroundings
     */
    private boolean hasDistinctBackground(Bitmap image, Rect rect) {
        int width = image.getWidth();
        int height = image.getHeight();
        
        // Sample points within the rectangle
        int centerX = rect.centerX();
        int centerY = rect.centerY();
        int centerColor = image.getPixel(centerX, centerY);
        
        // Check points outside the rectangle
        int numSamples = 8;
        int colorThreshold = 50;
        
        // Define sample positions around the rectangle
        int margin = 10;
        int[][] samplePositions = {
            {rect.left - margin, centerY},                // left
            {rect.right + margin, centerY},               // right
            {centerX, rect.top - margin},                 // top
            {centerX, rect.bottom + margin},              // bottom
            {rect.left - margin, rect.top - margin},      // top-left
            {rect.right + margin, rect.top - margin},     // top-right
            {rect.left - margin, rect.bottom + margin},   // bottom-left
            {rect.right + margin, rect.bottom + margin}   // bottom-right
        };
        
        int distinctCount = 0;
        
        // Check each sample position
        for (int[] pos : samplePositions) {
            int x = pos[0];
            int y = pos[1];
            
            // Skip if outside image bounds
            if (x < 0 || x >= width || y < 0 || y >= height) {
                continue;
            }
            
            int pixel = image.getPixel(x, y);
            
            if (!isSimilarColor(pixel, centerColor, colorThreshold)) {
                distinctCount++;
            }
        }
        
        // If most samples are different, the background is distinct
        return distinctCount >= 4;
    }
    
    /**
     * Check if rectangle has rounded corners
     */
    private boolean hasRoundedCorners(Bitmap image, Rect rect) {
        // Check the four corners
        int cornerSize = Math.min(rect.width(), rect.height()) / 4;
        
        // Get color at center
        int centerColor = image.getPixel(rect.centerX(), rect.centerY());
        
        // Check if corners have different color from center
        boolean topLeftDifferent = !isSimilarColor(image.getPixel(rect.left, rect.top), centerColor, 50);
        boolean topRightDifferent = !isSimilarColor(image.getPixel(rect.right, rect.top), centerColor, 50);
        boolean bottomLeftDifferent = !isSimilarColor(image.getPixel(rect.left, rect.bottom), centerColor, 50);
        boolean bottomRightDifferent = !isSimilarColor(image.getPixel(rect.right, rect.bottom), centerColor, 50);
        
        // If at least two corners are different, likely has rounded corners
        int differentCount = (topLeftDifferent ? 1 : 0) + 
                             (topRightDifferent ? 1 : 0) + 
                             (bottomLeftDifferent ? 1 : 0) + 
                             (bottomRightDifferent ? 1 : 0);
        
        return differentCount >= 2;
    }
    
    /**
     * Check if a checkbox is checked
     */
    private boolean isCheckboxChecked(Bitmap image, Rect rect) {
        // Sample points in the center area
        int centerX = rect.centerX();
        int centerY = rect.centerY();
        
        // Region to sample (center portion)
        int sampleSize = rect.width() / 3;
        int left = centerX - sampleSize / 2;
        int right = centerX + sampleSize / 2;
        int top = centerY - sampleSize / 2;
        int bottom = centerY + sampleSize / 2;
        
        // Count dark pixels
        int darkCount = 0;
        int totalCount = 0;
        
        for (int y = top; y <= bottom; y++) {
            for (int x = left; x <= right; x++) {
                if (x >= rect.left && x <= rect.right && y >= rect.top && y <= rect.bottom) {
                    int pixel = image.getPixel(x, y);
                    if (isDarkColor(pixel)) {
                        darkCount++;
                    }
                    totalCount++;
                }
            }
        }
        
        // If significant portion is dark, assume checked
        return totalCount > 0 && (float) darkCount / totalCount > 0.3f;
    }
    
    /**
     * Check if a toggle is on
     */
    private boolean isToggleOn(Bitmap image, Rect rect) {
        // Sample right half of toggle (where the knob would be if on)
        int midX = rect.centerX();
        int right = rect.right;
        int top = rect.top;
        int bottom = rect.bottom;
        
        // Count light pixels (likely knob)
        int lightCount = 0;
        int totalCount = 0;
        
        for (int y = top; y <= bottom; y++) {
            for (int x = midX; x <= right; x++) {
                int pixel = image.getPixel(x, y);
                if (isLightColor(pixel)) {
                    lightCount++;
                }
                totalCount++;
            }
        }
        
        // If significant portion is light, assume on
        return totalCount > 0 && (float) lightCount / totalCount > 0.5f;
    }
    
    /**
     * Check if color is dark
     */
    private boolean isDarkColor(int color) {
        int r = Color.red(color);
        int g = Color.green(color);
        int b = Color.blue(color);
        
        // Calculate brightness
        float brightness = (r * 0.299f + g * 0.587f + b * 0.114f) / 255.0f;
        
        return brightness < 0.35f;
    }
    
    /**
     * Check if two colors are similar
     */
    private boolean isSimilarColor(int color1, int color2, int threshold) {
        int r1 = Color.red(color1);
        int g1 = Color.green(color1);
        int b1 = Color.blue(color1);
        
        int r2 = Color.red(color2);
        int g2 = Color.green(color2);
        int b2 = Color.blue(color2);
        
        int rDiff = Math.abs(r1 - r2);
        int gDiff = Math.abs(g1 - g2);
        int bDiff = Math.abs(b1 - b2);
        
        return rDiff <= threshold && gDiff <= threshold && bDiff <= threshold;
    }
    
    /**
     * Count buttons, text fields, etc. in the image
     */
    public int countButtons(Bitmap image) {
        Map<String, Object> results = detectElements(image);
        
        // Get elements
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> elements = (List<Map<String, Object>>) results.get("elements");
        
        if (elements == null) {
            return 0;
        }
        
        // Count buttons
        int buttonCount = 0;
        for (Map<String, Object> element : elements) {
            String type = (String) element.get("type");
            if (ElementType.BUTTON.name().equals(type)) {
                buttonCount++;
            }
        }
        
        return buttonCount;
    }
    
    /**
     * Detect text in the image
     */
    public Map<String, Object> detectText(Bitmap image) {
        Map<String, Object> results = new HashMap<>();
        
        try {
            if (textRecognitionModel != null) {
                // Use text recognition model
                results.putAll(detectTextWithModel(image));
            } else {
                // Fall back to extracting text regions based on color patterns
                results.put("text_regions", findTextRegions(image));
            }
        } catch (Exception e) {
            Log.e(TAG, "Error detecting text: " + e.getMessage());
        }
        
        return results;
    }
    
    /**
     * Detect text using OCR model
     */
    private Map<String, Object> detectTextWithModel(Bitmap image) {
        Map<String, Object> results = new HashMap<>();
        
        // Implement TFLite OCR model inference here
        // For now, just return a placeholder
        
        List<Map<String, Object>> textRegions = findTextRegions(image);
        results.put("text_regions", textRegions);
        
        return results;
    }
    
    /**
     * Find potential text regions based on color patterns
     */
    private List<Map<String, Object>> findTextRegions(Bitmap image) {
        List<Map<String, Object>> textRegions = new ArrayList<>();
        
        // This is a simplified approach to find text regions
        // based on contrast between text and background
        
        // Downsample for faster processing
        Bitmap downsampled = Bitmap.createScaledBitmap(image, image.getWidth() / 2, image.getHeight() / 2, true);
        
        // Find high contrast regions
        List<Rect> contrastRegions = findHighContrastRegions(downsampled);
        
        // Scale back to original image
        float scaleX = (float) image.getWidth() / downsampled.getWidth();
        float scaleY = (float) image.getHeight() / downsampled.getHeight();
        
        for (Rect rect : contrastRegions) {
            Map<String, Object> region = new HashMap<>();
            region.put("bounds", new Rect(
                    (int) (rect.left * scaleX),
                    (int) (rect.top * scaleY),
                    (int) (rect.right * scaleX),
                    (int) (rect.bottom * scaleY)
            ));
            
            // We don't know the text content without OCR
            textRegions.add(region);
        }
        
        return textRegions;
    }
    
    /**
     * Find high contrast regions that might contain text
     */
    private List<Rect> findHighContrastRegions(Bitmap image) {
        List<Rect> regions = new ArrayList<>();
        
        int width = image.getWidth();
        int height = image.getHeight();
        
        // Contrast threshold
        int contrastThreshold = 60;
        
        // Minimum region size
        int minWidth = 30;
        int minHeight = 10;
        
        // Step size for scanning
        int stepSize = 5;
        
        // Keep track of processed areas
        boolean[][] processed = new boolean[width][height];
        
        // Scan image
        for (int y = 0; y < height - stepSize; y += stepSize) {
            for (int x = 0; x < width - stepSize; x += stepSize) {
                if (processed[x][y]) continue;
                
                // Check local contrast
                if (hasHighLocalContrast(image, x, y, contrastThreshold)) {
                    // Found high contrast point, expand to region
                    Rect region = expandContrastRegion(image, x, y, contrastThreshold, processed);
                    
                    // Check size
                    if (region.width() >= minWidth && region.height() >= minHeight) {
                        regions.add(region);
                    }
                } else {
                    processed[x][y] = true;
                }
            }
        }
        
        return regions;
    }
    
    /**
     * Check if point has high local contrast
     */
    private boolean hasHighLocalContrast(Bitmap image, int x, int y, int threshold) {
        int width = image.getWidth();
        int height = image.getHeight();
        
        // Check surrounding pixels
        int size = 5;
        List<Integer> pixels = new ArrayList<>();
        
        for (int dy = -size/2; dy <= size/2; dy++) {
            for (int dx = -size/2; dx <= size/2; dx++) {
                int nx = x + dx;
                int ny = y + dy;
                
                if (nx >= 0 && nx < width && ny >= 0 && ny < height) {
                    pixels.add(image.getPixel(nx, ny));
                }
            }
        }
        
        if (pixels.size() < 4) return false;
        
        // Calculate min and max brightness
        int minBrightness = 255;
        int maxBrightness = 0;
        
        for (int pixel : pixels) {
            int r = Color.red(pixel);
            int g = Color.green(pixel);
            int b = Color.blue(pixel);
            int brightness = (r + g + b) / 3;
            
            minBrightness = Math.min(minBrightness, brightness);
            maxBrightness = Math.max(maxBrightness, brightness);
        }
        
        // Check contrast
        return (maxBrightness - minBrightness) >= threshold;
    }
    
    /**
     * Expand from a point to find a region of high contrast
     */
    private Rect expandContrastRegion(Bitmap image, int startX, int startY, int threshold, boolean[][] processed) {
        int width = image.getWidth();
        int height = image.getHeight();
        
        // Initialize region
        int left = startX;
        int right = startX;
        int top = startY;
        int bottom = startY;
        
        // Mark starting point as processed
        processed[startX][startY] = true;
        
        // Try to expand in all directions
        boolean canExpandRight = true;
        boolean canExpandLeft = true;
        boolean canExpandDown = true;
        boolean canExpandUp = true;
        
        // Limit growth
        int maxSize = 300;
        
        while (canExpandRight || canExpandLeft || canExpandDown || canExpandUp) {
            // Check if region is too large
            if (right - left > maxSize || bottom - top > maxSize) {
                break;
            }
            
            // Try to expand right
            if (canExpandRight && right < width - 1) {
                if (hasHighLocalContrast(image, right + 1, (top + bottom) / 2, threshold)) {
                    right++;
                    
                    // Mark column as processed
                    for (int y = top; y <= bottom; y++) {
                        if (y < height) {
                            processed[right][y] = true;
                        }
                    }
                } else {
                    canExpandRight = false;
                }
            } else {
                canExpandRight = false;
            }
            
            // Try to expand left
            if (canExpandLeft && left > 0) {
                if (hasHighLocalContrast(image, left - 1, (top + bottom) / 2, threshold)) {
                    left--;
                    
                    // Mark column as processed
                    for (int y = top; y <= bottom; y++) {
                        if (y < height) {
                            processed[left][y] = true;
                        }
                    }
                } else {
                    canExpandLeft = false;
                }
            } else {
                canExpandLeft = false;
            }
            
            // Try to expand down
            if (canExpandDown && bottom < height - 1) {
                if (hasHighLocalContrast(image, (left + right) / 2, bottom + 1, threshold)) {
                    bottom++;
                    
                    // Mark row as processed
                    for (int x = left; x <= right; x++) {
                        if (x < width) {
                            processed[x][bottom] = true;
                        }
                    }
                } else {
                    canExpandDown = false;
                }
            } else {
                canExpandDown = false;
            }
            
            // Try to expand up
            if (canExpandUp && top > 0) {
                if (hasHighLocalContrast(image, (left + right) / 2, top - 1, threshold)) {
                    top--;
                    
                    // Mark row as processed
                    for (int x = left; x <= right; x++) {
                        if (x < width) {
                            processed[x][top] = true;
                        }
                    }
                } else {
                    canExpandUp = false;
                }
            } else {
                canExpandUp = false;
            }
        }
        
        return new Rect(left, top, right, bottom);
    }
    
    /**
     * Extract text from a specific region of the image
     */
    public String extractTextFromRegion(Bitmap image, Rect region) {
        try {
            // Extract region from image
            Bitmap regionBitmap = Bitmap.createBitmap(
                    image, 
                    region.left, 
                    region.top, 
                    region.width(), 
                    region.height()
            );
            
            if (textRecognitionModel != null) {
                // Use text recognition model
                return recognizeTextWithModel(regionBitmap);
            } else {
                // No OCR available, return empty string
                return "";
            }
        } catch (Exception e) {
            Log.e(TAG, "Error extracting text from region: " + e.getMessage());
            return "";
        }
    }
    
    /**
     * Recognize text in image using TFLite model
     */
    private String recognizeTextWithModel(Bitmap image) {
        try {
            // This would use the TFLite text recognition model
            // For now, return a placeholder
            
            return "";
        } catch (Exception e) {
            Log.e(TAG, "Error recognizing text with model: " + e.getMessage());
            return "";
        }
    }
    
    /**
     * Extract text from the entire image
     */
    public String extractTextFromImage(Bitmap image) {
        // Use the full image
        return extractTextFromRegion(image, new Rect(0, 0, image.getWidth(), image.getHeight()));
    }
    
    /**
     * Convert element type index to ElementType
     */
    private ElementType getElementTypeFromIndex(int index) {
        ElementType[] types = ElementType.values();
        if (index >= 0 && index < types.length) {
            return types[index];
        }
        return ElementType.OTHER;
    }
    
    /**
     * Count elements by type
     */
    private Map<ElementType, Integer> countElementsByType(Map<String, Object> results) {
        Map<ElementType, Integer> counts = new HashMap<>();
        
        // Initialize counts
        for (ElementType type : ElementType.values()) {
            counts.put(type, 0);
        }
        
        // Get elements
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> elements = (List<Map<String, Object>>) results.get("elements");
        
        if (elements != null) {
            // Count by type
            for (Map<String, Object> element : elements) {
                String typeStr = (String) element.get("type");
                
                try {
                    ElementType type = ElementType.valueOf(typeStr);
                    counts.put(type, counts.getOrDefault(type, 0) + 1);
                } catch (IllegalArgumentException e) {
                    // Invalid type, ignore
                }
            }
        }
        
        return counts;
    }
    
    // Random number generator for sampling
    private final java.util.Random random = new java.util.Random();
    
    /**
     * Clean up resources
     */
    public void cleanup() {
        // Close TensorFlow Lite interpreters
        if (objectDetectionModel != null) {
            objectDetectionModel.close();
            objectDetectionModel = null;
        }
        
        if (textRecognitionModel != null) {
            textRecognitionModel.close();
            textRecognitionModel = null;
        }
        
        // Shutdown executor
        if (executorService != null && !executorService.isShutdown()) {
            executorService.shutdown();
        }
    }
}