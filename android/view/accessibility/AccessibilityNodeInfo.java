package android.view.accessibility;

import android.os.Bundle;
import android.os.Parcel;
import android.os.Parcelable;
import java.util.ArrayList;
import java.util.List;
import utils.Rect;
import android.view.accessibility.AccessibilityNodeInfoParcelable;

/**
 * Mock implementation of Android's AccessibilityNodeInfo class.
 */
public class AccessibilityNodeInfo implements Parcelable {
    /**
     * Action constant: Focus this node.
     */
    public static final int ACTION_FOCUS = 0x00000001;
    
    /**
     * Action constant: Clear focus from this node.
     */
    public static final int ACTION_CLEAR_FOCUS = 0x00000002;
    
    /**
     * Action constant: Select this node.
     */
    public static final int ACTION_SELECT = 0x00000004;
    
    /**
     * Action constant: Clear selection from this node.
     */
    public static final int ACTION_CLEAR_SELECTION = 0x00000008;
    
    /**
     * Action constant: Click on this node.
     */
    public static final int ACTION_CLICK = 0x00000010;
    
    /**
     * Action constant: Long click on this node.
     */
    public static final int ACTION_LONG_CLICK = 0x00000020;
    
    /**
     * Action constant: Set text.
     */
    public static final int ACTION_SET_TEXT = 0x00200000;
    
    /**
     * The node's parent.
     */
    private AccessibilityNodeInfo mParent;
    
    /**
     * The node's children.
     */
    private List<AccessibilityNodeInfo> mChildren;
    
    /**
     * The node's content description.
     */
    private String mContentDescription;
    
    /**
     * The node's text.
     */
    private String mText;
    
    /**
     * The node's package name.
     */
    private String mPackageName;
    
    /**
     * The node's class name.
     */
    private String mClassName;
    
    /**
     * The node's bounds in parent coordinates.
     */
    private Rect mBoundsInParent;
    
    /**
     * The node's bounds in screen coordinates.
     */
    private Rect mBoundsInScreen;
    
    /**
     * Whether the node is clickable.
     */
    private boolean mClickable;
    
    /**
     * Whether the node is focusable.
     */
    private boolean mFocusable;
    
    /**
     * Whether the node is focused.
     */
    private boolean mFocused;
    
    /**
     * Whether the node is selected.
     */
    private boolean mSelected;
    
    /**
     * Whether the node is scrollable.
     */
    private boolean mScrollable;
    
    /**
     * Whether the node is enabled.
     */
    private boolean mEnabled;
    
    /**
     * Whether the node is password.
     */
    private boolean mPassword;
    
    /**
     * Whether the node is visible to the user.
     */
    private boolean mVisibleToUser;
    
    /**
     * Constructor.
     */
    public AccessibilityNodeInfo() {
        mChildren = new ArrayList<>();
        mBoundsInParent = new Rect();
        mBoundsInScreen = new Rect();
        mEnabled = true;
        mVisibleToUser = true;
    }
    
    /**
     * Get the package name.
     * 
     * @return The package name
     */
    public String getPackageName() {
        return mPackageName;
    }
    
    /**
     * Set the package name.
     * 
     * @param packageName The package name
     */
    public void setPackageName(String packageName) {
        mPackageName = packageName;
    }
    
    /**
     * Get the class name.
     * 
     * @return The class name
     */
    public String getClassName() {
        return mClassName;
    }
    
    /**
     * Set the class name.
     * 
     * @param className The class name
     */
    public void setClassName(String className) {
        mClassName = className;
    }
    
    /**
     * Get the text.
     * 
     * @return The text
     */
    public String getText() {
        return mText;
    }
    
    /**
     * Set the text.
     * 
     * @param text The text
     */
    public void setText(String text) {
        mText = text;
    }
    
    /**
     * Get the content description.
     * 
     * @return The content description
     */
    public String getContentDescription() {
        return mContentDescription;
    }
    
    /**
     * Set the content description.
     * 
     * @param contentDescription The content description
     */
    public void setContentDescription(String contentDescription) {
        mContentDescription = contentDescription;
    }
    
    /**
     * Get the parent.
     * 
     * @return The parent
     */
    public AccessibilityNodeInfo getParent() {
        return mParent;
    }
    
    /**
     * Set the parent.
     * 
     * @param parent The parent
     */
    public void setParent(AccessibilityNodeInfo parent) {
        mParent = parent;
    }
    
    /**
     * Get the bounds in parent coordinates.
     * 
     * @param outBounds The bounds
     */
    public void getBoundsInParent(Rect outBounds) {
        outBounds.set(mBoundsInParent);
    }
    
    /**
     * Set the bounds in parent coordinates.
     * 
     * @param bounds The bounds
     */
    public void setBoundsInParent(Rect bounds) {
        mBoundsInParent.set(bounds);
    }
    
    /**
     * Get the bounds in screen coordinates.
     * 
     * @param outBounds The bounds
     */
    public void getBoundsInScreen(Rect outBounds) {
        outBounds.set(mBoundsInScreen);
    }
    
