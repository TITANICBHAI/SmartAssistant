package android.os;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

/**
 * Mock implementation of Android Message class for development outside of Android.
 * Defines a message containing a description and arbitrary data object that can be
 * sent to a {@link Handler}.
 */
public final class Message implements Parcelable {
    /**
     * User-defined message code so that the recipient can identify
     * what this message is about.
     */
    public int what;
    
    /**
     * Arbitrary value associated with this message.
     */
    public int arg1;
    
    /**
     * Arbitrary value associated with this message.
     */
    public int arg2;
    
    /**
     * An arbitrary object to send to the recipient.
     */
    public Object obj;
    
    /**
     * Callback interface for handling messages.
     */
    public Runnable callback;
    
    /**
     * The target handler for this message.
     */
    Handler target;
    
    /**
     * Private constructor to disable construction of Messages except via the obtain methods.
     */
    private Message() {
        // Do nothing
    }
    
    /**
     * Return a new Message instance from the global pool.
     * 
     * @return A new Message instance.
     */
    @NonNull
    public static Message obtain() {
        return new Message();
    }
    
    /**
     * Return a new Message instance from the global pool, copying values from original.
     * 
     * @param original The original Message.
     * @return A new Message instance.
     */
    @NonNull
    public static Message obtain(@NonNull Message original) {
        Message msg = obtain();
        msg.what = original.what;
        msg.arg1 = original.arg1;
        msg.arg2 = original.arg2;
        msg.obj = original.obj;
        msg.target = original.target;
        msg.callback = original.callback;
        return msg;
    }
    
    /**
     * Return a new Message instance from the global pool, initialized with the given parameters.
     * 
     * @param h The target handler for the message.
     * @return A new Message instance.
     */
    @NonNull
    public static Message obtain(@NonNull Handler h) {
        Message msg = obtain();
        msg.target = h;
        return msg;
    }
    
    /**
     * Return a new Message instance from the global pool, initialized with the given parameters.
     * 
     * @param h The target handler for the message.
     * @param what The message's what value.
     * @return A new Message instance.
     */
    @NonNull
    public static Message obtain(@NonNull Handler h, int what) {
        Message msg = obtain();
        msg.target = h;
        msg.what = what;
        return msg;
    }
    
    /**
     * Return a new Message instance from the global pool, initialized with the given parameters.
     * 
     * @param h The target handler for the message.
     * @param what The message's what value.
     * @param obj The message's obj value.
     * @return A new Message instance.
     */
    @NonNull
    public static Message obtain(@NonNull Handler h, int what, @Nullable Object obj) {
        Message msg = obtain();
        msg.target = h;
        msg.what = what;
        msg.obj = obj;
        return msg;
    }
    
    /**
     * Return a new Message instance from the global pool, initialized with the given parameters.
     * 
     * @param h The target handler for the message.
     * @param what The message's what value.
     * @param arg1 The message's arg1 value.
     * @param arg2 The message's arg2 value.
     * @return A new Message instance.
     */
    @NonNull
    public static Message obtain(@NonNull Handler h, int what, int arg1, int arg2) {
        Message msg = obtain();
        msg.target = h;
        msg.what = what;
        msg.arg1 = arg1;
        msg.arg2 = arg2;
        return msg;
    }
    
    /**
     * Return a new Message instance from the global pool, initialized with the given parameters.
     * 
     * @param h The target handler for the message.
     * @param what The message's what value.
     * @param arg1 The message's arg1 value.
     * @param arg2 The message's arg2 value.
     * @param obj The message's obj value.
     * @return A new Message instance.
     */
    @NonNull
    public static Message obtain(@NonNull Handler h, int what, int arg1, int arg2, @Nullable Object obj) {
        Message msg = obtain();
        msg.target = h;
        msg.what = what;
        msg.arg1 = arg1;
        msg.arg2 = arg2;
        msg.obj = obj;
        return msg;
    }
    
    /**
     * Return a String representation of the message.
     * 
     * @return A String representation of the message.
     */
    @NonNull
    @Override
    public String toString() {
        return "Message { what=" + what + " arg1=" + arg1 + " arg2=" + arg2 + " }";
    }
    
    /**
     * Recycle this Message instance. Use this function instead of creating a new
     * Message to obtain a fresh instance.
     * 
     * Recycles the message by returning it to the message pool.
     */
    public void recycle() {
        // Mock implementation, does nothing
    }
    
    /**
     * Make this message like another. Performs a shallow copy of the data field.
     * Does not copy the linked list fields, nor the timestamp or target/callback of the
     * original message.
     * 
     * @param o The message to copy from.
     */
    public void copyFrom(@NonNull Message o) {
        this.what = o.what;
        this.arg1 = o.arg1;
        this.arg2 = o.arg2;
        this.obj = o.obj;
    }
    
    /**
     * Send this message to the target it was created with.
     */
    public void sendToTarget() {
        if (target == null) {
            throw new IllegalStateException("Message has no target");
        }
        target.sendMessage(this);
    }
    
    /**
     * Describe the kinds of special objects contained in this Parcelable
     * instance's marshaled representation.
     * 
     * @return A bitmask indicating the set of special object types marshaled by this Parcelable.
     */
    @Override
    public int describeContents() {
        return 0;
    }
    
    /**
     * Flatten this object in to a Parcel.
     * 
     * @param dest The Parcel in which the object should be written.
     * @param flags Additional flags about how the object should be written.
     */
    @Override
    public void writeToParcel(@NonNull Parcel dest, int flags) {
        // Mock implementation, does nothing
    }
    
    /**
     * Creator for Message Parcelable objects.
     */
    public static final Creator<Message> CREATOR = new Creator<Message>() {
        @Override
        @NonNull
        public Message createFromParcel(Parcel source) {
            return new Message();
        }
        
        @Override
        @NonNull
        public Message[] newArray(int size) {
            return new Message[size];
        }
    };
}