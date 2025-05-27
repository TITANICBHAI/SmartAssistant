package java.lang.reflect;

/**
 * Compatibility implementation of InvocationTargetException.
 */
public class InvocationTargetException extends Exception {
    private Throwable targetException;
    
    public InvocationTargetException(Throwable target) {
        super((target == null ? null : target.getMessage()));
        this.targetException = target;
    }
    
    public InvocationTargetException(Throwable target, String message) {
        super(message);
        this.targetException = target;
    }
    
    public Throwable getTargetException() {
        return targetException;
    }
    
    public Throwable getCause() {
        return targetException;
    }
}
