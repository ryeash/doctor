package vest.doctor.http.server.rest;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Mark a class or method with an HTTP endpoint path.
 * For classes, indicates the root uri paths to use for the method endpoints.
 * <p>
 * For methods, indicates the suffix (prepended with the root paths from the
 * class level Path annotation) for the uri paths to use for the endpoint method.
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE, ElementType.METHOD})
public @interface Path {
    /**
     * The uri paths for the route.
     */
    String[] value();
}
