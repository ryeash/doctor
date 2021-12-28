package vest.doctor.http.server.rest;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotates endpoint parameters to indicate from which part of the HTTP request to get the value.
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.PARAMETER, ElementType.FIELD, ElementType.METHOD})
public @interface Param {

    enum Type {
        /**
         * The parameter value will be pulled from the path matching variables.
         */
        Path,
        /**
         * The parameter value will be pulled from URI query parameters.
         */
        Query,
        /**
         * The parameter value will be pulled from a header value.
         */
        Header,
        /**
         * The parameter value will be pulled from a cookie value.
         */
        Cookie,
        /**
         * The parameter value will be converted from the body bytes.
         */
        Body,
        /**
         * The parameter value will be built as a bean parameter.
         */
        Bean,
        /**
         * The parameter value will be pulled from the {@link vest.doctor.ProviderRegistry} that started
         * the server environment.
         */
        Provided,
        /**
         * The parameter value will be pulled from the request attributes using {@link vest.doctor.http.server.Request#attribute(String)}.
         */
        Attribute
    }

    /**
     * The type of the parameter.
     */
    Type type();

    /**
     * The name to use to retrieve the value of the parameter. If left default the parameter name
     * as it appears in code will be used.
     */
    String name() default "";
}
