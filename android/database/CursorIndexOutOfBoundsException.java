package android.database;

/**
 * Mock implementation of Android CursorIndexOutOfBoundsException class for development outside of Android.
 * Exception thrown when a Cursor is asked to get a column or position
 * that is outside of its bounds.
 */
public class CursorIndexOutOfBoundsException extends IndexOutOfBoundsException {
    /**
     * Constructs a new CursorIndexOutOfBoundsException with the provided message.
     *
     * @param message The detailed message for this exception.
     */
    public CursorIndexOutOfBoundsException(String message) {
        super(message);
    }
    
    /**
     * Constructs a new CursorIndexOutOfBoundsException with a default message.
     *
     * @param index The invalid index that caused the exception.
     * @param size The size of the cursor.
     */
    public CursorIndexOutOfBoundsException(int index, int size) {
        super("Index " + index + " requested, with a size of " + size);
    }
}