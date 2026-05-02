package com.aiassistant.services;

import android.accessibilityservice.AccessibilityService;
import android.accessibilityservice.AccessibilityServiceInfo;
import android.accessibilityservice.GestureDescription;
import android.content.ComponentName;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.graphics.Path;
import android.graphics.Rect;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityNodeInfo;

import androidx.preference.PreferenceManager;

import com.aiassistant.core.AIController;
import com.aiassistant.learning.LearningEngine;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Accessibility service that monitors and interacts with UI across the device.
 *
 * Improvements over the original:
 *  - Static instance reference so AIController can dispatch gestures directly.
 *  - Real GestureDescription-based gesture execution (click, long-press, swipe).
 *  - LearningEngine integration: every observed interaction is recorded.
 *  - Handles TYPE_VIEW_LONG_CLICKED and TYPE_VIEW_SCROLLED in addition to the
 *    four original event types.
 *  - Rate-limiting: no more than MAX_EVENTS_PER_SECOND events are forwarded to
 *    the background thread, preventing overload during fast UI animation.
 *  - Privacy mode is respected for all new event types.
 *  - findInteractiveElements now performs bounded-depth tree traversal to avoid
 *    StackOverflow on deep view hierarchies.
 */
public class AIAccessibilityService extends AccessibilityService {
    private static final String TAG = "AIAccessibilityService";

    // Static instance used by AIController to dispatch gestures
    private static volatile AIAccessibilityService instance;

    // Rate-limiting
    private static final int MAX_EVENTS_PER_SECOND = 20;
    private long   rateLimitWindowStart = 0;
    private int    rateLimitCount       = 0;

    // Max tree-traversal depth to avoid StackOverflow
    private static final int MAX_TRAVERSE_DEPTH = 30;

    private ExecutorService executor;
    private volatile boolean isLearningEnabled  = true;
    private volatile boolean isPrivacyModeEnabled = false;

    private LearningEngine learningEngine;
    private AIController   aiController;

    // Interaction tracking
    private String lastPackageName   = "";
    private String lastActivityName  = "";
    private long   lastInteractionTime = 0;

    // Gesture result latch & flag
    private final AtomicBoolean gestureResult = new AtomicBoolean(false);

    // Sensitive app set for fast lookup
    private static final Set<String> SENSITIVE_APPS = new HashSet<>();
    static {
        SENSITIVE_APPS.add("com.infonaut.bofa");
        SENSITIVE_APPS.add("com.chase.sig.android");
        SENSITIVE_APPS.add("com.wf.wellsfargomobile");
        SENSITIVE_APPS.add("com.paypal.android.p2pmobile");
        SENSITIVE_APPS.add("com.venmo");
        SENSITIVE_APPS.add("org.thoughtcrime.securesms");
        SENSITIVE_APPS.add("com.whatsapp");
        SENSITIVE_APPS.add("com.lastpass.lpandroid");
        SENSITIVE_APPS.add("com.agilebits.onepassword");
        SENSITIVE_APPS.add("com.google.android.apps.authenticator2");
    }

    // -------------------------------------------------------------------------
    // Static accessor
    // -------------------------------------------------------------------------

    /** Returns the running service instance, or null if not yet connected. */
    public static AIAccessibilityService getInstance() { return instance; }

    // -------------------------------------------------------------------------
    // Lifecycle
    // -------------------------------------------------------------------------

    @Override
    public void onCreate() {
        super.onCreate();
        executor = Executors.newSingleThreadExecutor();
        loadPreferences();
        instance = this;

        // Initialise subsystems
        learningEngine = new LearningEngine(getApplicationContext());
        aiController   = AIController.getInstance(getApplicationContext());

        Log.i(TAG, "AIAccessibilityService created");
    }

    @Override
    protected void onServiceConnected() {
        super.onServiceConnected();
        instance = this;

        AccessibilityServiceInfo info = getServiceInfo();
        if (info == null) info = new AccessibilityServiceInfo();

        info.eventTypes =
                AccessibilityEvent.TYPE_VIEW_CLICKED          |
                AccessibilityEvent.TYPE_VIEW_LONG_CLICKED     |
                AccessibilityEvent.TYPE_VIEW_FOCUSED          |
                AccessibilityEvent.TYPE_VIEW_TEXT_CHANGED     |
                AccessibilityEvent.TYPE_VIEW_SCROLLED         |
                AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED  |
                AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED;

        info.packageNames = null; // monitor all packages
        info.feedbackType = AccessibilityServiceInfo.FEEDBACK_GENERIC;
        info.flags =
                AccessibilityServiceInfo.FLAG_INCLUDE_NOT_IMPORTANT_VIEWS |
                AccessibilityServiceInfo.FLAG_REPORT_VIEW_IDS             |
                AccessibilityServiceInfo.FLAG_REQUEST_TOUCH_EXPLORATION_MODE;
        info.notificationTimeout = 100;

        setServiceInfo(info);
        Log.i(TAG, "AIAccessibilityService connected");
    }

