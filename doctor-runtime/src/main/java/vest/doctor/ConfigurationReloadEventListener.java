package vest.doctor;

import vest.doctor.message.ReloadConfiguration;

public class ConfigurationReloadEventListener implements EventConsumer {

    private final ProviderRegistry providerRegistry;

    public ConfigurationReloadEventListener(ProviderRegistry providerRegistry) {
        this.providerRegistry = providerRegistry;
    }

    @Override
    public boolean isCompatible(Object event) {
        return event instanceof ReloadConfiguration;
    }

    @Override
    public void accept(Object event) {
        providerRegistry.configuration().reload();
    }
}
