package vest.doctor;

import vest.doctor.event.EventBus;
import vest.doctor.event.EventProducer;
import vest.doctor.event.ReloadConfiguration;

import java.util.Arrays;
import java.util.Properties;

public class BuiltInAppLoader implements AppLoader {

    public static final String LOAD_BUILT_INS = "doctor.load.builtins";
    public static final String DEFAULT_EXECUTOR_NAME = "default";
    public static final String DEFAULT_SCHEDULED_EXECUTOR_NAME = "defaultScheduled";

    @Override
    public void preProcess(ProviderRegistry providerRegistry) {
        providerRegistry.register(new AdHocProvider<>(ConfigurationFacade.class, providerRegistry.configuration(), null));
        providerRegistry.register(new AdHocProvider<>(Properties.class, providerRegistry.configuration().toProperties(), null));

        if (loadBuiltIns(providerRegistry)) {
            providerRegistry.register(new SingletonScopedProvider<>(new ConfigurationDrivenExecutorServiceProvider(providerRegistry, DEFAULT_EXECUTOR_NAME, null)));
            providerRegistry.register(new SingletonScopedProvider<>(new ConfigurationDrivenExecutorServiceProvider(providerRegistry, DEFAULT_SCHEDULED_EXECUTOR_NAME, ConfigurationDrivenExecutorServiceProvider.ThreadPoolType.scheduled)));
            EventBus eventBus = new EventBus();
            providerRegistry.register(new AdHocProvider<>(EventBus.class, eventBus, null, Arrays.asList(EventBus.class, EventProducer.class)));
            eventBus.addConsumer(obj -> {
                if (obj instanceof ReloadConfiguration) {
                    providerRegistry.configuration().reload();
                }
            });
        }
    }

    @Override
    public int priority() {
        return 10;
    }

    @Override
    public void close() {
        // no-op
    }

    private boolean loadBuiltIns(ProviderRegistry providerRegistry) {
        return providerRegistry.configuration().get(LOAD_BUILT_INS, true, Boolean::valueOf);
    }

}
