package vest.doctor.http.server;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Serves in two capacities:
 * - At the class level: sets the root HTTP endpoint path(s) for all endpoint methods in the class
 * - At the method level: sets the HTTP endpoint path suffix(es) for the specific endpoint method
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE, ElementType.METHOD})
public @interface Endpoint {
    /**
     * The uri paths for the route.
     */
    String[] value();
}
