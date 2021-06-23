package vest.doctor.runtime;

import vest.doctor.ConfigurationSource;

import java.util.stream.Stream;

/**
 * Configuration source that gets properties from {@link System#getProperty(String)}.
 */
public class SystemPropertiesConfigurationSource implements ConfigurationSource {
    @Override
    public String get(String propertyName) {
        return System.getProperty(propertyName);
    }

    @Override
    public Stream<String> propertyNames() {
        return System.getProperties().stringPropertyNames().stream();
    }
}
