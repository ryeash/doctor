package vest.doctor.runtime;

import vest.doctor.DoctorProvider;

import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.stream.Stream;

final class ProviderIndex {

    private final Lock writeLock = new ReentrantLock();
    private final Map<String, Map<String, DoctorProvider<?>>> primary = new HashMap<>(64);
    private final Map<String, Map<String, Collection<DoctorProvider<?>>>> secondary = new HashMap<>(128);
    private final Map<String, Collection<DoctorProvider<?>>> annotationTypeToProvider = new HashMap<>(128);

    void setProvider(DoctorProvider<?> provider) {
        Objects.requireNonNull(provider);
        Objects.requireNonNull(provider.type());
        writeLock.lock();
        try {
            // primary
            Map<String, DoctorProvider<?>> qualifierToProvider = primary.computeIfAbsent(provider.type().getName(), t -> new HashMap<>());
            if (qualifierToProvider.containsKey(provider.qualifier())) {
                throw new IllegalArgumentException("there is already a provider registered under: " + provider.qualifier() + ":" + provider.type());
            }
            qualifierToProvider.put(provider.qualifier(), provider);

            // secondary
            for (Class<?> type : provider.allProvidedTypes()) {
                Map<String, Collection<DoctorProvider<?>>> sub = secondary.computeIfAbsent(type.getName(), t -> new HashMap<>());
                sub.computeIfAbsent(provider.qualifier(), q -> new ArrayList<>()).add(provider);
            }

            for (Class<? extends Annotation> annotation : provider.allAnnotationTypes()) {
                annotationTypeToProvider.computeIfAbsent(annotation.getName(), a -> new HashSet<>(16)).add(provider);
            }
        } finally {
            writeLock.unlock();
        }
    }

    @SuppressWarnings("unchecked")
    <T> Optional<DoctorProvider<T>> getProvider(Class<T> type, String qualifier) {
        // check primary
        DoctorProvider<?> doctorProvider = primary.getOrDefault(type.getName(), Collections.emptyMap()).get(qualifier);
        if (doctorProvider != null) {
            return Optional.of((DoctorProvider<T>) doctorProvider);
        }

        // fallback to secondary
        return secondary.getOrDefault(type.getName(), Collections.emptyMap())
                .getOrDefault(qualifier, Collections.emptyList())
                .stream()
                .map(p -> (DoctorProvider<T>) p)
                .findFirst();
    }

    @SuppressWarnings("unchecked")
    <T> Stream<DoctorProvider<T>> getProviders(Class<T> type) {
        return Optional.ofNullable(secondary.get(type.getName()))
                .map(Map::values)
                .stream()
                .flatMap(Collection::stream)
                .flatMap(Collection::stream)
                .distinct()
                .map(p -> (DoctorProvider<T>) p);
    }

    Stream<DoctorProvider<?>> getProvidersWithAnnotation(Class<? extends Annotation> type) {
        return annotationTypeToProvider.getOrDefault(type.getName(), Collections.emptyList()).stream();
    }

    Stream<DoctorProvider<?>> allProviders() {
        return primary.values()
                .stream()
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