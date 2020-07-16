package vest.doctor;

import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.Objects;

/**
 * Can be used as the main-class when packaging/running applications backed by doctor.
 * <p>
 * Supported flags:
 * -m, --modules : a comma delimited list of modules to enable
 * -p, --properties : a comma delimited list of properties files to load (in order)
 */
public final class Main {

    private static Doctor doctor;

    public static void main(String[] args) {
        Args a = new Args(args);
        String modules = a.anyFlagValue("m", "modules", "");
        doctor = new Doctor(mainConfig(a), DefaultConfigurationFacade.split(modules), new ArgsLoader(a));
    }

    public static Doctor doctor() {
        return Objects.requireNonNull(doctor, Main.class.getCanonicalName() + " was not used as the main entry point");
    }

    private static ConfigurationFacade mainConfig(Args args) {
        String properties = args.anyFlagValue("p", "properties", "");

        ConfigurationFacade facade = new DefaultConfigurationFacade()
                .addSource(new EnvironmentVariablesConfigurationSource())
                .addSource(new SystemPropertiesConfigurationSource());

        DefaultConfigurationFacade.split(properties)
                .stream()
                .map(props -> {
                    try {
                        return new File(props).toURI().toURL();
                    } catch (IOException e) {
                        throw new UncheckedIOException("error reading properties file: " + props, e);
                    }
                })
                .map(StructuredConfigurationSource::new)
                .forEach(facade::addSource);
        return facade;
    }

    private static final class ArgsLoader implements AppLoader {
        private final Args args;

        private ArgsLoader(Args args) {
            this.args = args;
        }

        @Override
        public void preProcess(ProviderRegistry providerRegistry) {
            providerRegistry.register(new AdHocProvider<>(Args.class, args, null));
        }

        @Override
        public int priority() {
            return Integer.MIN_VALUE;
        }
    }
}
