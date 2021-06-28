package vest.doctor.runtime;

import vest.doctor.AdHocProvider;
import vest.doctor.ApplicationLoader;
import vest.doctor.ConfigurationFacade;
import vest.doctor.ProviderRegistry;
import vest.doctor.SingletonScopedProvider;
import vest.doctor.event.EventBus;
import vest.doctor.event.EventProducer;
import vest.doctor.event.ReloadConfiguration;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

public class BuiltInApplicationLoader implements ApplicationLoader {

    public static final String LOAD_BUILT_INS = "doctor.load.builtins";
    public static final String DEFAULT_EXECUTOR_NAME = "default";
    public static final String DEFAULT_SCHEDULED_EXECUTOR_NAME = "scheduled";

    @Override
    public void stage1(ProviderRegistry providerRegistry) {
        providerRegistry.register(new AdHocProvider<>(ConfigurationFacade.class, providerRegistry.configuration(), null));
        providerRegistry.register(new AdHocProvider<>(Properties.class, providerRegistry.configuration().toProperties(), null));

        Map<String, ConfigurationDrivenExecutorServiceProvider.ThreadPoolType> executors = new HashMap<>();
        providerRegistry.configuration().uniquePropertyGroups("executors.")
                .forEach(group -> executors.put(group, null));
        if (loadBuiltIns(providerRegistry)) {
            EventBus eventBus = new EventBus();
            providerRegistry.register(new AdHocProvider<>(EventBus.class, eventBus, null, List.of(EventBus.class, EventProducer.class)));
            eventBus.addConsumer(ReloadConfiguration.class, obj -> providerRegistry.configuration().reload());
            executors.put(DEFAULT_EXECUTOR_NAME, null);
            executors.put(DEFAULT_SCHEDULED_EXECUTOR_NAME, ConfigurationDrivenExecutorServiceProvider.ThreadPoolType.scheduled);
        }

        executors.entrySet()
                .stream()
                .map(e -> new ConfigurationDrivenExecutorServiceProvider(providerRegistry, e.getKey(), e.getValue()))
                .map(SingletonScopedProvider::new)
                .forEach(providerRegistry::register);
    }

    @Override
    public int priority() {
        return 10;
    }

    private boolean loadBuiltIns(ProviderRegistry providerRegistry) {
        return providerRegistry.configuration().get(LOAD_BUILT_INS, true, Boolean::valueOf);
    }

}
