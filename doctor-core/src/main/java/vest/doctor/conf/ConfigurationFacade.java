package vest.doctor.conf;

import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.function.Function;

/**
 * A facade in front of N configuration sources that manages the priority and parsing of
 * property values.
 */
public interface ConfigurationFacade {

    char NESTING_DELIMITER = '.';

    /**
     * Add a source to this facade.
     *
     * @param source the source to add
     * @return this object
     */
    ConfigurationFacade addSource(ConfigurationSource source);

    String get(String propertyName);

    String get(String propertyName, String defaultValue);

    <T> T get(String propertyName, Function<String, T> converter);

    <T> T get(String propertyName, T defaultValue, Function<String, T> converter);

    List<String> getList(String propertyName);

    <T> List<T> getList(String propertyName, Function<String, T> converter);

    <T> List<T> getList(String propertyName, List<T> defaultValue, Function<String, T> converter);

    Set<String> getSet(String propertyName);

    <T> Set<T> getSet(String propertyName, Function<String, T> converter);

    <T> Set<T> getSet(String propertyName, Set<T> defaultValue, Function<String, T> converter);

    ConfigurationFacade getSubConfiguration(String path);

    List<ConfigurationFacade> getSubConfigurations(String path);

    String resolvePlaceholders(String value);

    Collection<String> propertyNames();

    void reload();
}
