package utils;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Rect;
import android.graphics.RectF;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.lang.reflect.Method;

/**
 * Helper class for video processing operations
 * Provides compatibility between different video processing implementations
 */
public class VideoProcessingHelper {
    
    private static final String TAG = "VideoProcessingHelper";
    
    /**
     * Detect objects in a video frame
     * 
     * @param detector The detector object (any version)
     * @param bitmap The bitmap frame to analyze
     * @param minimumConfidence Minimum confidence threshold (0.0-1.0)
     * @param context The Android context
     * @return List of detected objects
     */
    public static List<DetectedObject> detectObjects(Object detector, Bitmap bitmap, 
                                                    float minimumConfidence, Context context) {
        if (detector == null || bitmap == null) {
            return new ArrayList<>();
        }
        
        try {
            // Try to call detectObjects with different parameter signatures
            Object result = null;
            
            try {
                // Try with Context parameter
                Method detectMethod = detector.getClass().getMethod("detectObjects", 
                        Bitmap.class, float.class, Context.class);
                result = detectMethod.invoke(detector, bitmap, minimumConfidence, context);
            } catch (NoSuchMethodException e) {
                try {
                    // Try without Context parameter
                    Method detectMethod = detector.getClass().getMethod("detectObjects", 
                            Bitmap.class, float.class);
                    result = detectMethod.invoke(detector, bitmap, minimumConfidence);
                } catch (NoSuchMethodException e2) {
                    // Try with different parameter order
                    try {
                        Method detectMethod = detector.getClass().getMethod("detectObjects", 
                                Context.class, Bitmap.class, float.class);
                        result = detectMethod.invoke(detector, context, bitmap, minimumConfidence);
                    } catch (NoSuchMethodException e3) {
                        // No suitable method found
                        return new ArrayList<>();
                    }
                }
            }
            
            return convertToDetectedObjects(result);
            
        } catch (Exception e) {
            System.err.println("Error detecting objects: " + e.getMessage());
            e.printStackTrace();
        }
        
        return new ArrayList<>();
    }
    
    /**
     * Recognize text in a video frame
     * 
     * @param recognizer The text recognizer (any version)
     * @param bitmap The bitmap frame to analyze
     * @param context The Android context
     * @return List of recognized text blocks
     */
    public static List<RecognizedText> recognizeText(Object recognizer, Bitmap bitmap, Context context) {
        if (recognizer == null || bitmap == null) {
            return new ArrayList<>();
        }
        
        try {
            // Try to call recognizeText with different parameter signatures
            Object result = null;
            
            try {
                // Try with Context parameter
                Method recognizeMethod = recognizer.getClass().getMethod("recognizeText", 
                        Bitmap.class, Context.class);
                result = recognizeMethod.invoke(recognizer, bitmap, context);
            } catch (NoSuchMethodException e) {
                try {
                    // Try without Context parameter
                    Method recognizeMethod = recognizer.getClass().getMethod("recognizeText", 
                            Bitmap.class);
                    result = recognizeMethod.invoke(recognizer, bitmap);
                } catch (NoSuchMethodException e2) {
                    // Try with different parameter order
                    try {
                        Method recognizeMethod = recognizer.getClass().getMethod("recognizeText", 
                                Context.class, Bitmap.class);
                        result = recognizeMethod.invoke(recognizer, context, bitmap);
                    } catch (NoSuchMethodException e3) {
                        // No suitable method found
                        return new ArrayList<>();
                    }
                }
            }
            
            // List conversion
            return convertToRecognizedText(result);
            
        } catch (Exception e) {
            System.err.println("Error recognizing text: " + e.getMessage());
            e.printStackTrace();
        }
        
        return new ArrayList<>();
    }
    
