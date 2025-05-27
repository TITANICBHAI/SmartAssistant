package java.lang;

import java.lang.reflect.Method;

/**
 * Placeholder Class representation for compatibility purposes.
 */
public final class Class<T> {
    private String name;
    
    private Class(String name) {
        this.name = name;
    }
    
    /**
     * Returns the name of the entity (class, interface, array class,
     * primitive type, or void) represented by this Class object.
     */
    public String getName() {
        return name;
    }
    
    /**
     * Returns the simple name of the underlying class.
     */
    public String getSimpleName() {
        int lastDot = name.lastIndexOf('.');
        if (lastDot < 0) {
            return name;
        }
        return name.substring(lastDot + 1);
    }
    
    /**
     * Returns a Method object that reflects the specified public member method
     * of the class or interface represented by this Class object.
     */
    public Method getMethod(String name, Class<?>... parameterTypes) {
        throw new NoSuchMethodException("Method not found: " + name);
    }
    
    /**
     * Determines if the specified Object is assignment-compatible with the object
     * represented by this Class.
     */
    public boolean isInstance(Object obj) {
        throw new UnsupportedOperationException("isInstance is not supported in this placeholder implementation");
    }
    
    /**
     * Determines if the class or interface represented by this Class object is
     * either the same as, or is a superclass or superinterface of, the class
     * or interface represented by the specified Class parameter.
     */
    public boolean isAssignableFrom(Class<?> cls) {
        throw new UnsupportedOperationException("isAssignableFrom is not supported in this placeholder implementation");
    }
    
    /**
     * Returns true if this Class object represents an interface.
     */
    public boolean isInterface() {
        throw new UnsupportedOperationException("isInterface is not supported in this placeholder implementation");
    }
    
    /**
     * Returns true if this Class object represents an annotation type.
     */
    public boolean isAnnotation() {
        throw new UnsupportedOperationException("isAnnotation is not supported in this placeholder implementation");
    }
    
    /**
     * Returns true if this Class object represents a primitive type.
     */
    public boolean isPrimitive() {
        throw new UnsupportedOperationException("isPrimitive is not supported in this placeholder implementation");
    }
    
    /**
     * Returns true if this Class object represents an array class.
     */
    public boolean isArray() {
        throw new UnsupportedOperationException("isArray is not supported in this placeholder implementation");
    }
    
    /**
     * Returns true if and only if the underlying class is a enum type.
     */
    public boolean isEnum() {
        throw new UnsupportedOperationException("isEnum is not supported in this placeholder implementation");
    }
}