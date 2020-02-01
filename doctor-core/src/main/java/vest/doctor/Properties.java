package vest.doctor;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks an interface as a properties object. An implementation of the interface will be created by the annotation
 * processor and exposed via a provider. The interface must use {@link Property} annotations on it's methods.
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE})
public @interface Properties {
    /**
     * Prefix for all properties
     */
    String value() default "";
}
