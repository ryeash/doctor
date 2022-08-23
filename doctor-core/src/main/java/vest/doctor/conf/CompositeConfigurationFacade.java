package vest.doctor.conf;

import vest.doctor.runtime.FileLocation;
import vest.doctor.runtime.RuntimeUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;

/**
 * A configuration facade implementation that delegates property requests to
 * a list of {@link ConfigurationSource configuration sources}. The first non-null
 * property value returned by a source is used as the property value.
 */
public class CompositeConfigurationFacade implements ConfigurationFacade {

    public static final String PROPERTIES = "doctor.app.properties";

    private static final String MACRO_OPEN = "${";
    private static final String MACRO_CLOSE = "}";

    public static ConfigurationFacade defaultConfigurationFacade() {
        CompositeConfigurationFacade facade = new CompositeConfigurationFacade(new ArrayList<>());
        facade.addSource(new EnvironmentVariablesConfigurationSource());
        facade.addSource(new SystemPropertiesConfigurationSource());

        facade.getList(PROPERTIES, Function.identity())
                .stream()
                .map(FileLocation::new)
                .map(StructuredConfigurationSource::new)
                .forEach(facade::addSource);
        return facade;
    }

    private final List<ConfigurationSource> sources;

    public CompositeConfigurationFacade() {
        this(new ArrayList<>());
    }

    public CompositeConfigurationFacade(List<ConfigurationSource> sources) {
        this.sources = Objects.requireNonNull(sources);
    }

    @Override
    public ConfigurationFacade addSource(ConfigurationSource source) {
        sources.add(source);
        return this;
    }

    @Override
    public String get(String propertyName) {
        return get(propertyName, null, Function.identity());
    }

    @Override
    public String get(String propertyName, String defaultValue) {
        return get(propertyName, defaultValue, Function.identity());
    }

    @Override
    public <T> T get(String propertyName, Function<String, T> converter) {
        return get(propertyName, null, converter);
    }

    @Override
    public <T> T get(String propertyName, T defaultValue, Function<String, T> converter) {
        for (ConfigurationSource source : sources) {
            String s = source.get(propertyName);
            if (s != null) {
                return converter.apply(resolvePlaceholders(s));
            }
        }
        return defaultValue;
    }

    /**
     * Audit a property. Generates a debug string of where the property was found, where
     * it was found but not used (due to an override), and where the property was not present.
     *
     * @param propertyName the property to audit
     * @return a debug string for the property
     */
    public String audit(String propertyName) {
        StringBuilder sb = new StringBuilder();
        sb.append("Result:\n");
        sb.append("value: [").append(get(propertyName)).append("]\n");
        int i = 1;
        boolean found = false;
        for (ConfigurationSource source : sources) {
            String s = source.get(propertyName);
            sb.append(i++).append(". ").append(source).append(" - ");
            if (s != null) {
                if (!found) {
                    found = true;
                    sb.append("* found and used: ").append(s);
                } else {
                    sb.append("! found but overridden: ").append(s);
                }
            } else {
                sb.append("not found");
            }
            sb.append('\n');
        }
        return sb.toString();
    }

    @Override
    public List<String> getList(String propertyName) {
        return getList(propertyName, List.of(), Function.identity());
    }

    @Override
    public <T> List<T> getList(String propertyName, Function<String, T> converter) {
        return getList(propertyName, List.of(), converter);
    }

    @Override
    public <T> List<T> getList(String propertyName, List<T> defaultValue, Function<String, T> converter) {
        return getCol(propertyName, defaultValue, converter, ArrayList::new);
    }

    @Override
    public Set<String> getSet(String propertyName) {
        return getSet(propertyName, Set.of(), Function.identity());
    }

    @Override
    public <T> Set<T> getSet(String propertyName, Function<String, T> converter) {
        return getSet(propertyName, Set.of(), converter);
    }

    @Override
    public <T> Set<T> getSet(String propertyName, Set<T> defaultValue, Function<String, T> converter) {
        return getCol(propertyName, defaultValue, converter, LinkedHashSet::new);
    }

    private <C extends Collection<T>, T> C getCol(String propertyName, C defaultValue, Function<String, T> converter, Supplier<C> supplier) {
        for (ConfigurationSource source : sources) {
            String list = source.get(propertyName);
            if (list != null) {
                return RuntimeUtils.split(list, ConfigurationFacade.LIST_DELIMITER)
                        .stream()
                        .map(this::resolvePlaceholders)
                        .map(converter)
                        .collect(Collectors.toCollection(supplier));
            }
        }
        return defaultValue;
    }

    @Override
    public String resolvePlaceholders(String value) {
        if (value == null || value.isEmpty()) {
            return null;
        }
        if (!value.contains(MACRO_OPEN)) {
            return value;
        }
        StringBuilder sb = new StringBuilder();

        int prev = 0;
        int i;
        boolean changed = false;
        while ((i = value.indexOf(MACRO_OPEN, prev)) >= 0) {
            changed = true;
            sb.append(value, prev, i);
            i += MACRO_OPEN.length();
            prev = i;
            i = value.indexOf(MACRO_CLOSE, i);
            if (i < 0) {
                throw new IllegalArgumentException("unclosed macro statement in " + value);
            }
            String macro = value.substring(prev, i);
            String[] split = splitColon(macro);
            String subName = split[0];
            String defaultValue = split[1];

            String subValue = get(subName, defaultValue);
            if (subValue == null) {
                throw new IllegalArgumentException("missing interpolation value for property [" + subName + "] while trying to resolve placeholders in [" + value + "]");
            }
            sb.append(subValue);
            i += MACRO_CLOSE.length();
            prev = i;
        }
        if (!changed) {
            return value;
        }
        sb.append(value, prev, value.length());
        return sb.toString();
    }

    @Override
    public Collection<String> getSubGroups(String prefix) {
        return getSubGroups(prefix, String.valueOf(ConfigurationFacade.NESTING_DELIMITER));
    }

    @Override
    public Collection<String> getSubGroups(String prefix, String terminal) {
        return sources.stream()
                .map(ConfigurationSource::propertyNames)
                .flatMap(Collection::stream)
                .filter(name -> name.startsWith(prefix))
                .map(name -> {
                    int start = name.indexOf(prefix) + prefix.length();
                    int end = name.indexOf(terminal, start + 1);
                    return end > 0
                            ? name.substring(start, end)
                            : name.substring(start);
                })
                .collect(Collectors.toCollection(LinkedHashSet::new));
    }

    @Override
    public ConfigurationFacade prefix(String prefix) {
        return new PrefixedConfigurationFacade(prefix, this);
    }

    @Override
    public Collection<String> propertyNames() {
        return sources.stream()
                .map(ConfigurationSource::propertyNames)
                .flatMap(Collection::stream)
                .collect(Collectors.toCollection(LinkedHashSet::new));
    }

    @Override
    public void reload() {
        for (ConfigurationSource source : sources) {
            source.reload();
        }
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

    private static String[] splitColon(String str) {
        int i = str.indexOf(':');
        if (i < 0) {
            return new String[]{str, null};
        } else {
            return new String[]{str.substring(0, i), str.substring(i + 1)};
        }
    }
}
