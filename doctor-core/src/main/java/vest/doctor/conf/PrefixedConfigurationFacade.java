package vest.doctor.conf;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

public class PrefixedConfigurationFacade implements ConfigurationFacade {
    private final String prefix;
    private final ConfigurationFacade delegate;

    public PrefixedConfigurationFacade(String prefix, ConfigurationFacade delegate) {
        this.prefix = prefix;
        this.delegate = delegate;
    }

    @Override
    public ConfigurationFacade addSource(ConfigurationSource source) {
        return delegate.addSource(source);
    }

    @Override
    public String get(String propertyName) {
        return delegate.get(prefix + propertyName);
    }

    @Override
    public String get(String propertyName, String defaultValue) {
        return delegate.get(prefix + propertyName, defaultValue);
    }

    @Override
    public <T> T get(String propertyName, Function<String, T> converter) {
        return delegate.get(prefix + propertyName, converter);
    }

    @Override
    public <T> T get(String propertyName, T defaultValue, Function<String, T> converter) {
        return delegate.get(prefix + propertyName, defaultValue, converter);
    }

    @Override
    public List<String> getList(String propertyName) {
        return delegate.getList(prefix + propertyName);
    }

    @Override
    public <T> List<T> getList(String propertyName, Function<String, T> converter) {
        return delegate.getList(prefix + propertyName, converter);
    }

    @Override
    public <T> List<T> getList(String propertyName, List<T> defaultValue, Function<String, T> converter) {
        return delegate.getList(prefix + propertyName, defaultValue, converter);
    }

    @Override
    public Set<String> getSet(String propertyName) {
        return delegate.getSet(prefix + propertyName);
    }

    @Override
    public <T> Set<T> getSet(String propertyName, Function<String, T> converter) {
        return delegate.getSet(prefix + propertyName, converter);
    }

    @Override
    public <T> Set<T> getSet(String propertyName, Set<T> defaultValue, Function<String, T> converter) {
        return delegate.getSet(prefix + propertyName, defaultValue, converter);
    }

    @Override
    public String resolvePlaceholders(String value) {
        return delegate.resolvePlaceholders(value);
    }

    @Override
    public Collection<String> getSubGroups(String prefix) {
        return delegate.getSubGroups(prefix + prefix);
    }

    @Override
    public Collection<String> getSubGroups(String prefix, String terminal) {
        return delegate.getSubGroups(prefix + prefix, terminal);
    }

    @Override
    public ConfigurationFacade prefix(String prefix) {
        return delegate.prefix(this.prefix + prefix);
    }

    @Override
    public Collection<String> propertyNames() {
        return delegate.propertyNames()
                .stream()
                .filter(name -> name.startsWith(prefix))
                .collect(Collectors.toList());
    }

    @Override
    public void reload() {
        delegate.reload();
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        for (String propertyName : propertyNames()) {
            sb.append(propertyName).append('=');
            if (Arrays.stream(ConfigurationFacade.REDACT_KEYS).anyMatch(propertyName.toUpperCase()::contains)) {
                sb.append("[REDACTED]");
            } else {
                sb.append(get(propertyName));
            }
            sb.append('\n');
        }
        return sb.toString();
    }
}
