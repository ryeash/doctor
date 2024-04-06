package vest.doctor.conf;

import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.function.Function;

/**
 * Wraps a {@link ConfigurationFacade} to disable the {@link #addSource(ConfigurationSource)} method.
 */
public class UnmodifiableConfigurationFacade implements ConfigurationFacade {
    private final ConfigurationFacade delegate;

    public UnmodifiableConfigurationFacade(ConfigurationFacade delegate) {
        this.delegate = delegate;
    }

    @Override
    public ConfigurationFacade addSource(ConfigurationSource source) {
        throw new UnsupportedOperationException();
    }

    @Override
    public String get(String propertyName) {
        return delegate.get(propertyName);
    }

    @Override
    public String get(String propertyName, String defaultValue) {
        return delegate.get(propertyName, defaultValue);
    }

    @Override
    public <T> T get(String propertyName, Function<String, T> converter) {
        return delegate.get(propertyName, converter);
    }

    @Override
    public <T> T get(String propertyName, T defaultValue, Function<String, T> converter) {
        return delegate.get(propertyName, defaultValue, converter);
    }

    @Override
    public List<String> getList(String propertyName) {
        return delegate.getList(propertyName);
    }

    @Override
    public <T> List<T> getList(String propertyName, Function<String, T> converter) {
        return delegate.getList(propertyName, converter);
    }

    @Override
    public <T> List<T> getList(String propertyName, List<T> defaultValue, Function<String, T> converter) {
        return delegate.getList(propertyName, defaultValue, converter);
    }

    @Override
    public Set<String> getSet(String propertyName) {
        return delegate.getSet(propertyName);
    }

    @Override
    public <T> Set<T> getSet(String propertyName, Function<String, T> converter) {
        return delegate.getSet(propertyName, converter);
    }

    @Override
    public <T> Set<T> getSet(String propertyName, Set<T> defaultValue, Function<String, T> converter) {
        return delegate.getSet(propertyName, defaultValue, converter);
    }

    @Override
    public String resolvePlaceholders(String value) {
        return delegate.resolvePlaceholders(value);
    }

    @Override
    public Collection<String> getSubGroups(String prefix) {
        return delegate.getSubGroups(prefix);
    }

    @Override
    public Collection<String> getSubGroups(String prefix, String terminal) {
        return delegate.getSubGroups(prefix, terminal);
    }

    @Override
    public ConfigurationFacade prefix(String prefix) {
        return delegate.prefix(prefix);
    }

    @Override
    public Collection<String> propertyNames() {
        return delegate.propertyNames();
    }

    @Override
    public void reload() {
        delegate.reload();
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "{" + delegate.toString() + "}";
    }
}
