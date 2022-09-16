package vest.doctor.test;

import vest.doctor.conf.CompositeConfigurationFacade;
import vest.doctor.conf.ConfigurationFacade;
import vest.doctor.conf.SystemPropertiesConfigurationSource;

import java.util.function.Supplier;

public class TestConfigurationFacadeBuilder implements Supplier<ConfigurationFacade> {
    @Override
    public ConfigurationFacade get() {
        return new CompositeConfigurationFacade()
                .addSource(new SystemPropertiesConfigurationSource());
    }
}
