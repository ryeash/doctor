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
import java.util.stream.Stream;

final class ProviderIndex {

    private final Map<TypeQualifier, DoctorProvider<?>> primary = new HashMap<>();
    private final Map<String, Map<String, Collection<DoctorProvider<?>>>> secondary = new HashMap<>();

    void setProvider(DoctorProvider<?> provider) {
        Objects.requireNonNull(provider);
        Objects.requireNonNull(provider.typeInfo());
        synchronized (primary) {
            // primary
            TypeQualifier tq = new TypeQualifier(provider.typeInfo().getRawType(), provider.qualifier());
            primary.merge(tq, provider, (existing, insert) -> {
                throw new IllegalArgumentException("there is already a provider registered under: " + provider.qualifier() + ":" + provider.typeInfo().getRawType().getCanonicalName());
            });

            // secondary
            for (Class<?> type : provider.allProvidedTypes()) {
                secondary.computeIfAbsent(type.getName(), t -> new HashMap<>())
                        .computeIfAbsent(provider.qualifier(), q -> new ArrayList<>())
                        .add(provider);
            }
        }
    }

    @SuppressWarnings("unchecked")
    <T> Optional<DoctorProvider<T>> getProvider(Class<T> type, String qualifier) {
        // check primary
        TypeQualifier tq = new TypeQualifier(type, qualifier);
        DoctorProvider<?> doctorProvider = primary.get(tq);
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
                .filter(p -> p.typeInfo().annotationMetadata()
                        .stream()
                        .anyMatch(am -> am.annotationType() == type));
    }

    Stream<DoctorProvider<?>> allProviders() {
        return primary.values().stream();
    }

    int size() {
        return primary.size();
    }

    record TypeQualifier(Class<?> type, String qualifier) {
    }
}