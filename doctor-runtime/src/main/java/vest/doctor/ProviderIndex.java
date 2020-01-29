package vest.doctor;

import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Stream;

final class ProviderIndex {

    private final Map<Class<?>, Map<String, DoctorProvider<?>>> primary = new HashMap<>();
    private final Map<Class<?>, Map<String, List<DoctorProvider<?>>>> secondary = new HashMap<>();
    private final Map<Class<? extends Annotation>, Collection<DoctorProvider<?>>> annotationTypeToProvider = new HashMap<>(128);
    private int size = 0;

    void setProvider(DoctorProvider<?> provider) {
        synchronized (this) {
            // primary
            Map<String, DoctorProvider<?>> qualifierToProvider = primary.computeIfAbsent(provider.type(), t -> new HashMap<>());
            if (qualifierToProvider.containsKey(provider.qualifier())) {
                throw new IllegalArgumentException("there is already a provider registered under: " + provider.qualifier() + ":" + provider.type());
            }
            qualifierToProvider.put(provider.qualifier(), provider);

            // secondary
            for (Class<?> type : provider.allProvidedTypes()) {
                Map<String, List<DoctorProvider<?>>> sub = secondary.computeIfAbsent(type, t -> new HashMap<>());
                sub.computeIfAbsent(provider.qualifier(), q -> new ArrayList<>()).add(provider);
            }

            for (Class<? extends Annotation> annotation : provider.allAnnotationTypes()) {
                annotationTypeToProvider.computeIfAbsent(annotation, a -> new HashSet<>(16)).add(provider);
            }
            size++;
        }
    }

    @SuppressWarnings("unchecked")
    <T> Optional<DoctorProvider<T>> getProvider(Class<T> type, String qualifier) {
        // check primary
        DoctorProvider<?> doctorProvider = primary.getOrDefault(type, Collections.emptyMap()).get(qualifier);
        if (doctorProvider != null) {
            return Optional.of((DoctorProvider<T>) doctorProvider);
        }

        // fallback to secondary
        return secondary.getOrDefault(type, Collections.emptyMap())
                .getOrDefault(qualifier, Collections.emptyList())
                .stream()
                .map(p -> (DoctorProvider<T>) p)
                .findFirst();
    }

    @SuppressWarnings("unchecked")
    <T> Stream<DoctorProvider<T>> getProviders(Class<T> type) {
        return Optional.ofNullable(secondary.get(type))
                .map(Map::values)
                .map(Collection::stream)
                .orElse(Stream.empty())
                .flatMap(Collection::stream)
                .map(p -> (DoctorProvider<T>) p);
    }

    Stream<DoctorProvider<?>> getProvidersWithAnnotation(Class<? extends Annotation> type) {
        return annotationTypeToProvider.getOrDefault(type, Collections.emptyList()).stream();
    }

    Stream<DoctorProvider<?>> allProviders() {
        return primary.values().stream()
                .map(Map::values)
                .flatMap(Collection::stream);
    }

    int size() {
        return size;
    }
}