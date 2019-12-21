package vest.doctor;

public class SystemPropertiesConfigurationSource implements ConfigurationSource {
    @Override
    public String get(String fullyQualifierPropertyName) {
        return System.getProperty(fullyQualifierPropertyName);
    }

    @Override
    public Iterable<String> propertyNames() {
        return System.getProperties().stringPropertyNames();
    }
}
