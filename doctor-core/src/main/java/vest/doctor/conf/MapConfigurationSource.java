package vest.doctor.conf;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Properties;

/**
 * A {@link ConfigurationSource} backed by a {@link Map}.
 *
 * @param map the source for property values
 */
public record MapConfigurationSource(Map<String, String> map) implements ConfigurationSource {

    /**
     * Create a new map configuration source backed by the values from the {@link Properties}
     * object. The key-value pairs will be copied into an immutable map which will be used
     * as the backing maps for the configuration source.
     *
     * @param properties the properties source
     * @return a new map configuration source
     */
    public static MapConfigurationSource properties(Properties properties) {
        Map<String, String> copy = new LinkedHashMap<>();
        properties.forEach((k, v) -> copy.put(String.valueOf(k), String.valueOf(v)));
        return new MapConfigurationSource(Collections.unmodifiableMap(copy));
    }

    @Override
    public String get(String propertyName) {
        return map.get(propertyName);
    }

    @Override
    public Collection<String> propertyNames() {
        return Collections.unmodifiableSet(map.keySet());
    }

    @Override
    public void reload() {
    }

    @Override
    public String toString() {
        return "MapConfigurationSource";
    }
}
