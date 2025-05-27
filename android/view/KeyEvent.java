package android.view;

import android.os.Parcel;
import android.os.Parcelable;

/**
 * Mock implementation of Android's KeyEvent class.
 * This class represents keyboard/button press and release events.
 */
public class KeyEvent implements Parcelable {
    /**
     * Key code constant: Unknown key code.
     */
    public static final int KEYCODE_UNKNOWN = 0;
    
    /**
     * Key code constant: Soft Left key.
     */
    public static final int KEYCODE_SOFT_LEFT = 1;
    
    /**
     * Key code constant: Soft Right key.
     */
    public static final int KEYCODE_SOFT_RIGHT = 2;
    
    /**
     * Key code constant: Home key.
     */
    public static final int KEYCODE_HOME = 3;
    
    /**
     * Key code constant: Back key.
     */
    public static final int KEYCODE_BACK = 4;
    
    /**
     * Key code constant: Call key.
     */
    public static final int KEYCODE_CALL = 5;
    
    /**
     * Key code constant: End Call key.
     */
    public static final int KEYCODE_ENDCALL = 6;
    
    /**
     * Key code constant: '0' key.
     */
    public static final int KEYCODE_0 = 7;
    
    /**
     * Key code constant: '1' key.
     */
    public static final int KEYCODE_1 = 8;
    
    /**
     * Key code constant: '2' key.
     */
    public static final int KEYCODE_2 = 9;
    
    /**
     * Key code constant: '3' key.
     */
    public static final int KEYCODE_3 = 10;
    
    /**
     * Key code constant: '4' key.
     */
    public static final int KEYCODE_4 = 11;
    
    /**
     * Key code constant: '5' key.
     */
    public static final int KEYCODE_5 = 12;
    
    /**
     * Key code constant: '6' key.
     */
    public static final int KEYCODE_6 = 13;
    
    /**
     * Key code constant: '7' key.
     */
    public static final int KEYCODE_7 = 14;
    
    /**
     * Key code constant: '8' key.
     */
    public static final int KEYCODE_8 = 15;
    
    /**
     * Key code constant: '9' key.
     */
    public static final int KEYCODE_9 = 16;
    
    /**
     * Key code constant: '*' key.
     */
    public static final int KEYCODE_STAR = 17;
    
    /**
     * Key code constant: '#' key.
     */
    public static final int KEYCODE_POUND = 18;
    
    /**
     * Key code constant: Directional Pad Up key.
     */
    public static final int KEYCODE_DPAD_UP = 19;
    
    /**
     * Key code constant: Directional Pad Down key.
     */
    public static final int KEYCODE_DPAD_DOWN = 20;
    
    /**
     * Key code constant: Directional Pad Left key.
     */
    public static final int KEYCODE_DPAD_LEFT = 21;
    
    /**
     * Key code constant: Directional Pad Right key.
     */
    public static final int KEYCODE_DPAD_RIGHT = 22;
    
    /**
     * Key code constant: Directional Pad Center key.
     */
    public static final int KEYCODE_DPAD_CENTER = 23;
    
    /**
     * Key code constant: Volume Up key.
     */
    public static final int KEYCODE_VOLUME_UP = 24;
    
    /**
     * Key code constant: Volume Down key.
     */
    public static final int KEYCODE_VOLUME_DOWN = 25;
    
    /**
     * Key code constant: Power key.
     */
    public static final int KEYCODE_POWER = 26;
    
    /**
     * Key code constant: Camera key.
     */
    public static final int KEYCODE_CAMERA = 27;
    
    /**
     * Key code constant: Clear key.
     */
    public static final int KEYCODE_CLEAR = 28;
    
    /**
     * Key code constant: 'A' key.
     */
    public static final int KEYCODE_A = 29;
    
    /**
     * Key code constant: 'B' key.
     */
    public static final int KEYCODE_B = 30;
    
    /**
     * Key code constant: 'C' key.
     */
    public static final int KEYCODE_C = 31;
    
    /**
     * Key code constant: 'D' key.
     */
    public static final int KEYCODE_D = 32;
    
    /**
     * Key code constant: 'E' key.
     */
    public static final int KEYCODE_E = 33;
    
    /**
     * Key code constant: 'F' key.
     */
    public static final int KEYCODE_F = 34;
    
    /**
     * Key code constant: 'G' key.
     */
    public static final int KEYCODE_G = 35;
    
    /**
     * Key code constant: 'H' key.
     */
    public static final int KEYCODE_H = 36;
    
    /**
     * Key code constant: 'I' key.
     */
    public static final int KEYCODE_I = 37;
    
    /**
     * Key code constant: 'J' key.
     */
    public static final int KEYCODE_J = 38;
    
    /**
     * Key code constant: 'K' key.
     */
    public static final int KEYCODE_K = 39;
    
    /**
     * Key code constant: 'L' key.
     */
    public static final int KEYCODE_L = 40;
    
    /**
     * Key code constant: 'M' key.
     */
    public static final int KEYCODE_M = 41;
    
