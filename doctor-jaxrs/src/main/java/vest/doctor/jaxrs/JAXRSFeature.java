package vest.doctor.jaxrs;

import vest.doctor.Import;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Enable the jax-rs processing feature. When added to a bean factory class,
 * jax-rs http server classes will be wired together to initialize and execute when configured.
 * To use, add to a bean factory configuration class; example:
 * <pre>
 * <literal>@</literal>Configuration
 * <literal>@</literal>JAXRSFeature
 * public class AppConfig {
 *  ...
 * }
 * </pre>
 */
@Documented
@Import("vest.doctor.jaxrs")
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE})
public @interface JAXRSFeature {
}
