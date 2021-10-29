package vest.doctor.test;

import vest.doctor.ConfigurationFacade;
import vest.doctor.runtime.DefaultConfigurationFacade;
import vest.doctor.runtime.SystemPropertiesConfigurationSource;

import java.util.function.Supplier;

public class TestConfigurationFacadeBuilder implements Supplier<ConfigurationFacade> {
    @Override
    public ConfigurationFacade get() {
        return new DefaultConfigurationFacade()
                .addSource(new SystemPropertiesConfigurationSource());
    }
}
