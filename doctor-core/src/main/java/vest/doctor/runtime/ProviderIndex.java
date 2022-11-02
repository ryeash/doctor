package vest.doctor.runtime;

import vest.doctor.DoctorProvider;

import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.stream.Stream;

final class ProviderIndex {

    private final Lock writeLock = new ReentrantLock();
    private final Map<String, Map<String, DoctorProvider<?>>> primary = new ConcurrentSkipListMap<>();
    private final Map<String, Map<String, Collection<DoctorProvider<?>>>> secondary = new ConcurrentSkipListMap<>();

    void setProvider(DoctorProvider<?> provider) {
        Objects.requireNonNull(provider);
        Objects.requireNonNull(provider.type());
        writeLock.lock();
        try {
            // primary
            Map<String, DoctorProvider<?>> qualifierToProvider = primary.computeIfAbsent(provider.type().getName(), t -> new HashMap<>());
            qualifierToProvider.merge(provider.qualifier(), provider, (existing, insert) -> {
                throw new IllegalArgumentException("there is already a provider registered under: " + provider.qualifier() + ":" + provider.type());
            });

            // secondary
            for (Class<?> type : provider.allProvidedTypes()) {
                secondary.computeIfAbsent(type.getName(), t -> new HashMap<>())
                        .computeIfAbsent(provider.qualifier(), q -> new ArrayList<>())
                        .add(provider);
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
        return allProviders()
                .filter(p -> p.annotationMetadata()
                        .stream()
                        .anyMatch(am -> am.type() == type));
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