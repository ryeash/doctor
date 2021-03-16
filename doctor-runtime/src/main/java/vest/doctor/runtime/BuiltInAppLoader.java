package vest.doctor.runtime;

import vest.doctor.AdHocProvider;
import vest.doctor.AppLoader;
import vest.doctor.ConfigurationFacade;
import vest.doctor.ProviderRegistry;
import vest.doctor.SingletonScopedProvider;
import vest.doctor.event.EventBus;
import vest.doctor.event.EventProducer;
import vest.doctor.event.ReloadConfiguration;

import java.util.Arrays;
import java.util.Objects;
import java.util.Properties;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

public class BuiltInAppLoader implements AppLoader {

    public static final String LOAD_BUILT_INS = "doctor.load.builtins";
    public static final String DEFAULT_EXECUTOR_NAME = "default";
    public static final String DEFAULT_SCHEDULED_EXECUTOR_NAME = "scheduled";

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

        Set<String> executors = StreamSupport.stream(providerRegistry.configuration().propertyNames().spliterator(), false)
                .filter(name -> name.startsWith("executors."))
                .map(name -> getBetween(name, "executors.", "."))
                .filter(Objects::nonNull)
                .filter(n -> !(n.equals(DEFAULT_EXECUTOR_NAME) || n.equals(DEFAULT_SCHEDULED_EXECUTOR_NAME)))
                .collect(Collectors.toSet());

        for (String executor : executors) {
            providerRegistry.register(new SingletonScopedProvider<>(new ConfigurationDrivenExecutorServiceProvider(providerRegistry, executor, null)));
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
