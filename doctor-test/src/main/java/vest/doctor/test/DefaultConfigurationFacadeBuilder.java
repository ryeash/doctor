package vest.doctor.test;

import vest.doctor.ConfigurationFacade;
import vest.doctor.runtime.DefaultConfigurationFacade;

/**
 * Default implementation of {@link ConfigurationFacadeBuilder} that uses
 * {@link DefaultConfigurationFacade#defaultConfigurationFacade()} to build the configuration facade.
 */
public class DefaultConfigurationFacadeBuilder implements ConfigurationFacadeBuilder {
    @Override
    public ConfigurationFacade get() {
        return DefaultConfigurationFacade.defaultConfigurationFacade();
    }
}