    /**
     * Extract frames from a video
     * 
     * @param processor The video processor
     * @param videoPath Path to the video file
     * @param frameInterval Interval between frames (in milliseconds)
     * @param context The Android context
     * @return List of extracted frames as bitmaps
     */
    public static List<Bitmap> extractFrames(Object processor, String videoPath, 
                                            int frameInterval, Context context) {
        if (processor == null || videoPath == null || videoPath.isEmpty()) {
            return new ArrayList<>();
        }
        
        try {
            // Try to call extractFrames with different parameter signatures
            Object result = null;
            
            try {
                // Try with Context parameter
                Method extractMethod = processor.getClass().getMethod("extractFrames", 
                        String.class, int.class, Context.class);
                result = extractMethod.invoke(processor, videoPath, frameInterval, context);
            } catch (NoSuchMethodException e) {
                try {
                    // Try without Context parameter
                    Method extractMethod = processor.getClass().getMethod("extractFrames", 
                            String.class, int.class);
                    result = extractMethod.invoke(processor, videoPath, frameInterval);
                } catch (NoSuchMethodException e2) {
                    // Try with different parameter order
                    try {
                        Method extractMethod = processor.getClass().getMethod("extractFrames", 
                                Context.class, String.class, int.class);
                        result = extractMethod.invoke(processor, context, videoPath, frameInterval);
                    } catch (NoSuchMethodException e3) {
                        // No suitable method found
                        return new ArrayList<>();
                    }
                }
            }
            
            return convertToBitmaps(result);
            
        } catch (Exception e) {
            System.err.println("Error extracting frames: " + e.getMessage());
            e.printStackTrace();
        }
        
        return new ArrayList<>();
    }
    
    /**
     * Track an object across video frames
     * 
     * @param videoProcessor The video processor
     * @param initialObject The initial object to track
     * @param frames List of video frames
     * @param context The Android context
     * @return Map of frame index to tracked object bounds
     */
    public static Map<Integer, RectF> trackObject(Object videoProcessor, DetectedObject initialObject, 
                                                List<Bitmap> frames, Context context) {
        if (videoProcessor == null || initialObject == null || frames == null || frames.isEmpty()) {
            return new HashMap<>();
        }
        
        try {
            // Try to call trackObject with different parameter signatures
            Object result = null;
            
            try {
                // Try with Context parameter
                Method trackMethod = videoProcessor.getClass().getMethod("trackObject", 
                        DetectedObject.class, List.class, Context.class);
                result = trackMethod.invoke(videoProcessor, initialObject, frames, context);
            } catch (NoSuchMethodException e) {
                try {
                    // Try without Context parameter
                    Method trackMethod = videoProcessor.getClass().getMethod("trackObject", 
                            DetectedObject.class, List.class);
                    result = trackMethod.invoke(videoProcessor, initialObject, frames);
                } catch (NoSuchMethodException e2) {
                    // Try with different parameter order
                    try {
                        Method trackMethod = videoProcessor.getClass().getMethod("trackObject", 
                                Context.class, DetectedObject.class, List.class);
                        result = trackMethod.invoke(videoProcessor, context, initialObject, frames);
                    } catch (NoSuchMethodException e3) {
                        // No suitable method found
                        return new HashMap<>();
                    }
                }
            }
            
            return convertToTrackingResult(result);
            
        } catch (Exception e) {
            System.err.println("Error tracking object: " + e.getMessage());
            e.printStackTrace();
        }
        
        return new HashMap<>();
    }
    
    /**
     * Convert result to list of DetectedObject
     */
    @SuppressWarnings("unchecked")
    private static List<DetectedObject> convertToDetectedObjects(Object result) {
        List<DetectedObject> objects = new ArrayList<>();
        
        if (result == null) {
            return objects;
        }
        
        if (result instanceof List<?>) {
            List<?> resultList = (List<?>) result;
            
            // If list is empty, return empty list
            if (resultList.isEmpty()) {
                return objects;
            }
            
            // If list already contains DetectedObject instances, just return it
            if (resultList.get(0) instanceof DetectedObject) {
                return (List<DetectedObject>) result;
            }
            
            // Otherwise try to convert each item
            for (Object item : resultList) {
                DetectedObject obj = convertToDetectedObject(item);
                if (obj != null) {
                    objects.add(obj);
                }
            }
        }
        
        return objects;
    }
    
    /**
     * Convert an object to DetectedObject using reflection
     */
    private static DetectedObject convertToDetectedObject(Object obj) {
        if (obj == null) {
            return null;
        }
        
        // If it's already a DetectedObject, just return it
        if (obj instanceof DetectedObject) {
            return (DetectedObject) obj;
        }
        
        try {
            // Try to extract common properties using reflection
            String label = extractStringProperty(obj, "getLabel", "getClassName", "getName");
            float confidence = extractFloatProperty(obj, "getConfidence", "getScore", "getProbability");
            RectF bounds = extractRectProperty(obj, "getBounds", "getRect", "getRectF", "getBoundingBox");
            
            if (label != null && bounds != null) {
                return new DetectedObject(label, confidence, bounds);
            }
        } catch (Exception e) {
            System.err.println("Error converting to DetectedObject: " + e.getMessage());
        }
        
        return null;
    }
    
