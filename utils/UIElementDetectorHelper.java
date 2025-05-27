package utils;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Rect;
import android.graphics.RectF;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.lang.reflect.Method;
import java.lang.reflect.InvocationTargetException;

/**
 * Helper class for UIElementDetector compatibility
 * Provides methods for detecting UI elements across different Android API versions
 */
public class UIElementDetectorHelper {
    
    private static final String TAG = "UIElementDetectorHelper";
    
    /**
     * Detect UI elements from the given bitmap
     * 
     * @param detector The detector object (can be any version)
     * @param bitmap The bitmap to detect elements from
     * @param context The context
     * @return List of detected UI elements
     */
    public static List<utils.UIElement> detectElements(Object detector, Bitmap bitmap, Context context) {
        if (detector == null || bitmap == null) {
            return new ArrayList<>();
        }
        
        try {
            // Try to call the detectElements method with different parameter signatures
            Method detectMethod = findDetectMethod(detector.getClass());
            if (detectMethod != null) {
                return processDetectionResult(detectMethod.invoke(detector, bitmap, context));
            }
            
            // Fallback to other potential method signatures if needed
            Method detectMethod2 = findDetectMethodAlternative(detector.getClass());
            if (detectMethod2 != null) {
                return processDetectionResult(detectMethod2.invoke(detector, bitmap));
            }
        } catch (IllegalAccessException | InvocationTargetException e) {
            System.err.println("Error calling detectElements: " + e.getMessage());
            e.printStackTrace();
        }
        
        return new ArrayList<>();
    }
    
    /**
     * Detect UI elements with content descriptions
     * 
     * @param detector The detector object
     * @param bitmap The bitmap to detect elements from
     * @param contentDescriptions List of known content descriptions to match
     * @param context The context
     * @return List of detected UI elements with content descriptions
     */
    public static List<utils.UIElement> detectElementsWithContentDescriptions(
            Object detector, 
            Bitmap bitmap, 
            List<String> contentDescriptions,
            Context context) {
        
        if (detector == null || bitmap == null) {
            return new ArrayList<>();
        }
        
        try {
            // Try to call the method with content descriptions parameter
            Method detectMethod = findDetectMethodWithContentDesc(detector.getClass());
            if (detectMethod != null) {
                return processDetectionResult(
                        detectMethod.invoke(detector, bitmap, contentDescriptions, context));
            }
            
            // If no specific method exists, use the standard detection and add content descriptions afterward
            List<utils.UIElement> elements = detectElements(detector, bitmap, context);
            if (contentDescriptions != null && !contentDescriptions.isEmpty() && !elements.isEmpty()) {
                assignContentDescriptions(elements, contentDescriptions);
            }
            
            return elements;
        } catch (IllegalAccessException | InvocationTargetException e) {
            System.err.println("Error calling detectElementsWithContentDescriptions: " + e.getMessage());
            e.printStackTrace();
        }
        
        return new ArrayList<>();
    }
    
    /**
     * Find the detectElements method in the detector class
     */
    private static Method findDetectMethod(Class<?> detectorClass) {
        try {
            return detectorClass.getMethod("detectElements", Bitmap.class, Context.class);
        } catch (NoSuchMethodException e) {
            // Method not found
            return null;
        }
    }
    
    /**
     * Find alternative detectElements method signature
     */
    private static Method findDetectMethodAlternative(Class<?> detectorClass) {
        try {
            return detectorClass.getMethod("detectElements", Bitmap.class);
        } catch (NoSuchMethodException e) {
            // Method not found
            return null;
        }
    }
    
    /**
     * Find method for detecting elements with content descriptions
     */
    private static Method findDetectMethodWithContentDesc(Class<?> detectorClass) {
        try {
            return detectorClass.getMethod("detectElementsWithContentDescriptions", 
                    Bitmap.class, List.class, Context.class);
        } catch (NoSuchMethodException e) {
            // Method not found
            return null;
        }
    }
    
    /**
     * Process the detection result and convert it to a list of UIElements
     * @param result The detection result to process
     * @return A list of UIElement objects
     */
    @SuppressWarnings("unchecked")
    private static List<utils.UIElement> processDetectionResult(Object result) {
        List<utils.UIElement> elements = new ArrayList<>();
        
        if (result instanceof List<?>) {
            // If the result is already a list of UIElement, just cast and return
            List<?> resultList = (List<?>) result;
            if (!resultList.isEmpty() && resultList.get(0) instanceof utils.UIElement) {
                return (List<utils.UIElement>) result;
            }
            
            // Otherwise, try to convert each item to UIElement
            for (Object item : resultList) {
                utils.UIElement element = convertToUIElement(item);
                if (element != null) {
                    elements.add(element);
                }
            }
        }
        
        return elements;
    }
    
