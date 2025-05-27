package android.accessibilityservice;

import android.os.Parcel;
import android.os.Parcelable;

/**
 * Mock implementation of Android AccessibilityServiceInfo class for development outside of Android.
 * This class describes an AccessibilityService and its capabilities.
 */
public class AccessibilityServiceInfo implements Parcelable {
    /**
     * Denotes spoken feedback.
     */
    public static final int FEEDBACK_SPOKEN = 0x0001;
    
    /**
     * Denotes haptic feedback.
     */
    public static final int FEEDBACK_HAPTIC = 0x0002;
    
    /**
     * Denotes audible feedback.
     */
    public static final int FEEDBACK_AUDIBLE = 0x0004;
    
    /**
     * Denotes visual feedback.
     */
    public static final int FEEDBACK_VISUAL = 0x0008;
    
    /**
     * Denotes generic feedback.
     */
    public static final int FEEDBACK_GENERIC = 0x0010;
    
    /**
     * Denotes all feedback types.
     */
    public static final int FEEDBACK_ALL_MASK = -1;
    
    /**
     * For each type of feedback tells if it's enabled.
     */
    public int feedbackType;
    
    /**
     * Default delay between accessibility events in milliseconds.
     */
    public static final int DEFAULT_EVENT_TYPE_NOTIFICATION_TIMEOUT_MILLIS = 0;
    
    /**
     * The timeout after the most recent event of a given type before notifying the service.
     */
    public long notificationTimeout = DEFAULT_EVENT_TYPE_NOTIFICATION_TIMEOUT_MILLIS;
    
    /**
     * This field specifies the types of events an AccessibilityService will receive.
     */
    public int eventTypes;
    
    /**
     * These are the package names a service is interested in receiving events from.
     */
    public String[] packageNames;
    
    /**
     * Denotes service can retrieve the active window content.
     */
    public static final int FLAG_INCLUDE_NOT_IMPORTANT_VIEWS = 0x0002;
    
    /**
     * Denotes service can request touch exploration mode in which it receives touch events
     * before the target view.
     */
    public static final int FLAG_REQUEST_TOUCH_EXPLORATION_MODE = 0x0004;
    
    /**
     * Denotes service can retrieve the active window content.
     */
    public static final int FLAG_RETRIEVE_ACTIVE_WINDOW_CONTENT = 0x0008;
    
    /**
     * Denotes service wants to filter key events.
     */
    public static final int FLAG_REQUEST_FILTER_KEY_EVENTS = 0x0020;
    
    /**
     * Denotes service to receive events from all interactive windows.
     */
    public static final int FLAG_RETRIEVE_INTERACTIVE_WINDOWS = 0x0040;
    
    /**
     * Flags that are optional for this service.
     */
    public int flags;
    
    /**
     * The component name of the service.
     */
    public String id;
    
    /**
     * The accessible service description.
     */
    public String description;
    
    /**
     * The unique string ID for this service's summary.
     */
    public String summaryResId;
    
    /**
     * The service settings activity name.
     */
    public String settingsActivityName;
    
    /**
     * The unique string ID for this service's settings title.
     */
    public String settingsName;
    
    /**
     * Create a new instance.
     */
    public AccessibilityServiceInfo() {
        // Empty constructor
    }
    
    /**
     * Create a new instance from a Parcel.
     */
    public AccessibilityServiceInfo(Parcel parcel) {
        feedbackType = parcel.readInt();
        notificationTimeout = parcel.readLong();
        eventTypes = parcel.readInt();
        packageNames = parcel.createStringArray();
        flags = parcel.readInt();
        id = parcel.readString();
        description = parcel.readString();
        summaryResId = parcel.readString();
        settingsActivityName = parcel.readString();
        settingsName = parcel.readString();
    }
    
    @Override
    public void writeToParcel(Parcel parcel, int flags) {
        parcel.writeInt(feedbackType);
        parcel.writeLong(notificationTimeout);
        parcel.writeInt(eventTypes);
        parcel.writeStringArray(packageNames);
        parcel.writeInt(this.flags);
        parcel.writeString(id);
        parcel.writeString(description);
        parcel.writeString(summaryResId);
        parcel.writeString(settingsActivityName);
        parcel.writeString(settingsName);
    }
    
    @Override
    public int describeContents() {
        return 0;
    }
    
    public static final Parcelable.Creator<AccessibilityServiceInfo> CREATOR =
            new Parcelable.Creator<AccessibilityServiceInfo>() {
        @Override
        public AccessibilityServiceInfo createFromParcel(Parcel source) {
            return new AccessibilityServiceInfo(source);
        }
        
        @Override
        public AccessibilityServiceInfo[] newArray(int size) {
            return new AccessibilityServiceInfo[size];
        }
    };
}