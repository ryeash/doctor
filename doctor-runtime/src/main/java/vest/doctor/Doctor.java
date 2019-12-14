package vest.doctor;

import java.lang.annotation.Annotation;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.ServiceLoader;
import java.util.TreeMap;
import java.util.stream.Stream;

public class Doctor implements BeanProvider, AutoCloseable {

    public static Doctor load() {
        return new Doctor(DefaultConfigurationFacade.defaultConfigurationFacade(), Collections.emptyList());
    }

    public static Doctor load(String... modules) {
        return new Doctor(DefaultConfigurationFacade.defaultConfigurationFacade(), Arrays.asList(modules));
    }

    public static Doctor load(ConfigurationFacade configurationFacade, List<String> modules) {
        return new Doctor(configurationFacade, modules);
    }

    private final List<String> activeModules;
    private final List<AppLoader> loaders;
    private final ProviderIndex providerIndex = new ProviderIndex();
    private final ConfigurationFacade configurationFacade;

    public Doctor(ConfigurationFacade configurationFacade, List<String> activeModules) {
        this.activeModules = activeModules;
        this.configurationFacade = configurationFacade;

        this.loaders = new LinkedList<>();
        this.loaders.add(new BuiltInAppLoader());
        for (AppLoader appLoader : ServiceLoader.load(AppLoader.class)) {
            loaders.add(appLoader);
        }
        loaders.sort(Prioritized.COMPARATOR);
        for (AppLoader loader : loaders) {
            loader.preProcess(this);
        }
        for (AppLoader loader : loaders) {
            loader.load(this);
        }
        for (AppLoader loader : loaders) {
            loader.postProcess(this);
        }

        if (!configurationFacade.get("nurse.skip.validation", false, Boolean::valueOf)) {
            providerIndex.allProviders().forEach(np -> np.validateDependencies(this));
        }
        getInstance(EventProducer.class).publish(new ApplicationStartedEvent(this));
    }

    public List<String> getActiveModules() {
        return activeModules;
    }

    public void register(DoctorProvider<?> provider) {
        providerIndex.setProvider(provider);
    }

    public <T> T getInstance(Class<T> type) {
        return getProvider(type).get();
    }

    public <T> T getInstance(Class<T> type, String qualifier) {
        return getProvider(type, qualifier).get();
    }

    public <T> DoctorProvider<T> getProvider(Class<T> type) {
        return getProvider(type, null);
    }

    public <T> DoctorProvider<T> getProvider(Class<T> type, String qualifier) {
        return getProviderOpt(type, qualifier)
                .orElseThrow(() -> new IllegalArgumentException("no provider registered for " + qualifier + "/" + type.getCanonicalName()));
    }

    @SuppressWarnings("unchecked")
    public <T> Optional<DoctorProvider<T>> getProviderOpt(Class<T> type, String qualifier) {
        DoctorProvider<T> provider = (DoctorProvider<T>) providerIndex.getProvider(type, qualifier);
        return Optional.ofNullable(provider);
    }

    @SuppressWarnings("unchecked")
    public <T> Stream<DoctorProvider<T>> getProviders(Class<T> type) {
        return providerIndex.getProviders(type).map(c -> (DoctorProvider<T>) c);
    }

    public Stream<DoctorProvider<?>> getProvidersWithAnnotation(Class<? extends Annotation> annotationType) {
        return providerIndex.getProvidersWithAnnotation(annotationType);
    }

    public boolean hasProvider(Class<?> type) {
        return hasProvider(type, null);
    }

    public boolean hasProvider(Class<?> type, String qualifier) {
        return providerIndex.getProvider(type, qualifier) != null;
    }

    public ConfigurationFacade configuration() {
        return configurationFacade;
    }

    public String resolvePlaceholders(String string) {
        return configurationFacade.resolvePlaceholders(string);
    }

    @Override
    public void close() throws Exception {
        for (AppLoader loader : loaders) {
            loader.close();
        }
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("Doctor:\n");
        sb.append("Providers(").append(providerIndex.size()).append("):\n");

        Map<String, List<DoctorProvider<?>>> map = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);

        providerIndex.allProviders().forEach(p -> {
            for (Object type : p.allProvidedTypes()) {
                map.computeIfAbsent(((Class<?>) type).getSimpleName(), s -> new LinkedList<>()).add(p);
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
