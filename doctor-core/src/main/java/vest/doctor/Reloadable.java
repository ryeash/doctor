package vest.doctor;

import jakarta.inject.Scope;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Scope annotation indicating that the provider will (lazily) create one instance of the provided type
 * and cache it until a {@link vest.doctor.event.ReloadProviders} event is published to
 * the {@link vest.doctor.event.EventProducer} at which point the instance will be cleared and another
 * will be (lazily) created as necessary.
 */
@Scope
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE, ElementType.METHOD})
public @interface Reloadable {
}
