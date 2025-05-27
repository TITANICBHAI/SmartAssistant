package java.util;

/**
 * Placeholder Objects utility class for compatibility purposes.
 */
public final class Objects {
    private Objects() {
        // Prevent instantiation
    }
    
    /**
     * Returns true if the provided reference is null otherwise returns false.
     */
    public static boolean isNull(Object obj) {
        return obj == null;
    }
    
    /**
     * Returns true if the provided reference is non-null otherwise returns false.
     */
    public static boolean nonNull(Object obj) {
        return obj != null;
    }
    
    /**
     * Returns true if the arguments are equal to each other and false otherwise.
     */
    public static boolean equals(Object a, Object b) {
        return (a == b) || (a != null && a.equals(b));
    }
    
    /**
     * Returns the hash code of a non-null argument and 0 for a null argument.
     */
    public static int hashCode(Object o) {
        return o != null ? o.hashCode() : 0;
    }
    
    /**
     * Returns the result of calling toString on the first argument if the first
     * argument is not null and returns the second argument otherwise.
     */
    public static String toString(Object o, String nullDefault) {
        return (o != null) ? o.toString() : nullDefault;
    }
    
    /**
     * Returns the result of calling toString on the argument if the argument is
     * not null and returns "null" otherwise.
     */
    public static String toString(Object o) {
        return toString(o, "null");
    }
}