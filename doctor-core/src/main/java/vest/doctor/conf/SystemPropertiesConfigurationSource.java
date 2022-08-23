package vest.doctor.conf;

import java.util.Collection;

/**
 * Configuration source that gets properties from {@link System#getProperty(String)}.
 */
public class SystemPropertiesConfigurationSource implements ConfigurationSource {
    @Override
    public String get(String propertyName) {
        return System.getProperty(propertyName);
    }

    @Override
    public Collection<String> propertyNames() {
        return System.getProperties().stringPropertyNames();
    }

    @Override
    public void reload() {
    }

    @Override
    public String toString() {
        return "SystemPropertiesConfigurationSource";
    }
}
