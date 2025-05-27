package com.aiassistant.utils;

import android.accessibilityservice.AccessibilityService;
import android.accessibilityservice.GestureDescription;
import android.content.Context;
import android.graphics.Path;
import android.graphics.Point;
import android.graphics.Rect;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.accessibility.AccessibilityNodeInfo;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Utility class for accessibility operations
 */
public class AccessibilityUtils {
    private static final String TAG = "AccessibilityUtils";
    
    /**
     * Find a node by text exactly matching the given text
     */
    @Nullable
    public static AccessibilityNodeInfo findNodeByExactText(
            @Nullable AccessibilityNodeInfo root, @NonNull String text) {
        if (root == null || text == null) {
            return null;
        }
        
        // Check if this node matches
        CharSequence nodeText = root.getText();
        if (nodeText != null && text.equals(nodeText.toString())) {
            return AccessibilityNodeInfo.obtain(root);
        }
        
        // Check children
        for (int i = 0; i < root.getChildCount(); i++) {
            AccessibilityNodeInfo child = root.getChild(i);
            if (child != null) {
                AccessibilityNodeInfo result = findNodeByExactText(child, text);
                child.recycle();
                if (result != null) {
                    return result;
                }
            }
        }
        
        return null;
    }
    
    /**
     * Find a node by text containing the given text
     */
    @Nullable
    public static AccessibilityNodeInfo findNodeByTextContaining(
            @Nullable AccessibilityNodeInfo root, @NonNull String text) {
        if (root == null || text == null) {
            return null;
        }
        
        // Check if this node contains the text
        CharSequence nodeText = root.getText();
        if (nodeText != null && nodeText.toString().contains(text)) {
            return AccessibilityNodeInfo.obtain(root);
        }
        
        // Check content description
        CharSequence contentDesc = root.getContentDescription();
        if (contentDesc != null && contentDesc.toString().contains(text)) {
            return AccessibilityNodeInfo.obtain(root);
        }
        
        // Check children
        for (int i = 0; i < root.getChildCount(); i++) {
            AccessibilityNodeInfo child = root.getChild(i);
            if (child != null) {
                AccessibilityNodeInfo result = findNodeByTextContaining(child, text);
                child.recycle();
                if (result != null) {
                    return result;
                }
            }
        }
        
        return null;
    }
    
    /**
     * Find a node by ID
     */
    @Nullable
    public static AccessibilityNodeInfo findNodeById(
            @Nullable AccessibilityNodeInfo root, @NonNull String viewId) {
        if (root == null || viewId == null) {
            return null;
        }
        
        // Find by view ID directly
        List<AccessibilityNodeInfo> nodes = root.findAccessibilityNodeInfosByViewId(viewId);
        if (nodes != null && !nodes.isEmpty()) {
            AccessibilityNodeInfo result = AccessibilityNodeInfo.obtain(nodes.get(0));
            // Recycle all nodes
            for (AccessibilityNodeInfo node : nodes) {
                node.recycle();
            }
            return result;
        }
        
        return null;
    }
    
    /**
     * Find a node at the specified screen coordinates
     */
    @Nullable
    public static AccessibilityNodeInfo findNodeAtCoordinates(
            @Nullable AccessibilityNodeInfo root, int x, int y) {
        if (root == null) {
            return null;
        }
        
        // Check if point is inside this node
        Rect bounds = new Rect();
        root.getBoundsInScreen(bounds);
        if (bounds.contains(x, y)) {
            // Check children first (more specific)
            for (int i = 0; i < root.getChildCount(); i++) {
                AccessibilityNodeInfo child = root.getChild(i);
                if (child != null) {
                    AccessibilityNodeInfo result = findNodeAtCoordinates(child, x, y);
                    child.recycle();
                    if (result != null) {
                        return result;
                    }
                }
            }
            
            // No child contains the point, so this node contains it
            return AccessibilityNodeInfo.obtain(root);
        }
        
        return null;
    }
    
    /**
     * Find all clickable nodes in the hierarchy
     */
    @NonNull
    public static List<AccessibilityNodeInfo> findAllClickableNodes(
            @Nullable AccessibilityNodeInfo root) {
        List<AccessibilityNodeInfo> result = new ArrayList<>();
        findAllClickableNodesRecursive(root, result);
        return result;
    }
    
    /**
     * Helper method to find all clickable nodes recursively
     */
    private static void findAllClickableNodesRecursive(
            @Nullable AccessibilityNodeInfo node, @NonNull List<AccessibilityNodeInfo> result) {
        if (node == null) {
            return;
        }
        
        if (node.isClickable() && node.isVisibleToUser()) {
            result.add(AccessibilityNodeInfo.obtain(node));
        }
        
        for (int i = 0; i < node.getChildCount(); i++) {
            AccessibilityNodeInfo child = node.getChild(i);
            if (child != null) {
                findAllClickableNodesRecursive(child, result);
                child.recycle();
            }
        }
    }
    
