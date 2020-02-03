package vest.doctor.netty;

import vest.doctor.Prioritized;
import vest.doctor.ProviderRegistry;

/**
 * Define the contract for a router that will handle the workflow for an HTTP request/response.
 * <p>
 * An implementation of this interface will be generated based on the endpoints defined in the application.
 */
public interface Router extends Prioritized {

    void init(ProviderRegistry providerRegistry);

    boolean accept(RequestContext requestContext) throws Exception;

    Websocket getWebsocket(String uri);
}