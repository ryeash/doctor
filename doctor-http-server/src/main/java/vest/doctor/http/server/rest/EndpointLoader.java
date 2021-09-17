package vest.doctor.http.server.rest;

import vest.doctor.ApplicationLoader;
import vest.doctor.ProviderRegistry;
import vest.doctor.http.server.impl.Router;

/**
 * Internally used during endpoint annotation processing.
 */
public interface EndpointLoader extends ApplicationLoader {

    default boolean isRouterWired(ProviderRegistry providerRegistry) {
        return providerRegistry.getProviderOpt(Router.class).isPresent();
    }

    @Override
    default int priority() {
        return Integer.MAX_VALUE;
    }
}
