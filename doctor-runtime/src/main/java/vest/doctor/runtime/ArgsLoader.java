package vest.doctor.runtime;

import vest.doctor.AdHocProvider;
import vest.doctor.AppLoader;
import vest.doctor.ProviderRegistry;

final class ArgsLoader implements AppLoader {
    private final Args args;

    ArgsLoader(Args args) {
        this.args = args;
    }

    @Override
    public void preProcess(ProviderRegistry providerRegistry) {
        providerRegistry.register(new AdHocProvider<>(Args.class, args, null));
    }

    @Override
    public int priority() {
        return Integer.MIN_VALUE;
    }
}
