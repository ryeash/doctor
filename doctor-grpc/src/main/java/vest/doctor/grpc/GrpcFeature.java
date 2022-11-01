package vest.doctor.grpc;

import vest.doctor.Import;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Enable the gRPC processing feature. When added to a bean factory class,
 * gRPC server classes will be wired together to initialize and execute when configured.
 * <p><br>
 * To use, add to a bean factory configuration class; example:
 * <pre>
 * <literal>@</literal>Configuration
 * <literal>@</literal>GrpcFeature
 * public class AppConfig {
 *  ...
 * }
 * </pre>
 */
@Documented
@Import("vest.doctor.grpc")
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE})
public @interface GrpcFeature {
}
