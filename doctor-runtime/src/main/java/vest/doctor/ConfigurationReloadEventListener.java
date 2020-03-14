package vest.doctor;

public class ConfigurationReloadEventListener implements EventConsumer {

    private final ProviderRegistry providerRegistry;

    public ConfigurationReloadEventListener(ProviderRegistry providerRegistry) {
        this.providerRegistry = providerRegistry;
    }

    @Override
    public boolean isCompatible(Object event) {
        return event instanceof ReloadConfigurationEvent;
    }

    @Override
    public void accept(Object event) {
        providerRegistry.configuration().reload();
    }
}
