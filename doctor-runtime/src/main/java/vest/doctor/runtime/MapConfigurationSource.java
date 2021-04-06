package vest.doctor.runtime;

import vest.doctor.ConfigurationSource;

import java.util.HashMap;
import java.util.Map;

/**
 * Configuration source that gets properties from a {@link Map}.
 */
public class MapConfigurationSource implements ConfigurationSource {

    private final Map<String, String> map;

    public MapConfigurationSource(String key, String value, String... more) {
        this(buildMap(key, value, more));
    }

    public MapConfigurationSource(Map<String, String> map) {
        this.map = map;
    }

    @Override
    public String get(String fullyQualifiedPropertyName) {
        return map.get(fullyQualifiedPropertyName);
    }

    @Override
    public Iterable<String> propertyNames() {
        return map.keySet();
    }

    public static Map<String, String> buildMap(String key, String value, String... more) {
        Map<String, String> map = new HashMap<>();
        map.put(key, value);
        if (more != null && more.length > 0) {
            if (more.length % 2 != 0) {
                throw new IllegalArgumentException("map arguments must be in pairs");
            }
            for (int i = 0; i < more.length; i = i + 2) {
                map.put(more[0], more[1]);
            }
        }
        return map;
    }
}
