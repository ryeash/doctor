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
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.stream.Stream;

final class ProviderIndex {

    private final Node root = new Node();
    private final Map<Class<?>, List<Node>> inverse = new HashMap<>(128);
    private final Map<Class<? extends Annotation>, Collection<DoctorProvider<?>>> annotationTypeToProvider = new HashMap<>(128);
    private final Lock writeLock = new ReentrantLock();
    private int size = 0;

    void setProvider(DoctorProvider<?> provider) {
        writeLock.lock();
        try {
            Node temp = root;
            for (Class<?> type : provider.allProvidedTypes()) {
                temp = temp.getOrCreate(type);
                inverse.computeIfAbsent(type, v -> new ArrayList<>(3)).add(temp);
            }
            temp.putProvider(provider);

            for (Class<? extends Annotation> annotation : provider.allAnnotationTypes()) {
                annotationTypeToProvider.computeIfAbsent(annotation, a -> new HashSet<>(16)).add(provider);
            }
            size++;
        } finally {
            writeLock.unlock();
        }
    }

    DoctorProvider<?> getProvider(Class<?> type, String qualifier) {
        return Optional.ofNullable(inverse.get(type))
                .orElse(Collections.emptyList())
                .stream()
                .map(n -> n.getProvider(qualifier))
                .filter(Objects::nonNull)
                .findFirst()
                .orElse(null);
    }

    Stream<DoctorProvider<?>> getProviders(Class<?> type) {
        return Optional.ofNullable(inverse.get(type))
                .orElse(Collections.emptyList())
                .stream()
                .flatMap(Node::getProviders)
                .distinct();
    }

    Stream<DoctorProvider<?>> getProvidersWithAnnotation(Class<? extends Annotation> type) {
        return annotationTypeToProvider.getOrDefault(type, Collections.emptyList()).stream();
    }

    Stream<DoctorProvider<?>> allProviders() {
        return root.getProviders();
    }

    int size() {
        return size;
    }

    private static final class Node {
        private Map<Class<?>, Node> sub;
        private Map<String, DoctorProvider<?>> providers;

        void putProvider(DoctorProvider<?> provider) {
            if (providers == null) {
                providers = new HashMap<>(16);
            }
            if (providers.containsKey(provider.qualifier())) {
                throw new IllegalArgumentException("there is already a provider registered under: " + provider.qualifier() + ":" + provider.type());
            }
            providers.put(provider.qualifier(), provider);
        }

        Node getOrCreate(Class<?> type) {
            if (sub == null) {
                sub = new HashMap<>(16);
            }
            return sub.computeIfAbsent(type, t -> new Node());
        }

        DoctorProvider<?> getProvider(String qualifier) {
            if (providers != null && providers.containsKey(qualifier)) {
                DoctorProvider<?> provider = providers.get(qualifier);
                if (provider != null) {
                    return provider;
                }
            }
            if (sub != null) {
                for (Node value : sub.values()) {
                    DoctorProvider<?> provider = value.getProvider(qualifier);
                    if (provider != null) {
                        return provider;
                    }
                }
            }
            return null;
        }

        Stream<DoctorProvider<?>> getProviders() {
            Stream<DoctorProvider<?>> prim = Stream.empty();
            if (providers != null) {
                prim = providers.values().stream();
            }
            Stream<DoctorProvider<?>> desc = Stream.empty();
            if (sub != null) {
                desc = sub.values()
                        .stream()
                        .flatMap(Node::getProviders);
            }
            return Stream.concat(prim, desc);
        }
    }

}