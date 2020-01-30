package vest.doctor;

import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.Properties;
import java.util.ServiceLoader;

public class BuiltInAppLoader implements AppLoader {

    public static final String DEFAULT_EXECUTOR_NAME = "default";
    public static final String DEFAULT_SCHEDULED_EXECUTOR_NAME = "defaultScheduled";

    @Override
    public void preProcess(ProviderRegistry providerRegistry) {
        providerRegistry.register(new AdHocProvider<>(ProviderRegistry.class, providerRegistry, null));
        providerRegistry.register(new AdHocProvider<>(ConfigurationFacade.class, providerRegistry.configuration(), null));
        providerRegistry.register(new AdHocProvider<>(Properties.class, providerRegistry.configuration().toProperties(), null));

        Boolean loadBuiltIns = providerRegistry.configuration().get("doctor.load.builtins", true, Boolean::valueOf);
        if (!loadBuiltIns) {
            return;
        }

        List<EventManager> managers = new LinkedList<>();
        ServiceLoader.load(EventManager.class).forEach(managers::add);
        if (!managers.isEmpty()) {
            EventManagerFacade facade = new EventManagerFacade(managers);
            AdHocProvider<EventManager> eventManagerAdHocProvider = new AdHocProvider<>(EventManager.class, facade, null, Arrays.asList(EventProducer.class, EventManager.class));
            providerRegistry.register(eventManagerAdHocProvider);
        }

        providerRegistry.register(new SingletonScopedProvider<>(new ConfigurationDrivenExecutorServiceProvider(providerRegistry, DEFAULT_EXECUTOR_NAME, null)));
        providerRegistry.register(new SingletonScopedProvider<>(new ConfigurationDrivenExecutorServiceProvider(providerRegistry, DEFAULT_SCHEDULED_EXECUTOR_NAME, ConfigurationDrivenExecutorServiceProvider.ThreadPoolType.scheduled)));
    }

    @Override
    public void load(ProviderRegistry providerRegistry) {

    }

    @Override
    public void postProcess(ProviderRegistry providerRegistry) {
        EventManager instance = providerRegistry.getInstance(EventManager.class);
        instance.initialize(providerRegistry);
    }

    @Override
    public int priority() {
        return 10;
    }

    @Override
    public void close() {
        // no-op
    }

    private static class EventManagerFacade implements EventManager {
        private final List<EventManager> managers;

        private EventManagerFacade(List<EventManager> managers) {
            this.managers = managers;
        }

        @Override
        public void initialize(ProviderRegistry providerRegistry) {
            for (EventManager manager : managers) {
                manager.initialize(providerRegistry);
            }
        }

        @Override
        public void publish(Object event) {
            for (EventManager manager : managers) {
                manager.publish(event);
            }
        }
    }
}
