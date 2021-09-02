package vest.doctor.jersey;

import org.glassfish.jersey.server.ResourceConfig;

/**
 * Customizes the {@link ResourceConfig} for the jersey JAX-RS provider.
 */
public interface ResourceConfigCustomizer {

    /**
     * Customize the ResourceConfig for jersey.
     *
     * @param config the resource configuration
     */
    void customize(ResourceConfig config);
}
