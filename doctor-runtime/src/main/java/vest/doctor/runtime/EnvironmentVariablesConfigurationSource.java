package vest.doctor.runtime;

import vest.doctor.ConfigurationSource;

/**
 * Configuration source that gets properties from {@link System#getenv(String)}.
 */
public class EnvironmentVariablesConfigurationSource implements ConfigurationSource {
    @Override
    public String get(String fullyQualifiedPropertyName) {
        return System.getenv(fullyQualifiedPropertyName);
    }

    @Override
    public Iterable<String> propertyNames() {
        return System.getenv().keySet();
    }
}
