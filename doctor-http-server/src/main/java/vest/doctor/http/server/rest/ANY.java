package vest.doctor.http.server.rest;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Indicates the route method responds to any HTTP method name.
 */
@Documented
@HttpMethod("_ANY_")
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.METHOD})
public @interface ANY {
}
