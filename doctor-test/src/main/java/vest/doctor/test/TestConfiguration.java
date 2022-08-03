package vest.doctor.test;

import vest.doctor.conf.ConfigurationFacade;
import vest.doctor.conf.StructuredConfigurationSource;
import vest.doctor.runtime.Doctor;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;
import java.util.function.Supplier;

import static java.lang.annotation.RetentionPolicy.RUNTIME;

/**
 * Configures the {@link Doctor} instance that will be wired into the marked test class. Each
 * unique test configuration will build only one doctor and the instances will be cached for the
 * lifetime of the test suite.
 * <p>
 * This annotation is inherited so subclasses of the marked class will be configured with the same
 * doctor instance as the marked class (without needing to duplicate the annotation).
 */
@Documented
@Retention(RUNTIME)
@Target({ElementType.TYPE})
@Inherited
public @interface TestConfiguration {

    /**
     * Define the active modules that the Doctor instance will be started with.
     */
    String[] modules() default {};

    /**
     * Set the configuration builder that will build the {@link vest.doctor.conf.ConfigurationFacade}
     * used with the doctor instance. The class must have a public zero-arg constructor.
     */
    Class<? extends Supplier<? extends ConfigurationFacade>> configurationBuilder() default DefaultConfigurationFacadeBuilder.class;

    /**
     * Adds additional property files to the configuration facade
     * (built from {@link #configurationBuilder()}). The property files will be loaded
     * as {@link StructuredConfigurationSource StructuredConfigurationSources}.
     */
    String[] propertyFiles() default {};
}
