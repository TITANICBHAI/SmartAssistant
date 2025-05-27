package utils;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.lang.annotation.ElementType;

/**
 * Denotes that a parameter, field or method return value can never be null.
 * This is a mock of the androidx.annotation.NonNull annotation to avoid
 * having to include the Android Support Library.
 */
@Documented
@Retention(RetentionPolicy.CLASS)
@Target({ElementType.METHOD, ElementType.PARAMETER, ElementType.FIELD, ElementType.LOCAL_VARIABLE, ElementType.ANNOTATION_TYPE, ElementType.PACKAGE})
public @interface NonNull {
    /**
     * Optional message explaining why null is not allowed
     */
    String value() default "";
}