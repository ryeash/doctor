package vest.doctor;

import vest.doctor.conf.ConfigurationFacade;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Serves in two capacities:
 * <p>
 * Marks constructor and method parameters to indicate that they should be wired using values from the {@link ConfigurationFacade}
 * associated with the {@link ProviderRegistry}.
 * <p>
 * Marks methods in {@link Properties} interfaces to indicate how to generate the method implementations.
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.METHOD, ElementType.PARAMETER})
public @interface Property {

    /**
     * The name of the property.
     */
    String value();
}