    /**
     * Set the bounds in screen coordinates.
     * 
     * @param bounds The bounds
     */
    public void setBoundsInScreen(Rect bounds) {
        mBoundsInScreen.set(bounds);
    }
    
    /**
     * Check if the node is checkable.
     * 
     * @return True if checkable
     */
    public boolean isCheckable() {
        return false;
    }
    
    /**
     * Check if the node is checked.
     * 
     * @return True if checked
     */
    public boolean isChecked() {
        return false;
    }
    
    /**
     * Check if the node is focusable.
     * 
     * @return True if focusable
     */
    public boolean isFocusable() {
        return mFocusable;
    }
    
    /**
     * Set whether the node is focusable.
     * 
     * @param focusable True if focusable
     */
    public void setFocusable(boolean focusable) {
        mFocusable = focusable;
    }
    
    /**
     * Check if the node is focused.
     * 
     * @return True if focused
     */
    public boolean isFocused() {
        return mFocused;
    }
    
    /**
     * Set whether the node is focused.
     * 
     * @param focused True if focused
     */
    public void setFocused(boolean focused) {
        mFocused = focused;
    }
    
    /**
     * Check if the node is selected.
     * 
     * @return True if selected
     */
    public boolean isSelected() {
        return mSelected;
    }
    
    /**
     * Set whether the node is selected.
     * 
     * @param selected True if selected
     */
    public void setSelected(boolean selected) {
        mSelected = selected;
    }
    
    /**
     * Check if the node is clickable.
     * 
     * @return True if clickable
     */
    public boolean isClickable() {
        return mClickable;
    }
    
    /**
     * Set whether the node is clickable.
     * 
     * @param clickable True if clickable
     */
    public void setClickable(boolean clickable) {
        mClickable = clickable;
    }
    
    /**
     * Check if the node is long clickable.
     * 
     * @return True if long clickable
     */
    public boolean isLongClickable() {
        return false;
    }
    
    /**
     * Check if the node is enabled.
     * 
     * @return True if enabled
     */
    public boolean isEnabled() {
        return mEnabled;
    }
    
    /**
     * Set whether the node is enabled.
     * 
     * @param enabled True if enabled
     */
    public void setEnabled(boolean enabled) {
        mEnabled = enabled;
    }
    
    /**
     * Check if the node is password.
     * 
     * @return True if password
     */
    public boolean isPassword() {
        return mPassword;
    }
    
    /**
     * Set whether the node is password.
     * 
     * @param password True if password
     */
    public void setPassword(boolean password) {
        mPassword = password;
    }
    
    /**
     * Check if the node is scrollable.
     * 
     * @return True if scrollable
     */
    public boolean isScrollable() {
        return mScrollable;
    }
    
    /**
     * Set whether the node is scrollable.
     * 
     * @param scrollable True if scrollable
     */
    public void setScrollable(boolean scrollable) {
        mScrollable = scrollable;
    }
    
    /**
     * Check if the node is visible to the user.
     * 
     * @return True if visible to the user
     */
    public boolean isVisibleToUser() {
        return mVisibleToUser;
    }
    
    /**
     * Set whether the node is visible to the user.
     * 
     * @param visibleToUser True if visible to the user
     */
    public void setVisibleToUser(boolean visibleToUser) {
        mVisibleToUser = visibleToUser;
    }
    
    /**
     * Add a child.
     * 
     * @param child The child
     */
    public void addChild(AccessibilityNodeInfo child) {
        mChildren.add(child);
    }
    
    /**
     * Get the child count.
     * 
     * @return The child count
     */
    public int getChildCount() {
        return mChildren.size();
    }
    
    /**
     * Get a child.
     * 
     * @param index The index
     * @return The child
     */
    public AccessibilityNodeInfo getChild(int index) {
        return mChildren.get(index);
    }
    
    /**
     * Perform an action.
     * 
     * @param action The action
     * @return True if the action was performed
     */
    public boolean performAction(int action) {
        // In a real implementation, this would actually perform the action
        return true;
    }
    
    /**
     * Perform an action with arguments.
     * 
     * @param action The action
     * @param arguments The arguments
     * @return True if the action was performed
     */
    public boolean performAction(int action, Bundle arguments) {
        // In a real implementation, this would actually perform the action
        return true;
    }
    
    /**
     * Find focus.
     * 
     * @param focus The type of focus
     * @return The node with focus
     */
    public AccessibilityNodeInfo findFocus(int focus) {
        // In a real implementation, this would find the focused node
        return null;
    }
    
    /**
     * Find accessiblity node infos by text.
     * 
     * @param text The text
     * @return The list of matching nodes
     */
    public List<AccessibilityNodeInfo> findAccessibilityNodeInfosByText(String text) {
        List<AccessibilityNodeInfo> result = new ArrayList<>();
        
        if (text == null) {
            return result;
        }
        
        // Check this node
        if ((mText != null && mText.contains(text)) || 
            (mContentDescription != null && mContentDescription.contains(text))) {
            result.add(this);
        }
        
        // Check children
        for (AccessibilityNodeInfo child : mChildren) {
            result.addAll(child.findAccessibilityNodeInfosByText(text));
        }
        
        return result;
    }
    
