package android.os;

import java.util.concurrent.PriorityBlockingQueue;
import java.util.Comparator;

/**
 * Mock implementation of Android's MessageQueue class.
 * This class manages a queue of Message objects for a Looper.
 */
public final class MessageQueue {
    private final PriorityBlockingQueue<Message> queue;
    
    /**
     * Create a new MessageQueue.
     */
    MessageQueue() {
        this.queue = new PriorityBlockingQueue<>(11, Comparator.comparingLong(o -> o.when));
    }
    
    /**
     * Add a Message to the queue.
     * 
     * @param msg The message to add
     * @return True if the message was added successfully
     */
    boolean enqueueMessage(Message msg) {
        if (msg.target == null) {
            throw new IllegalArgumentException("Message must have a target");
        }
        if (msg.isInUse()) {
            throw new IllegalStateException("Message is already in use");
        }
        
        try {
            queue.put(msg);
            return true;
        } catch (Exception e) {
            return false;
        }
    }
    
    /**
     * Get the next Message from the queue.
     * 
     * @return The next message, or null if no messages are available
     */
    Message next() {
        try {
            long now = SystemClock.uptimeMillis();
            Message msg = queue.peek();
            
            if (msg != null && msg.when <= now) {
                return queue.poll();
            }
            
            return null;
        } catch (Exception e) {
            return null;
        }
    }
    
    /**
     * Remove all messages from the queue that match the specified target and callback.
     * 
     * @param target The target handler
     * @param callback The callback, or null to remove all messages for the target
     */
    void removeCallbacksAndMessages(Handler target, Object callback) {
        if (target == null) {
            return;
        }
        
        // We can't directly remove elements from a PriorityBlockingQueue while iterating,
        // so we create a new queue without the elements we want to remove
        PriorityBlockingQueue<Message> newQueue = new PriorityBlockingQueue<>(11, Comparator.comparingLong(o -> o.when));
        
        for (Message msg : queue) {
            if (msg.target == target && (callback == null || msg.callback == callback)) {
                // This message should be removed, so don't add it to the new queue
                msg.recycle();
            } else {
                // Keep this message
                newQueue.add(msg);
            }
        }
        
        // Clear the existing queue and add all the messages from the new queue
        queue.clear();
        queue.addAll(newQueue);
    }
    
    /**
     * Check if the queue is idle (has no messages).
     * 
     * @return True if the queue is idle, false otherwise
     */
    public boolean isIdle() {
        return queue.isEmpty();
    }
    
    /**
     * Get the number of messages in the queue.
     * 
     * @return The number of messages
     */
    public int size() {
        return queue.size();
    }
}