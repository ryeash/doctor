package demo.app;

import vest.doctor.conf.CompositeConfigurationFacade;
import vest.doctor.conf.ConfigurationFacade;

import java.util.function.Supplier;

public class CustomConfigBuilder implements Supplier<ConfigurationFacade> {
    @Override
    public ConfigurationFacade get() {
        return CompositeConfigurationFacade.defaultConfigurationFacade()
                .addSource(new TCConfigReload());
    }
}
