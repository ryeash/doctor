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
     * @return the converted property value or null if no property is found
     */
    <T> T get(String fullyQualifiedPropertyName, Function<String, T> converter);

    /**
     * Get a property value, converting it to another value using the given converter function.
     *
     * @param fullyQualifiedPropertyName the full name of the property
     * @param defaultValue               the default value to fall back to
     * @param converter                  the converter to use to convert the property value
     * @return the converted property value, or the default value if no property is found
     */
    <T> T get(String fullyQualifiedPropertyName, T defaultValue, Function<String, T> converter);

    /**
     * Get a collection of property values, delimited by commas. Individual values will be converted using the given
     * converter function.
     *
     * @param fullyQualifiedPropertyName the full name of the property
     * @param converter                  the converter to use to convert the property value
     * @return the property values, or an empty collection if no property is found
     */
    <T> Collection<T> getCollection(String fullyQualifiedPropertyName, Function<String, T> converter);

    /**
     * Get a list of property values, delimited by commas. Individual values will be converted using the given
     * converter function.
     *
     * @param fullyQualifiedPropertyName the full name of the property
     * @param converter                  the converter to use to convert the property value
     * @return the property values, or an empty list if no property is found
     */
    <T> List<T> getList(String fullyQualifiedPropertyName, Function<String, T> converter);

    /**
     * Get a set of property values, delimited by commas. Individual values will be converted using the given
     * converter function.
     *
     * @param fullyQualifiedPropertyName the full name of the property
     * @param converter                  the converter to use to convert the property value
     * @return the property values, or an empty set if no property is found
     */
    <T> Set<T> getSet(String fullyQualifiedPropertyName, Function<String, T> converter);

    /**
     * Get a stream of property values, delimited by commas. Individual values will be converted using the given
     * converter function.
     *
     * @param fullyQualifiedPropertyName the full name of the property
     * @param converter                  the converter to use to convert the property value
     * @return the property values, or an empty stream if no property is found
     */
    <T> Stream<T> getSplit(String fullyQualifiedPropertyName, Function<String, T> converter);

    /**
     * Resolve placeholder values in the given string. Placeholders are designated like `${property.name}` and can be
     * used in places like annotation string values to parameterize otherwise static values.
     *
     * @param value the value to fill
     * @return a new string with the placeholder values filled with property values
     */
    String resolvePlaceholders(String value);

    /**
     * Convert this configuration facade into a {@link Properties} object.
     *
     * @return this facade as a properties object
     */
    Properties toProperties();
}
