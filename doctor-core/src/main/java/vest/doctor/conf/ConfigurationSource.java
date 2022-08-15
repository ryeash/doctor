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
