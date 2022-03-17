package vest.doctor.reactor.http;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Meta annotation that marks an endpoint parameter annotation.
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.ANNOTATION_TYPE})
public @interface Param {

    /**
     * HTTP endpoint parameter annotation.
     * Parameter value will be pulled from {@link HttpRequest#pathParam(String)}.
     */
    @Documented
    @Retention(RetentionPolicy.RUNTIME)
    @Target({ElementType.PARAMETER, ElementType.METHOD, ElementType.FIELD})
    @Param
    @interface Path {
        /**
         * The name of the path parameter, e.g. for the endpoint path "/app/v1/{type}"
         * the string "type" can be used as the path parameter name.
         */
        String value() default "";
    }

    /**
     * Parameter value will be pulled from {@link HttpRequest#queryParam(String)}.
     */
    @Documented
    @Retention(RetentionPolicy.RUNTIME)
    @Target({ElementType.PARAMETER, ElementType.METHOD, ElementType.FIELD})
    @Param
    @interface Query {
        /**
         * The name of the query parameter.
         */
        String value() default "";
    }

    /**
     * Parameter value will be pulled from {@link HttpRequest#header(CharSequence)}.
     */
    @Documented
    @Retention(RetentionPolicy.RUNTIME)
    @Target({ElementType.PARAMETER, ElementType.METHOD, ElementType.FIELD})
    @Param
    @interface Header {
        /**
         * The name of the header.
         */
        String value() default "";
    }

    /**
     * Parameter value will be pulled from {@link HttpRequest#cookie(String)}.
     */
    @Documented
    @Retention(RetentionPolicy.RUNTIME)
    @Target({ElementType.PARAMETER, ElementType.METHOD, ElementType.FIELD})
    @Param
    @interface Cookie {
        /**
         * The name of the cookie.
         */
        String value() default "";
    }

    /**
     * Parameter value will be created from an annotated java bean. Bean parameters can use
     * any parameter annotations to mark methods, constructors, and fields to indicate
     * how to create and initialize the bean.
     */
    @Documented
    @Retention(RetentionPolicy.RUNTIME)
    @Target({ElementType.PARAMETER, ElementType.METHOD, ElementType.FIELD})
    @Param
    @interface Bean {
    }

    /**
     * Parameter value will be pulled from the {@link vest.doctor.ProviderRegistry}.
     */
    @Documented
    @Retention(RetentionPolicy.RUNTIME)
    @Target({ElementType.PARAMETER, ElementType.METHOD, ElementType.FIELD})
    @Param
    @interface Provided {
    }

    /**
     * Parameter value will be pulled from {@link RequestContext#attribute(String)}.
     */
    @Documented
    @Retention(RetentionPolicy.RUNTIME)
    @Target({ElementType.PARAMETER, ElementType.METHOD, ElementType.FIELD})
    @Param
    @interface Attribute {
        /**
         * The name of the attribute.
         */
        String value() default "";
    }

    /**
     * Parameter value will be created from the request body data using one of the available
     * {@link BodyReader body readers}.
     */
    @Documented
    @Retention(RetentionPolicy.RUNTIME)
    @Target({ElementType.PARAMETER, ElementType.METHOD, ElementType.FIELD})
    @Param
    @interface Body {
    }

    /**
     * Parameter value will be one of the supported context values. One of:
     * <ul>
     *     <li>{@link RequestContext}</li>
     *     <li>{@link HttpRequest}</li>
     *     <li>{@link HttpResponse}</li>
     *     <li>{@link java.net.URI}</li>
     * </ul>
     */
    @Documented
    @Retention(RetentionPolicy.RUNTIME)
    @Target({ElementType.PARAMETER, ElementType.METHOD, ElementType.FIELD})
    @Param
    @interface Context {
    }
}
