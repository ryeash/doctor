package vest.doctor.runtime;

import jakarta.inject.Provider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import vest.doctor.AdHocProvider;
import vest.doctor.ApplicationLoader;
import vest.doctor.ConfigurationFacade;
import vest.doctor.DoctorProvider;
import vest.doctor.Prioritized;
import vest.doctor.ProviderRegistry;
import vest.doctor.event.ApplicationShutdown;
import vest.doctor.event.ApplicationStarted;
import vest.doctor.event.EventBus;

import java.lang.annotation.Annotation;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.ServiceLoader;
import java.util.TreeMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * The entrypoint for applications to access generated {@link jakarta.inject.Provider}s.
 * Initializes (and serves as) the {@link ProviderRegistry} for an application.
 */
public class Doctor implements ProviderRegistry, AutoCloseable {

    private static final String ASCII_ART = """
              _
             / | _  _ _/__  __
            /_.'/_//_ / /_//
            """;

    private static final Logger log = LoggerFactory.getLogger(Doctor.class);

    /**
     * Initialize the application with default configuration and no active modules.
     *
     * @return a new Doctor instance
     */
    public static Doctor load() {
        return new Doctor(DefaultConfigurationFacade.defaultConfigurationFacade(), Collections.emptyList());
    }

    /**
     * Initialize the application with default configuration and the given active modules.
     *
     * @param modules the active modules
     * @return a new Doctor instance
     */
    public static Doctor load(List<String> modules) {
        return new Doctor(DefaultConfigurationFacade.defaultConfigurationFacade(), modules);
    }

    /**
     * Initialize the application with default configuration and the given active modules.
     *
     * @param modules the active modules
     * @return a new Doctor instance
     */
    public static Doctor load(String... modules) {
        return new Doctor(DefaultConfigurationFacade.defaultConfigurationFacade(), List.of(modules));
    }

    /**
     * Initialize the application with the given configuration.
     *
     * @param configurationFacade the configuration for the application
     * @return a new Doctor instance
     */
    public static Doctor load(ConfigurationFacade configurationFacade) {
        return new Doctor(configurationFacade, Collections.emptyList());
    }

    /**
     * Initialize the application with the given configuration and active modules.
     *
     * @param configurationFacade the configuration for the application
     * @param modules             the active modules
     * @return a new Doctor instance
     */
    public static Doctor load(ConfigurationFacade configurationFacade, String... modules) {
        return new Doctor(configurationFacade, List.of(modules));
    }

    /**
     * Initialize the application with the given configuration and active modules.
     *
     * @param configurationFacade the configuration for the application
     * @param modules             the active modules
     * @return a new Doctor instance
     */
    public static Doctor load(ConfigurationFacade configurationFacade, List<String> modules) {
        return new Doctor(configurationFacade, modules);
    }

    private final List<String> activeModules;
    private final ProviderIndex providerIndex;
    private final ConfigurationFacade configurationFacade;
    private final AtomicBoolean closed = new AtomicBoolean(false);

    /**
     * Create a new Doctor instance, loading all available providers and services automatically.
     *
     * @param configurationFacade the configuration for the application
     * @param activeModules       the active modules
     * @param additionalLoaders   additional {@link ApplicationLoader AppLoaders} to use during initialization
     */
    public Doctor(ConfigurationFacade configurationFacade, List<String> activeModules, ApplicationLoader... additionalLoaders) {
        log.debug("Doctor initializing...");
        long start = System.currentTimeMillis();
        this.providerIndex = new ProviderIndex();
        providerIndex.setProvider(new AdHocProvider<>(Doctor.class, this, null, List.of(Doctor.class, ProviderRegistry.class)));
        this.activeModules = activeModules;
        this.configurationFacade = configurationFacade;

        log.debug("Active modules: {}", this.activeModules);
        log.debug("Configuration: {}", this.configurationFacade);

        List<ApplicationLoader> loaders = new LinkedList<>();
        loaders.add(new BuiltInApplicationLoader());
        for (ApplicationLoader applicationLoader : ServiceLoader.load(ApplicationLoader.class)) {
            loaders.add(applicationLoader);
        }
        if (additionalLoaders != null) {
            Collections.addAll(loaders, additionalLoaders);
        }
        loaders.sort(Prioritized.COMPARATOR);
        log.debug("Loaders (in order): {}", loaders.stream().map(l -> l + ":" + l.priority()).collect(Collectors.joining(", ")));
        for (ApplicationLoader loader : loaders) {
            loader.stage1(this);
        }
        for (ApplicationLoader loader : loaders) {
            loader.stage2(this);
        }
        for (ApplicationLoader loader : loaders) {
            loader.stage3(this);
        }
        for (ApplicationLoader loader : loaders) {
            loader.stage4(this);
        }
        for (ApplicationLoader loader : loaders) {
            loader.stage5(this);
        }

        if (!configurationFacade.get("doctor.skip.validation", false, Boolean::valueOf)) {
            providerIndex.allProviders().forEach(np -> np.validateDependencies(this));
        }

        if (configurationFacade.get("doctor.autoShutdown", true, Boolean::valueOf)) {
            Runtime.getRuntime().addShutdownHook(new Thread(this::close, "doctor-shutdown-" + this.hashCode()));
        }
        EventBus eventBus = getInstance(EventBus.class);
        eventBus.publish(new ApplicationStarted(this));
        log.info("\n{}\ninitialized in {}ms", ASCII_ART, (System.currentTimeMillis() - start));
    }