    /**
     * Count the total number of nodes in the hierarchy
     */
    public static int countNodes(@Nullable AccessibilityNodeInfo root) {
        if (root == null) {
            return 0;
        }
        
        int count = 1; // Count this node
        
        for (int i = 0; i < root.getChildCount(); i++) {
            AccessibilityNodeInfo child = root.getChild(i);
            if (child != null) {
                count += countNodes(child);
                child.recycle();
            }
        }
        
        return count;
    }
    
    /**
     * Count the number of clickable nodes in the hierarchy
     */
    public static int countClickableNodes(@Nullable AccessibilityNodeInfo root) {
        if (root == null) {
            return 0;
        }
        
        int count = root.isClickable() ? 1 : 0;
        
        for (int i = 0; i < root.getChildCount(); i++) {
            AccessibilityNodeInfo child = root.getChild(i);
            if (child != null) {
                count += countClickableNodes(child);
                child.recycle();
            }
        }
        
        return count;
    }
    
    /**
     * Perform a click on the specified node
     */
    public static boolean performClick(@Nullable AccessibilityNodeInfo node) {
        if (node == null) {
            return false;
        }
        
        if (node.isClickable()) {
            return node.performAction(AccessibilityNodeInfo.ACTION_CLICK);
        } else {
            // Try to find clickable parent
            AccessibilityNodeInfo parent = node.getParent();
            if (parent != null) {
                try {
                    if (parent.isClickable()) {
                        return parent.performAction(AccessibilityNodeInfo.ACTION_CLICK);
                    }
                } finally {
                    parent.recycle();
                }
            }
        }
        
        return false;
    }
    
    /**
     * Perform a long click on the specified node
     */
    public static boolean performLongClick(@Nullable AccessibilityNodeInfo node) {
        if (node == null) {
            return false;
        }
        
        if (node.isLongClickable()) {
            return node.performAction(AccessibilityNodeInfo.ACTION_LONG_CLICK);
        } else {
            // Try to find long-clickable parent
            AccessibilityNodeInfo parent = node.getParent();
            if (parent != null) {
                try {
                    if (parent.isLongClickable()) {
                        return parent.performAction(AccessibilityNodeInfo.ACTION_LONG_CLICK);
                    }
                } finally {
                    parent.recycle();
                }
            }
        }
        
        return false;
    }
    
    /**
     * Set text in an editable node
     */
    public static boolean setText(@Nullable AccessibilityNodeInfo node, @NonNull String text) {
        if (node == null) {
            return false;
        }
        
        if (node.isEditable()) {
            Bundle arguments = new Bundle();
            arguments.putCharSequence(AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE, text);
            return node.performAction(AccessibilityNodeInfo.ACTION_SET_TEXT, arguments);
        }
        
        return false;
    }
    
    /**
     * Perform a scroll forward action on a scrollable node
     */
    public static boolean scrollForward(@Nullable AccessibilityNodeInfo node) {
        if (node == null) {
            return false;
        }
        
        if (node.isScrollable()) {
            return node.performAction(AccessibilityNodeInfo.ACTION_SCROLL_FORWARD);
        } else {
            // Try to find scrollable parent
            AccessibilityNodeInfo parent = node.getParent();
            if (parent != null) {
                try {
                    if (parent.isScrollable()) {
                        return parent.performAction(AccessibilityNodeInfo.ACTION_SCROLL_FORWARD);
                    }
                } finally {
                    parent.recycle();
                }
            }
        }
        
        return false;
    }
    
    /**
     * Perform a scroll backward action on a scrollable node
     */
    public static boolean scrollBackward(@Nullable AccessibilityNodeInfo node) {
        if (node == null) {
            return false;
        }
        
        if (node.isScrollable()) {
            return node.performAction(AccessibilityNodeInfo.ACTION_SCROLL_BACKWARD);
        } else {
            // Try to find scrollable parent
            AccessibilityNodeInfo parent = node.getParent();
            if (parent != null) {
                try {
                    if (parent.isScrollable()) {
                        return parent.performAction(AccessibilityNodeInfo.ACTION_SCROLL_BACKWARD);
                    }
                } finally {
                    parent.recycle();
                }
            }
        }
        
        return false;
    }
    
