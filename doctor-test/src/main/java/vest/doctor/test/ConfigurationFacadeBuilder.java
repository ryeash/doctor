package vest.doctor.test;

import vest.doctor.ConfigurationFacade;

import java.util.function.Supplier;

/**
 * Creates a {@link ConfigurationFacade} for use in testing.
 */
@FunctionalInterface
public interface ConfigurationFacadeBuilder extends Supplier<ConfigurationFacade> {
}
