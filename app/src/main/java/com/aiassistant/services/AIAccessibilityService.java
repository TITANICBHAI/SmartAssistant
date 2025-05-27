package com.aiassistant.services;

import android.accessibilityservice.AccessibilityService;
import android.accessibilityservice.AccessibilityServiceInfo;
import android.content.ComponentName;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.util.Log;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityNodeInfo;

import androidx.preference.PreferenceManager;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Accessibility service to monitor and interact with UI elements across the device
 */
public class AIAccessibilityService extends AccessibilityService {
    private static final String TAG = "AIAccessibilityService";
    
    private ExecutorService executor;
    private boolean isLearningEnabled = true;
    private boolean isPrivacyModeEnabled = false;
    
    // Store the last interaction data
    private String lastPackageName = "";
    private String lastActivityName = "";
    private long lastInteractionTime = 0;
    
    @Override
    public void onCreate() {
        super.onCreate();
        executor = Executors.newSingleThreadExecutor();
        loadPreferences();
    }
    
    @Override
    public void onAccessibilityEvent(AccessibilityEvent event) {
        if (!isLearningEnabled) {
            return;
        }
        
        int eventType = event.getEventType();
        CharSequence packageName = event.getPackageName();
        
        if (packageName == null) {
            return;
        }
        
        // Record interaction time
        lastInteractionTime = System.currentTimeMillis();
        
        // Process events in a background thread to avoid blocking UI
        final AccessibilityEvent finalEvent = AccessibilityEvent.obtain(event);
        executor.execute(() -> processEventBackground(finalEvent));
    }
    
    private void processEventBackground(AccessibilityEvent event) {
        try {
            int eventType = event.getEventType();
            String packageName = String.valueOf(event.getPackageName());
            
            // Don't monitor our own app
            if (packageName.equals(getPackageName())) {
                return;
            }
            
            // Don't monitor sensitive apps in privacy mode
            if (isPrivacyModeEnabled && isSensitiveApp(packageName)) {
                return;
            }
            
            // Get activity name if possible
            ComponentName componentName = getEventActivity(event);
            String activityName = componentName != null ? 
                    componentName.getClassName().toString() : "";
            
            // Update package and activity tracking if changed
            if (!packageName.equals(lastPackageName) || !activityName.equals(lastActivityName)) {
                onAppChanged(packageName, activityName);
                lastPackageName = packageName;
                lastActivityName = activityName;
            }
            
            // Process different event types
            switch (eventType) {
                case AccessibilityEvent.TYPE_VIEW_CLICKED:
                    processViewClick(event);
                    break;
                    
                case AccessibilityEvent.TYPE_VIEW_TEXT_CHANGED:
                    processTextChange(event);
                    break;
                    
                case AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED:
                    processWindowChange(event);
                    break;
                    
                case AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED:
                    processContentChange(event);
                    break;
            }
        } catch (Exception e) {
            Log.e(TAG, "Error processing accessibility event", e);
        } finally {
            event.recycle();
        }
    }
    
