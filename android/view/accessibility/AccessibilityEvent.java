package android.view.accessibility;

import android.os.Parcel;
import android.os.Parcelable;
import java.util.ArrayList;
import java.util.List;
import android.view.accessibility.AccessibilityNodeInfoParcelable;

/**
 * Mock implementation of Android's AccessibilityEvent class.
 * This class represents an event that describes a state change in the user interface.
 */
public class AccessibilityEvent implements Parcelable {
    // Event types
    
    /**
     * Event type: Represents the event of clicking on a view.
     */
    public static final int TYPE_VIEW_CLICKED = 0x00000001;
    
    /**
     * Event type: Represents the event of long clicking on a view.
     */
    public static final int TYPE_VIEW_LONG_CLICKED = 0x00000002;
    
    /**
     * Event type: Represents the event of selecting an item.
     */
    public static final int TYPE_VIEW_SELECTED = 0x00000004;
    
    /**
     * Event type: Represents the event of focusing a view.
     */
    public static final int TYPE_VIEW_FOCUSED = 0x00000008;
    
    /**
     * Event type: Represents the event of a view's text changing.
     */
    public static final int TYPE_VIEW_TEXT_CHANGED = 0x00000010;
    
    /**
     * Event type: Represents the event of scrolling a view.
     */
    public static final int TYPE_VIEW_SCROLLED = 0x00000800;
    
    /**
     * Event type: Represents the event of a window state changing.
     */
    public static final int TYPE_WINDOW_STATE_CHANGED = 0x00000020;
    
    /**
     * Event type: Represents the event of a window content changing.
     */
    public static final int TYPE_WINDOW_CONTENT_CHANGED = 0x00000400;
    
    /**
     * Event type: Represents the event of a notification being shown.
     */
    public static final int TYPE_NOTIFICATION_STATE_CHANGED = 0x00000040;
    
    /**
     * Event type: Represents the event of a change in touch exploration mode.
     */
    public static final int TYPE_TOUCH_EXPLORATION_GESTURE_START = 0x00000100;
    
    /**
     * Event type: Represents the event of touch exploration ending.
     */
    public static final int TYPE_TOUCH_EXPLORATION_GESTURE_END = 0x00000200;
    
    /**
     * Event type: Represents the event of gaining accessibility focus.
     */
    public static final int TYPE_VIEW_ACCESSIBILITY_FOCUSED = 0x00008000;
    
    /**
     * Event type: Represents the event of clearing accessibility focus.
     */
    public static final int TYPE_VIEW_ACCESSIBILITY_FOCUS_CLEARED = 0x00010000;
    
    /**
     * Event type: Represents the event of a window showing.
     */
    public static final int TYPE_WINDOWS_CHANGED = 0x00400000;
    
    /**
     * Event type: User interface announcement.
     */
    public static final int TYPE_ANNOUNCEMENT = 0x00004000;
    
    /**
     * Event type: The event of showing a hover enter over a view.
     */
    public static final int TYPE_VIEW_HOVER_ENTER = 0x00000080;
    
    /**
     * Event type: The event of showing a hover exit over a view.
     */
    public static final int TYPE_VIEW_HOVER_EXIT = 0x00000100;
    
    /**
     * Event type: The event of a window being opened.
     */
    public static final int TYPE_WINDOW_ADDED = 0x00000800;
    
    /**
     * Event type: The event of a window being hidden.
     */
    public static final int TYPE_WINDOW_REMOVED = 0x00001000;
    
    /**
     * Represents all the possible event types.
     */
    public static final int TYPES_ALL_MASK = 0xFFFFFFFF;
    
    // Event properties
    private int mEventType;
    private String mPackageName;
    private String mClassName;
    private String mText;
    private String mContentDescription;
    private int mItemCount;
    private int mCurrentItemIndex;
    private int mFromIndex;
    private int mToIndex;
    private long mEventTime;
    private AccessibilityNodeInfo mSource;
    private List<CharSequence> mTextList;
    
    /**
     * Constructor.
     */
    public AccessibilityEvent() {
        mEventType = 0;
        mEventTime = System.currentTimeMillis();
        mTextList = new ArrayList<>();
    }
    
    /**
     * Get the type of the event.
     * 
     * @return The event type
     */
    public int getEventType() {
        return mEventType;
    }
    
    /**
     * Set the type of the event.
     * 
     * @param eventType The event type
     */
    public void setEventType(int eventType) {
        mEventType = eventType;
    }
    
    /**
     * Get the time this event was sent.
     * 
     * @return The event time
     */
    public long getEventTime() {
        return mEventTime;
    }
    
    /**
     * Set the time this event was sent.
     * 
     * @param eventTime The event time
     */
    public void setEventTime(long eventTime) {
        mEventTime = eventTime;
    }
    
    /**
     * Get the package name of the source.
     * 
     * @return The package name
     */
    public String getPackageName() {
        return mPackageName;
    }
    
    /**
     * Set the package name of the source.
     * 
     * @param packageName The package name
     */
    public void setPackageName(String packageName) {
        mPackageName = packageName;
    }
    
    /**
     * Get the class name of the source.
     * 
     * @return The class name
     */
    public String getClassName() {
        return mClassName;
    }
    
    /**
     * Set the class name of the source.
     * 
     * @param className The class name
     */
    public void setClassName(String className) {
        mClassName = className;
    }
    
    /**
     * Get the text of the event.
     * 
     * @return The text
     */
    public String getText() {
        return mText;
    }
    
    /**
     * Set the text of the event.
     * 
     * @param text The text
     */
    public void setText(String text) {
        mText = text;
    }
    
    /**
     * Get the content description of the source.
     * 
     * @return The content description
     */
    public String getContentDescription() {
        return mContentDescription;
    }
    