    /**
     * Convert result to list of RecognizedText
     */
    @SuppressWarnings("unchecked")
    private static List<RecognizedText> convertToRecognizedText(Object result) {
        List<RecognizedText> texts = new ArrayList<>();
        
        if (result == null) {
            return texts;
        }
        
        if (result instanceof List<?>) {
            List<?> resultList = (List<?>) result;
            
            // If list is empty, return empty list
            if (resultList.isEmpty()) {
                return texts;
            }
            
            // If list already contains RecognizedText instances, just return it
            if (resultList.get(0) instanceof RecognizedText) {
                return (List<RecognizedText>) result;
            }
            
            // Otherwise try to convert each item
            for (Object item : resultList) {
                RecognizedText text = convertSingleObjectToRecognizedText(item);
                if (text != null) {
                    texts.add(text);
                }
            }
        }
        
        return texts;
    }
    
    /**
     * Convert a single object to RecognizedText using reflection
     */
    private static RecognizedText convertSingleObjectToRecognizedText(Object obj) {
        if (obj == null) {
            return null;
        }
        
        // If it's already a RecognizedText, just return it
        if (obj instanceof RecognizedText) {
            return (RecognizedText) obj;
        }
        
        try {
            // Try to extract common properties using reflection
            String text = extractStringProperty(obj, "getText", "getString", "getValue");
            float confidence = extractFloatProperty(obj, "getConfidence", "getScore", "getProbability");
            RectF bounds = extractRectProperty(obj, "getBounds", "getRect", "getRectF", "getBoundingBox");
            
            if (text != null && bounds != null) {
                // Convert RectF to Rect
                Rect rectBounds = new Rect(
                    (int)bounds.left,
                    (int)bounds.top,
                    (int)bounds.right,
                    (int)bounds.bottom
                );
                return new RecognizedText(text, rectBounds, confidence);
            }
        } catch (Exception e) {
            System.err.println("Error converting to RecognizedText: " + e.getMessage());
        }
        
        return null;
    }
    
    /**
     * Convert result to list of Bitmaps
     */
    @SuppressWarnings("unchecked")
    private static List<Bitmap> convertToBitmaps(Object result) {
        List<Bitmap> bitmaps = new ArrayList<>();
        
        if (result == null) {
            return bitmaps;
        }
        
        if (result instanceof List<?>) {
            List<?> resultList = (List<?>) result;
            
            // If list is empty, return empty list
            if (resultList.isEmpty()) {
                return bitmaps;
            }
            
            // If list already contains Bitmap instances, just return it
            if (resultList.get(0) instanceof Bitmap) {
                return (List<Bitmap>) result;
            }
            
            // Otherwise try to convert each item
            for (Object item : resultList) {
                Bitmap bitmap = convertToBitmap(item);
                if (bitmap != null) {
                    bitmaps.add(bitmap);
                }
            }
        }
        
        return bitmaps;
    }
    
    /**
     * Convert an object to Bitmap
     */
    private static Bitmap convertToBitmap(Object obj) {
        if (obj == null) {
            return null;
        }
        
        // If it's already an Android Bitmap, just return it
        if (obj instanceof Bitmap) {
            return (Bitmap) obj;
        }
        
        // If it's a utils.Bitmap, use BitmapHelper
        if (obj instanceof utils.Bitmap) {
            return BitmapHelper.toAndroidBitmap((utils.Bitmap) obj);
        }
        
        return null;
    }
    
