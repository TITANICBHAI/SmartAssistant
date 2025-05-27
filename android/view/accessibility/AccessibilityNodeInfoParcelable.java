package android.view.accessibility;

import android.os.Parcel;
import android.os.Parcelable;

/**
 * A Parcelable wrapper for AccessibilityNodeInfo objects
 */
public class AccessibilityNodeInfoParcelable implements Parcelable {
    private AccessibilityNodeInfo mNodeInfo;
    
    /**
     * Constructor
     * @param nodeInfo The AccessibilityNodeInfo to wrap
     */
    public AccessibilityNodeInfoParcelable(AccessibilityNodeInfo nodeInfo) {
        mNodeInfo = nodeInfo;
    }
    
    /**
     * Get the wrapped AccessibilityNodeInfo
     * @return The AccessibilityNodeInfo object
     */
    public AccessibilityNodeInfo getNodeInfo() {
        return mNodeInfo;
    }
    
    @Override
    public int describeContents() {
        return 0;
    }
    
    @Override
    public void writeToParcel(Parcel dest, int flags) {
        // In a real implementation, we would serialize the nodeInfo
        // For now we just serialize a placeholder
        dest.writeString("NODEINFO_PLACEHOLDER");
    }
    
    /**
     * Creator for Parcelable
     */
    public static final Parcelable.Creator<AccessibilityNodeInfoParcelable> CREATOR = 
        new Parcelable.Creator<AccessibilityNodeInfoParcelable>() {
            @Override
            public AccessibilityNodeInfoParcelable createFromParcel(Parcel source) {
                // Read the placeholder string
                source.readString();
                // Return a new AccessibilityNodeInfo
                return new AccessibilityNodeInfoParcelable(new AccessibilityNodeInfo());
            }
            
            @Override
            public AccessibilityNodeInfoParcelable[] newArray(int size) {
                return new AccessibilityNodeInfoParcelable[size];
            }
        };
}