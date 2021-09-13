package vest.doctor.jersey;


import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Injects a value from {@link jakarta.ws.rs.container.ContainerRequestContext#getProperty(String)}
 * into a request parameter.
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.PARAMETER, ElementType.METHOD, ElementType.FIELD})
public @interface Attribute {
    /**
     * The name of the attribute.
     */
    String value();
}
