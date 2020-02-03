package vest.doctor.netty;

import vest.doctor.ProviderRegistry;

import java.util.List;

/**
 * Internally used to join multiple routers together as a single router.
 */
public final class Routers implements Router {

    private final List<Router> routers;

    public Routers(List<Router> routers) {
        this.routers = routers;
    }

    @Override
    public void init(ProviderRegistry providerRegistry) {
        for (Router router : routers) {
            router.init(providerRegistry);
        }
    }

    @Override
    public boolean accept(RequestContext requestContext) throws Exception {
        for (Router router : routers) {
            boolean handled = router.accept(requestContext);
            if (handled) {
                return true;
            }
        }
        return false;
    }

    @Override
    public Websocket getWebsocket(String uri) {
        for (Router router : routers) {
            Websocket websocket = router.getWebsocket(uri);
            if (websocket != null) {
                return websocket;
            }
        }
        return null;
    }
}