    /**
     * Create a new instance from a node.
     * 
     * @param node The node
     * @return The new instance
     */
    public static AccessibilityNodeInfo obtain(AccessibilityNodeInfo node) {
        AccessibilityNodeInfo info = new AccessibilityNodeInfo();
        info.mContentDescription = node.mContentDescription;
        info.mText = node.mText;
        info.mPackageName = node.mPackageName;
        info.mClassName = node.mClassName;
        info.mBoundsInParent.set(node.mBoundsInParent);
        info.mBoundsInScreen.set(node.mBoundsInScreen);
        info.mClickable = node.mClickable;
        info.mFocusable = node.mFocusable;
        info.mFocused = node.mFocused;
        info.mSelected = node.mSelected;
        info.mScrollable = node.mScrollable;
        info.mEnabled = node.mEnabled;
        info.mPassword = node.mPassword;
        info.mVisibleToUser = node.mVisibleToUser;
        info.mParent = node.mParent;
        info.mChildren.addAll(node.mChildren);
        
        return info;
    }
    
    /**
     * Create a new instance.
     * 
     * @return The new instance
     */
    public static AccessibilityNodeInfo obtain() {
        return new AccessibilityNodeInfo();
    }
    
    /**
     * Recycle this instance.
     */
    public void recycle() {
        // In a real implementation, this would recycle the instance
    }
    
    /**
     * Describe the contents of this object.
     */
    @Override
    public int describeContents() {
        return 0;
    }
    
    /**
     * Write this object to a parcel.
     */
    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(mContentDescription);
        dest.writeString(mText);
        dest.writeString(mPackageName);
        dest.writeString(mClassName);
        dest.writeInt(mBoundsInParent.left);
        dest.writeInt(mBoundsInParent.top);
        dest.writeInt(mBoundsInParent.right);
        dest.writeInt(mBoundsInParent.bottom);
        dest.writeInt(mBoundsInScreen.left);
        dest.writeInt(mBoundsInScreen.top);
        dest.writeInt(mBoundsInScreen.right);
        dest.writeInt(mBoundsInScreen.bottom);
        dest.writeBoolean(mClickable);
        dest.writeBoolean(mFocusable);
        dest.writeBoolean(mFocused);
        dest.writeBoolean(mSelected);
        dest.writeBoolean(mScrollable);
        dest.writeBoolean(mEnabled);
        dest.writeBoolean(mPassword);
        dest.writeBoolean(mVisibleToUser);
        dest.writeParcelable(mParent != null ? new AccessibilityNodeInfoParcelable(mParent) : null, flags);
        dest.writeInt(mChildren.size());
        for (AccessibilityNodeInfo child : mChildren) {
            dest.writeParcelable(child != null ? new AccessibilityNodeInfoParcelable(child) : null, flags);
        }
    }
    
    /**
     * Creator for AccessibilityNodeInfo.
     */
    public static final Parcelable.Creator<AccessibilityNodeInfo> CREATOR = new Parcelable.Creator<AccessibilityNodeInfo>() {
        @Override
        public AccessibilityNodeInfo createFromParcel(Parcel source) {
            AccessibilityNodeInfo info = new AccessibilityNodeInfo();
            info.mContentDescription = source.readString();
            info.mText = source.readString();
            info.mPackageName = source.readString();
            info.mClassName = source.readString();
            info.mBoundsInParent.left = source.readInt();
            info.mBoundsInParent.top = source.readInt();
            info.mBoundsInParent.right = source.readInt();
            info.mBoundsInParent.bottom = source.readInt();
            info.mBoundsInScreen.left = source.readInt();
            info.mBoundsInScreen.top = source.readInt();
            info.mBoundsInScreen.right = source.readInt();
            info.mBoundsInScreen.bottom = source.readInt();
            info.mClickable = source.readBoolean();
            info.mFocusable = source.readBoolean();
            info.mFocused = source.readBoolean();
            info.mSelected = source.readBoolean();
            info.mScrollable = source.readBoolean();
            info.mEnabled = source.readBoolean();
            info.mPassword = source.readBoolean();
            info.mVisibleToUser = source.readBoolean();
            AccessibilityNodeInfoParcelable parentParcelable = source.readParcelable(AccessibilityNodeInfoParcelable.class.getClassLoader());
            info.mParent = parentParcelable != null ? parentParcelable.getNodeInfo() : null;
            int childCount = source.readInt();
            for (int i = 0; i < childCount; i++) {
                AccessibilityNodeInfoParcelable childParcelable = source.readParcelable(AccessibilityNodeInfoParcelable.class.getClassLoader());
                if (childParcelable != null) {
                    info.mChildren.add(childParcelable.getNodeInfo());
                }
            }
            
            return info;
        }
        
        @Override
        public AccessibilityNodeInfo[] newArray(int size) {
            return new AccessibilityNodeInfo[size];
        }
    };
}