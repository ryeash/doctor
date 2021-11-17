package demo.app;

import vest.doctor.ConfigurationFacade;
import vest.doctor.runtime.DefaultConfigurationFacade;

import java.util.function.Supplier;

public class CustomConfigBuilder implements Supplier<ConfigurationFacade> {
    @Override
    public ConfigurationFacade get() {
        return DefaultConfigurationFacade.defaultConfigurationFacade()
                .addSource(new TCConfigReload());
    }
}
