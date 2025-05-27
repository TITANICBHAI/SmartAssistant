package com.aiassistant.utils;

import android.graphics.Rect;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import models.StandardizedUIElement;
import models.StandardizedUIElementType;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Utility class for converting between different UIElement implementations
 */
public class UIElementConverter {
    
    /**
     * Private constructor to prevent instantiation
     */
    private UIElementConverter() {
        // Utility class should not be instantiated
    }
    
    /**
     * Convert a detection.UIElement to a StandardizedUIElement
     * 
     * @param detectionElement Detection UIElement
     * @return StandardizedUIElement
     */
    @Nullable
    public static StandardizedUIElement fromDetectionUIElement(@Nullable Object detectionElement) {
        // Introspection is used instead of direct cast to handle the case where
        // detection.UIElement might not be available at compile time
        if (detectionElement == null) {
            return null;
        }
        
        try {
            // Get element ID
            String elementId = getStringProperty(detectionElement, "getId");
            if (elementId == null) {
                elementId = getStringProperty(detectionElement, "getElementId");
                if (elementId == null) {
                    elementId = String.valueOf(System.nanoTime());
                }
            }
            
            // Get element type
            String elementTypeStr = getStringProperty(detectionElement, "getType");
            if (elementTypeStr == null) {
                elementTypeStr = getStringProperty(detectionElement, "getElementType");
                if (elementTypeStr == null) {
                    elementTypeStr = "unknown";
                }
            }
            StandardizedUIElementType elementType = StandardizedUIElementType.fromString(elementTypeStr);
            
            // Get bounds
            Rect bounds = getBoundsProperty(detectionElement);
            if (bounds == null) {
                // Use an empty rect if bounds not available
                bounds = new Rect(0, 0, 0, 0);
            }
            
            // Create builder with required parameters
            StandardizedUIElement.Builder builder = new StandardizedUIElement.Builder(
                    elementId,
                    elementType,
                    bounds
            );
            
            // Set optional parameters
            String text = getStringProperty(detectionElement, "getText");
            if (text != null) {
                builder.setText(text);
            }
            
            String contentDescription = getStringProperty(detectionElement, "getContentDescription");
            if (contentDescription != null) {
                builder.setContentDescription(contentDescription);
            }
            
            Boolean clickable = getBooleanProperty(detectionElement, "isClickable");
            if (clickable != null) {
                builder.setClickable(clickable);
            }
            
            Boolean focusable = getBooleanProperty(detectionElement, "isFocusable");
            if (focusable != null) {
                builder.setFocusable(focusable);
            }
            
            Boolean visible = getBooleanProperty(detectionElement, "isVisible");
            if (visible != null) {
                builder.setVisible(visible);
            }
            
            Boolean enabled = getBooleanProperty(detectionElement, "isEnabled");
            if (enabled != null) {
                builder.setEnabled(enabled);
            }
            
            Float confidence = getFloatProperty(detectionElement, "getConfidence");
            if (confidence != null) {
                builder.setConfidence(confidence);
            }
            
            // Get additional attributes
            Map<String, Object> attributes = getAttributesMap(detectionElement);
            if (attributes != null && !attributes.isEmpty()) {
                builder.addAttributes(attributes);
            }
            
            // Build the standardized UI element
            return builder.build();
        } catch (Exception e) {
            // Log the exception and return null
            System.err.println("Error converting detection UIElement: " + e.getMessage());
            return null;
        }
    }
    
