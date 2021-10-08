package vest.doctor.runtime;

import vest.doctor.ConfigurationFacade;

import java.util.Objects;

/**
 * Can be used as the main-class when packaging/running applications backed by doctor.
 * <p>
 * Supported flags:<br>
 * -m, --modules : a comma delimited list of modules to enable<br>
 * -p, --properties : a comma delimited list of properties files to load (in precedence order)
 */
public final class Main {

    private static Doctor doctor;

    public static void main(String[] args) {
        Args a = new Args(args);
        String modules = a.option("modules", 'm');
        String properties = a.option("properties", 'p', "");

        ConfigurationFacade facade = new DefaultConfigurationFacade()
                .addSource(new EnvironmentVariablesConfigurationSource())
                .addSource(new SystemPropertiesConfigurationSource());

        DefaultConfigurationFacade.split(properties.trim())
                .stream()
                .map(FileLocation::new)
                .map(StructuredConfigurationSource::new)
                .forEach(facade::addSource);
        doctor = new Doctor(facade, DefaultConfigurationFacade.split(modules), new ArgsLoader(a));
    }

    /**
     * If Main was used as the main class get the initialized doctor instance, else throw a {@link NullPointerException}.
     *
     * @return the {@link Doctor}
     */
    public static Doctor doctor() {
        return Objects.requireNonNull(doctor, Main.class.getCanonicalName() + " was not used as the main class");
    }
}
