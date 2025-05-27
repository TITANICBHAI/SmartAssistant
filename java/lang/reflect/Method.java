package java.lang.reflect;

/**
 * Compatibility implementation of the Method class.
 */
public class Method {
    private String name;
    private Class<?> declaringClass;
    private Class<?>[] parameterTypes;
    
    public Method(String name, Class<?> declaringClass, Class<?>[] parameterTypes) {
        this.name = name;
        this.declaringClass = declaringClass;
        this.parameterTypes = parameterTypes;
    }
    
    public String getName() {
        return name;
    }
    
    public Class<?> getDeclaringClass() {
        return declaringClass;
    }
    
    public Class<?>[] getParameterTypes() {
        return parameterTypes;
    }
    
    public Object invoke(Object obj, Object... args) throws InvocationTargetException, IllegalAccessException {
        throw new UnsupportedOperationException("Method invocation not supported in this compatibility implementation");
    }
}
