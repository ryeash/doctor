package vest.doctor.test;

import vest.doctor.ConfigurationFacade;
import vest.doctor.runtime.DefaultConfigurationFacade;

import java.util.function.Supplier;

/**
 * Default implementation for {@link TestConfiguration#configurationBuilder()} that uses
 * {@link DefaultConfigurationFacade#defaultConfigurationFacade()} to build the configuration facade.
 */
public class DefaultConfigurationFacadeBuilder implements Supplier<ConfigurationFacade> {
    @Override
    public ConfigurationFacade get() {
        return DefaultConfigurationFacade.defaultConfigurationFacade();
    }
}
