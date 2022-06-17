package vest.doctor.restful.http;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Meta-annotation to mark HTTP method annotations with their appropriate string value.
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.ANNOTATION_TYPE})
public @interface HttpMethod {

    /**
     * The method name.
     */
    String value();

    /**
     * Marks an endpoint method to indicate the handler responds to HTTP DELETE requests.
     */
    @Documented
    @Retention(RetentionPolicy.RUNTIME)
    @Target({ElementType.METHOD, ElementType.TYPE})
    @HttpMethod("DELETE")
    @interface DELETE {
    }

    /**
     * Marks an endpoint method to indicate the handler responds to HTTP GET requests.
     */
    @Documented
    @Retention(RetentionPolicy.RUNTIME)
    @Target({ElementType.METHOD, ElementType.TYPE})
    @HttpMethod("GET")
    @interface GET {
    }

    /**
     * Marks an endpoint method to indicate the handler responds to HTTP OPTIONS requests.
     */
    @Documented
    @Retention(RetentionPolicy.RUNTIME)
    @Target({ElementType.METHOD, ElementType.TYPE})
    @HttpMethod("OPTIONS")
    @interface OPTIONS {
    }

    /**
     * Marks an endpoint method to indicate the handler responds to HTTP POST requests.
     */
    @Documented
    @Retention(RetentionPolicy.RUNTIME)
    @Target({ElementType.METHOD, ElementType.TYPE})
    @HttpMethod("POST")
    @interface POST {
    }

    /**
     * Marks an endpoint method to indicate the handler responds to HTTP PUT requests.
     */
    @Documented
    @Retention(RetentionPolicy.RUNTIME)
    @Target({ElementType.METHOD, ElementType.TYPE})
    @HttpMethod("PUT")
    @interface PUT {
    }
}
