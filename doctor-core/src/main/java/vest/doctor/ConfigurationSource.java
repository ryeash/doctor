package vest.doctor;

public interface ConfigurationSource {

    String get(String fullyQualifierPropertyName);

    Iterable<String> propertyNames();

    default void reload() {
    }
}