    @Override
    public void onInterrupt() {
        Log.w(TAG, "Accessibility service interrupted");
    }

    @Override
    public void onDestroy() {
        instance = null;
        if (executor != null) executor.shutdown();
        super.onDestroy();
        Log.i(TAG, "AIAccessibilityService destroyed");
    }

    // -------------------------------------------------------------------------
    // Event handling
    // -------------------------------------------------------------------------

    @Override
    public void onAccessibilityEvent(AccessibilityEvent event) {
        if (!isLearningEnabled) return;

        // Rate-limiting
        long now = System.currentTimeMillis();
        if (now - rateLimitWindowStart >= 1000) {
            rateLimitWindowStart = now;
            rateLimitCount = 0;
        }
        if (++rateLimitCount > MAX_EVENTS_PER_SECOND) return;

        CharSequence packageName = event.getPackageName();
        if (packageName == null) return;

        lastInteractionTime = now;
        final AccessibilityEvent copy = AccessibilityEvent.obtain(event);
        executor.execute(() -> processEventBackground(copy));
    }

    private void processEventBackground(AccessibilityEvent event) {
        try {
            int    eventType    = event.getEventType();
            String packageName  = String.valueOf(event.getPackageName());

            // Never monitor our own app
            if (packageName.equals(getPackageName())) return;

            // Privacy mode: skip sensitive apps
            if (isPrivacyModeEnabled && SENSITIVE_APPS.contains(packageName)) return;

            // App-change detection
            ComponentName cn           = getEventActivity(event);
            String         activityName = cn != null ? cn.getClassName() : "";
            if (!packageName.equals(lastPackageName) || !activityName.equals(lastActivityName)) {
                onAppChanged(packageName, activityName);
                lastPackageName  = packageName;
                lastActivityName = activityName;
            }

            switch (eventType) {
                case AccessibilityEvent.TYPE_VIEW_CLICKED:
                    processViewClick(event);
                    break;
                case AccessibilityEvent.TYPE_VIEW_LONG_CLICKED:
                    processViewLongClick(event);
                    break;
                case AccessibilityEvent.TYPE_VIEW_TEXT_CHANGED:
                    processTextChange(event);
                    break;
                case AccessibilityEvent.TYPE_VIEW_SCROLLED:
                    processViewScroll(event);
                    break;
                case AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED:
                    processWindowChange(event);
                    break;
                case AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED:
                    processContentChange(event);
                    break;
            }
        } catch (Exception e) {
            Log.e(TAG, "Error processing event", e);
        } finally {
            event.recycle();
        }
    }

    // -------------------------------------------------------------------------
    // Specific event processors
    // -------------------------------------------------------------------------

    private void processViewClick(AccessibilityEvent event) {
        AccessibilityNodeInfo node = event.getSource();
        if (node == null) return;
        try {
            String id   = safeString(node.getViewIdResourceName());
            String text = safeString(node.getText());
            Rect bounds = new Rect();
            node.getBoundsInScreen(bounds);

            Map<String, Object> data = new HashMap<>();
            data.put("elementId",   id);
            data.put("elementText", text);
            data.put("bounds",      bounds.toShortString());
            data.put("package",     String.valueOf(event.getPackageName()));
            data.put("class",       String.valueOf(event.getClassName()));

            recordInteraction("click", String.valueOf(event.getPackageName()),
                    String.valueOf(event.getClassName()), id, text, data);
        } finally {
            node.recycle();
        }
    }

    private void processViewLongClick(AccessibilityEvent event) {
        AccessibilityNodeInfo node = event.getSource();
        if (node == null) return;
        try {
            String id   = safeString(node.getViewIdResourceName());
            String text = safeString(node.getText());
            Map<String, Object> data = new HashMap<>();
            data.put("elementId", id); data.put("elementText", text);
            data.put("package", String.valueOf(event.getPackageName()));
            recordInteraction("long_click", String.valueOf(event.getPackageName()),
                    String.valueOf(event.getClassName()), id, text, data);
        } finally {
            node.recycle();
        }
    }

    private void processTextChange(AccessibilityEvent event) {
        if (isPrivacyModeEnabled) return; // never record text in privacy mode
        AccessibilityNodeInfo node = event.getSource();
        if (node == null) return;
        try {
            String id = safeString(node.getViewIdResourceName());
            Map<String, Object> data = new HashMap<>();
            data.put("elementId", id);
            data.put("package", String.valueOf(event.getPackageName()));
            recordInteraction("text_input", String.valueOf(event.getPackageName()),
                    String.valueOf(event.getClassName()), id, "text_field_interaction", data);
        } finally {
            node.recycle();
        }
    }

