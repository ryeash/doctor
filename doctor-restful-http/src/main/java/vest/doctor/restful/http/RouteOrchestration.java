package vest.doctor.restful.http;

import vest.doctor.Prioritized;
import vest.doctor.ProviderRegistry;
import vest.doctor.http.server.impl.Router;

public interface RouteOrchestration extends Prioritized {

    void addRoutes(ProviderRegistry registry, Router router);
}
