package vest.doctor;

/**
 * Event sent by the {@link Doctor} instance when all providers have been loaded.
 */
public class ApplicationStartedEvent {

    private final ProviderRegistry providerRegistry;

    ApplicationStartedEvent(ProviderRegistry providerRegistry) {
        this.providerRegistry = providerRegistry;
    }

    /**
     * Get the {@link ProviderRegistry} associated with the application.
     */
    public ProviderRegistry beanProvider() {
        return providerRegistry;
    }
}
