package vest.doctor;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks a method as a factory for an object. The method will be turned into an instance of
 * {@link DoctorProvider}, and it's return value will be made available for injection elsewhere.
 * The method must have a scope annotation and must return a non-void, non-null value.
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.METHOD})
public @interface Factory {
}
