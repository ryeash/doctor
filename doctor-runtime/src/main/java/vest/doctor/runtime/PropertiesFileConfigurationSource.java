package vest.doctor.runtime;

import vest.doctor.ConfigurationSource;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.URI;
import java.util.Objects;
import java.util.Properties;

/**
 * Configuration source that reads properties from a file.
 */
public class PropertiesFileConfigurationSource implements ConfigurationSource {

    private final String propertiesFileLocation;
    private Properties properties;

    public PropertiesFileConfigurationSource(String propertiesFileLocation) {
        this.propertiesFileLocation = Objects.requireNonNull(propertiesFileLocation);
        reload();
    }

    @Override
    public void reload() {
        Properties properties = new Properties();
        try {
            properties.load(URI.create(propertiesFileLocation).toURL().openStream());
        } catch (IOException e) {
            throw new UncheckedIOException("error reading properties file: " + propertiesFileLocation, e);
        }
        this.properties = properties;
    }

    @Override
    public String get(String fullyQualifiedPropertyName) {
        return properties.getProperty(fullyQualifiedPropertyName);
    }

    @Override
    public Iterable<String> propertyNames() {
        return properties.stringPropertyNames();
    }
}
