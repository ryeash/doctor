package vest.doctor;

public class ApplicationStartedEvent {

    private final ProviderRegistry providerRegistry;

    public ApplicationStartedEvent(ProviderRegistry providerRegistry) {
        this.providerRegistry = providerRegistry;
    }

    public ProviderRegistry beanProvider() {
        return providerRegistry;
    }
}
