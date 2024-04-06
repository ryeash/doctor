package vest.doctor;

import jakarta.inject.Provider;

import java.lang.annotation.Annotation;
import java.util.Collections;
import java.util.List;

/**
 * Extension of the {@link Provider} interface to add additional metadata for the provided tye.
 */
public interface DoctorProvider<T> extends Provider<T>, AutoCloseable {

    /**
     * The primary provided type.
     */
    @SuppressWarnings("unchecked")
    default Class<T> type() {
        return (Class<T>) typeInfo().getRawType();
    }

    /**
     * The {@link TypeInfo} for the primary provided type.
     */
    TypeInfo typeInfo();

    /**
     * The qualifier, or null if this provider doesn't have one.
     */
    String qualifier();

    /**
     * The scope of the provider.
     */
    Class<? extends Annotation> scope();

    /**
     * All provided types. This is a list of all interfaces and super classes that the provided
     * type can satisfy as injection targets. This list will always include the class returned
     * by {@link #type()}. This will not include {@link Object}.
     */
    List<Class<?>> allProvidedTypes();

    /**
     * The modules that activate this provider.
     */
    default List<String> modules() {
        return Collections.emptyList();
    }

    /**
     * Check all dependencies are met by the current state of the given {@link ProviderRegistry}.
     * Called automatically by the doctor runtime during initialization.
     */
    void validateDependencies(ProviderRegistry providerRegistry);

    /**
     * Destroy the given instance that was provided by this provider.
     *
     * @param instance the instance to destroy
     */
    void destroy(T instance) throws Exception;
}