    private void processViewScroll(AccessibilityEvent event) {
        int fromIndex = event.getFromIndex();
        int itemCount = event.getItemCount();
        String pkg    = String.valueOf(event.getPackageName());
        Map<String, Object> data = new HashMap<>();
        data.put("fromIndex", fromIndex);
        data.put("itemCount", itemCount);
        data.put("package", pkg);
        recordInteraction("scroll", pkg, String.valueOf(event.getClassName()),
                "", "", data);
    }

    private void processWindowChange(AccessibilityEvent event) {
        String pkg      = String.valueOf(event.getPackageName());
        String cls      = String.valueOf(event.getClassName());
        String title    = event.getText().isEmpty() ? "" : event.getText().get(0).toString();
        Map<String, Object> data = new HashMap<>();
        data.put("package", pkg); data.put("class", cls); data.put("title", title);
        recordInteraction("window_change", pkg, cls, "", title, data);
    }

    private void processContentChange(AccessibilityEvent event) {
        if (event.getContentChangeTypes() != AccessibilityEvent.CONTENT_CHANGE_TYPE_SUBTREE) return;
        AccessibilityNodeInfo node = event.getSource();
        if (node != null) node.recycle();
    }

    // -------------------------------------------------------------------------
    // Interaction recording (feeds into LearningEngine)
    // -------------------------------------------------------------------------

    private void recordInteraction(String action, String packageName, String className,
                                   String elementId, String content, Map<String, Object> extra) {
        Log.d(TAG, action + " | " + packageName + " | " + className + " | " + elementId);
        if (learningEngine != null) {
            Map<String, Object> record = new HashMap<>(extra);
            record.put("action", action);
            record.put("packageName", packageName);
            record.put("className", className);
            record.put("elementId", elementId);
            record.put("content", content);
            record.put("timestamp", System.currentTimeMillis());
            // Forward to LearningEngine as a screen analysis event
            learningEngine.processScreenAnalysis(record);
        }
    }

    // -------------------------------------------------------------------------
    // Gesture execution
    // -------------------------------------------------------------------------

