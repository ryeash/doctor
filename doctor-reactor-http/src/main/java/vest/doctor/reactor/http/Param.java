package vest.doctor.reactor.http;

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
@Target({ElementType.ANNOTATION_TYPE})
public @interface Param {

    /**
     * Parameter value will be pulled from {@link HttpRequest#pathParam(String)}.
     */
    @Documented
    @Retention(RetentionPolicy.RUNTIME)
    @Target({ElementType.PARAMETER, ElementType.METHOD, ElementType.FIELD})
    @Param
    @interface Path {
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
}
