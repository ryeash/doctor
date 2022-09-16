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

    /**
     * The nesting delimiter used by various methods for parsing subgroups of configuration
     * values; e.g. properties names are assumed to use this value to namespace properties
     * like "http.tcp.selector.threads=1".
     */
    char NESTING_DELIMITER = '.';

    /**
     * The delimiter used to define a list of property values.
     */
    char LIST_DELIMITER = ',';

    /**
     * A set of strings that categorizes a property name as "sensitive" and thus it's value
     * should not be included in a debug string.
     */
    String[] REDACT_KEYS = new String[]{"PASS", "KEY", "SECRET", "TOKEN", "AUTH"};

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
     * @return the converted property value or the default value if it does not exist in configuration
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
     * Resolve placeholder values in the given string. Placeholders are designated like `${property.name}` and can be
     * used in places like annotation string values to parameterize otherwise static values.
     *
     * @param value the value to fill
     * @return a new string with the placeholder values filled with property values
     */
    String resolvePlaceholders(String value);

    /**
     * Get a collection of unique property subgroups that exist in this configuration facade.
     * Equivalent to calling <code>getSubGroups(prefix, String.valueOf(ConfigurationFacade.NESTING_DELIMITER))</code>.
     *
     * @param prefix the subgroup prefix
     * @return a collection of subgroups
     * @see #getSubGroups(String, String)
     */
    Collection<String> getSubGroups(String prefix);

    /**
     * Get a collection of unique property subgroups that exist in this configuration facade.
     * <p>
     * For example, with properties like:
     * <pre>
     * executors.fixed.threadCount=1
     * executors.background.threadCount=3
     * executors.io.threadCount=32
     * </pre>
     * Calling <code>config.getSubGroups("executors.", ".")</code> will return the collection
     * <code>["fixed", "background", "io"]</code>.
     *
     * @param prefix   the subgroup prefix
     * @param terminal the terminal value for the subgroup
     * @return a collection of subgroups
     */
    Collection<String> getSubGroups(String prefix, String terminal);

    /**
     * Get a view into this configuration facade that will prefix all requests to get properties
     * with the given prefix.
     *
     * @param prefix the prefix to add to property requests in the returned configuration facade
     * @return a configuration facade that uses the given prefix for property values
     */
    ConfigurationFacade prefix(String prefix);

    /**
     * Get a list of all property names this configuration facade knows about.
     *
     * @return a collection of property names
     */
    Collection<String> propertyNames();

    /**
     * Reload this configuration facade, calling {@link ConfigurationSource#reload()} on registered sources.
     */
    void reload();
}
