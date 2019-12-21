package vest.doctor;

public class EnvironmentVariablesConfigurationSource implements ConfigurationSource {
    @Override
    public String get(String fullyQualifierPropertyName) {
        return System.getenv(fullyQualifierPropertyName);
    }

    @Override
    public Iterable<String> propertyNames() {
        return System.getenv().keySet();
    }
}
