package vest.doctor.event;

import vest.doctor.ProviderRegistry;

/**
 * Event sent by the doctor instance when shutdown has started.
 */
public final class ApplicationShutdown extends ProviderRegistryHolder {
    public ApplicationShutdown(ProviderRegistry providerRegistry) {
        super(providerRegistry);
    }
}
