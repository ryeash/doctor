package vest.doctor;

import jakarta.inject.Scope;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Scope annotation indicating that the provider will create a new instance
 * when a {@link vest.doctor.event.ReloadProviders} event is published to
 * the {@link vest.doctor.event.EventProducer}.
 */
@Scope
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE, ElementType.METHOD})
public @interface Reloadable {
}
