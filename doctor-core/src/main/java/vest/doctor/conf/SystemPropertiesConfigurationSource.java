package vest.doctor.conf;

import vest.doctor.runtime.RuntimeUtils;

import java.util.Collection;
import java.util.List;

/**
 * Configuration source that gets properties from {@link System#getProperty(String)}.
 */
public class SystemPropertiesConfigurationSource implements ConfigurationSource {
    @Override
    public String get(String propertyName) {
        return System.getProperty(propertyName);
    }

    @Override
    public List<String> getList(String propertyName) {
        String s = get(propertyName);
        if (s == null) {
            return null;
        } else {
            return RuntimeUtils.split(s, ConfigurationFacade.LIST_DELIMITER);
        }
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
