package demo.app;

import vest.doctor.conf.ConfigurationFacade;
import vest.doctor.runtime.CompositeConfigurationFacade;

import java.util.function.Supplier;

public class CustomConfigBuilder implements Supplier<ConfigurationFacade> {
    @Override
    public ConfigurationFacade get() {
        return CompositeConfigurationFacade.defaultConfigurationFacade()
                .addSource(new TCConfigReload());
    }
}
