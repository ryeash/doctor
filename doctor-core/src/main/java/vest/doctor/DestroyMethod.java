package vest.doctor;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Used to indicate that the instances of the provided type have a destroy method that must
 * be called during shutdown.
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE, ElementType.METHOD})
public @interface DestroyMethod {

    /**
     * The name of the method to call to destroy the instances of the provided type.
     */
    String value();
}
