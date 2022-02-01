package vest.doctor.reactor.http;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks an endpoint method to indicate the handler responds to HTTP OPTIONS requests.
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.METHOD})
@HttpMethod("OPTIONS")
public @interface OPTIONS {
}