    private ComponentName getEventActivity(AccessibilityEvent event) {
        ComponentName componentName = null;
        try {
            PackageManager packageManager = getPackageManager();
            if (event.getPackageName() != null && packageManager != null) {
                String packageName = String.valueOf(event.getPackageName());
                
                // Check if we have a relevant activity class name from the event
                if (event.getClassName() != null) {
                    String className = String.valueOf(event.getClassName());
                    
                    // Try to get activity info to verify it's actually an activity
                    try {
                        ActivityInfo activityInfo = packageManager.getActivityInfo(
                                new ComponentName(packageName, className), 0);
                        if (activityInfo != null) {
                            componentName = new ComponentName(packageName, className);
                        }
                    } catch (PackageManager.NameNotFoundException e) {
                        // Not an activity, ignore
                    }
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Error getting activity from event", e);
        }
        return componentName;
    }
    
    private void onAppChanged(String packageName, String activityName) {
        Log.d(TAG, "App changed: " + packageName + " / " + activityName);
        
        // In a real implementation, this would notify learning services about the app change
    }
    
    private void processViewClick(AccessibilityEvent event) {
        Log.d(TAG, "View clicked: " + event.getClassName());
        
        // Extract information about the clicked element
        String elementText = "";
        String elementId = "";
        
        if (event.getSource() != null) {
            // Try to get text content
            if (event.getSource().getText() != null && !event.getSource().getText().toString().isEmpty()) {
                elementText = event.getSource().getText().toString();
            }
            
            // Try to get element ID
            if (event.getSource().getViewIdResourceName() != null) {
                elementId = event.getSource().getViewIdResourceName();
            }
            
            // In a real implementation, record this interaction for learning
            recordElementInteraction(
                    "click",
                    String.valueOf(event.getPackageName()),
                    String.valueOf(event.getClassName()),
                    elementId,
                    elementText
            );
            
            // Release the node info
            event.getSource().recycle();
        }
    }
    
    private void processTextChange(AccessibilityEvent event) {
        // In privacy mode, we don't track text input
        if (isPrivacyModeEnabled) {
            return;
        }
        
        Log.d(TAG, "Text changed: " + event.getClassName());
        
        // In a real implementation, record the text change for learning
        if (event.getSource() != null) {
            // Get the field ID if available
            String elementId = "";
            if (event.getSource().getViewIdResourceName() != null) {
                elementId = event.getSource().getViewIdResourceName();
            }
            
            // Record only that a field was typed in, not the actual text
            recordElementInteraction(
                    "text_input",
                    String.valueOf(event.getPackageName()),
                    String.valueOf(event.getClassName()),
                    elementId,
                    "text_field_interaction"  // Not recording actual text for privacy
            );
            
            // Release the node info
            event.getSource().recycle();
        }
    }
    
    private void processWindowChange(AccessibilityEvent event) {
        Log.d(TAG, "Window state changed: " + event.getClassName());
        
        // In a real implementation, record window changes for learning about app navigation
    }
    
    private void processContentChange(AccessibilityEvent event) {
        // Only process significant content changes to avoid overwhelming the system
        if (event.getContentChangeTypes() == AccessibilityEvent.CONTENT_CHANGE_TYPE_SUBTREE) {
            if (event.getSource() != null) {
                // In a real implementation, we would analyze the view hierarchy for changes
                event.getSource().recycle();
            }
        }
    }
    
    private void recordElementInteraction(String action, String packageName, 
                                         String className, String elementId, String content) {
        // In a real implementation, this would send the interaction to the learning system
        Log.d(TAG, "Interaction: " + action + " on " + className + " (" + elementId + ") in " + packageName);
    }
    
    private boolean isSensitiveApp(String packageName) {
        // Check if this is a sensitive app that should be excluded in privacy mode
        List<String> sensitiveApps = new ArrayList<>();
        
        // Banking apps
        sensitiveApps.add("com.infonaut.bofa");
        sensitiveApps.add("com.chase.sig.android");
        sensitiveApps.add("com.wf.wellsfargomobile");
        
        // Payment apps
        sensitiveApps.add("com.paypal.android.p2pmobile");
        sensitiveApps.add("com.venmo");
        
        // Messaging apps
        sensitiveApps.add("org.thoughtcrime.securesms"); // Signal
        sensitiveApps.add("com.whatsapp");
        
        // Password managers
        sensitiveApps.add("com.lastpass.lpandroid");
        sensitiveApps.add("com.agilebits.onepassword");
        
        return sensitiveApps.contains(packageName);
    }
    
    /**
     * Load user preferences
     */
    private void loadPreferences() {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        isLearningEnabled = prefs.getBoolean("enable_learning", true);
        isPrivacyModeEnabled = prefs.getBoolean("privacy_mode", false);
    }
    
    /**
     * Get all interactive elements from the current screen
     */
    public List<AccessibilityNodeInfo> findInteractiveElements(AccessibilityNodeInfo root) {
        List<AccessibilityNodeInfo> elements = new ArrayList<>();
        if (root == null) {
            return elements;
        }
        
        // Add the root if it's clickable
        if (root.isClickable() || root.isCheckable() || root.isScrollable() || 
                (root.getText() != null && !root.getText().toString().isEmpty())) {
            elements.add(AccessibilityNodeInfo.obtain(root));
        }
        
        // Recursively add all children
        for (int i = 0; i < root.getChildCount(); i++) {
            AccessibilityNodeInfo child = root.getChild(i);
            if (child != null) {
                elements.addAll(findInteractiveElements(child));
                child.recycle();
            }
        }
        
        return elements;
    }
    
    @Override
    public void onInterrupt() {
        Log.d(TAG, "Accessibility service interrupted");
    }
    
    @Override
    public void onDestroy() {
        super.onDestroy();
        
        if (executor != null) {
            executor.shutdown();
        }
        
        Log.d(TAG, "Accessibility service destroyed");
    }
    
    @Override
    protected void onServiceConnected() {
        super.onServiceConnected();
        Log.d(TAG, "Accessibility service connected");
        
        AccessibilityServiceInfo info = getServiceInfo();
        
        // Configure service capabilities
        info.eventTypes = AccessibilityEvent.TYPE_VIEW_CLICKED |
                          AccessibilityEvent.TYPE_VIEW_FOCUSED |
                          AccessibilityEvent.TYPE_VIEW_TEXT_CHANGED |
                          AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED |
                          AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED;
        
        info.packageNames = null; // Monitor all packages
        info.feedbackType = AccessibilityServiceInfo.FEEDBACK_GENERIC;
        info.flags = AccessibilityServiceInfo.FLAG_INCLUDE_NOT_IMPORTANT_VIEWS |
                     AccessibilityServiceInfo.FLAG_REPORT_VIEW_IDS |
                     AccessibilityServiceInfo.FLAG_REQUEST_TOUCH_EXPLORATION_MODE;
                     
        info.notificationTimeout = 100; // 100ms
        
        setServiceInfo(info);
    }
}