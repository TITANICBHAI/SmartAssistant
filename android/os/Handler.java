package android.os;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Mock implementation of Android Handler class for development outside of Android.
 * A Handler allows you to send and process messages and runnable objects associated with a thread.
 */
public class Handler {
    private static final ExecutorService sExecutor = Executors.newCachedThreadPool();
    private final Looper mLooper;
    
    /**
     * Default constructor associates this handler with the main thread.
     */
    public Handler() {
        mLooper = Looper.getMainLooper();
    }
    
    /**
     * Constructor associate this handler with the specified looper.
     * 
     * @param looper The looper to run messages on.
     */
    public Handler(Looper looper) {
        mLooper = looper;
    }
    
    /**
     * Returns the looper associated with this handler.
     * 
     * @return The looper.
     */
    public Looper getLooper() {
        return mLooper;
    }
    
    /**
     * Causes the Runnable to be added to the message queue.
     * The runnable will be run on the thread to which this handler is attached.
     * 
     * @param r The Runnable that will be executed.
     * @return Returns true if the message was successfully placed in to the message queue.
     */
    public final boolean post(Runnable r) {
        return sendMessageDelayed(getPostMessage(r), 0);
    }
    
    /**
     * Causes the Runnable to be added to the message queue, to be run after the specified amount of time.
     * The runnable will be run on the thread to which this handler is attached.
     * 
     * @param r The Runnable that will be executed.
     * @param delayMillis The delay (in milliseconds) until the Runnable will be executed.
     * @return Returns true if the message was successfully placed in to the message queue.
     */
    public final boolean postDelayed(Runnable r, long delayMillis) {
        return sendMessageDelayed(getPostMessage(r), delayMillis);
    }
    
    /**
     * Remove any pending posts of Runnable r that are in the message queue.
     * 
     * @param r The Runnable to remove from the message queue.
     */
    public final void removeCallbacks(Runnable r) {
        // Not implemented for mock
    }
    
    /**
     * Pushes a message onto the end of the message queue after all pending messages before the current time.
     * 
     * @param msg The message to enqueue.
     * @return Returns true if the message was successfully placed in to the message queue.
     */
    public final boolean sendMessage(Message msg) {
        return sendMessageDelayed(msg, 0);
    }
    
    /**
     * Enqueue a message into the message queue after all pending messages before
     * (current time + delayMillis).
     * 
     * @param msg The message to enqueue.
     * @param delayMillis The delay (in milliseconds) until the message is sent.
     * @return Returns true if the message was successfully placed in to the message queue.
     */
    public final boolean sendMessageDelayed(Message msg, long delayMillis) {
        if (delayMillis < 0) {
            delayMillis = 0;
        }
        
        // Create final copies for use in lambda
        final long finalDelayMillis = delayMillis;
        final Message finalMsg = msg;
        
        // Use an executor service to mimic delayed message delivery
        sExecutor.submit(() -> {
            if (finalDelayMillis > 0) {
                try {
                    Thread.sleep(finalDelayMillis);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    return false;
                }
            }
            handleMessage(finalMsg);
            return true;
        });
        
        return true;
    }
    
    /**
     * Remove any pending posts of messages with callback and target
     * whose obj is null.
     * 
     * @param what The identifier of the message to remove.
     */
    public final void removeMessages(int what) {
        // Not implemented for mock
    }
    
    /**
     * Handle system messages here.
     * 
     * @param msg The message to handle.
     */
    public void handleMessage(Message msg) {
        // Default implementation does nothing
    }
    
    /**
     * Obtain a message object from the global pool.
     * 
     * @return A Message object from the global pool.
     */
    public static Message obtainMessage() {
        return Message.obtain();
    }
    
    // Method removed to avoid duplication
    // The obtainMessage() method is already defined elsewhere in the class
    
    /**
     * Create a new Message to be sent to this handler, with a custom what value.
     * 
     * @param what The custom what value.
     * @return A Message object.
     */
    public final Message obtainMessage(int what) {
        return Message.obtain(this, what);
    }
    
