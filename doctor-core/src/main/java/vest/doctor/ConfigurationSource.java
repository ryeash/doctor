package vest.doctor;

/**
 * A source of property values.
 */
public interface ConfigurationSource {

    /**
     * Get a property value.
     *
     * @param fullyQualifiedPropertyName the full name of the property
     * @return the property value, or null if no property is found
     */
    String get(String fullyQualifiedPropertyName);

    /**
     * Get an iterable for all property names contained within this source.
     */
    Iterable<String> propertyNames();

    /**
     * Reload this source. It is up to the implementation to decide what this means.
     */
    default void reload() {
    }
}
