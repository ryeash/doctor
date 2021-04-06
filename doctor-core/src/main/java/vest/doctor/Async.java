package vest.doctor;

import jakarta.inject.Inject;
import vest.doctor.event.EventListener;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks a method to be injected asynchronously during processing. Applies to {@link Inject}
 * and {@link EventListener} methods.
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.METHOD})
public @interface Async {

    /**
     * The name of the {@link java.util.concurrent.ExecutorService}
     * that will be used to execute the method.
     */
    String value() default "default";
}
