package vest.doctor.runtime;

import vest.doctor.ConfigurationFacade;

import java.util.Objects;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Can be used as the main-class when packaging/running applications backed by doctor.
 * <p>
 * Supported flags:<br>
 * -m, --modules : a comma delimited list of modules to enable<br>
 * -p, --properties : a comma delimited list of properties files to load (in precedence order)
 */
public final class Main {

    private static final AtomicReference<Doctor> doctorRef = new AtomicReference<>(null);

    public static void main(String[] args) {
        if (doctorRef.get() != null) {
            throw new IllegalStateException("the main method has already been called");
        }
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
        Doctor doctor = new Doctor(facade, DefaultConfigurationFacade.split(modules), new ArgsLoader(a));
        if (!doctorRef.compareAndSet(null, doctor)) {
            throw new IllegalStateException("the main method has already been called");
        }
    }

    /**
     * If Main was used as the main class get the initialized doctor instance, else throw a {@link NullPointerException}.
     *
     * @return the {@link Doctor}
     */
    public static Doctor doctor() {
        return Objects.requireNonNull(doctorRef.get(), Main.class.getCanonicalName() + " was not used as the main class or has not finished initializing");
    }
}
