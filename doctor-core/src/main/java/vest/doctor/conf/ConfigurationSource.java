package vest.doctor.conf;

import java.util.Collection;

/**
 * A source of property values. Can be e.g. a properties file or an in-memory map.
 */
public interface ConfigurationSource {

    /**
     * Get the string property value.
     *
     * @param propertyName the name of the property
     * @return the property value or null if this source does not have a property for the given name
     */
    String get(String propertyName);

    /**
     * Get all property names in the configuration source.
     *
     * @return a collection of property names
     */
    Collection<String> propertyNames();

    /**
     * Reload this configuration source.
     */
    void reload();

    /**
     * Configuration source that gets properties from {@link System#getenv(String)}.
     */
    final class Env implements ConfigurationSource {

        @Override
        public String get(String propertyName) {
            return System.getenv(propertyName);
        }

        @Override
        public Collection<String> propertyNames() {
            return System.getenv().keySet();
        }

        @Override
        public void reload() {
        }

        @Override
        public String toString() {
            return getClass().getSimpleName();
        }
    }

    /**
     * Configuration source that gets properties from {@link System#getProperty(String)}.
     */
    final class SystemProperties implements ConfigurationSource {
        @Override
        public String get(String propertyName) {
            return System.getProperty(propertyName);
        }

        @Override
        public Collection<String> propertyNames() {
            return System.getProperties().stringPropertyNames();
        }

        @Override
        public void reload() {
        }

        @Override
        public String toString() {
            return getClass().getSimpleName();
        }
    }
}
