package vest.doctor;

import vest.doctor.processing.NewInstanceCustomizer;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Indicates that the instances produced by the provider generated for the marked class or factory method should
 * not be post processed by any {@link NewInstanceCustomizer}s; e.g. no {@link jakarta.inject.Inject} or
 * {@link vest.doctor.scheduled.Scheduled} methods will be executed for the provided instances.
 * This does not affect the initial construction of the instances.
 * <p>
 * <strong>Note</strong>: This should be used sparingly and may be indicative of architectural issues.
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE, ElementType.METHOD})
public @interface SkipInjection {
}
