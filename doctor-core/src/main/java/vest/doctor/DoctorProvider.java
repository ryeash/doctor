package vest.doctor;

import jakarta.inject.Provider;

import java.lang.annotation.Annotation;
import java.util.Collections;
import java.util.List;

/**
 * Extension of the {@link Provider} interface to add additional metadata for the provided tye.
 */
public interface DoctorProvider<T> extends Provider<T> {

    /**
     * The primary provided type.
     */
    Class<T> type();

    /**
     * The qualifier, or null if there isn't one.
     */
    String qualifier();

    /**
     * The scope of the provider, or null is there isn't one.
     */
    Class<? extends Annotation> scope();

    /**
     * All provided types. This is a list of all interfaces and super classes that the provided
     * type can satisfy as injection targets. This list will always include the class returned
     * by {@link #type()}This will not include {@link Object}.
     */
    List<Class<?>> allProvidedTypes();

    /**
     * All annotation types attached to the provided type.
     */
    default List<Class<? extends Annotation>> allAnnotationTypes() {
        return Collections.emptyList();
    }

    /**
     * The modules that activate this provider.
     */
    default List<String> modules() {
        return Collections.emptyList();
    }

    /**
     * Check all dependencies are met by the current state of the given {@link ProviderRegistry}.
     */
    void validateDependencies(ProviderRegistry providerRegistry);
}