    @Override
    public List<String> getActiveModules() {
        return activeModules;
    }

    @Override
    public void register(DoctorProvider<?> provider) {
        providerIndex.setProvider(provider);
    }

    @Override
    public <T> DoctorProvider<T> getProvider(Class<T> type) {
        return getProvider(type, null);
    }

    @Override
    public <T> DoctorProvider<T> getProvider(Class<T> type, String qualifier) {
        return getProviderOpt(type, qualifier)
                .orElseThrow(() -> new IllegalArgumentException("no provider registered for " + qualifier + "/" + type.getCanonicalName()));
    }

    @Override
    public <T> Optional<DoctorProvider<T>> getProviderOpt(Class<T> type, String qualifier) {
        return providerIndex.getProvider(type, qualifier);
    }

    @Override
    public <T> Stream<DoctorProvider<T>> getProviders(Class<T> type) {
        return providerIndex.getProviders(type);
    }

    @Override
    public <T> Stream<DoctorProvider<T>> getProviders(Class<T> type, String qualifier) {
        if (qualifier == null) {
            return getProviders(type);
        }
        return getProviders(type).filter(p -> Objects.equals(p.qualifier(), qualifier));
    }

    @Override
    public <T> Stream<T> getInstances(Class<T> type) {
        return getProviders(type).map(Provider::get);
    }

    @Override
    public <T> Stream<T> getInstances(Class<T> type, String qualifier) {
        return getProviders(type, qualifier).map(Provider::get);
    }

    @Override
    public Stream<DoctorProvider<?>> allProviders() {
        return providerIndex.allProviders();
    }

    @Override
    public Stream<DoctorProvider<?>> getProvidersWithAnnotation(Class<? extends Annotation> annotationType) {
        return providerIndex.getProvidersWithAnnotation(annotationType);
    }

    @Override
    public boolean hasProvider(Class<?> type) {
        return hasProvider(type, null);
    }

    @Override
    public boolean hasProvider(Class<?> type, String qualifier) {
        return providerIndex.getProvider(type, qualifier).isPresent();
    }

    @Override
    public ConfigurationFacade configuration() {
        return configurationFacade;
    }

    @Override
    public String resolvePlaceholders(String string) {
        return configurationFacade.resolvePlaceholders(string);
    }

    @Override
    public void close() {
        if (closed.compareAndSet(false, true)) {
            providerIndex.allProviders().forEach(p -> {
                try {
                    p.close();
                } catch (Throwable t) {
                    log.error("error closing provider {}", p, t);
                }
            });
            getProviderOpt(EventBus.class, null)
                    .map(Provider::get)
                    .ifPresent(ep -> ep.publish(new ApplicationShutdown(this)));
        }
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("Doctor:\n");
        sb.append("Providers(").append(providerIndex.size()).append("):\n");

        Map<String, List<DoctorProvider<?>>> map = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);

        providerIndex.allProviders().forEach(p -> {
            for (Class<?> type : p.allProvidedTypes()) {
                map.computeIfAbsent(type.getSimpleName(), s -> new LinkedList<>()).add(p);
            }
        });

        for (Map.Entry<String, List<DoctorProvider<?>>> e : map.entrySet()) {
            String type = e.getKey();
            List<DoctorProvider<?>> providers = e.getValue();
            providers.sort(Comparator.comparing(Object::toString));
            sb.append(type).append(":\n");
            for (DoctorProvider<?> p : providers) {
                sb.append("\t[").append(p);
                if (p.scope() != null) {
                    sb.append(", scope: ").append(p.scope().getSimpleName());
                }
                if (p.qualifier() != null) {
                    sb.append(", qualifier: ").append(p.qualifier());
                }
                sb.append("]\n");
            }
        }
        return sb.toString();
    }
}