    /**
     * Convert an object to UIElement using reflection
     * @param obj The object to convert
     * @return A UIElement representation of the object
     */
    private static utils.UIElement convertToUIElement(Object obj) {
        if (obj == null) {
            return null;
        }
        
        // If it's already a UIElement, just return it
        if (obj instanceof utils.UIElement) {
            return (utils.UIElement) obj;
        }
        
        // Create a new UIElement and try to copy properties using reflection
        utils.UIElementImpl element = new utils.UIElementImpl();
        
        try {
            // Try to get common properties
            copyProperty(obj, element, "id", "getId", "setId", String.class);
            copyProperty(obj, element, "text", "getText", "setText", String.class);
            copyProperty(obj, element, "contentDescription", "getContentDescription", "setContentDescription", String.class);
            copyProperty(obj, element, "clickable", "isClickable", "setClickable", boolean.class);
            copyProperty(obj, element, "visible", "isVisible", "setVisible", boolean.class);
            copyProperty(obj, element, "confidence", "getConfidence", "setConfidence", float.class);
            
            // Try to get bounds
            Method getBoundsMethod = obj.getClass().getMethod("getBounds");
            Object bounds = getBoundsMethod.invoke(obj);
            
            if (bounds instanceof RectF) {
                RectF rectF = (RectF)bounds;
                Rect rect = new Rect((int)rectF.left, (int)rectF.top, (int)rectF.right, (int)rectF.bottom);
                utils.Rect utilsRect = new utils.Rect(rect.left, rect.top, rect.right, rect.bottom);
                element.setBounds(utilsRect);
            } else if (bounds instanceof Rect) {
                Rect androidRect = (Rect) bounds;
                utils.Rect utilsRect = new utils.Rect(androidRect.left, androidRect.top, androidRect.right, androidRect.bottom);
                element.setBounds(utilsRect);
            } else if (bounds != null) {
                // Try to convert other rectangle types
                RectF rectF = extractRectFFromObject(bounds);
                if (rectF != null) {
                    utils.Rect utilsRect = new utils.Rect(
                        (int)rectF.left, 
                        (int)rectF.top, 
                        (int)rectF.right, 
                        (int)rectF.bottom
                    );
                    element.setBounds(utilsRect);
                }
            }
            
            // Try to get type
            Method getTypeMethod = findMethodByName(obj.getClass(), "getType");
            if (getTypeMethod != null) {
                Object type = getTypeMethod.invoke(obj);
                if (type != null) {
                    if (type instanceof utils.UIElement.ElementType) {
                        element.setType((utils.UIElement.ElementType) type);
                    } else if (type instanceof Enum) {
                        // Convert enum string to our ElementType
                        String typeName = ((Enum<?>) type).name();
                        try {
                            element.setType(utils.UIElement.ElementType.valueOf(typeName));
                        } catch (IllegalArgumentException e) {
                            element.setType(utils.UIElement.ElementType.UNKNOWN);
                        }
                    } else if (type instanceof String) {
                        // Try to parse the string as an ElementType
                        try {
                            element.setType(((String) type).toUpperCase());
                        } catch (IllegalArgumentException e) {
                            element.setType(utils.UIElement.ElementType.UNKNOWN);
                        }
                    }
                }
            }
            
            // Try to get attributes
            Method getAttributesMethod = findMethodByName(obj.getClass(), "getAttributes");
            if (getAttributesMethod != null) {
                Object attributes = getAttributesMethod.invoke(obj);
                if (attributes instanceof Map) {
                    @SuppressWarnings("unchecked")
                    Map<String, Object> attrMap = (Map<String, Object>) attributes;
                    for (Map.Entry<String, Object> entry : attrMap.entrySet()) {
                        element.addAttribute(entry.getKey(), entry.getValue());
                    }
                }
            }
            
        } catch (Exception e) {
            System.err.println("Error converting to UIElement: " + e.getMessage());
            return null;
        }
        
        // Create UIElement compatible interface
        if (element instanceof UIElement) {
            return (UIElement) element;
        } else {
            // Create a new adapter if not directly castable
            return new UIElementAdapter(element);
        }
    }
    
    /**
     * Copy a property from source to target using reflection
     */
    private static <T> void copyProperty(Object source, Object target, String propertyName, 
                                         String getterName, String setterName, Class<T> propertyType) 
            throws NoSuchMethodException, IllegalAccessException, InvocationTargetException {
        
        Method getter = findMethodByName(source.getClass(), getterName);
        if (getter == null) {
            return;
        }
        
        Object value = getter.invoke(source);
        if (value != null) {
            Method setter = target.getClass().getMethod(setterName, propertyType);
            setter.invoke(target, propertyType.cast(value));
        }
    }
    
