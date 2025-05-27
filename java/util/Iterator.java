package java.util;

/**
 * Placeholder Iterator interface for compatibility purposes.
 */
public interface Iterator<E> {
    boolean hasNext();
    E next();
    default void remove() {
        throw new UnsupportedOperationException("remove");
    }
}