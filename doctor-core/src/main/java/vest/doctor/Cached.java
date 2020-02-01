package vest.doctor;

import javax.inject.Scope;
import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.concurrent.TimeUnit;

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
     * The duration to cache returned instances.
     */
    long ttl();

    /**
     * The unit for the duration time.
     */
    TimeUnit unit() default TimeUnit.MILLISECONDS;
}
