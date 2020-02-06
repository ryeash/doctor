package vest.doctor;

import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Properties;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

/**
 * Default implementation of the {@link ConfigurationFacade}.
 */
public class DefaultConfigurationFacade implements ConfigurationFacade {

    /**
     * Creates a new configuration facade and automatically adds configuration sources (in query order):
     * - environment (via {@link System#getenv(String)})
     * - system properties (via {@link System#getProperty(String)}
     * - external properties files based on the value of 'doctor.app.properties' (using {@link StructuredConfigurationSource})
     *
     * @return a new configuration facade
     */
    public static ConfigurationFacade defaultConfigurationFacade() {
        ConfigurationFacade facade = new DefaultConfigurationFacade()
                .addSource(new EnvironmentVariablesConfigurationSource())
                .addSource(new SystemPropertiesConfigurationSource());

        facade.getSplit(DefaultConfigurationFacade.PROPERTIES, Function.identity())
                .map(props -> {
                    try {
                        return new File(props).toURI().toURL();
                    } catch (IOException e) {
                        throw new UncheckedIOException("error reading properties file: " + props, e);
                    }
                })
                .map(StructuredConfigurationSource::new)
                .forEach(facade::addSource);
        return facade;
    }

    public static final String PROPERTIES = "doctor.app.properties";
    private static final String MACRO_OPEN = "${";
    private static final String MACRO_CLOSE = "}";
    private static final char LIST_DELIMITER = ',';

    private final List<ConfigurationSource> sources = new ArrayList<>();

    @Override
    public ConfigurationFacade addSource(ConfigurationSource source) {
        sources.add(source);
        return this;
    }

    @Override
    public void reload() {
        for (ConfigurationSource source : sources) {
            source.reload();
        }
    }

    @Override
    public String get(String fullyQualifiedPropertyName) {
        for (ConfigurationSource source : sources) {
            String s = source.get(fullyQualifiedPropertyName);
            if (s != null) {
                try {
                    return resolvePlaceholders(s);
                } catch (IllegalArgumentException e) {
                    throw new IllegalArgumentException("failed to interpolate property: " + fullyQualifiedPropertyName, e);
                }
            }
        }
        return null;
    }

    @Override
    public Iterable<String> propertyNames() {
        return sources.stream()
                .map(ConfigurationSource::propertyNames)
                .flatMap(i -> StreamSupport.stream(i.spliterator(), false))
                .distinct()::iterator;
    }

    @Override
    public String get(String fullyQualifiedPropertyName, String defaultValue) {
        return Optional.ofNullable(get(fullyQualifiedPropertyName))
                .orElse(defaultValue);
    }

    @Override
    public <T> T get(String fullyQualifiedPropertyName, Function<String, T> converter) {
        return Optional.ofNullable(get(fullyQualifiedPropertyName))
                .map(converter)
                .orElse(null);
    }

    @Override
    public <T> T get(String fullyQualifiedPropertyName, T defaultValue, Function<String, T> converter) {
        return Optional.ofNullable(get(fullyQualifiedPropertyName))
                .map(converter)
                .orElse(defaultValue);
    }

    @Override
    public <T> Collection<T> getCollection(String fullyQualifiedPropertyName, Function<String, T> converter) {
        return getList(fullyQualifiedPropertyName, converter);
    }

    @Override
    public <T> List<T> getList(String fullyQualifiedPropertyName, Function<String, T> converter) {
        return getSplit(fullyQualifiedPropertyName, converter).collect(Collectors.toList());
    }

    @Override
    public <T> Set<T> getSet(String fullyQualifiedPropertyName, Function<String, T> converter) {
        return getSplit(fullyQualifiedPropertyName, converter).collect(Collectors.toCollection(LinkedHashSet::new));
    }

    @Override
    public <T> Stream<T> getSplit(String fullyQualifiedPropertyName, Function<String, T> converter) {
        String value = get(fullyQualifiedPropertyName);
        if (value == null) {
            return Stream.empty();
        }
        List<String> split = split(value, LIST_DELIMITER);
        return split.stream()
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .map(converter);
    }

    @Override
    public String resolvePlaceholders(String value) {
        if (value == null || value.isEmpty()) {
            return null;
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
            String subName = value.substring(prev, i);
            String subValue = get(subName);
            if (subValue == null) {
                throw new IllegalArgumentException("missing interpolation value for property [" + subName + "] while trying to resolvePlaceholders in [" + value + "]");
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
    public Properties toProperties() {
        return new FacadeToProperties(this);
    }

    private static List<String> split(String str, char delimiter) {
        List<String> split = new ArrayList<>();
        int i = 0;
        int next;
        while (i >= 0) {
            next = str.indexOf(delimiter, i);
            if (next < 0) {
                break;
            } else {
                split.add(str.substring(i, next));
                i = next + 1;
            }
        }
        split.add(str.substring(i));
        return split;
    }
}
