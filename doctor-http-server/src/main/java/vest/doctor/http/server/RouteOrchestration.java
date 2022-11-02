package vest.doctor.http.server;

import vest.doctor.Prioritized;
import vest.doctor.ProviderRegistry;
import vest.doctor.http.server.impl.Router;

/**
 * Internal use.
 */
public interface RouteOrchestration extends Prioritized {

    void addRoutes(ProviderRegistry registry, Router router);
}
