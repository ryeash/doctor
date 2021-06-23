package vest.doctor.event;

import vest.doctor.ProviderRegistry;

/**
 * Base class for messages that hold the {@link ProviderRegistry} along with their payload.
 */
public abstract class ProviderRegistryHolder {

    private final ProviderRegistry providerRegistry;

    protected ProviderRegistryHolder(ProviderRegistry providerRegistry) {
        this.providerRegistry = providerRegistry;
    }

    /**
     * Get the {@link ProviderRegistry} associated with the application.
     */
    public ProviderRegistry providerRegistry() {
        return providerRegistry;
    }
}
