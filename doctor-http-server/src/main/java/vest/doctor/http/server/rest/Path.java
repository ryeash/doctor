package vest.doctor.http.server.rest;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Mark a class with an HTTP endpoint path.
 * Indicates the root uri paths to use for the method endpoints.
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE})
public @interface Path {
    /**
     * The uri paths for the route.
     */
    String[] value();
}
