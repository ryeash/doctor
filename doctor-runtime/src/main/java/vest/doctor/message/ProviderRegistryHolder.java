package vest.doctor.message;

import vest.doctor.ProviderRegistry;

public abstract class ProviderRegistryHolder {

    protected final ProviderRegistry providerRegistry;

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
