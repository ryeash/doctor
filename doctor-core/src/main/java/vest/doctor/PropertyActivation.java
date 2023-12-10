package vest.doctor;

import vest.doctor.runtime.PropertyActivationPredicate;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Repeatable;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.RetentionPolicy.RUNTIME;

/**
 * Annotates a provider with a property activation. The property
 * must exist and equal the value in order for the provider to be
 * activated.
 *
 * @see PropertyActivationPredicate
 */
@Documented
@Retention(RUNTIME)
@Target({ElementType.TYPE, ElementType.METHOD})
@Repeatable(PropertyActivation.PropertyActivations.class)
@Activation(PropertyActivationPredicate.class)
public @interface PropertyActivation {

    /**
     * The name of the property.
     */
    String name();

    /**
     * The value of the property.
     */
    String value();

    @Documented
    @Retention(RUNTIME)
    @Target({ElementType.TYPE, ElementType.METHOD})
    @interface PropertyActivations {
        PropertyActivation[] value();
    }
}