    /**
     * Convert a models.UIElement to a StandardizedUIElement
     * 
     * @param modelElement Model UIElement
     * @return StandardizedUIElement
     */
    @Nullable
    public static StandardizedUIElement fromModelUIElement(@Nullable Object modelElement) {
        // Introspection is used instead of direct cast to handle the case where
        // models.UIElement might not be available at compile time
        if (modelElement == null) {
            return null;
        }
        
        try {
            // Get element ID
            String elementId = getStringProperty(modelElement, "getId");
            if (elementId == null) {
                elementId = getStringProperty(modelElement, "getElementId");
                if (elementId == null) {
                    elementId = String.valueOf(System.nanoTime());
                }
            }
            
            // Get element type
            String elementTypeStr = getStringProperty(modelElement, "getType");
            if (elementTypeStr == null) {
                elementTypeStr = getStringProperty(modelElement, "getElementType");
                if (elementTypeStr == null) {
                    elementTypeStr = "unknown";
                }
            }
            StandardizedUIElementType elementType = StandardizedUIElementType.fromString(elementTypeStr);
            
            // Get bounds
            Rect bounds = getBoundsProperty(modelElement);
            if (bounds == null) {
                // Use an empty rect if bounds not available
                bounds = new Rect(0, 0, 0, 0);
            }
            
            // Create builder with required parameters
            StandardizedUIElement.Builder builder = new StandardizedUIElement.Builder(
                    elementId,
                    elementType,
                    bounds
            );
            
            // Set optional parameters
            String text = getStringProperty(modelElement, "getText");
            if (text != null) {
                builder.setText(text);
            }
            
            String contentDescription = getStringProperty(modelElement, "getContentDescription");
            if (contentDescription != null) {
                builder.setContentDescription(contentDescription);
            }
            
            Boolean clickable = getBooleanProperty(modelElement, "isClickable");
            if (clickable != null) {
                builder.setClickable(clickable);
            }
            
            Boolean focusable = getBooleanProperty(modelElement, "isFocusable");
            if (focusable != null) {
                builder.setFocusable(focusable);
            }
            
            Boolean visible = getBooleanProperty(modelElement, "isVisible");
            if (visible != null) {
                builder.setVisible(visible);
            }
            
            Boolean enabled = getBooleanProperty(modelElement, "isEnabled");
            if (enabled != null) {
                builder.setEnabled(enabled);
            }
            
            // Get additional attributes
            Map<String, Object> attributes = getAttributesMap(modelElement);
            if (attributes != null && !attributes.isEmpty()) {
                builder.addAttributes(attributes);
            }
            
            // Build the standardized UI element
            return builder.build();
        } catch (Exception e) {
            // Log the exception and return null
            System.err.println("Error converting model UIElement: " + e.getMessage());
            return null;
        }
    }
    
    /**
     * Convert a list of detection UIElements to StandardizedUIElements
     * 
     * @param detectionElements List of detection UIElements
     * @return List of StandardizedUIElements
     */
    @NonNull
    public static List<StandardizedUIElement> fromDetectionUIElements(
            @Nullable List<?> detectionElements) {
        List<StandardizedUIElement> result = new ArrayList<>();
        
        if (detectionElements == null) {
            return result;
        }
        
        for (Object element : detectionElements) {
            StandardizedUIElement converted = fromDetectionUIElement(element);
            if (converted != null) {
                result.add(converted);
            }
        }
        
        return result;
    }
    
    /**
     * Convert a list of model UIElements to StandardizedUIElements
     * 
     * @param modelElements List of model UIElements
     * @return List of StandardizedUIElements
     */
    @NonNull
    public static List<StandardizedUIElement> fromModelUIElements(
            @Nullable List<?> modelElements) {
        List<StandardizedUIElement> result = new ArrayList<>();
        
        if (modelElements == null) {
            return result;
        }
        
        for (Object element : modelElements) {
            StandardizedUIElement converted = fromModelUIElement(element);
            if (converted != null) {
                result.add(converted);
            }
        }
        
        return result;
    }
    
