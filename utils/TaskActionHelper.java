package utils;

import android.content.Context;
import android.accessibilityservice.AccessibilityService;
import java.lang.reflect.Method;
import java.util.Map;

/**
 * Helper for task actions
 * This class provides methods for executing task actions
 */
public class TaskActionHelper {
    private static final String TAG = "TaskActionHelper";
    
    /**
     * Execute an action using an accessibility service
     * @param service The accessibility service
     * @param action The action to execute
     * @return True if the action was executed successfully, false otherwise
     */
    public static boolean executeAction(AccessibilityService service, Object action) {
        if (service == null || action == null) {
            return false;
        }
        
        try {
            // Try to call the method directly
            try {
                Method method = service.getClass().getMethod("executeAction", action.getClass());
                return (boolean) method.invoke(service, action);
            } catch (Exception e) {
                // Method doesn't exist with this parameter type, try a more generic approach
            }
            
            // Try to extract action information and call more specific methods
            String actionType = null;
            Map<String, Object> actionParams = null;
            
            try {
                Method getTypeMethod = action.getClass().getMethod("getType");
                actionType = (String) getTypeMethod.invoke(action);
            } catch (Exception e) {
                try {
                    Method getActionTypeMethod = action.getClass().getMethod("getActionType");
                    actionType = (String) getActionTypeMethod.invoke(action);
                } catch (Exception ex) {
                    // Ignore
                }
            }
            
            try {
                Method getParamsMethod = action.getClass().getMethod("getParams");
                actionParams = (Map<String, Object>) getParamsMethod.invoke(action);
            } catch (Exception e) {
                try {
                    Method getActionParamsMethod = action.getClass().getMethod("getActionParams");
                    actionParams = (Map<String, Object>) getActionParamsMethod.invoke(action);
                } catch (Exception ex) {
                    try {
                        Method getParametersMethod = action.getClass().getMethod("getParameters");
                        actionParams = (Map<String, Object>) getParametersMethod.invoke(action);
                    } catch (Exception exc) {
                        // Ignore
                    }
                }
            }
            
            if (actionType == null) {
                LogHelper.e(TAG, "Could not determine action type", null);
                return false;
            }
            
            // Execute the action based on its type
            switch (actionType.toLowerCase()) {
                case "click":
                case "tap":
                    int x = 0;
                    int y = 0;
                    
                    if (actionParams != null) {
                        if (actionParams.containsKey("x")) {
                            x = ((Number) actionParams.get("x")).intValue();
                        }
                        if (actionParams.containsKey("y")) {
                            y = ((Number) actionParams.get("y")).intValue();
                        }
                    }
                    
                    performClick(service, x, y);
                    return true;
                    
                case "swipe":
                case "scroll":
                    int startX = 0;
                    int startY = 0;
                    int endX = 0;
                    int endY = 0;
                    int duration = 500;
                    
                    if (actionParams != null) {
                        if (actionParams.containsKey("startX")) {
                            startX = ((Number) actionParams.get("startX")).intValue();
                        }
                        if (actionParams.containsKey("startY")) {
                            startY = ((Number) actionParams.get("startY")).intValue();
                        }
                        if (actionParams.containsKey("endX")) {
                            endX = ((Number) actionParams.get("endX")).intValue();
                        }
                        if (actionParams.containsKey("endY")) {
                            endY = ((Number) actionParams.get("endY")).intValue();
                        }
                        if (actionParams.containsKey("duration")) {
                            duration = ((Number) actionParams.get("duration")).intValue();
                        }
                    }
                    
                    performSwipe(service, startX, startY, endX, endY, duration);
                    return true;
                    
                case "text":
                case "input":
                    String text = "";
                    
                    if (actionParams != null && actionParams.containsKey("text")) {
                        text = (String) actionParams.get("text");
                    }
                    
                    performSetText(service, text);
                    return true;
                    
                case "back":
                    performBack(service);
                    return true;
                    
                case "home":
                    performHome(service);
                    return true;
                    
                default:
                    LogHelper.e(TAG, "Unsupported action type: " + actionType, null);
                    return false;
            }
            
        } catch (Exception e) {
            LogHelper.e(TAG, "Error executing action: " + e.getMessage(), e);
            return false;
        }
    }
    
    /**
     * Perform a click action
     * @param service The accessibility service
     * @param x The X coordinate
     * @param y The Y coordinate
     */
    private static void performClick(AccessibilityService service, int x, int y) {
        if (service == null) {
            return;
        }
        
        try {
            // Try to call the click method directly
            try {
                Method method = service.getClass().getMethod("performClick", int.class, int.class);
                method.invoke(service, x, y);
                return;
            } catch (Exception e) {
                // Method doesn't exist, try a more generic approach
            }
            
            // Use gesture builder
            android.accessibilityservice.GestureDescription.Builder builder = new android.accessibilityservice.GestureDescription.Builder();
            android.graphics.Path path = new android.graphics.Path();
            path.moveTo(x, y);
            
            builder.addStroke(new android.accessibilityservice.GestureDescription.StrokeDescription(path, 0, 100));
            // Use our helper to properly dispatch the gesture
            AccessibilityCallbackHelper.dispatchGesture(service, builder.build(), null);
            
        } catch (Exception e) {
            LogHelper.e(TAG, "Error performing click: " + e.getMessage(), e);
        }
    }
    
