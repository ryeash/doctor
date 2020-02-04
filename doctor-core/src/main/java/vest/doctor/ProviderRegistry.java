package vest.doctor;

import java.lang.annotation.Annotation;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

/**
 * The primary means of interacting with the generated provider instances at runtime in application code.
 */
public interface ProviderRegistry {

    /**
     * The list of active modules the app was started with.
     */
    List<String> getActiveModules();

    /**
     * Register a new provider.
     *
     * @param provider the provider to register
     */
    void register(DoctorProvider<?> provider);

    /**
     * Get an instance of a provided type.
     *
     * @param type the type to get the instance of
     * @return an instance of the provided type
     * @throws IllegalArgumentException if the type can not be provided
     */
    <T> T getInstance(Class<T> type);

    /**
     * Get an instance of a provided type.
     *
     * @param type      the type to get the instance of
     * @param qualifier the qualifier of the instance
     * @return an instance of the provided type
     * @throws IllegalArgumentException if the type can not be provided
     */
    <T> T getInstance(Class<T> type, String qualifier);

    /**
     * Get a provider for the given type.
     *
     * @param type the type to get the provider for
     * @return a provider for the type
     * @throws IllegalArgumentException if the type can not be provided
     */
    <T> DoctorProvider<T> getProvider(Class<T> type);

    /**
     * Get a provider for the given type.
     *
     * @param type      the type to get the provider for
     * @param qualifier the qualifier of the provider
     * @return a provider for the type
     * @throws IllegalArgumentException if the type can not be provided
     */
    <T> DoctorProvider<T> getProvider(Class<T> type, String qualifier);

    /**
     * Get an optional provider for the given type. Unlike {@link #getProvider(Class)} this method does not throw
     * an exception if the type can not be provided.
     *
     * @param type the type to get the provider for
     * @return an optional provider for the type
     */
    default <T> Optional<DoctorProvider<T>> getProviderOpt(Class<T> type) {
        return getProviderOpt(type, null);
    }

    /**
     * Get an optional provider for the given type. Unlike {@link #getProvider(Class)} this method does not throw
     * an exception if the type can not be provided.
     *
     * @param type      the type to get the provider for
     * @param qualifier the qualifier of the provider
     * @return an optional provider for the type and qualifier
     */
    <T> Optional<DoctorProvider<T>> getProviderOpt(Class<T> type, String qualifier);

    /**
     * Get all providers that can satisfy the given type.
     *
     * @param type the type to get provider for
     * @return a stream of all providers for the given type
     */
    <T> Stream<DoctorProvider<T>> getProviders(Class<T> type);

    /**
     * Get all providers that can satisfy the given type and qualifier.
     *
     * @param type      the type to get providers for
     * @param qualifier the required qualifier for the providers
     * @return a stream of all provider for the given type and qualifier
     */
    <T> Stream<DoctorProvider<T>> getProviders(Class<T> type, String qualifier);

    /**
     * Get all providers marked with the get annotation.
     *
     * @param annotationType the annotation to filter on
     * @return a stream of all providers for the given annotation
     */
    Stream<DoctorProvider<?>> getProvidersWithAnnotation(Class<? extends Annotation> annotationType);

    /**
     * Determine if the given type can be satisfied by a registered provider.
     *
     * @param type the type to check
     * @return true if a provider exists that can satisfy the given type
     */
    boolean hasProvider(Class<?> type);

    /**
     * Determine if the given type and qualifier can be satisfied by a registered provider.
     *
     * @param type      the type to check
     * @param qualifier the qualifier to check
     * @return true if a provider exists that can satisfy the given type and qualfier
     */
    boolean hasProvider(Class<?> type, String qualifier);

    /**
     * Get the {@link ConfigurationFacade} associated with this registry.
     *
     * @return the configuration facade
     */
    ConfigurationFacade configuration();

    /**
     * Convenience method for <code>configuration().resolvePlaceholders()</code>.
     *
     * @param string the string to resolve
     * @return a new string with placeholders resolved using properties
     */
    String resolvePlaceholders(String string);

}