package vest.doctor.netty;

import vest.doctor.BeanProvider;

import java.util.List;

public final class Routers implements Router {

    private final List<Router> routers;

    public Routers(List<Router> routers) {
        this.routers = routers;
    }

    @Override
    public void init(BeanProvider beanProvider) {
        for (Router router : routers) {
            router.init(beanProvider);
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
