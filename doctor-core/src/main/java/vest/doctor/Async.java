package vest.doctor;

import vest.doctor.event.EventListener;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks a method to be injected asynchronously during processing. Applies to {@link jakarta.inject.Inject}
 * and {@link EventListener} methods.
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.METHOD})
public @interface Async {
}
