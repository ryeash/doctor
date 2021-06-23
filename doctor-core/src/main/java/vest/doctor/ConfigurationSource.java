package vest.doctor;

import java.util.stream.Stream;

/**
 * A source of property values.
 */
public interface ConfigurationSource {

    /**
     * Get a property value.
     *
     * @param propertyName the name of the property
     * @return the property value, or null if no property is found
     */
    String get(String propertyName);

    /**
     * Get a stream of all property names contained within this source.
     */
    Stream<String> propertyNames();

    /**
     * Reload this source. It is up to the implementation to decide what this means.
     */
    default void reload() {
    }
}
