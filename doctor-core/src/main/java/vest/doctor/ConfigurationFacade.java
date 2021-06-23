package vest.doctor;

import java.util.List;
import java.util.Properties;
import java.util.Set;
import java.util.function.Function;

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
     * @param propertyName the name of the property
     * @param defaultValue the value to return if the property is not set
     * @return the property value, or the default value if the property is not found
     */
    String get(String propertyName, String defaultValue);

    /**
     * Get a property value, converting it to another value using the given converter function.
     *
     * @param propertyName the name of the property
     * @param converter    the converter to use to convert the property value
     * @return the converted property value or null if the property is not found
     */
    <T> T get(String propertyName, Function<String, T> converter);

    /**
     * Get a property value, converting it to another value using the given converter function.
     *
     * @param propertyName the name of the property
     * @param defaultValue the value to return if the property is not set
     * @param converter    the converter to use to convert the property value
     * @return the converted property value, or the default value if the property is not found
     */
    <T> T get(String propertyName, T defaultValue, Function<String, T> converter);

    /**
     * Get a list of property values, delimited by commas. Individual values will be converted using the given
     * converter function.
     *
     * @param propertyName the name of the property
     * @param converter    the converter to use to convert the property value
     * @return the property values, or an empty list if the property is not found
     */
    <T> List<T> getList(String propertyName, Function<String, T> converter);

    /**
     * Get a list of property values, delimited by commas. Individual values will be converted using the given
     * converter function.
     *
     * @param propertyName the name of the property
     * @param defaultValue the value to return if the property is not set
     * @param converter    the converter to use to convert the property value
     * @return the property values, or the default value if the property is not found
     */
    <T> List<T> getList(String propertyName, List<T> defaultValue, Function<String, T> converter);

    /**
     * Get a set of property values, delimited by commas. Individual values will be converted using the given
     * converter function.
     *
     * @param propertyName the name of the property
     * @param converter    the converter to use to convert the property value
     * @return the property values, or an empty set if the property is not found
     */
    <T> Set<T> getSet(String propertyName, Function<String, T> converter);

    /**
     * Get a set of property values, delimited by commas. Individual values will be converted using the given
     * converter function.
     *
     * @param propertyName the name of the property
     * @param defaultValue the value to return if the property is not set
     * @param converter    the converter to use to convert the property value
     * @return the property values, or the default value if the property is not found
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
     * Convert this configuration facade into a {@link Properties} object.
     *
     * @return this facade as a properties object
     */
    Properties toProperties();

    /**
     * Get the unique set of property groups with the given prefix and terminal ".". Alias
     * for {@link #uniquePropertyGroups(String, String) uniquePropertyGroups(prefix, ".")}.
     *
     * @param prefix the prefix for the grouping, e.g. "executors."
     * @return a set of unique property sub groups
     */
    Set<String> uniquePropertyGroups(String prefix);

    /**
     * Get the unique set of property groups with the given prefix and suffix, scraping
     * the names of a sub-grouping within the configured property values.
     * For example, with these properties:
     * <pre>
     * executors.background.type = fixed
     * executors.background.minThread = 4
     * executors.io.type = cached
     * executors.io.minThread = 16
     * </pre><br/>
     * calling <code>uniquePropertyGroups("executors.", ".")</code> will return
     * <code>["background", "io"]</code>.
     *
     * @param prefix   the prefix for the grouping, e.g. "executors."
     * @param terminal the terminal for the group, e.g. "."
     * @return a set of unique property sub groups
     */
    Set<String> uniquePropertyGroups(String prefix, String terminal);

    /**
     * Get a view into a subsection of the configuration. The returned configuration facade
     * will automatically prefix all property requests with the given prefix.
     * <p>
     * Example:
     * <pre>
     * ConfigurationFacade sub = facade.subsection("executors.default.");
     * sub.get("minThreads"); // this will look for the "executors.default.minThreads" property
     * </pre>
     *
     * @param prefix the subsection prefix
     * @return a ConfigurationFacade that is a view into the subsection's properties
     */
    ConfigurationFacade subsection(String prefix);
}
