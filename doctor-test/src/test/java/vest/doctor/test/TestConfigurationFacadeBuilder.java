package vest.doctor.test;

import vest.doctor.conf.ConfigurationFacade;
import vest.doctor.runtime.CompositeConfigurationFacade;
import vest.doctor.runtime.SystemPropertiesConfigurationSource;

import java.util.function.Supplier;

public class TestConfigurationFacadeBuilder implements Supplier<ConfigurationFacade> {
    @Override
    public ConfigurationFacade get() {
        return new CompositeConfigurationFacade()
                .addSource(new SystemPropertiesConfigurationSource());
    }
}
