package vest.doctor.http.server.rest;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Mark a method as an HTTP endpoint.
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE, ElementType.METHOD})
public @interface Endpoint {

    /**
     * The HTTP method that the endpoint responds to.
     */
    String[] method() default {HttpMethod.GET};

    /**
     * The uri path for the endpoint.
     */
    String[] path() default {""};
}
