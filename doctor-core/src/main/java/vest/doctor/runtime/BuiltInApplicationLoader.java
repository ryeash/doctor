package vest.doctor.runtime;

import vest.doctor.AdHocProvider;
import vest.doctor.ApplicationLoader;
import vest.doctor.ProviderRegistry;
import vest.doctor.conf.ConfigurationFacade;
import vest.doctor.event.EventBus;
import vest.doctor.event.ReloadConfiguration;

public final class BuiltInApplicationLoader implements ApplicationLoader {
    @Override
    public void stage1(ProviderRegistry providerRegistry) {
        providerRegistry.register(new AdHocProvider<>(ConfigurationFacade.class, providerRegistry.configuration(), null));
        EventBus eventBus = new EventBusImpl();
        providerRegistry.register(new AdHocProvider<>(EventBus.class, eventBus, null));
        eventBus.addConsumer(ReloadConfiguration.class, rc -> providerRegistry.configuration().reload());
    }

    @Override
    public int priority() {
        return 10;
    }
}
