package vest.doctor.conf;

import java.util.Collection;
import java.util.List;

/**
 * A source of property values. Can be e.g. a properties file or an in-memory map.
 */
public interface ConfigurationSource {

    /**
     * Get the string property value.
     *
     * @param propertyName the name of the property
     * @return the property value or null if this source does not have a property for the given name
     */
    String get(String propertyName);

    /**
     * Get a list of property values.
     *
     * @param propertyName the name of the property
     * @return the property value or null if this source does not have a property for the given name
     */
    List<String> getList(String propertyName);

    /**
     * Get a nested configuration source, i.e. a nested configuration map.
     *
     * @param path the path to the configuration
     * @return the sub config object or null if this source does not have a property map under the given name
     */
    ConfigurationSource getSubConfiguration(String path);

    /**
     * Get a list of nested configuration sources, i.e. an array of nested configuration maps.
     *
     * @param path the path to the configuration array
     * @return the sub config list or null if this source does not have a property map under the given name
     */
    List<ConfigurationSource> getSubConfigurations(String path);

    /**
     * The names of all properties at this level of the configuration source.
     *
     * @return a collection of property names
     */
    Collection<String> propertyNames();

    /**
     * Reload this configuration source.
     */
    void reload();
}
