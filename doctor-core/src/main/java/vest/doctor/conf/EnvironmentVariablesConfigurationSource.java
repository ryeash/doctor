package vest.doctor.conf;

import java.util.Collection;

/**
 * Configuration source that gets properties from {@link System#getenv(String)}.
 */
public class EnvironmentVariablesConfigurationSource implements ConfigurationSource {

    @Override
    public String get(String propertyName) {
        return System.getenv(propertyName);
    }

    @Override
    public Collection<String> propertyNames() {
        return System.getenv().keySet();
    }

    @Override
    public void reload() {
    }

    @Override
    public String toString() {
        return "EnvironmentVariablesConfigurationSource";
    }
}
