package vest.doctor.runtime;

import vest.doctor.AdHocProvider;
import vest.doctor.AppLoader;
import vest.doctor.ConfigurationFacade;
import vest.doctor.ProviderRegistry;
import vest.doctor.SingletonScopedProvider;
import vest.doctor.event.EventBus;
import vest.doctor.event.EventProducer;
import vest.doctor.event.ReloadConfiguration;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Properties;
import java.util.stream.StreamSupport;

public class BuiltInAppLoader implements AppLoader {

    public static final String LOAD_BUILT_INS = "doctor.load.builtins";
    public static final String DEFAULT_EXECUTOR_NAME = "default";
    public static final String DEFAULT_SCHEDULED_EXECUTOR_NAME = "scheduled";

    @Override
    public void preProcess(ProviderRegistry providerRegistry) {
        providerRegistry.register(new AdHocProvider<>(ConfigurationFacade.class, providerRegistry.configuration(), null));
        providerRegistry.register(new AdHocProvider<>(Properties.class, providerRegistry.configuration().toProperties(), null));

        Map<String, ConfigurationDrivenExecutorServiceProvider.ThreadPoolType> executors = new HashMap<>();
        StreamSupport.stream(providerRegistry.configuration().propertyNames().spliterator(), false)
                .filter(name -> name.startsWith("executors."))
                .map(name -> getBetween(name, "executors.", "."))
                .filter(Objects::nonNull)
                .forEach(name -> executors.put(name, null));
        if (loadBuiltIns(providerRegistry)) {
            EventBus eventBus = new EventBus();
            providerRegistry.register(new AdHocProvider<>(EventBus.class, eventBus, null, List.of(EventBus.class, EventProducer.class)));
            eventBus.addConsumer(obj -> {
                if (obj instanceof ReloadConfiguration) {
                    providerRegistry.configuration().reload();
                }
            });
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

    @Override
    public void close() {
        // no-op
    }

    private boolean loadBuiltIns(ProviderRegistry providerRegistry) {
        return providerRegistry.configuration().get(LOAD_BUILT_INS, true, Boolean::valueOf);
    }

    private static String getBetween(String string, String start, String end) {
        int begin = string.indexOf(start);
        if (begin >= 0) {
            begin += start.length();
            int stop = string.indexOf(end, begin);
            return string.substring(begin, stop);
        } else {
            return null;
        }
    }

}