    /**
     * Key code constant: 'N' key.
     */
    public static final int KEYCODE_N = 42;
    
    /**
     * Key code constant: 'O' key.
     */
    public static final int KEYCODE_O = 43;
    
    /**
     * Key code constant: 'P' key.
     */
    public static final int KEYCODE_P = 44;
    
    /**
     * Key code constant: 'Q' key.
     */
    public static final int KEYCODE_Q = 45;
    
    /**
     * Key code constant: 'R' key.
     */
    public static final int KEYCODE_R = 46;
    
    /**
     * Key code constant: 'S' key.
     */
    public static final int KEYCODE_S = 47;
    
    /**
     * Key code constant: 'T' key.
     */
    public static final int KEYCODE_T = 48;
    
    /**
     * Key code constant: 'U' key.
     */
    public static final int KEYCODE_U = 49;
    
    /**
     * Key code constant: 'V' key.
     */
    public static final int KEYCODE_V = 50;
    
    /**
     * Key code constant: 'W' key.
     */
    public static final int KEYCODE_W = 51;
    
    /**
     * Key code constant: 'X' key.
     */
    public static final int KEYCODE_X = 52;
    
    /**
     * Key code constant: 'Y' key.
     */
    public static final int KEYCODE_Y = 53;
    
    /**
     * Key code constant: 'Z' key.
     */
    public static final int KEYCODE_Z = 54;
    
    /**
     * Key code constant: ',' key.
     */
    public static final int KEYCODE_COMMA = 55;
    
    /**
     * Key code constant: '.' key.
     */
    public static final int KEYCODE_PERIOD = 56;
    
    /**
     * Key code constant: Left Alt modifier key.
     */
    public static final int KEYCODE_ALT_LEFT = 57;
    
    /**
     * Key code constant: Right Alt modifier key.
     */
    public static final int KEYCODE_ALT_RIGHT = 58;
    
    /**
     * Key code constant: Left Shift modifier key.
     */
    public static final int KEYCODE_SHIFT_LEFT = 59;
    
    /**
     * Key code constant: Right Shift modifier key.
     */
    public static final int KEYCODE_SHIFT_RIGHT = 60;
    
    /**
     * Key code constant: Tab key.
     */
    public static final int KEYCODE_TAB = 61;
    
    /**
     * Key code constant: Space key.
     */
    public static final int KEYCODE_SPACE = 62;
    
    /**
     * Key code constant: Enter key.
     */
    public static final int KEYCODE_ENTER = 66;
    
    /**
     * Key code constant: Delete key.
     */
    public static final int KEYCODE_DEL = 67;
    
    /**
     * Key code constant: Escape key.
     */
    public static final int KEYCODE_ESCAPE = 111;
    
    // Event actions
    
    /**
     * Action: Key down event.
     */
    public static final int ACTION_DOWN = 0;
    
    /**
     * Action: Key up event.
     */
    public static final int ACTION_UP = 1;
    
    /**
     * Action: Multiple key event.
     */
    public static final int ACTION_MULTIPLE = 2;
    
    // Meta state flags
    
    /**
     * Meta state: No meta keys are pressed.
     */
    public static final int META_NONE = 0;
    
    /**
     * Meta state: Shift key is pressed.
     */
    public static final int META_SHIFT_ON = 0x1;
    
    /**
     * Meta state: Alt key is pressed.
     */
    public static final int META_ALT_ON = 0x2;
    
    /**
     * Meta state: Ctrl key is pressed.
     */
    public static final int META_CTRL_ON = 0x1000;
    
    // Properties
    private int mAction;
    private int mKeyCode;
    private int mMetaState;
    private int mScanCode;
    private int mRepeatCount;
    private long mDownTime;
    private long mEventTime;
    
    /**
     * Constructor.
     * 
     * @param action The action code
     * @param keyCode The key code
     */
    public KeyEvent(int action, int keyCode) {
        mAction = action;
        mKeyCode = keyCode;
        mMetaState = META_NONE;
        mScanCode = 0;
        mRepeatCount = 0;
        mDownTime = System.currentTimeMillis();
        mEventTime = mDownTime;
    }
    
    /**
     * Constructor.
     * 
     * @param downTime Time in milliseconds when the key went down
     * @param eventTime Time in milliseconds when the event occurred
     * @param action The action code
     * @param keyCode The key code
     * @param repeatCount Number of repeats for this key event
     */
    public KeyEvent(long downTime, long eventTime, int action, int keyCode, int repeatCount) {
        mAction = action;
        mKeyCode = keyCode;
        mMetaState = META_NONE;
        mScanCode = 0;
        mRepeatCount = repeatCount;
        mDownTime = downTime;
        mEventTime = eventTime;
    }
    
    /**
     * Constructor.
     * 
     * @param downTime Time in milliseconds when the key went down
     * @param eventTime Time in milliseconds when the event occurred
     * @param action The action code
     * @param keyCode The key code
     * @param repeatCount Number of repeats for this key event
     * @param metaState Meta key state
     */
    public KeyEvent(long downTime, long eventTime, int action, int keyCode, int repeatCount, int metaState) {
        mAction = action;
        mKeyCode = keyCode;
        mMetaState = metaState;
        mScanCode = 0;
        mRepeatCount = repeatCount;
        mDownTime = downTime;
        mEventTime = eventTime;
    }
    
