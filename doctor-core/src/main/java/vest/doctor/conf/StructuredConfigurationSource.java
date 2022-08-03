package vest.doctor.conf;

import vest.doctor.runtime.FileLocation;

import java.util.Collection;
import java.util.List;
import java.util.Objects;

/**
 * A configuration source that reads a structured properties file and builds a map of properties.
 * <p>
 * Structured properties files allow for organizing/name-spacing properties in a less dense, easier to read manner.
 * <p>
 * Definition:
 * Nesting is defined by '{' and '}':
 * <code>
 * <pre>
 * root {
 *  child {
 *   propertyName = propertyValue
 *   ... more properties defined ...
 *  }
 * }
 * </pre>
 * </code>
 * This will be parsed as <code>root.child.propertyName = propertyValue</code>.
 * <br/>
 * <br/>
 * Reserved characters:
 * <pre>
 * '{' : used to nest a level deeper in the structure
 * '}' : used to close a nested structure
 * '=', ':' : sets the value of a property, e.g. name = value OR name: value
 * ';' : can be used to signify the end of a line (though it is not necessary)
 * '#' : comments
 * </pre>
 * Quoted strings using either ' or " can be used to escape reserved characters
 * e.g. <pre>name = "value contains { } = : and ;"</pre>
 * Quotes are necessary when interpolating values, i.e. values like: <pre>http://${referenced.property}/</pre>
 */
public class StructuredConfigurationSource implements ConfigurationSource {

    private final FileLocation propertyFile;
    private MapConfigurationSource delegate;

    public StructuredConfigurationSource(String location) {
        this(new FileLocation(location));
    }

    public StructuredConfigurationSource(FileLocation url) {
        this.propertyFile = Objects.requireNonNull(url, "the configuration url can not be null");
        reload();
    }

    @Override
    public String get(String propertyName) {
        return delegate.get(propertyName);
    }

    @Override
    public List<String> getList(String propertyName) {
        return delegate.getList(propertyName);
    }

    @Override
    public ConfigurationSource getSubConfiguration(String path) {
        return delegate.getSubConfiguration(path);
    }

    @Override
    public List<ConfigurationSource> getSubConfigurations(String path) {
        return delegate.getSubConfigurations(path);
    }

    @Override
    public Collection<String> propertyNames() {
        return delegate.propertyNames();
    }

    @Override
    public void reload() {
        this.delegate = new MapConfigurationSource(new StructuredPropertiesParser(propertyFile).parse());
    }
}