package vest.doctor;

import jakarta.inject.Provider;
import vest.doctor.conf.ConfigurationFacade;

import java.lang.annotation.Annotation;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

/**
 * The primary means of interacting with the generated provider instances at runtime in application code.
 */
public interface ProviderRegistry {

    /**
     * The list of active modules.
     */
    List<String> getActiveModules();

    /**
     * Register a new provider.
     *
     * @param provider the provider to register
     * @throws IllegalArgumentException if there is already a provider that satisfies the given provider's type and
     *                                  qualifier
     */
    void register(DoctorProvider<?> provider);

    /**
     * Get an instance of a provided type.
     *
     * @param type the provided type to get
     * @return an instance of the provided type
     * @throws IllegalArgumentException if the type can not be provided
     */
    default <T> T getInstance(Class<T> type) {
        return getProvider(type, null).get();
    }

    /**
     * Get an instance of a provided type.
     *
     * @param type      the provided type to get
     * @param qualifier the qualifier of the instance
     * @return an instance of the provided type
     * @throws IllegalArgumentException if the type can not be provided
     */
    default <T> T getInstance(Class<T> type, String qualifier) {
        return getProvider(type, qualifier).get();
    }

    /**
     * Optionally get an instance of a provided type, without throwing an exception if the type is not provided.
     *
     * @param type the provided type to get
     * @return an optional instance of the provided type
     */
    default <T> Optional<T> getInstanceOpt(Class<T> type) {
        return getProviderOpt(type, null).map(Provider::get);
    }

    /**
     * Optionally get an instance of a provided type, without throwing an exception if the type is not provided.
     *
     * @param type      the provided type to get
     * @param qualifier the qualifier of the instance
     * @return an optional instance of the provided type
     */
    default <T> Optional<T> getInstanceOpt(Class<T> type, String qualifier) {
        return getProviderOpt(type, qualifier).map(Provider::get);
    }

    /**
     * Get a provider for the given type.
     *
     * @param type the provided type to get
     * @return a provider for the type
     * @throws IllegalArgumentException if the type can not be provided
     */
    <T> DoctorProvider<T> getProvider(Class<T> type);

    /**
     * Get a provider for the given type.
     *
     * @param type      the provided type to get
     * @param qualifier the qualifier of the provider
     * @return a provider for the type
     * @throws IllegalArgumentException if the type can not be provided
     */
    <T> DoctorProvider<T> getProvider(Class<T> type, String qualifier);

    /**
     * Get an optional provider for the given type. Unlike {@link #getProvider(Class)} this method does not throw
     * an exception if the type can not be provided.
     *
     * @param type the provided type to get
     * @return an optional provider for the type
     */
    default <T> Optional<DoctorProvider<T>> getProviderOpt(Class<T> type) {
        return getProviderOpt(type, null);
    }

    /**
     * Get an optional provider for the given type. Unlike {@link #getProvider(Class)} this method does not throw
     * an exception if the type can not be provided.
     *
     * @param type      the provided type to get
     * @param qualifier the qualifier of the provider
     * @return an optional provider for the type and qualifier
     */
    <T> Optional<DoctorProvider<T>> getProviderOpt(Class<T> type, String qualifier);

    /**
     * Get all providers that can satisfy the given type.
     *
     * @param type the provided type to get
     * @return a stream of all providers for the given type
     */
    <T> Stream<DoctorProvider<T>> getProviders(Class<T> type);

    /**
     * Get all providers that can satisfy the given type and qualifier.
     *
     * @param type      the provided type to get
     * @param qualifier the required qualifier for the providers
     * @return a stream of all provider for the given type and qualifier
     */
    <T> Stream<DoctorProvider<T>> getProviders(Class<T> type, String qualifier);

    /**
     * Get all provided instances that can satisfy the given type.
     *
     * @param type the provided type to get
     * @return a stream of all instances for the given type
     */
    <T> Stream<T> getInstances(Class<T> type);

    /**
     * Get all provided instances that can satisfy the given type and qualifier.
     *
     * @param type      the provided type to get
     * @param qualifier the required qualifier for the providers
     * @return a stream of all instances for the given type and qualifier
     */
    <T> Stream<T> getInstances(Class<T> type, String qualifier);

    /**
     * Get all providers registered.
     *
     * @return a stream of providers
     */
    Stream<DoctorProvider<?>> allProviders();

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
     * @return true if a provider exists that can satisfy the given type (and a null qualifier)
     */
    boolean hasProvider(Class<?> type);

    /**
     * Determine if the given type and qualifier can be satisfied by a registered provider.
     *
     * @param type      the type to check
     * @param qualifier the qualifier to check
     * @return true if a provider exists that can satisfy the given type and qualifier
     */
    boolean hasProvider(Class<?> type, String qualifier);

    /**
     * Get the {@link ConfigurationFacade} associated with this registry.
     *
     * @return the configuration facade
     */
    ConfigurationFacade configuration();

    /**
     * Convenience method for <code>configuration().resolvePlaceholders(string)</code>.
     *
     * @param string the string to resolve
     * @return a new string with placeholders resolved using properties
     */
    String resolvePlaceholders(String string);
}
