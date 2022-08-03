package vest.doctor.test;

import vest.doctor.conf.CompositeConfigurationFacade;
import vest.doctor.conf.ConfigurationFacade;

import java.util.function.Supplier;

/**
 * Default implementation for {@link TestConfiguration#configurationBuilder()} that uses
 * {@link CompositeConfigurationFacade#defaultConfigurationFacade()} to build the configuration facade.
 */
public class DefaultConfigurationFacadeBuilder implements Supplier<ConfigurationFacade> {
    @Override
    public ConfigurationFacade get() {
        return CompositeConfigurationFacade.defaultConfigurationFacade();
    }
}