    /** Performs a tap at (x, y). Returns true if the gesture was dispatched. */
    public boolean performClick(int x, int y) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N) return false;
        Path path = new Path();
        path.moveTo(x, y);
        GestureDescription.StrokeDescription stroke =
                new GestureDescription.StrokeDescription(path, 0, 50);
        return dispatchGestureSync(new GestureDescription.Builder().addStroke(stroke).build());
    }

    /** Performs a long-press at (x, y). Returns true if dispatched. */
    public boolean performLongPress(int x, int y) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N) return false;
        Path path = new Path();
        path.moveTo(x, y);
        GestureDescription.StrokeDescription stroke =
                new GestureDescription.StrokeDescription(path, 0, 800);
        return dispatchGestureSync(new GestureDescription.Builder().addStroke(stroke).build());
    }

    /** Performs a swipe from (sx,sy) to (ex,ey) over the given duration. */
    public boolean performSwipe(int sx, int sy, int ex, int ey, long durationMs) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N) return false;
        Path path = new Path();
        path.moveTo(sx, sy);
        path.lineTo(ex, ey);
        GestureDescription.StrokeDescription stroke =
                new GestureDescription.StrokeDescription(path, 0, Math.max(durationMs, 1));
        return dispatchGestureSync(new GestureDescription.Builder().addStroke(stroke).build());
    }

    private boolean dispatchGestureSync(GestureDescription gesture) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N) return false;
        final CountDownLatch latch = new CountDownLatch(1);
        gestureResult.set(false);
        boolean accepted = dispatchGesture(gesture, new GestureResultCallback() {
            @Override public void onCompleted(GestureDescription g) {
                gestureResult.set(true); latch.countDown();
            }
            @Override public void onCancelled(GestureDescription g) {
                gestureResult.set(false); latch.countDown();
            }
        }, null);
        if (!accepted) return false;
        try { latch.await(2, TimeUnit.SECONDS); } catch (InterruptedException ignored) {}
        return gestureResult.get();
    }

    /** Performs a global action (e.g. GLOBAL_ACTION_BACK) on the UI thread. */
    public boolean performGlobalActionSafe(int action) {
        return performGlobalAction(action);
    }

    // -------------------------------------------------------------------------
    // Screen reading helpers
    // -------------------------------------------------------------------------

    /**
     * Returns all interactive nodes under {@code root} with bounded depth traversal
     * to avoid StackOverflow on very deep view hierarchies.
     */
    public List<AccessibilityNodeInfo> findInteractiveElements(AccessibilityNodeInfo root) {
        List<AccessibilityNodeInfo> result = new ArrayList<>();
        if (root == null) return result;
        // Iterative BFS with depth guard
        Deque<Object[]> queue = new ArrayDeque<>();
        queue.add(new Object[]{root, 0});
        while (!queue.isEmpty()) {
            Object[] item  = queue.poll();
            AccessibilityNodeInfo node  = (AccessibilityNodeInfo) item[0];
            int depth = (int) item[1];
            if (node == null) continue;
            if (node.isClickable() || node.isCheckable() || node.isScrollable() ||
                    (node.getText() != null && node.getText().length() > 0)) {
                result.add(AccessibilityNodeInfo.obtain(node));
            }
            if (depth < MAX_TRAVERSE_DEPTH) {
                for (int i = 0; i < node.getChildCount(); i++) {
                    AccessibilityNodeInfo child = node.getChild(i);
                    if (child != null) queue.add(new Object[]{child, depth + 1});
                }
            }
            if (node != root) node.recycle();
        }
        return result;
    }

    /** Finds the first node whose text or content-desc contains {@code label}. */
    public AccessibilityNodeInfo findNodeByText(String label) {
        AccessibilityNodeInfo root = getRootInActiveWindow();
        if (root == null || label == null) return null;
        List<AccessibilityNodeInfo> matches = root.findAccessibilityNodeInfosByText(label);
        root.recycle();
        return matches.isEmpty() ? null : matches.get(0);
    }

    /** Finds the first node whose resource-id ends with {@code resourceId}. */
    public AccessibilityNodeInfo findNodeById(String resourceId) {
        AccessibilityNodeInfo root = getRootInActiveWindow();
        if (root == null || resourceId == null) return null;
        List<AccessibilityNodeInfo> matches = root.findAccessibilityNodeInfosByViewId(resourceId);
        root.recycle();
        return matches.isEmpty() ? null : matches.get(0);
    }

    /** Tap on the node described by its on-screen text. */
    public boolean tapNodeByText(String label) {
        AccessibilityNodeInfo node = findNodeByText(label);
        if (node == null) return false;
        Rect bounds = new Rect();
        node.getBoundsInScreen(bounds);
        node.recycle();
        return performClick(bounds.centerX(), bounds.centerY());
    }

    /** Type {@code text} into the currently focused input field. */
    public boolean typeText(String text) {
        AccessibilityNodeInfo focused = findFocus(FOCUS_INPUT);
        if (focused == null) return false;
        Bundle args = new Bundle();
        args.putCharSequence(AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE, text);
        boolean ok = focused.performAction(AccessibilityNodeInfo.ACTION_SET_TEXT, args);
        focused.recycle();
        return ok;
    }

    // -------------------------------------------------------------------------
    // Internal helpers
    // -------------------------------------------------------------------------

    private void onAppChanged(String packageName, String activityName) {
        Log.d(TAG, "App changed: " + packageName + " / " + activityName);
        if (aiController != null) {
            models.AppInfo info = new models.AppInfo();
            info.setPackageName(packageName);
            info.setGameType(AIController.GameType.fromPackageName(packageName));
            aiController.setCurrentApp(info);
        }
    }

    private ComponentName getEventActivity(AccessibilityEvent event) {
        try {
            if (event.getPackageName() == null || event.getClassName() == null) return null;
            String pkg = String.valueOf(event.getPackageName());
            String cls = String.valueOf(event.getClassName());
            ActivityInfo info = getPackageManager().getActivityInfo(
                    new ComponentName(pkg, cls), 0);
            return info != null ? new ComponentName(pkg, cls) : null;
        } catch (PackageManager.NameNotFoundException ignored) {
            return null;
        } catch (Exception e) {
            return null;
        }
    }

    private boolean isSensitiveApp(String packageName) {
        return SENSITIVE_APPS.contains(packageName);
    }

    private void loadPreferences() {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        isLearningEnabled     = prefs.getBoolean("enable_learning", true);
        isPrivacyModeEnabled  = prefs.getBoolean("privacy_mode", false);
    }

    private static String safeString(CharSequence cs) {
        return cs == null ? "" : cs.toString();
    }

    // -------------------------------------------------------------------------
    // Public settings API
    // -------------------------------------------------------------------------

    public void setLearningEnabled(boolean enabled)  { isLearningEnabled     = enabled; }
    public void setPrivacyMode(boolean enabled)       { isPrivacyModeEnabled  = enabled; }
    public long getLastInteractionTime()              { return lastInteractionTime; }
    public String getCurrentPackageName()             { return lastPackageName; }
}
