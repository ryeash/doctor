package vest.doctor.runtime;

import vest.doctor.ConfigurationSource;

import java.util.stream.Stream;

/**
 * Configuration source that gets properties from {@link System#getenv(String)}.
 */
public class EnvironmentVariablesConfigurationSource implements ConfigurationSource {

    @Override
    public String get(String propertyName) {
        return System.getenv(propertyName);
    }

    @Override
    public Stream<String> propertyNames() {
        return System.getenv().keySet().stream();
    }
}
