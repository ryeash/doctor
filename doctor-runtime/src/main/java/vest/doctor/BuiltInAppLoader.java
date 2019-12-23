package vest.doctor;

import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.ServiceLoader;

public class BuiltInAppLoader implements AppLoader {

    public static final String DEFAULT_EXECUTOR_NAME = "default";
    public static final String DEFAULT_SCHEDULED_EXECUTOR_NAME = "defaultScheduled";

    @Override
    public void preProcess(BeanProvider beanProvider) {
        beanProvider.register(new AdHocProvider<>(BeanProvider.class, beanProvider, null));
        beanProvider.register(new AdHocProvider<>(ConfigurationFacade.class, beanProvider.configuration(), null));

        Boolean loadBuiltIns = beanProvider.configuration().get("doctor.load.builtins", true, Boolean::valueOf);
        if (!loadBuiltIns) {
            return;
        }

        List<EventManager> managers = new LinkedList<>();
        ServiceLoader.load(EventManager.class).forEach(managers::add);
        if (!managers.isEmpty()) {
            EventManagerFacade facade = new EventManagerFacade(managers);
            AdHocProvider<EventManager> eventManagerAdHocProvider = new AdHocProvider<>(EventManager.class, facade, null, Arrays.asList(EventProducer.class, EventManager.class));
            beanProvider.register(eventManagerAdHocProvider);
        }

        beanProvider.register(new SingletonScopedProvider<>(new ConfigurationDrivenExecutorServiceProvider(beanProvider, DEFAULT_EXECUTOR_NAME, null)));
        beanProvider.register(new SingletonScopedProvider<>(new ConfigurationDrivenExecutorServiceProvider(beanProvider, DEFAULT_SCHEDULED_EXECUTOR_NAME, ConfigurationDrivenExecutorServiceProvider.ThreadPoolType.scheduled)));
    }

    @Override
    public void load(BeanProvider beanProvider) {

    }

    @Override
    public void postProcess(BeanProvider beanProvider) {
        EventManager instance = beanProvider.getInstance(EventManager.class);
        instance.initialize(beanProvider);
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
        public void initialize(BeanProvider beanProvider) {
            for (EventManager manager : managers) {
                manager.initialize(beanProvider);
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
