package utils;

import android.accessibilityservice.AccessibilityService;
import android.accessibilityservice.GestureDescription;

/**
 * Helper class to handle accessibility service gesture callbacks
 */
public class AccessibilityCallbackHelper {
    
    /**
     * Interface for gesture result callbacks
     */
    public interface GestureResultCallback {
        /**
         * Called when a gesture completes successfully
         * 
         * @param gestureDescription The gesture description that completed
         */
        void onCompleted(GestureDescription gestureDescription);
        
        /**
         * Called when a gesture is cancelled
         * 
         * @param gestureDescription The gesture description that was cancelled
         */
        void onCancelled(GestureDescription gestureDescription);
    }
    
    /**
     * Adapter class for gesture result callbacks with default implementations
     */
    public static class GestureResultCallbackAdapter implements GestureResultCallback {
        @Override
        public void onCompleted(GestureDescription gestureDescription) {
            // Default implementation does nothing
        }
        
        @Override
        public void onCancelled(GestureDescription gestureDescription) {
            // Default implementation does nothing
        }
    }
    
    /**
     * Dispatch a gesture with callback
     * 
     * @param service The accessibility service
     * @param gesture The gesture description
     * @param callback The callback
     * @return true if the gesture was dispatched successfully
     */
    public static boolean dispatchGesture(AccessibilityService service, GestureDescription gesture, 
            GestureResultCallback callback) {
        if (service == null || gesture == null) {
            return false;
        }
        
        // Create a compatibility wrapper for the callback
        AccessibilityService.GestureResultCallback gestureCallback = null;
        
        if (callback != null) {
            gestureCallback = new AccessibilityService.GestureResultCallback() {
                @Override
                public void onCompleted(GestureDescription gestureDescription) {
                    callback.onCompleted(gestureDescription);
                }
                
                @Override
                public void onCancelled(GestureDescription gestureDescription) {
                    callback.onCancelled(gestureDescription);
                }
            };
        }
        
        try {
            // Call the actual dispatch method
            return service.dispatchGesture(gesture, gestureCallback, null);
        } catch (Exception e) {
            LogHelper.e("AccessibilityCallbackHelper", "Error dispatching gesture", e);
            return false;
        }
    }
}