    /**
     * Convert tracking result to map of frame index to bounds
     */
    @SuppressWarnings("unchecked")
    private static Map<Integer, RectF> convertToTrackingResult(Object result) {
        Map<Integer, RectF> trackingMap = new HashMap<>();
        
        if (result == null) {
            return trackingMap;
        }
        
        if (result instanceof Map) {
            Map<?, ?> resultMap = (Map<?, ?>) result;
            
            // If map is empty, return empty map
            if (resultMap.isEmpty()) {
                return trackingMap;
            }
            
            // Check if this is already the correct format
            if (resultMap.keySet().iterator().next() instanceof Integer) {
                Object value = resultMap.values().iterator().next();
                if (value instanceof RectF) {
                    return (Map<Integer, RectF>) result;
                }
            }
            
            // Otherwise try to convert each entry
            for (Map.Entry<?, ?> entry : resultMap.entrySet()) {
                Integer frameIndex = convertToInteger(entry.getKey());
                RectF bounds = convertToRectF(entry.getValue());
                
                if (frameIndex != null && bounds != null) {
                    trackingMap.put(frameIndex, bounds);
                }
            }
        }
        
        return trackingMap;
    }
    
    /**
     * Convert an object to Integer
     */
    private static Integer convertToInteger(Object obj) {
        if (obj == null) {
            return null;
        }
        
        if (obj instanceof Integer) {
            return (Integer) obj;
        } else if (obj instanceof Number) {
            return ((Number) obj).intValue();
        } else if (obj instanceof String) {
            try {
                return Integer.parseInt((String) obj);
            } catch (NumberFormatException e) {
                return null;
            }
        }
        
        return null;
    }
    
    /**
     * Convert an object to RectF
     */
    private static RectF convertToRectF(Object obj) {
        if (obj == null) {
            return null;
        }
        
        if (obj instanceof RectF) {
            return (RectF) obj;
        } else if (obj instanceof Rect) {
            Rect rect = (Rect) obj;
            return new RectF(rect);
        }
        
        try {
            // Try to extract bounds using reflection
            return extractRectProperty(obj, "getBounds", "getRect", "getRectF", "getBoundingBox");
        } catch (Exception e) {
            System.err.println("Error converting to RectF: " + e.getMessage());
            return null;
        }
    }
    
    /**
     * Extract a string property using reflection
     */
    private static String extractStringProperty(Object obj, String... methodNames) {
        for (String methodName : methodNames) {
            try {
                Method method = obj.getClass().getMethod(methodName);
                Object result = method.invoke(obj);
                if (result instanceof String) {
                    return (String) result;
                }
            } catch (Exception e) {
                // Try next method name
            }
        }
        return null;
    }
    
    /**
     * Extract a float property using reflection
     */
    private static float extractFloatProperty(Object obj, String... methodNames) {
        for (String methodName : methodNames) {
            try {
                Method method = obj.getClass().getMethod(methodName);
                Object result = method.invoke(obj);
                if (result instanceof Float) {
                    return (Float) result;
                } else if (result instanceof Double) {
                    return ((Double) result).floatValue();
                } else if (result instanceof Integer) {
                    return ((Integer) result).floatValue();
                }
            } catch (Exception e) {
                // Try next method name
            }
        }
        return 0.0f;
    }
    
    /**
     * Extract a RectF property using reflection
     */
    private static RectF extractRectProperty(Object obj, String... methodNames) {
        for (String methodName : methodNames) {
            try {
                Method method = obj.getClass().getMethod(methodName);
                Object result = method.invoke(obj);
                if (result instanceof RectF) {
                    return (RectF) result;
                } else if (result instanceof Rect) {
                    return new RectF((Rect) result);
                } else if (result != null) {
                    // Try to create a RectF from the object's properties
                    return extractRectFromProperties(result);
                }
            } catch (Exception e) {
                // Try next method name
            }
        }
        return null;
    }
    
    /**
     * Try to create a RectF from an object's properties
     */
    private static RectF extractRectFromProperties(Object obj) {
        try {
            float left = extractFloatProperty(obj, "getLeft", "left", "x");
            float top = extractFloatProperty(obj, "getTop", "top", "y");
            float right = extractFloatProperty(obj, "getRight", "right");
            float bottom = extractFloatProperty(obj, "getBottom", "bottom");
            
            // If right/bottom not found, try with width/height
            if (right == 0 && bottom == 0) {
                float width = extractFloatProperty(obj, "getWidth", "width");
                float height = extractFloatProperty(obj, "getHeight", "height");
                if (width > 0 && height > 0) {
                    right = left + width;
                    bottom = top + height;
                }
            }
            
            return new RectF(left, top, right, bottom);
        } catch (Exception e) {
            return null;
        }
    }
}