    /**
     * Create a new Message to be sent to this handler, with a custom what and obj value.
     * 
     * @param what The custom what value.
     * @param obj The custom obj value.
     * @return A Message object.
     */
    public final Message obtainMessage(int what, Object obj) {
        return Message.obtain(this, what, obj);
    }
    
    /**
     * Create a new Message to be sent to this handler, with custom what, arg1, and arg2 values.
     * 
     * @param what The custom what value.
     * @param arg1 The custom arg1 value.
     * @param arg2 The custom arg2 value.
     * @return A Message object.
     */
    public final Message obtainMessage(int what, int arg1, int arg2) {
        return Message.obtain(this, what, arg1, arg2);
    }
    
    /**
     * Create a new Message to be sent to this handler, with custom what, arg1, arg2, and obj values.
     * 
     * @param what The custom what value.
     * @param arg1 The custom arg1 value.
     * @param arg2 The custom arg2 value.
     * @param obj The custom obj value.
     * @return A Message object.
     */
    public final Message obtainMessage(int what, int arg1, int arg2, Object obj) {
        return Message.obtain(this, what, arg1, arg2, obj);
    }
    
    /**
     * Create and return a Message object from a runnable.
     */
    private static Message getPostMessage(Runnable r) {
        Message m = Message.obtain();
        m.callback = r;
        return m;
    }
    
    /**
     * Mock implementation of Message for testing.
     */
    public static class Message {
        /**
         * User-defined message code so that the recipient can identify what this message is about.
         */
        public int what;
        
        /**
         * arg1 and arg2 are lower-cost alternatives to using a Bundle if you
         * only need to store a few integer values.
         */
        public int arg1;
        
        /**
         * arg1 and arg2 are lower-cost alternatives to using a Bundle if you
         * only need to store a few integer values.
         */
        public int arg2;
        
        /**
         * An arbitrary object to send to the recipient.
         */
        public Object obj;
        
        /**
         * Optional Runnable to execute when the message is handled.
         */
        public Runnable callback;
        
        /**
         * The target which will receive the message.
         */
        private Handler target;
        
        /**
         * Create a new Message instance.
         */
        private Message() {
        }
        
        /**
         * Return a Message instance to the global pool.
         * <p>
         * You MUST NOT touch the Message after calling this function because it has
         * effectively been freed.
         * </p>
         */
        public void recycle() {
            what = 0;
            arg1 = 0;
            arg2 = 0;
            obj = null;
            callback = null;
            target = null;
        }
        
        /**
         * Make this message like o.
         */
        public void copyFrom(Message o) {
            this.what = o.what;
            this.arg1 = o.arg1;
            this.arg2 = o.arg2;
            this.obj = o.obj;
            this.callback = o.callback;
            this.target = o.target;
        }
        
        /**
         * Return a new Message instance.
         */
        public static Message obtain() {
            return new Message();
        }
        
        /**
         * Return a new Message instance with a target set.
         */
        public static Message obtain(Handler h) {
            Message m = obtain();
            m.target = h;
            return m;
        }
        
        /**
         * Return a new Message instance with a target and what set.
         */
        public static Message obtain(Handler h, int what) {
            Message m = obtain(h);
            m.what = what;
            return m;
        }
        
        /**
         * Return a new Message instance with a target, what, and obj set.
         */
        public static Message obtain(Handler h, int what, Object obj) {
            Message m = obtain(h, what);
            m.obj = obj;
            return m;
        }
        
        /**
         * Return a new Message instance with a target, what, arg1, and arg2 set.
         */
        public static Message obtain(Handler h, int what, int arg1, int arg2) {
            Message m = obtain(h, what);
            m.arg1 = arg1;
            m.arg2 = arg2;
            return m;
        }
        
        /**
         * Return a new Message instance with a target, what, arg1, arg2, and obj set.
         */
        public static Message obtain(Handler h, int what, int arg1, int arg2, Object obj) {
            Message m = obtain(h, what, arg1, arg2);
            m.obj = obj;
            return m;
        }
        
        /**
         * Return a new Message instance from an existing Message.
         */
        public static Message obtain(Message orig) {
            Message m = obtain();
            m.copyFrom(orig);
            return m;
        }
    }
}