    /**
     * Convert a StandardizedUIElement to a detection.UIElement
     * 
     * @param standardizedElement Standardized UI element
     * @param detectionElementClass Class of the detection UIElement
     * @return Detection UIElement or null if conversion fails
     */
    @Nullable
    public static Object toDetectionUIElement(
            @Nullable StandardizedUIElement standardizedElement,
            @NonNull Class<?> detectionElementClass) {
        if (standardizedElement == null) {
            return null;
        }
        
        try {
            // Create a new instance of the detection UIElement
            Object detectionElement = detectionElementClass.getConstructor().newInstance();
            
            // Set properties using reflection
            setProperty(detectionElement, "setElementId", standardizedElement.getElementId());
            setProperty(detectionElement, "setType", standardizedElement.getElementType().getValue());
            setProperty(detectionElement, "setBounds", standardizedElement.getBounds());
            setProperty(detectionElement, "setText", standardizedElement.getText());
            setProperty(detectionElement, "setContentDescription", standardizedElement.getContentDescription());
            setProperty(detectionElement, "setClickable", standardizedElement.isClickable());
            setProperty(detectionElement, "setFocusable", standardizedElement.isFocusable());
            setProperty(detectionElement, "setVisible", standardizedElement.isVisible());
            setProperty(detectionElement, "setEnabled", standardizedElement.isEnabled());
            setProperty(detectionElement, "setConfidence", standardizedElement.getConfidence());
            
            // Set attributes if method exists
            try {
                setProperty(detectionElement, "setAttributes", standardizedElement.getAttributes());
            } catch (NoSuchMethodException e) {
                // Ignore if method doesn't exist
            }
            
            return detectionElement;
        } catch (Exception e) {
            // Log the exception and return null
            System.err.println("Error converting to detection UIElement: " + e.getMessage());
            return null;
        }
    }
    
    /**
     * Convert a StandardizedUIElement to a models.UIElement
     * 
     * @param standardizedElement Standardized UI element
     * @param modelElementClass Class of the model UIElement
     * @return Model UIElement or null if conversion fails
     */
    @Nullable
    public static Object toModelUIElement(
            @Nullable StandardizedUIElement standardizedElement,
            @NonNull Class<?> modelElementClass) {
        if (standardizedElement == null) {
            return null;
        }
        
        try {
            // Create a new instance of the model UIElement
            Object modelElement = modelElementClass.getConstructor().newInstance();
            
            // Set properties using reflection
            setProperty(modelElement, "setElementId", standardizedElement.getElementId());
            setProperty(modelElement, "setType", standardizedElement.getElementType().getValue());
            setProperty(modelElement, "setBounds", standardizedElement.getBounds());
            setProperty(modelElement, "setText", standardizedElement.getText());
            setProperty(modelElement, "setContentDescription", standardizedElement.getContentDescription());
            setProperty(modelElement, "setClickable", standardizedElement.isClickable());
            setProperty(modelElement, "setFocusable", standardizedElement.isFocusable());
            setProperty(modelElement, "setVisible", standardizedElement.isVisible());
            setProperty(modelElement, "setEnabled", standardizedElement.isEnabled());
            
            // Set attributes if method exists
            try {
                setProperty(modelElement, "setAttributes", standardizedElement.getAttributes());
            } catch (NoSuchMethodException e) {
                // Ignore if method doesn't exist
            }
            
            return modelElement;
        } catch (Exception e) {
            // Log the exception and return null
            System.err.println("Error converting to model UIElement: " + e.getMessage());
            return null;
        }
    }
    
    /**
     * Convert a list of StandardizedUIElements to detection UIElements
     * 
     * @param standardizedElements List of standardized UI elements
     * @param detectionElementClass Class of the detection UIElement
     * @return List of detection UIElements
     */
    @NonNull
    public static List<Object> toDetectionUIElements(
            @Nullable List<StandardizedUIElement> standardizedElements,
            @NonNull Class<?> detectionElementClass) {
        List<Object> result = new ArrayList<>();
        
        if (standardizedElements == null) {
            return result;
        }
        
        for (StandardizedUIElement element : standardizedElements) {
            Object converted = toDetectionUIElement(element, detectionElementClass);
            if (converted != null) {
                result.add(converted);
            }
        }
        
        return result;
    }
    
    /**
     * Convert a list of StandardizedUIElements to model UIElements
     * 
     * @param standardizedElements List of standardized UI elements
     * @param modelElementClass Class of the model UIElement
     * @return List of model UIElements
     */
    @NonNull
    public static List<Object> toModelUIElements(
            @Nullable List<StandardizedUIElement> standardizedElements,
            @NonNull Class<?> modelElementClass) {
        List<Object> result = new ArrayList<>();
        
        if (standardizedElements == null) {
            return result;
        }
        
        for (StandardizedUIElement element : standardizedElements) {
            Object converted = toModelUIElement(element, modelElementClass);
            if (converted != null) {
                result.add(converted);
            }
        }
        
        return result;
    }
    
