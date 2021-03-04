package vest.doctor;

import jakarta.inject.Provider;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Indicates that the provider generated from the marked element should be eagerly instantiated. Effectively causing
 * the {@link Provider#get()} method to be called a single time during startup of the application. Makes the most
 * sense for singletons.
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE, ElementType.METHOD})
public @interface Eager {
}
