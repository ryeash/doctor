package demo.app;

import vest.doctor.ConfigurationSource;

import java.util.Collections;

public class TCConfigReload implements ConfigurationSource {
    public static boolean reloaded = false;

    @Override
    public String get(String fullyQualifiedPropertyName) {
        return null;
    }

    @Override
    public Iterable<String> propertyNames() {
        return Collections.emptyList();
    }

    @Override
    public void reload() {
        reloaded = true;
    }
}