    /**
     * Get a string property from an object using reflection
     * 
     * @param object Object to get property from
     * @param methodName Method name to call
     * @return String property value or null if not found
     */
    @Nullable
    private static String getStringProperty(@NonNull Object object, @NonNull String methodName) {
        try {
            return (String) object.getClass().getMethod(methodName).invoke(object);
        } catch (Exception e) {
            return null;
        }
    }
    
    /**
     * Get a boolean property from an object using reflection
     * 
     * @param object Object to get property from
     * @param methodName Method name to call
     * @return Boolean property value or null if not found
     */
    @Nullable
    private static Boolean getBooleanProperty(@NonNull Object object, @NonNull String methodName) {
        try {
            return (Boolean) object.getClass().getMethod(methodName).invoke(object);
        } catch (Exception e) {
            return null;
        }
    }
    
    /**
     * Get a float property from an object using reflection
     * 
     * @param object Object to get property from
     * @param methodName Method name to call
     * @return Float property value or null if not found
     */
    @Nullable
    private static Float getFloatProperty(@NonNull Object object, @NonNull String methodName) {
        try {
            return (Float) object.getClass().getMethod(methodName).invoke(object);
        } catch (Exception e) {
            return null;
        }
    }
    
    /**
     * Get bounds from an object using reflection
     * 
     * @param object Object to get bounds from
     * @return Rect bounds or null if not found
     */
    @Nullable
    private static Rect getBoundsProperty(@NonNull Object object) {
        try {
            return (Rect) object.getClass().getMethod("getBounds").invoke(object);
        } catch (Exception e) {
            // Try alternative methods
            try {
                Object bounds = object.getClass().getMethod("getRect").invoke(object);
                if (bounds instanceof Rect) {
                    return (Rect) bounds;
                }
                return null;
            } catch (Exception e2) {
                // Try to get individual coordinates
                try {
                    int left = (int) object.getClass().getMethod("getLeft").invoke(object);
                    int top = (int) object.getClass().getMethod("getTop").invoke(object);
                    int right = (int) object.getClass().getMethod("getRight").invoke(object);
                    int bottom = (int) object.getClass().getMethod("getBottom").invoke(object);
                    return new Rect(left, top, right, bottom);
                } catch (Exception e3) {
                    return null;
                }
            }
        }
    }
    
    /**
     * Get attributes map from an object using reflection
     * 
     * @param object Object to get attributes from
     * @return Map of attributes or null if not found
     */
    @Nullable
    @SuppressWarnings("unchecked")
    private static Map<String, Object> getAttributesMap(@NonNull Object object) {
        try {
            return (Map<String, Object>) object.getClass().getMethod("getAttributes").invoke(object);
        } catch (Exception e) {
            try {
                return (Map<String, Object>) object.getClass().getMethod("getMetadata").invoke(object);
            } catch (Exception e2) {
                return new HashMap<>();
            }
        }
    }
    
    /**
     * Set a property on an object using reflection
     * 
     * @param object Object to set property on
     * @param methodName Method name to call
     * @param value Value to set
     */
    private static void setProperty(
            @NonNull Object object,
            @NonNull String methodName,
            @Nullable Object value) throws Exception {
        if (value == null) {
            return;
        }
        
        // Find method with matching parameter type
        for (java.lang.reflect.Method method : object.getClass().getMethods()) {
            if (method.getName().equals(methodName) && method.getParameterCount() == 1) {
                Class<?> paramType = method.getParameterTypes()[0];
                if (paramType.isAssignableFrom(value.getClass())) {
                    method.invoke(object, value);
                    return;
                }
            }
        }
        
        throw new NoSuchMethodException("Method not found: " + methodName);
    }
}