    /**
     * Perform a tap gesture at the specified coordinates
     */
    @RequiresApi(api = Build.VERSION_CODES.N)
    public static boolean performTap(
            @NonNull AccessibilityService service, float x, float y, long timeout) {
        GestureDescription.Builder builder = new GestureDescription.Builder();
        Path path = new Path();
        path.moveTo(x, y);
        
        builder.addStroke(new GestureDescription.StrokeDescription(path, 0, 10));
        GestureDescription gesture = builder.build();
        
        final CountDownLatch latch = new CountDownLatch(1);
        final AtomicBoolean result = new AtomicBoolean(false);
        
        service.dispatchGesture(gesture, new AccessibilityService.GestureResultCallback() {
            @Override
            public void onCompleted(GestureDescription gestureDescription) {
                result.set(true);
                latch.countDown();
            }
            
            @Override
            public void onCancelled(GestureDescription gestureDescription) {
                result.set(false);
                latch.countDown();
            }
        }, null);
        
        try {
            latch.await(timeout, TimeUnit.MILLISECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return false;
        }
        
        return result.get();
    }
    
    /**
     * Perform a long press gesture at the specified coordinates
     */
    @RequiresApi(api = Build.VERSION_CODES.N)
    public static boolean performLongPress(
            @NonNull AccessibilityService service, float x, float y, long timeout) {
        GestureDescription.Builder builder = new GestureDescription.Builder();
        Path path = new Path();
        path.moveTo(x, y);
        
        // Long press duration (ms)
        long pressDuration = 1000;
        
        builder.addStroke(new GestureDescription.StrokeDescription(path, 0, pressDuration));
        GestureDescription gesture = builder.build();
        
        final CountDownLatch latch = new CountDownLatch(1);
        final AtomicBoolean result = new AtomicBoolean(false);
        
        service.dispatchGesture(gesture, new AccessibilityService.GestureResultCallback() {
            @Override
            public void onCompleted(GestureDescription gestureDescription) {
                result.set(true);
                latch.countDown();
            }
            
            @Override
            public void onCancelled(GestureDescription gestureDescription) {
                result.set(false);
                latch.countDown();
            }
        }, null);
        
        try {
            latch.await(timeout, TimeUnit.MILLISECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return false;
        }
        
        return result.get();
    }
    
    /**
     * Perform a swipe gesture
     */
    @RequiresApi(api = Build.VERSION_CODES.N)
    public static boolean performSwipe(
            @NonNull AccessibilityService service,
            float startX, float startY, float endX, float endY,
            long duration, long timeout) {
        GestureDescription.Builder builder = new GestureDescription.Builder();
        Path path = new Path();
        path.moveTo(startX, startY);
        path.lineTo(endX, endY);
        
        builder.addStroke(new GestureDescription.StrokeDescription(path, 0, duration));
        GestureDescription gesture = builder.build();
        
        final CountDownLatch latch = new CountDownLatch(1);
        final AtomicBoolean result = new AtomicBoolean(false);
        
        service.dispatchGesture(gesture, new AccessibilityService.GestureResultCallback() {
            @Override
            public void onCompleted(GestureDescription gestureDescription) {
                result.set(true);
                latch.countDown();
            }
            
            @Override
            public void onCancelled(GestureDescription gestureDescription) {
                result.set(false);
                latch.countDown();
            }
        }, null);
        
        try {
            latch.await(timeout, TimeUnit.MILLISECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return false;
        }
        
        return result.get();
    }
    
    /**
     * Get the center point of a node's bounds
     */
    @NonNull
    public static Point getNodeCenter(@NonNull AccessibilityNodeInfo node) {
        Rect bounds = new Rect();
        node.getBoundsInScreen(bounds);
        return new Point(bounds.centerX(), bounds.centerY());
    }
    
    /**
     * Get the node description for debugging
     */
    @NonNull
    public static String getNodeDescription(@Nullable AccessibilityNodeInfo node) {
        if (node == null) {
            return "null";
        }
        
        StringBuilder sb = new StringBuilder();
        
        // Get class name
        if (node.getClassName() != null) {
            sb.append("Class: ").append(node.getClassName()).append(", ");
        }
        
        // Get text
        if (node.getText() != null) {
            sb.append("Text: \"").append(node.getText()).append("\", ");
        }
        
        // Get content description
        if (node.getContentDescription() != null) {
            sb.append("Description: \"").append(node.getContentDescription()).append("\", ");
        }
        
        // Get bounds
        Rect bounds = new Rect();
        node.getBoundsInScreen(bounds);
        sb.append("Bounds: ").append(bounds.toShortString()).append(", ");
        
        // Get actions
        sb.append("Actions: ");
        for (AccessibilityNodeInfo.AccessibilityAction action : node.getActionList()) {
            sb.append(action.getId()).append(" ");
        }
        
        return sb.toString();
    }
    
    /**
     * Print node hierarchy for debugging
     */
    public static void printNodeHierarchy(@Nullable AccessibilityNodeInfo node, int depth) {
        if (node == null) {
            return;
        }
        
        StringBuilder indent = new StringBuilder();
        for (int i = 0; i < depth; i++) {
            indent.append("  ");
        }
        
        String description = getNodeDescription(node);
        Log.d(TAG, indent + "Node: " + description);
        
        for (int i = 0; i < node.getChildCount(); i++) {
            AccessibilityNodeInfo child = node.getChild(i);
            if (child != null) {
                printNodeHierarchy(child, depth + 1);
                child.recycle();
            }
        }
    }
}