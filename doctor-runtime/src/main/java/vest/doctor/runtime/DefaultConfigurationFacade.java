package vest.doctor.runtime;

import vest.doctor.ConfigurationFacade;
import vest.doctor.ConfigurationSource;

import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Properties;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

/**
 * Default implementation of the {@link ConfigurationFacade}.
 */
public class DefaultConfigurationFacade implements ConfigurationFacade {

    /**
     * The property name that will point to the properties file(s) to load. For example
     * <code>-Ddoctor.app.properties=application.override.conf,application.conf</code>.
     */
    public static final String PROPERTIES = "doctor.app.properties";
    private static final String MACRO_OPEN = "${";
    private static final String MACRO_CLOSE = "}";
    private static final char LIST_DELIMITER = ',';

    /**
     * Creates a new configuration facade and automatically adds configuration sources (in query order):
     * <br>- environment (via {@link System#getenv(String)})
     * <br>- system properties (via {@link System#getProperty(String)}
     * <br>- external properties files based on the value of 'doctor.app.properties' (using {@link StructuredConfigurationSource})
     *
     * @return a new configuration facade
     */
    public static ConfigurationFacade defaultConfigurationFacade() {
        ConfigurationFacade facade = new DefaultConfigurationFacade()
                .addSource(new EnvironmentVariablesConfigurationSource())
                .addSource(new SystemPropertiesConfigurationSource());

        facade.getList(DefaultConfigurationFacade.PROPERTIES, Function.identity())
                .stream()
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
    public String get(String propertyName) {
        for (ConfigurationSource source : sources) {
            String s = source.get(propertyName);
            if (s != null) {
                try {
                    return resolvePlaceholders(s);
                } catch (IllegalArgumentException e) {
                    throw new IllegalArgumentException("failed to interpolate property: " + propertyName, e);
                }
            }
        }
        return null;
    }

    @Override
    public Stream<String> propertyNames() {
        return sources.stream()
                .map(ConfigurationSource::propertyNames)
                .flatMap(i -> StreamSupport.stream(i.spliterator(), false))
                .filter(Objects::nonNull)
                .distinct();
    }

    @Override
    public String get(String propertyName, String defaultValue) {
        return Optional.ofNullable(get(propertyName))
                .orElse(defaultValue);
    }

    @Override
    public <T> T get(String propertyName, Function<String, T> converter) {
        return Optional.ofNullable(get(propertyName))
                .map(converter)
                .orElse(null);
    }

    @Override
    public <T> T get(String propertyName, T defaultValue, Function<String, T> converter) {
        return Optional.ofNullable(get(propertyName))
                .map(converter)
                .orElse(defaultValue);
    }

    @Override
    public <T> List<T> getList(String propertyName, Function<String, T> converter) {
        return getList(propertyName, Collections.emptyList(), converter);
    }

    @Override
    public <T> List<T> getList(String propertyName, List<T> defaultValue, Function<String, T> converter) {
        return spl(propertyName, defaultValue, converter, ArrayList::new);
    }

    @Override
    public <T> Set<T> getSet(String propertyName, Function<String, T> converter) {
        return getSet(propertyName, Collections.emptySet(), converter);
    }

    @Override
    public <T> Set<T> getSet(String propertyName, Set<T> defaultValue, Function<String, T> converter) {
        return spl(propertyName, defaultValue, converter, LinkedHashSet::new);
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
            String macro = value.substring(prev, i);
            String[] split = splitColon(macro);
            String subName = split[0];
            String defaultValue = split[1];

            String subValue = get(subName, defaultValue);
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

    @Override
    public Set<String> uniquePropertyGroups(String prefix) {
        return uniquePropertyGroups(prefix, ".");
    }

    @Override
    public Set<String> uniquePropertyGroups(String prefix, String terminal) {
        return StreamSupport.stream(propertyNames().spliterator(), false)
                .filter(name -> name.startsWith(prefix))
                .map(name -> getBetween(name, prefix, terminal))
                .filter(Objects::nonNull)
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .collect(Collectors.toSet());
    }

    @Override
    public ConfigurationFacade subsection(String prefix) {
        return new SubsectionConfigurationFacade(this, prefix);
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("Sources (in order): ");
        sb.append(sources);
        sb.append("\nProperties:");
        StreamSupport.stream(propertyNames().spliterator(), false)
                .sorted()
                .map(propertyName -> propertyName + '=' + get(propertyName))
                .peek(l -> sb.append('\n'))
                .forEach(sb::append);
        return sb.toString();
    }

    public ConfigurationFacade copy() {
        DefaultConfigurationFacade clone = new DefaultConfigurationFacade();
        for (ConfigurationSource source : sources) {
            clone.addSource(source);
        }
        return clone;
    }

    private <C extends Collection<T>, T> C spl(String fullyQualifiedPropertyName, C defaultValue, Function<String, T> converter, Supplier<C> collectionFactory) {
        String value = get(fullyQualifiedPropertyName);
        if (value == null) {
            return defaultValue;
        }
        List<String> split = split(value);
        return split.stream()
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .map(converter)
                .collect(Collectors.toCollection(collectionFactory));
    }

    public static List<String> split(String str) {
        if (str == null || str.isEmpty()) {
            return Collections.emptyList();
        }
        List<String> split = new ArrayList<>();
        int i = 0;
        int next;
        while (i >= 0) {
            next = str.indexOf(LIST_DELIMITER, i);
            if (next < 0) {
                break;
            } else {
                split.add(str.substring(i, next).trim());
                i = next + 1;
            }
        }
        split.add(str.substring(i).trim());
        return split;
    }

    private static String[] splitColon(String str) {
        int i = str.indexOf(':');
        if (i < 0) {
            return new String[]{str, null};
        } else {
            return new String[]{str.substring(0, i), str.substring(i + 1)};
        }
    }

    private static String getBetween(String string, String start, String end) {
        int begin = string.indexOf(start);
        if (begin >= 0) {
            begin += start.length();
            int stop = string.indexOf(end, begin);
            if (stop >= begin) {
                return string.substring(begin, stop);
            }
        }
        return null;
    }
}
