package vest.doctor.http.server.rest;

import vest.doctor.http.server.Request;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Route parameter annotation the directs the router to pull a value from {@link Request#attribute(String)}.
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.PARAMETER, ElementType.FIELD})
public @interface Attribute {
    /**
     * The name of the attribute.
     */
    String value();
}
