package vest.doctor.message;

import vest.doctor.ProviderRegistry;

public class ApplicationShutdown extends ProviderRegistryHolder {
    public ApplicationShutdown(ProviderRegistry providerRegistry) {
        super(providerRegistry);
    }
}
