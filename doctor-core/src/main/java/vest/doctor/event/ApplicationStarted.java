package vest.doctor.event;

import vest.doctor.ApplicationLoader;
import vest.doctor.ProviderRegistry;

/**
 * Event published when all {@link ApplicationLoader ApplicationLoaders} have finished processing
 * and the application is done initializing.
 */
public final class ApplicationStarted extends ProviderRegistryHolder {

    public ApplicationStarted(ProviderRegistry providerRegistry) {
        super(providerRegistry);
    }
}
