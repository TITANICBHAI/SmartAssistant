package java.util.function;

/**
 * Placeholder Predicate interface for compatibility purposes.
 */
public interface Predicate<T> {
    boolean test(T t);
    
    default Predicate<T> and(Predicate<? super T> other) {
        return (T t) -> test(t) && other.test(t);
    }
    
    default Predicate<T> negate() {
        return (T t) -> !test(t);
    }
    
    default Predicate<T> or(Predicate<? super T> other) {
        return (T t) -> test(t) || other.test(t);
    }
    
    static <T> Predicate<T> isEqual(Object targetRef) {
        return (null == targetRef)
                ? Objects::isNull
                : object -> targetRef.equals(object);
    }
}