    /**
     * Set the content description of the source.
     * 
     * @param contentDescription The content description
     */
    public void setContentDescription(String contentDescription) {
        mContentDescription = contentDescription;
    }
    
    /**
     * Get the total number of items.
     * 
     * @return The item count
     */
    public int getItemCount() {
        return mItemCount;
    }
    
    /**
     * Set the total number of items.
     * 
     * @param itemCount The item count
     */
    public void setItemCount(int itemCount) {
        mItemCount = itemCount;
    }
    
    /**
     * Get the index of the current item.
     * 
     * @return The current item index
     */
    public int getCurrentItemIndex() {
        return mCurrentItemIndex;
    }
    
    /**
     * Set the index of the current item.
     * 
     * @param currentItemIndex The current item index
     */
    public void setCurrentItemIndex(int currentItemIndex) {
        mCurrentItemIndex = currentItemIndex;
    }
    
    /**
     * Get the start index of a text selection or content change.
     * 
     * @return The from index
     */
    public int getFromIndex() {
        return mFromIndex;
    }
    
    /**
     * Set the start index of a text selection or content change.
     * 
     * @param fromIndex The from index
     */
    public void setFromIndex(int fromIndex) {
        mFromIndex = fromIndex;
    }
    
    /**
     * Get the end index of a text selection or content change.
     * 
     * @return The to index
     */
    public int getToIndex() {
        return mToIndex;
    }
    
    /**
     * Set the end index of a text selection or content change.
     * 
     * @param toIndex The to index
     */
    public void setToIndex(int toIndex) {
        mToIndex = toIndex;
    }
    
    /**
     * Get the source node.
     * 
     * @return The source
     */
    public AccessibilityNodeInfo getSource() {
        return mSource;
    }
    
    /**
     * Set the source node.
     * 
     * @param source The source
     */
    public void setSource(AccessibilityNodeInfo source) {
        mSource = source;
    }
    
    /**
     * Get the text list.
     * 
     * @return The text list
     */
    public List<CharSequence> getTextList() {
        return mTextList;
    }
    
    /**
     * Add text to the text list.
     * 
     * @param text The text to add
     */
    public void addText(CharSequence text) {
        mTextList.add(text);
    }
    
    /**
     * Create a new instance of AccessibilityEvent.
     * 
     * @param eventType The event type
     * @return A new instance of AccessibilityEvent
     */
    public static AccessibilityEvent obtain(int eventType) {
        AccessibilityEvent event = new AccessibilityEvent();
        event.setEventType(eventType);
        return event;
    }
    
    /**
     * Create a new instance from an existing AccessibilityEvent.
     * 
     * @param event The event to copy
     * @return A new instance of AccessibilityEvent
     */
    public static AccessibilityEvent obtain(AccessibilityEvent event) {
        AccessibilityEvent newEvent = new AccessibilityEvent();
        newEvent.setEventType(event.getEventType());
        newEvent.setEventTime(event.getEventTime());
        newEvent.setPackageName(event.getPackageName());
        newEvent.setClassName(event.getClassName());
        newEvent.setText(event.getText());
        newEvent.setContentDescription(event.getContentDescription());
        newEvent.setItemCount(event.getItemCount());
        newEvent.setCurrentItemIndex(event.getCurrentItemIndex());
        newEvent.setFromIndex(event.getFromIndex());
        newEvent.setToIndex(event.getToIndex());
        newEvent.setSource(event.getSource());
        newEvent.mTextList.addAll(event.mTextList);
        
        return newEvent;
    }
    
    /**
     * Create a new instance of AccessibilityEvent.
     * 
     * @return A new instance of AccessibilityEvent
     */
    public static AccessibilityEvent obtain() {
        return new AccessibilityEvent();
    }
    
    /**
     * Recycle this instance.
     */
    public void recycle() {
        // In a real implementation, this would recycle the instance
    }
    
    @Override
    public int describeContents() {
        return 0;
    }
    
    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeInt(mEventType);
        dest.writeString(mPackageName);
        dest.writeString(mClassName);
        dest.writeString(mText);
        dest.writeString(mContentDescription);
        dest.writeInt(mItemCount);
        dest.writeInt(mCurrentItemIndex);
        dest.writeInt(mFromIndex);
        dest.writeInt(mToIndex);
        dest.writeLong(mEventTime);
        dest.writeParcelable(mSource != null ? new AccessibilityNodeInfoParcelable(mSource) : null, flags);
        dest.writeList(mTextList);
    }
    
    public static final Parcelable.Creator<AccessibilityEvent> CREATOR = new Parcelable.Creator<AccessibilityEvent>() {
        @Override
        public AccessibilityEvent createFromParcel(Parcel source) {
            AccessibilityEvent event = new AccessibilityEvent();
            event.mEventType = source.readInt();
            event.mPackageName = source.readString();
            event.mClassName = source.readString();
            event.mText = source.readString();
            event.mContentDescription = source.readString();
            event.mItemCount = source.readInt();
            event.mCurrentItemIndex = source.readInt();
            event.mFromIndex = source.readInt();
            event.mToIndex = source.readInt();
            event.mEventTime = source.readLong();
            AccessibilityNodeInfoParcelable nodeInfoParcelable = source.readParcelable(AccessibilityNodeInfoParcelable.class.getClassLoader());
            event.mSource = nodeInfoParcelable != null ? nodeInfoParcelable.getNodeInfo() : null;
            source.readList(event.mTextList, CharSequence.class.getClassLoader());
            
            return event;
        }
        
        @Override
        public AccessibilityEvent[] newArray(int size) {
            return new AccessibilityEvent[size];
        }
    };
}