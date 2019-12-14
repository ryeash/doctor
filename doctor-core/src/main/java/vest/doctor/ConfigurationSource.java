package vest.doctor;

public interface ConfigurationSource {

    String get(String fullyQualifierPropertyName);

    default void reload() {
    }
}
