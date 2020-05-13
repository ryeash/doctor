package vest.doctor.event;

import vest.doctor.ProviderRegistry;

/**
 * Event sent by the doctor instance when all providers have been loaded.
 */
public final class ApplicationStarted extends ProviderRegistryHolder {

    public ApplicationStarted(ProviderRegistry providerRegistry) {
        super(providerRegistry);
    }
}