    /**
     * Get the key code.
     * 
     * @return The key code
     */
    public int getKeyCode() {
        return mKeyCode;
    }
    
    /**
     * Get the action.
     * 
     * @return The action
     */
    public int getAction() {
        return mAction;
    }
    
    /**
     * Get the meta state.
     * 
     * @return The meta state
     */
    public int getMetaState() {
        return mMetaState;
    }
    
    /**
     * Get the scan code.
     * 
     * @return The scan code
     */
    public int getScanCode() {
        return mScanCode;
    }
    
    /**
     * Get the repeat count.
     * 
     * @return The repeat count
     */
    public int getRepeatCount() {
        return mRepeatCount;
    }
    
    /**
     * Get the time when the key went down.
     * 
     * @return The down time
     */
    public long getDownTime() {
        return mDownTime;
    }
    
    /**
     * Get the time when the event occurred.
     * 
     * @return The event time
     */
    public long getEventTime() {
        return mEventTime;
    }
    
    /**
     * Check if a key code corresponds to a "system" key.
     * 
     * @param keyCode The key code
     * @return Whether the key is a system key
     */
    public static boolean isSystemKey(int keyCode) {
        switch (keyCode) {
            case KEYCODE_HOME:
            case KEYCODE_BACK:
            case KEYCODE_CALL:
            case KEYCODE_ENDCALL:
            case KEYCODE_VOLUME_UP:
            case KEYCODE_VOLUME_DOWN:
            case KEYCODE_POWER:
                return true;
            default:
                return false;
        }
    }
    
    /**
     * Check if this is a navigation key.
     * 
     * @return Whether this is a navigation key
     */
    public final boolean isNavigationKey() {
        switch (mKeyCode) {
            case KEYCODE_DPAD_UP:
            case KEYCODE_DPAD_DOWN:
            case KEYCODE_DPAD_LEFT:
            case KEYCODE_DPAD_RIGHT:
            case KEYCODE_DPAD_CENTER:
                return true;
            default:
                return false;
        }
    }
    
    /**
     * Check if this is a media key.
     * 
     * @return Whether this is a media key
     */
    public final boolean isMediaKey() {
        switch (mKeyCode) {
            case KEYCODE_VOLUME_UP:
            case KEYCODE_VOLUME_DOWN:
                return true;
            default:
                return false;
        }
    }
    
    /**
     * Check if the Shift meta key is pressed.
     * 
     * @return Whether the Shift meta key is pressed
     */
    public final boolean isShiftPressed() {
        return (mMetaState & META_SHIFT_ON) != 0;
    }
    
    /**
     * Check if the Alt meta key is pressed.
     * 
     * @return Whether the Alt meta key is pressed
     */
    public final boolean isAltPressed() {
        return (mMetaState & META_ALT_ON) != 0;
    }
    
    /**
     * Check if the Ctrl meta key is pressed.
     * 
     * @return Whether the Ctrl meta key is pressed
     */
    public final boolean isCtrlPressed() {
        return (mMetaState & META_CTRL_ON) != 0;
    }
    
    /**
     * Get a new KeyEvent with the same properties but a different action.
     * 
     * @param action The new action
     * @return A new KeyEvent
     */
    public KeyEvent changeAction(int action) {
        return new KeyEvent(mDownTime, mEventTime, action, mKeyCode, mRepeatCount, mMetaState);
    }
    
    /**
     * Get a new KeyEvent with the same properties but different flags.
     * 
     * @param metaState The new meta state
     * @return A new KeyEvent
     */
    public KeyEvent withMetaState(int metaState) {
        return new KeyEvent(mDownTime, mEventTime, mAction, mKeyCode, mRepeatCount, metaState);
    }
    
    @Override
    public int describeContents() {
        return 0;
    }
    
    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeInt(mAction);
        dest.writeInt(mKeyCode);
        dest.writeInt(mMetaState);
        dest.writeInt(mScanCode);
        dest.writeInt(mRepeatCount);
        dest.writeLong(mDownTime);
        dest.writeLong(mEventTime);
    }
    
    public static final Parcelable.Creator<KeyEvent> CREATOR = new Parcelable.Creator<KeyEvent>() {
        @Override
        public KeyEvent createFromParcel(Parcel source) {
            int action = source.readInt();
            int keyCode = source.readInt();
            int metaState = source.readInt();
            int scanCode = source.readInt();
            int repeatCount = source.readInt();
            long downTime = source.readLong();
            long eventTime = source.readLong();
            
            KeyEvent event = new KeyEvent(downTime, eventTime, action, keyCode, repeatCount, metaState);
            return event;
        }
        
        @Override
        public KeyEvent[] newArray(int size) {
            return new KeyEvent[size];
        }
    };
}