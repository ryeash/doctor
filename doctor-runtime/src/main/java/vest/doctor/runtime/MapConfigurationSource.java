package vest.doctor.runtime;

import vest.doctor.ConfigurationSource;

import java.util.Map;
import java.util.stream.Stream;

/**
 * Configuration source that gets properties from a {@link Map}.
 */
public class MapConfigurationSource implements ConfigurationSource {

    private final Map<String, String> map;

    /**
     * Create a new configuration source with the properties in the map.
     *
     * @param map the map of property name to values
     */
    public MapConfigurationSource(Map<String, String> map) {
        this.map = map;
    }

    @Override
    public String get(String propertyName) {
        return map.get(propertyName);
    }

    @Override
    public Stream<String> propertyNames() {
        return map.keySet().stream();
    }
}
