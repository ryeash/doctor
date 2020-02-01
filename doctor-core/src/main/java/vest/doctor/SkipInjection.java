package vest.doctor;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Indicates that the instances produced by the provider generated for the marked class or factory method should
 * not be post processed by any {@link NewInstanceCustomizer}s; i.e. no @Inject or @Scheduled methods will be
 * executed for the provided instances. This does not affect the initial construction of the instances.
 * <p>
 * Note: This should be used sparingly as it may be indicative of poor application architecture.
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE, ElementType.METHOD})
public @interface SkipInjection {
}
