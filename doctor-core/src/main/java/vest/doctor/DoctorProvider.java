package vest.doctor;

import javax.inject.Provider;
import java.lang.annotation.Annotation;
import java.util.Collections;
import java.util.List;

public interface DoctorProvider<T> extends Provider<T> {

    /**
     * The type provided.
     */
    Class<T> type();

    /**
     * The provider qualifier, or null if there isn't one.
     */
    String qualifier();

    /**
     * The scope of the provider, or null is there isn't one.
     */
    Class<? extends Annotation> scope();

    /**
     * All provided types.
     */
    List<Class<?>> allProvidedTypes();

    /**
     * All annotation types attached to the provided type
     */
    default List<Class<? extends Annotation>> allAnnotationTypes() {
        return Collections.emptyList();
    }

    /**
     * The modules that this provider are active with.
     */
    default List<String> modules() {
        return Collections.emptyList();
    }

    /**
     * Check all dependencies are met by the current state of the {@link ProviderRegistry}.
     */
    void validateDependencies(ProviderRegistry providerRegistry);
}
