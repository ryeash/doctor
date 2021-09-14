package vest.doctor.http.server.rest;

import vest.doctor.ApplicationLoader;
import vest.doctor.ProviderRegistry;
import vest.doctor.http.server.Request;
import vest.doctor.http.server.impl.Router;

import java.util.Map;

/**
 * Internally used during endpoint annotation processing.
 */
public interface EndpointLoader extends ApplicationLoader {

    default String pathParam(Request request, String name) {
        Map<String, String> map = request.attribute(Router.PATH_PARAMS);
        if (map == null) {
            throw new IllegalStateException("path matching did not produce a parameter map?");
        } else {
            return map.get(name);
        }
    }

    default boolean isRouterWired(ProviderRegistry providerRegistry) {
        return providerRegistry.getProviderOpt(Router.class).isPresent();
    }

    @Override
    default int priority() {
        return Integer.MAX_VALUE;
    }
}
