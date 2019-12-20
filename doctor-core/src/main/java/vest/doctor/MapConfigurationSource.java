package vest.doctor;

import java.util.HashMap;
import java.util.Map;

public class MapConfigurationSource implements ConfigurationSource {

    private final Map<String, String> map;

    public MapConfigurationSource(Map<String, String> map) {
        this.map = map;
    }

    public MapConfigurationSource(String key, String value, String... more) {
        Map<String, String> map = new HashMap<>();
        map.put(key, value);
        if (more.length % 2 != 0) {
            throw new IllegalArgumentException("map arguments must be in pairs");
        }
        for (int i = 0; i < more.length; i = i + 2) {
            map.put(more[0], more[1]);
        }
        this.map = map;
    }

    @Override
    public String get(String fullyQualifierPropertyName) {
        return map.get(fullyQualifierPropertyName);
    }
}
