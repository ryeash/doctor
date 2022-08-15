package vest.doctor.conf;

import vest.doctor.runtime.RuntimeUtils;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public record MapConfigurationSource(Map<String, String> map) implements ConfigurationSource {

    @Override
    public String get(String propertyName) {
        return map.get(propertyName);
    }

    @Override
    public List<String> getList(String propertyName) {
        return Optional.ofNullable(map.get(propertyName))
                .map(value -> RuntimeUtils.split(value, ConfigurationFacade.LIST_DELIMITER))
                .orElse(null);
    }

    @Override
    public Collection<String> propertyNames() {
        return Collections.unmodifiableSet(map.keySet());
    }

    @Override
    public void reload() {
    }
}
