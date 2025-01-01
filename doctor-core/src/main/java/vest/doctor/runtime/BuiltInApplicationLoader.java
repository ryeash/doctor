package vest.doctor.runtime;

import vest.doctor.AdHocProvider;
import vest.doctor.ApplicationLoader;
import vest.doctor.ProviderRegistry;
import vest.doctor.conf.ConfigurationFacade;

public final class BuiltInApplicationLoader implements ApplicationLoader {
    @Override
    public void stage1(ProviderRegistry providerRegistry) {
        providerRegistry.register(new AdHocProvider<>(ConfigurationFacade.class, providerRegistry.configuration(), null));
    }

    @Override
    public int priority() {
        return 10;
    }
}
