package vest.doctor.conf;

import vest.doctor.runtime.FileLocation;

import java.util.ArrayList;
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
            List<String> list = source.getList(propertyName);
            if (list != null) {
                return list.stream()
                        .map(this::resolvePlaceholders)
                        .map(converter)
                        .collect(Collectors.toCollection(supplier));
            }
        }
        return defaultValue;
    }

    @Override
    public ConfigurationFacade getSubConfiguration(String path) {
        return sources.stream()
                .map(s -> s.getSubConfiguration(path))
                .filter(Objects::nonNull)
                .collect(Collectors.collectingAndThen(Collectors.toList(), CompositeConfigurationFacade::new));
    }

    @Override
    public List<ConfigurationFacade> getSubConfigurations(String path) {
        return sources.stream()
                .map(s -> s.getSubConfigurations(path))
                .filter(Objects::nonNull)
                .map(CompositeConfigurationFacade::new)
                .map(ConfigurationFacade.class::cast)
                .toList();
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
    public Collection<String> propertyNames() {
        return sources.stream()
                .map(ConfigurationSource::propertyNames)
                .flatMap(Collection::stream)
                .collect(Collectors.toSet());
    }

    @Override
    public void reload() {
        for (ConfigurationSource source : sources) {
            source.reload();
        }
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
