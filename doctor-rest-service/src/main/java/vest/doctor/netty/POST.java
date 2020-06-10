package vest.doctor.netty;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Indicates the route method responds to HTTP POST requests.
 */
@Documented
@HttpMethod("POST")
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.METHOD})
public @interface POST {
}