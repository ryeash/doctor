package demo.app;

import vest.doctor.ConfigurationSource;

import java.util.stream.Stream;

public class TCConfigReload implements ConfigurationSource {
    public static boolean reloaded = false;

    @Override
    public String get(String propertyName) {
        return null;
    }

    @Override
    public Stream<String> propertyNames() {
        return Stream.empty();
    }

    @Override
    public void reload() {
        reloaded = true;
    }
}
