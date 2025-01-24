package vest.doctor.runtime;

import vest.doctor.DoctorProvider;

import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Stream;

final class ProviderIndex {

    private final Map<TypeQualifier, List<DoctorProvider<?>>> index = new HashMap<>();
    private int size = 0;

    void setProvider(DoctorProvider<?> provider) {
        Objects.requireNonNull(provider);
        Objects.requireNonNull(provider.typeInfo());
        synchronized (index) {
            // primary
            TypeQualifier tq = new TypeQualifier(provider.typeInfo().getRawType(), provider.qualifier(), true);
            index.merge(tq, List.of(provider), (existing, insert) -> {
                throw new IllegalArgumentException("there is already a provider registered under: " + provider.qualifier() + ":" + provider.typeInfo().getRawType().getCanonicalName());
            });
            size++;

            // secondary
            for (Class<?> type : provider.allProvidedTypes()) {
                TypeQualifier secondary = new TypeQualifier(type, provider.qualifier(), false);
                index.computeIfAbsent(secondary, v -> new ArrayList<>()).add(provider);
            }
        }
    }

    @SuppressWarnings("unchecked")
    <T> Optional<DoctorProvider<T>> getProvider(Class<T> type, String qualifier) {
        // check primary
        TypeQualifier primary = new TypeQualifier(type, qualifier, true);
        List<DoctorProvider<?>> primaryList = index.get(primary);
        if (primaryList != null) {
            return Optional.of((DoctorProvider<T>) primaryList.getFirst());
        }

        // check secondary
        TypeQualifier secondary = new TypeQualifier(type, qualifier, false);
        List<DoctorProvider<?>> secondaryList = index.get(secondary);
        if (secondaryList != null) {
            return Optional.of((DoctorProvider<T>) secondaryList.getFirst());
        }

        // not found
        return Optional.empty();
    }

    @SuppressWarnings("unchecked")
    <T> Stream<DoctorProvider<T>> getProviders(Class<T> type) {
        return index.entrySet()
                .stream()
                .filter(e -> e.getKey().primary)
                .flatMap(e -> e.getValue().stream())
                .filter(p -> p.allProvidedTypes().contains(type))
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
        return index.entrySet()
                .stream()
                .filter(e -> e.getKey().primary)
                .flatMap(e -> e.getValue().stream())
                .distinct();
    }

    int size() {
        return size;
    }

    private record TypeQualifier(Class<?> type, String qualifier, boolean primary) {
    }
}