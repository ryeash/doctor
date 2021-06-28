package vest.doctor.runtime;

import vest.doctor.AdHocProvider;
import vest.doctor.ApplicationLoader;
import vest.doctor.ProviderRegistry;

final class ArgsLoader implements ApplicationLoader {
    private final Args args;

    ArgsLoader(Args args) {
        this.args = args;
    }

    @Override
    public void stage1(ProviderRegistry providerRegistry) {
        providerRegistry.register(new AdHocProvider<>(Args.class, args, null));
    }

    @Override
    public int priority() {
        return Integer.MIN_VALUE;
    }
}