    /**
     * Find a method by name, trying different common naming conventions
     */
    private static Method findMethodByName(Class<?> clazz, String methodName) {
        // Try exact name match
        try {
            return clazz.getMethod(methodName);
        } catch (NoSuchMethodException e) {
            // Ignore and try alternatives
        }
        
        // Try with "get" prefix if not already there
        if (!methodName.startsWith("get") && !methodName.startsWith("is")) {
            try {
                return clazz.getMethod("get" + capitalize(methodName));
            } catch (NoSuchMethodException e) {
                // Ignore and try alternatives
            }
        }
        
        // Try with "is" prefix for boolean properties
        if (!methodName.startsWith("is") && !methodName.startsWith("get")) {
            try {
                return clazz.getMethod("is" + capitalize(methodName));
            } catch (NoSuchMethodException e) {
                // Ignore
            }
        }
        
        return null;
    }
    
    /**
     * Capitalize the first letter of a string
     */
    private static String capitalize(String str) {
        if (str == null || str.isEmpty()) {
            return str;
        }
        return Character.toUpperCase(str.charAt(0)) + str.substring(1);
    }
    
    /**
     * Extract a RectF from various rectangle types
     */
    private static RectF extractRectFFromObject(Object rectangle) {
        if (rectangle == null) {
            return null;
        }
        
        try {
            // Try common property names for rectangles
            float left = getFloatProperty(rectangle, "left", "getLeft", "x", "getX");
            float top = getFloatProperty(rectangle, "top", "getTop", "y", "getY");
            float right = getFloatProperty(rectangle, "right", "getRight");
            float bottom = getFloatProperty(rectangle, "bottom", "getBottom");
            
            // If right/bottom not found, try with width/height
            if (right == Float.MIN_VALUE) {
                float width = getFloatProperty(rectangle, "width", "getWidth");
                if (width != Float.MIN_VALUE) {
                    right = left + width;
                }
            }
            
            if (bottom == Float.MIN_VALUE) {
                float height = getFloatProperty(rectangle, "height", "getHeight");
                if (height != Float.MIN_VALUE) {
                    bottom = top + height;
                }
            }
            
            // Only create RectF if we have valid values
            if (left != Float.MIN_VALUE && top != Float.MIN_VALUE && 
                right != Float.MIN_VALUE && bottom != Float.MIN_VALUE) {
                return new RectF(left, top, right, bottom);
            }
        } catch (Exception e) {
            System.err.println("Error extracting RectF: " + e.getMessage());
        }
        
        return null;
    }
    
    /**
     * Get a float property value using reflection, trying different property names
     */
    private static float getFloatProperty(Object obj, String... propertyNames) {
        for (String name : propertyNames) {
            try {
                Method method = findMethodByName(obj.getClass(), name);
                if (method != null) {
                    Object result = method.invoke(obj);
                    if (result instanceof Float) {
                        return (Float) result;
                    } else if (result instanceof Integer) {
                        return ((Integer) result).floatValue();
                    } else if (result instanceof Double) {
                        return ((Double) result).floatValue();
                    }
                }
                
                // Try to access the field directly
                try {
                    java.lang.reflect.Field field = obj.getClass().getField(name);
                    Object value = field.get(obj);
                    if (value instanceof Float) {
                        return (Float) value;
                    } else if (value instanceof Integer) {
                        return ((Integer) value).floatValue();
                    } else if (value instanceof Double) {
                        return ((Double) value).floatValue();
                    }
                } catch (NoSuchFieldException | IllegalAccessException e) {
                    // Ignore
                }
            } catch (Exception e) {
                // Ignore and try next property name
            }
        }
        
        return Float.MIN_VALUE; // Sentinel value for "not found"
    }
    
    /**
     * Assign content descriptions to UI elements
     * This method tries to match elements with content descriptions based on their position and text
     * @param elements The list of UI elements to assign content descriptions to
     * @param contentDescriptions The list of content descriptions to assign
     */
    private static void assignContentDescriptions(List<utils.UIElement> elements, List<String> contentDescriptions) {
        if (elements.isEmpty() || contentDescriptions == null || contentDescriptions.isEmpty()) {
            return;
        }
        
        // Create a map of elements that already have text values
        Map<String, utils.UIElement> textElements = new HashMap<>();
        
        for (utils.UIElement element : elements) {
            if (element.getText() != null && !element.getText().isEmpty()) {
                textElements.put(element.getText(), element);
            }
        }
        
        // Try to match content descriptions with text values first
        for (String contentDesc : contentDescriptions) {
            utils.UIElement matchedElement = textElements.get(contentDesc);
            if (matchedElement != null) {
                // Use reflection to safely check/set content description
                try {
                    Method getContentDescMethod = matchedElement.getClass().getMethod("getContentDescription");
                    Object contentDescObj = getContentDescMethod.invoke(matchedElement);
                    
                    if (contentDescObj == null) {
                        Method setContentDescMethod = matchedElement.getClass().getMethod("setContentDescription", String.class);
                        setContentDescMethod.invoke(matchedElement, contentDesc);
                    }
                } catch (Exception e) {
                    // Element may not have content description methods, so just continue
                    System.err.println("Error setting content description: " + e.getMessage());
                }
            }
        }
    }
}