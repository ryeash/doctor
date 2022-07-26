package vest.doctor.runtime;

import vest.doctor.conf.ConfigurationFacade;
import vest.doctor.conf.ConfigurationSource;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;

public record MapConfigurationSource(Map<String, Object> map) implements ConfigurationSource {

    @Override
    public String get(String propertyName) {
        Object val = getInternal(propertyName);
        if (val == null) {
            return null;
        } else if (val instanceof String str) {
            return str;
        } else {
            throw new IllegalArgumentException("property is not a string: " + propertyName + " = " + val);
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public List<String> getList(String propertyName) {
        Object val = getInternal(propertyName);
        if (val == null) {
            return null;
        } else if (val instanceof List list) {
            return ((List<String>) list);
        } else {
            throw new IllegalArgumentException("property is not a list: " + propertyName + " = " + val);
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public ConfigurationSource getSubConfiguration(String path) {
        Object val = getInternal(path);
        if (val == null) {
            return null;
        } else if (val instanceof Map map) {
            return new MapConfigurationSource(map);
        } else {
            throw new IllegalArgumentException("property is not an object: " + path + " = " + val);
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public List<ConfigurationSource> getSubConfigurations(String path) {
        Object val = getInternal(path);
        if (val == null) {
            return null;
        } else if (val instanceof List list) {
            return list.stream()
                    .map(m -> {
                        if (m instanceof Map map) {
                            return new MapConfigurationSource(map);
                        } else {
                            throw new IllegalArgumentException("property is not a list of objects: " + path + " = " + val);
                        }
                    })
                    .toList();
        } else {
            throw new IllegalArgumentException("property is not a list of objects: " + path + " = " + val);
        }
    }

    @Override
    public Collection<String> propertyNames() {
        return Collections.unmodifiableSet(map.keySet());
    }

    @Override
    public void reload() {
    }

    @SuppressWarnings("unchecked")
    private Object getInternal(String path) {
        if (path.indexOf('.') < 0) {
            return map.get(path);
        }
        List<String> segments = RuntimeUtils.split(path, ConfigurationFacade.NESTING_DELIMITER);
        Map<String, Object> temp = map;
        for (int i = 0; i < segments.size() - 1; i++) {
            Object o = temp.get(segments.get(i));
            if (o == null) {
                return null;
            } else if (o instanceof Map map) {
                temp = (Map<String, Object>) map;
            } else {
                throw new IllegalArgumentException("invalid configuration path: " + path);
            }
        }
        return temp.get(segments.get(segments.size() - 1));
    }
}
