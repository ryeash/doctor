package vest.doctor.message;

import vest.doctor.Doctor;
import vest.doctor.ProviderRegistry;

/**
 * Event sent by the {@link Doctor} instance when all providers have been loaded.
 */
public final class ApplicationStarted extends ProviderRegistryHolder {

    public ApplicationStarted(ProviderRegistry providerRegistry) {
        super(providerRegistry);
    }
}
