package vest.doctor.runtime;

import vest.doctor.ConfigurationSource;

import java.util.stream.Stream;

/**
 * Configuration source that gets properties from {@link System#getProperty(String)}.
 */
public class SystemPropertiesConfigurationSource implements ConfigurationSource {
    @Override
    public String get(String fullyQualifiedPropertyName) {
        return System.getProperty(fullyQualifiedPropertyName);
    }

    @Override
    public Stream<String> propertyNames() {
        return System.getProperties().stringPropertyNames().stream();
    }
}
