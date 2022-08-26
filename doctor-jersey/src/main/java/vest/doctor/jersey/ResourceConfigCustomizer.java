package vest.doctor.jersey;

import org.glassfish.jersey.server.ResourceConfig;

/**
 * Customizes the {@link ResourceConfig} for the jersey JAX-RS provider.
 */
public interface ResourceConfigCustomizer {

    /**
     * Customize the {@link ResourceConfig} for jersey.
     *
     * @param config the resource configuration
     * @return the config
     */
    ResourceConfig customize(ResourceConfig config);
}
