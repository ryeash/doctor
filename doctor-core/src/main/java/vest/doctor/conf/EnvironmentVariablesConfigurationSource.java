package vest.doctor.conf;

import vest.doctor.runtime.RuntimeUtils;

import java.util.Collection;
import java.util.List;

/**
 * Configuration source that gets properties from {@link System#getenv(String)}.
 */
public class EnvironmentVariablesConfigurationSource implements ConfigurationSource {

    @Override
    public String get(String propertyName) {
        return System.getenv(propertyName);
    }

    @Override
    public List<String> getList(String propertyName) {
        String s = get(propertyName);
        if (s == null) {
            return null;
        } else {
            return RuntimeUtils.split(s, ',');
        }
    }

    @Override
    public ConfigurationSource getSubConfiguration(String path) {
        return null;
    }

    @Override
    public List<ConfigurationSource> getSubConfigurations(String path) {
        return null;
    }

    @Override
    public Collection<String> propertyNames() {
        return System.getenv().keySet();
    }

    @Override
    public void reload() {

    }
}
