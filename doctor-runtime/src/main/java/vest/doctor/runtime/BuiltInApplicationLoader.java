package vest.doctor.runtime;

import vest.doctor.AdHocProvider;
import vest.doctor.ApplicationLoader;
import vest.doctor.ProviderRegistry;
import vest.doctor.SingletonScopedProvider;
import vest.doctor.conf.ConfigurationFacade;
import vest.doctor.event.EventBus;
import vest.doctor.event.ReloadConfiguration;

import java.util.HashMap;
import java.util.Map;

public class BuiltInApplicationLoader implements ApplicationLoader {

    public static final String DEFAULT_EXECUTOR_NAME = "default";
    public static final String DEFAULT_SCHEDULED_EXECUTOR_NAME = "scheduled";

    @Override
    public void stage1(ProviderRegistry providerRegistry) {
        providerRegistry.register(new AdHocProvider<>(ConfigurationFacade.class, providerRegistry.configuration(), null));

        Map<String, ConfigurationDrivenExecutorServiceProvider.ThreadPoolType> executors = new HashMap<>();
        providerRegistry.configuration()
                .getSubConfiguration("executors")
                .propertyNames()
                .forEach(n -> executors.put(n, null));

        EventBus eventBus = new EventBusImpl();
        providerRegistry.register(new AdHocProvider<>(EventBus.class, eventBus, null));
        eventBus.addConsumer(ReloadConfiguration.class, rc -> providerRegistry.configuration().reload());
        executors.put(DEFAULT_EXECUTOR_NAME, null);
        executors.put(DEFAULT_SCHEDULED_EXECUTOR_NAME, ConfigurationDrivenExecutorServiceProvider.ThreadPoolType.scheduled);

        for (Map.Entry<String, ConfigurationDrivenExecutorServiceProvider.ThreadPoolType> e : executors.entrySet()) {
            ConfigurationDrivenExecutorServiceProvider cdesp = new ConfigurationDrivenExecutorServiceProvider(providerRegistry, e.getKey(), e.getValue());
            providerRegistry.register(new SingletonScopedProvider<>(cdesp));
        }
    }

    @Override
    public int priority() {
        return 10;
    }
}