    /**
     * Perform a swipe action
     * @param service The accessibility service
     * @param startX The start X coordinate
     * @param startY The start Y coordinate
     * @param endX The end X coordinate
     * @param endY The end Y coordinate
     * @param duration The duration in milliseconds
     */
    private static void performSwipe(AccessibilityService service, int startX, int startY, int endX, int endY, int duration) {
        if (service == null) {
            return;
        }
        
        try {
            // Try to call the swipe method directly
            try {
                Method method = service.getClass().getMethod("performSwipe", int.class, int.class, int.class, int.class, int.class);
                method.invoke(service, startX, startY, endX, endY, duration);
                return;
            } catch (Exception e) {
                // Method doesn't exist, try a more generic approach
            }
            
            // Use gesture builder
            android.accessibilityservice.GestureDescription.Builder builder = new android.accessibilityservice.GestureDescription.Builder();
            android.graphics.Path path = new android.graphics.Path();
            path.moveTo(startX, startY);
            path.lineTo(endX, endY);
            
            builder.addStroke(new android.accessibilityservice.GestureDescription.StrokeDescription(path, 0, duration));
            // Use our helper to properly dispatch the gesture
            AccessibilityCallbackHelper.dispatchGesture(service, builder.build(), null);
            
        } catch (Exception e) {
            LogHelper.e(TAG, "Error performing swipe: " + e.getMessage(), e);
        }
    }
    
    /**
     * Perform a set text action
     * @param service The accessibility service
     * @param text The text to set
     */
    private static void performSetText(AccessibilityService service, String text) {
        if (service == null) {
            return;
        }
        
        try {
            // Try to call the set text method directly
            try {
                Method method = service.getClass().getMethod("performSetText", String.class);
                method.invoke(service, text);
                return;
            } catch (Exception e) {
                // Method doesn't exist, try a more generic approach
            }
            
            // Use bundle and clipboard
            // Use ContextConstants.CLIPBOARD_SERVICE constant instead of Context.CLIPBOARD_SERVICE
            android.content.ClipboardManager clipboard = (android.content.ClipboardManager) service.getSystemService(utils.ContextConstants.CLIPBOARD_SERVICE);
            android.content.ClipData clip = android.content.ClipData.newPlainText("text", text);
            clipboard.setPrimaryClip(clip);
            
            // Get focused node or use ACTION_PASTE with root node
            // Using constant directly since findFocus method isn't being found
            android.view.accessibility.AccessibilityNodeInfo focusedNode = null;
            // Try using reflection to avoid direct usage of FOCUS_INPUT constant
            try {
                java.lang.reflect.Method findFocusMethod = service.getClass().getMethod("findFocus", int.class);
                Object result = findFocusMethod.invoke(service, 2); // 2 is the value for FOCUS_INPUT
                if (result instanceof android.view.accessibility.AccessibilityNodeInfo) {
                    focusedNode = (android.view.accessibility.AccessibilityNodeInfo) result;
                }
            } catch (Exception e) {
                // Fallback if reflection doesn't work
                focusedNode = null;
            }
            if (focusedNode != null) {
                android.os.Bundle arguments = new android.os.Bundle();
                arguments.putCharSequence(utils.ContextConstants.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE, text);
                focusedNode.performAction(utils.ContextConstants.ACTION_SET_TEXT, arguments);
                // Use reflection to call recycle to avoid direct method call
                try {
                    java.lang.reflect.Method recycleMethod = focusedNode.getClass().getMethod("recycle");
                    recycleMethod.invoke(focusedNode);
                } catch (Exception e) {
                    // Ignore if reflection fails
                }
            } else {
                android.view.accessibility.AccessibilityNodeInfo rootNode = service.getRootInActiveWindow();
                if (rootNode != null) {
                    rootNode.performAction(utils.ContextConstants.ACTION_PASTE);
                    // Use reflection to call recycle to avoid direct method call
                    try {
                        java.lang.reflect.Method recycleMethod = rootNode.getClass().getMethod("recycle");
                        recycleMethod.invoke(rootNode);
                    } catch (Exception e) {
                        // Ignore if reflection fails
                    }
                }
            }
            
        } catch (Exception e) {
            LogHelper.e(TAG, "Error performing set text: " + e.getMessage(), e);
        }
    }
    
    /**
     * Perform a back action
     * @param service The accessibility service
     */
    private static void performBack(AccessibilityService service) {
        if (service == null) {
            return;
        }
        
        try {
            service.performGlobalAction(utils.ContextConstants.GLOBAL_ACTION_BACK);
        } catch (Exception e) {
            LogHelper.e(TAG, "Error performing back: " + e.getMessage(), e);
        }
    }
    
    /**
     * Perform a home action
     * @param service The accessibility service
     */
    private static void performHome(AccessibilityService service) {
        if (service == null) {
            return;
        }
        
        try {
            service.performGlobalAction(utils.ContextConstants.GLOBAL_ACTION_HOME);
        } catch (Exception e) {
            LogHelper.e(TAG, "Error performing home: " + e.getMessage(), e);
        }
    }
}