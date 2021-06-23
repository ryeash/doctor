package vest.doctor.runtime;

import vest.doctor.ConfigurationFacade;
import vest.doctor.ConfigurationSource;

import java.util.List;
import java.util.Properties;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Stream;

/**
 * Supports the {@link ConfigurationFacade#subsection(String)} functionality.
 */
class SubsectionConfigurationFacade implements ConfigurationFacade {

    private final ConfigurationFacade delegate;
    private final String propertyPrefix;

    public SubsectionConfigurationFacade(ConfigurationFacade delegate, String propertyPrefix) {
        this.delegate = delegate;
        this.propertyPrefix = propertyPrefix;
    }

    @Override
    public ConfigurationFacade addSource(ConfigurationSource source) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void reload() {
        delegate.reload();
    }

    @Override
    public String get(String propertyName) {
        return delegate.get(prependPrefix(propertyName));
    }

    @Override
    public Stream<String> propertyNames() {
        return delegate.propertyNames()
                .filter(name -> name.startsWith(propertyPrefix))
                .map(name -> name.substring(propertyPrefix.length()));
    }

    @Override
    public String get(String propertyName, String defaultValue) {
        return delegate.get(prependPrefix(propertyName), defaultValue);
    }

    @Override
    public <T> T get(String propertyName, Function<String, T> converter) {
        return delegate.get(prependPrefix(propertyName), converter);
    }

    @Override
    public <T> T get(String propertyName, T defaultValue, Function<String, T> converter) {
        return delegate.get(prependPrefix(propertyName), defaultValue, converter);
    }

    @Override
    public <T> List<T> getList(String propertyName, Function<String, T> converter) {
        return delegate.getList(prependPrefix(propertyName), converter);
    }

    @Override
    public <T> List<T> getList(String propertyName, List<T> defaultValue, Function<String, T> converter) {
        return delegate.getList(prependPrefix(propertyName), defaultValue, converter);
    }

    @Override
    public <T> Set<T> getSet(String propertyName, Function<String, T> converter) {
        return delegate.getSet(prependPrefix(propertyName), converter);
    }

    @Override
    public <T> Set<T> getSet(String propertyName, Set<T> defaultValue, Function<String, T> converter) {
        return delegate.getSet(prependPrefix(propertyName), defaultValue, converter);
    }

    @Override
    public String resolvePlaceholders(String value) {
        return delegate.resolvePlaceholders(value);
    }

    @Override
    public Properties toProperties() {
        return delegate.toProperties();
    }

    @Override
    public Set<String> uniquePropertyGroups(String prefix) {
        return delegate.uniquePropertyGroups(prependPrefix(prefix));
    }

    @Override
    public Set<String> uniquePropertyGroups(String prefix, String terminal) {
        return delegate.uniquePropertyGroups(prependPrefix(prefix), terminal);
    }

    @Override
    public ConfigurationFacade subsection(String prefix) {
        return delegate.subsection(prependPrefix(prefix));
    }

    private String prependPrefix(String propertyName) {
        return propertyPrefix + propertyName;
    }
}
