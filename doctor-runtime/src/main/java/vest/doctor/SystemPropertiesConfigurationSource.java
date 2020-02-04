package vest.doctor;

/**
 * Configuration source that gets properties from {@link System#getProperty(String)}.
 */
public class SystemPropertiesConfigurationSource implements ConfigurationSource {
    @Override
    public String get(String fullyQualifiedPropertyName) {
        return System.getProperty(fullyQualifiedPropertyName);
    }

    @Override
    public Iterable<String> propertyNames() {
        return System.getProperties().stringPropertyNames();
    }
}
