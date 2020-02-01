package vest.doctor;

import java.util.Collection;
import java.util.List;
import java.util.Properties;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Stream;

/**
 * Provides unified access to properties by corralling multiple {@link ConfigurationSource}s together and
 * querying them in the order registered.
 */
public interface ConfigurationFacade extends ConfigurationSource {

    /**
     * Add a new {@link ConfigurationSource} to the list of places.
     *
     * @param source the source to add
     * @return this facade
     */
    ConfigurationFacade addSource(ConfigurationSource source);

    /**
     * Trigger a reload for all configuration sources.
     */
    void reload();

    /**
     * Get a property value.
     *
     * @param fullyQualifiedPropertyName the full name of the property
     * @param defaultValue               the default value to fall back to
     * @return the property value, or the default value if no property is found
     */
    String get(String fullyQualifiedPropertyName, String defaultValue);

    /**
     * Get a property value, converting it to another value using the given converter function.
     *
     * @param fullyQualifiedPropertyName the full name of the property
     * @param converter                  the converter to use to convert the property value
     * @return the converted property value
     */
    <T> T get(String fullyQualifiedPropertyName, Function<String, T> converter);

    /**
     * Get a property value, converting it to another value using the given converter function.
     *
     * @param fullyQualifiedPropertyName the full name of the property.
     * @param defaultValue
     * @param converter
     * @param <T>
     * @return
     */
    <T> T get(String fullyQualifiedPropertyName, T defaultValue, Function<String, T> converter);

    <T> Collection<T> getCollection(String fullyQualfiedPropertyName, Function<String, T> converter);

    <T> List<T> getList(String fullyQualfiedPropertyName, Function<String, T> converter);

    <T> Set<T> getSet(String fullyQualfiedPropertyName, Function<String, T> converter);

    <T> Stream<T> getSplit(String fullyQualfiedPropertyName, Function<String, T> converter);

    String resolvePlaceholders(String value);

    Properties toProperties();
}
