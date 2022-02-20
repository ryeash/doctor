package demo.app;

import vest.doctor.conf.ConfigurationSource;

import java.util.Collection;
import java.util.List;

public class TCConfigReload implements ConfigurationSource {
    public static boolean reloaded = false;

    @Override
    public String get(String propertyName) {
        return null;
    }

    @Override
    public List<String> getList(String propertyName) {
        return null;
    }

    @Override
    public ConfigurationSource getSubConfiguration(String path) {
        return null;
    }

    @Override
    public List<ConfigurationSource> getSubConfigurations(String path) {
        return null;
    }

    @Override
    public Collection<String> propertyNames() {
        return List.of();
    }

    @Override
    public void reload() {
        reloaded = true;
    }
}
