package vest.doctor;

import java.util.Arrays;
import java.util.Properties;

public class BuiltInAppLoader implements AppLoader {

    public static final String LOAD_BUILD_INS = "doctor.load.builtins";
    public static final String DEFAULT_EXECUTOR_NAME = "default";
    public static final String DEFAULT_SCHEDULED_EXECUTOR_NAME = "defaultScheduled";

    @Override
    public void preProcess(ProviderRegistry providerRegistry) {
        providerRegistry.register(new AdHocProvider<>(ConfigurationFacade.class, providerRegistry.configuration(), null));
        providerRegistry.register(new AdHocProvider<>(Properties.class, providerRegistry.configuration().toProperties(), null));

        if (loadBuiltIns(providerRegistry)) {
            providerRegistry.register(new AdHocProvider<>(EventManager.class, new EventManagerImpl(), null, Arrays.asList(EventProducer.class, EventManager.class)));
            providerRegistry.register(new SingletonScopedProvider<>(new ConfigurationDrivenExecutorServiceProvider(providerRegistry, DEFAULT_EXECUTOR_NAME, null)));
            providerRegistry.register(new SingletonScopedProvider<>(new ConfigurationDrivenExecutorServiceProvider(providerRegistry, DEFAULT_SCHEDULED_EXECUTOR_NAME, ConfigurationDrivenExecutorServiceProvider.ThreadPoolType.scheduled)));
        }
    }

    @Override
    public void load(ProviderRegistry providerRegistry) {
    }

    @Override
    public void postProcess(ProviderRegistry providerRegistry) {
        if (loadBuiltIns(providerRegistry)) {
            EventManager instance = providerRegistry.getInstance(EventManager.class);
            instance.initialize(providerRegistry);
            instance.register(new ConfigurationReloadEventListener(providerRegistry));
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
        return providerRegistry.configuration().get(LOAD_BUILD_INS, true, Boolean::valueOf);
    }
}
