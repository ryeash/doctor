package vest.doctor;

import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Stream;

public interface ConfigurationFacade extends ConfigurationSource {
//    static ConfigurationFacade defaultConfigurationFacade() {
//        ConfigurationFacade facade = new DefaultConfigurationFacade()
//                .addSource(new EnvironmentVariablesConfigurationSource())
//                .addSource(new SystemPropertiesConfigurationSource());
//
//        facade.getSplit(DefaultConfigurationFacade.PROPERTIES, Function.identity())
//                .map(props -> {
//                    try {
//                        return new File(props).toURI().toURL();
//                    } catch (IOException e) {
//                        throw new UncheckedIOException("error reading properties file: " + props, e);
//                    }
//                })
//                .map(StructuredConfiguration::new)
//                .forEach(facade::addSource);
//        return facade;
//    }

    ConfigurationFacade addSource(ConfigurationSource source);

    void reload();

    String get(String fullyQualfiedPropertyName, String defaultValue);

    <T> T get(String fullyQualfiedPropertyName, Function<String, T> converter);

    <T> T get(String fullyQualfiedPropertyName, T defaultValue, Function<String, T> converter);

    <T> Collection<T> getCollection(String fullyQualfiedPropertyName, Function<String, T> converter);

    <T> List<T> getList(String fullyQualfiedPropertyName, Function<String, T> converter);

    <T> Set<T> getSet(String fullyQualfiedPropertyName, Function<String, T> converter);

    <T> Stream<T> getSplit(String fullyQualfiedPropertyName, Function<String, T> converter);

    String resolvePlaceholders(String value);
}
