package doctor.processor;

/**
 * Constants used during class generation.
 */
public final class Constants {

    /**
     * The name that will be used for the instance of {@link vest.doctor.ProviderRegistry} in all code generation.
     */
    public static final String PROVIDER_REGISTRY = "providerRegistry";

    /**
     * The name that will be used for the instance of {@link vest.doctor.ShutdownContainer} in all code generation.
     */
    public static final String SHUTDOWN_CONTAINER_NAME = "shutdownContainer";

    /**
     * The name for the default method in annotations; e.g. {@link javax.inject.Named}.
     */
    public static final String ANNOTATION_VALUE = "value";

    private Constants() {
    }
}
