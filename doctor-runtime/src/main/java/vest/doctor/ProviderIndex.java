package vest.doctor;

import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Stream;

final class ProviderIndex {

    private final Map<ClassKey, Map<String, DoctorProvider<?>>> primary = new HashMap<>();
    private final Map<ClassKey, Map<String, List<DoctorProvider<?>>>> secondary = new HashMap<>();
    private final Map<ClassKey, Collection<DoctorProvider<?>>> annotationTypeToProvider = new HashMap<>(128);

    void setProvider(DoctorProvider<?> provider) {
        Objects.requireNonNull(provider);
        Objects.requireNonNull(provider.type());
        synchronized (this) {
            // primary
            Map<String, DoctorProvider<?>> qualifierToProvider = primary.computeIfAbsent(new ClassKey(provider.type()), t -> new HashMap<>());
            if (qualifierToProvider.containsKey(provider.qualifier())) {
                throw new IllegalArgumentException("there is already a provider registered under: " + provider.qualifier() + ":" + provider.type());
            }
            qualifierToProvider.put(provider.qualifier(), provider);

            // secondary
            for (Class<?> type : provider.allProvidedTypes()) {
                Map<String, List<DoctorProvider<?>>> sub = secondary.computeIfAbsent(new ClassKey(type), t -> new HashMap<>());
                sub.computeIfAbsent(provider.qualifier(), q -> new ArrayList<>()).add(provider);
            }

            for (Class<? extends Annotation> annotation : provider.allAnnotationTypes()) {
                annotationTypeToProvider.computeIfAbsent(new ClassKey(annotation), a -> new HashSet<>(16)).add(provider);
            }
        }
    }

    @SuppressWarnings("unchecked")
    <T> Optional<DoctorProvider<T>> getProvider(Class<T> type, String qualifier) {
        ClassKey key = new ClassKey(type);
        // check primary
        DoctorProvider<?> doctorProvider = primary.getOrDefault(key, Collections.emptyMap()).get(qualifier);
        if (doctorProvider != null) {
            return Optional.of((DoctorProvider<T>) doctorProvider);
        }

        // fallback to secondary
        return secondary.getOrDefault(key, Collections.emptyMap())
                .getOrDefault(qualifier, Collections.emptyList())
                .stream()
                .map(p -> (DoctorProvider<T>) p)
                .findFirst();
    }

    @SuppressWarnings("unchecked")
    <T> Stream<DoctorProvider<T>> getProviders(Class<T> type) {
        return Optional.ofNullable(secondary.get(new ClassKey(type)))
                .map(Map::values)
                .map(Collection::stream)
                .orElse(Stream.empty())
                .flatMap(Collection::stream)
                .map(p -> (DoctorProvider<T>) p);
    }

    Stream<DoctorProvider<?>> getProvidersWithAnnotation(Class<? extends Annotation> type) {
        return annotationTypeToProvider.getOrDefault(new ClassKey(type), Collections.emptyList()).stream();
    }

    Stream<DoctorProvider<?>> allProviders() {
        return primary.values().stream()
                .map(Map::values)
                .flatMap(Collection::stream);
    }

    int size() {
        return (int) primary.values()
                .stream()
                .mapToInt(Map::size)
                .count();
    }
}