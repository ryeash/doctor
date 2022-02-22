package vest.doctor.conf;

import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.function.Function;

/**
 * A facade in front of N configuration sources that manages the priority and parsing of
 * property values.
 */
public interface ConfigurationFacade {

    char NESTING_DELIMITER = '.';

    /**
     * Add a source to this facade.
     *
     * @param source the source to add
     * @return this object
     */
    ConfigurationFacade addSource(ConfigurationSource source);

    /**
     * Get a property value.
     *
     * @param propertyName the property name
     * @return the value or null if no source has a value for the given name
     */
    String get(String propertyName);

    /**
     * Get a property value with a fallback.
     *
     * @param propertyName the property name
     * @param defaultValue the default value if no source has the given property
     * @return the value from configuration or the fallback value
     */
    String get(String propertyName, String defaultValue);

    /**
     * Get the property value, converted using the given function.
     *
     * @param propertyName the property name
     * @param converter    the mapping function to convert the property value into the desired type
     * @return the value or null if no source has a value for the given name
     */
    <T> T get(String propertyName, Function<String, T> converter);

    /**
     * Get the property value, converted using the given function, falling back to the default
     * value should the property not be set.
     *
     * @param propertyName the property name
     * @param defaultValue the default value if no source has the given property
     * @param converter    the mapping function to convert the property value into the desired type
     * @return the converted property value or the default value if it does not exists in configuration
     */
    <T> T get(String propertyName, T defaultValue, Function<String, T> converter);

    /**
     * Get a property value defined as an arrays of values.
     *
     * @param propertyName the property name
     * @return the property list, or an empty list if no source contains the property
     */
    List<String> getList(String propertyName);

    /**
     * Get a property value defined as an array of values.
     *
     * @param propertyName the property name
     * @param converter    the mapping function to convert the individual values in the array
     * @return the property list, with values converted using the function, or an empty list if no
     * source contains the property
     */
    <T> List<T> getList(String propertyName, Function<String, T> converter);

    /**
     * Get the property value defined as an array of values.
     *
     * @param propertyName the property name
     * @param defaultValue the fallback value should no source contain the property
     * @param converter    the mapping function to convert the individual values in the array
     * @return the property list, with values converted using the function, or the fallback list
     * if no source contains the property
     */
    <T> List<T> getList(String propertyName, List<T> defaultValue, Function<String, T> converter);

    /**
     * Get a property value defined as a set of values.
     *
     * @param propertyName the property name
     * @return the property set, or an empty set if no source contains the property
     */
    Set<String> getSet(String propertyName);

    /**
     * Get a property value defined as a set of values.
     *
     * @param propertyName the property name
     * @param converter    the mapping function to convert the individual values in the array
     * @return the property set, with values converted using the function, or an empty set if no
     * source contains the property
     */
    <T> Set<T> getSet(String propertyName, Function<String, T> converter);

    /**
     * Get the property value defined as a set of values.
     *
     * @param propertyName the property name
     * @param defaultValue the fallback value should no source contain the property
     * @param converter    the mapping function to convert the individual values in the array
     * @return the property set, with values converted using the function, or the fallback set
     * if no source contains the property
     */
    <T> Set<T> getSet(String propertyName, Set<T> defaultValue, Function<String, T> converter);

    /**
     * Get a nested configuration source, i.e. a nested configuration map.
     *
     * @param path the path to the configuration
     * @return the sub config object
     */
    ConfigurationFacade getSubConfiguration(String path);

    /**
     * Get a list of nested configuration sources, i.e. an array of nested configuration maps.
     *
     * @param path the path to the configuration array
     * @return the sub config list
     */
    List<ConfigurationFacade> getSubConfigurations(String path);

    /**
     * Resolve placeholder values in the given string. Placeholders are designated like `${property.name}` and can be
     * used in places like annotation string values to parameterize otherwise static values.
     *
     * @param value the value to fill
     * @return a new string with the placeholder values filled with property values
     */
    String resolvePlaceholders(String value);

    /**
     * Get a list of all first order property names at this level.
     */
    Collection<String> propertyNames();

    /**
     * Reload this configuration facade, calling {@link ConfigurationSource#reload()} on registered sources.
     */
    void reload();
}
