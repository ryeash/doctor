package vest.doctor.jersey;

import vest.doctor.Import;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Enable the jersey processing feature. When added to a bean factory class,
 * jersey http server classes will be wired together to initialize and execute when configured.
 * <p><br>
 * To use, add to a bean factory configuration class; example:
 * <pre>
 * <literal>@</literal>Configuration
 * <literal>@</literal>JerseyFeature
 * public class AppConfig {
 *  ...
 * }
 * </pre>
 */
@Documented
@Import("vest.doctor.jersey")
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE})
public @interface JerseyFeature {
}
