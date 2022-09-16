package vest.doctor.runtime;

import vest.doctor.AdHocProvider;
import vest.doctor.ApplicationLoader;
import vest.doctor.Prioritized;
import vest.doctor.ProviderRegistry;

record ArgsLoader(Args args) implements ApplicationLoader {

    @Override
    public void stage1(ProviderRegistry providerRegistry) {
        providerRegistry.register(AdHocProvider.createPrimary(args));
    }

    @Override
    public int priority() {
        return Prioritized.HIGHEST_PRIORITY;
    }
}
