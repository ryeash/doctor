package vest.doctor;

import jakarta.inject.Scope;
import vest.doctor.scheduled.Interval;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Scope annotation indicating that the provider should cache the provided instances for a period of time before
 * creating another instance.
 */
@Scope
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE, ElementType.METHOD})
public @interface Cached {

    /**
     * The interval to hold the cached value before refreshing it.
     *
     * @see Interval
     */
    String value();
}
