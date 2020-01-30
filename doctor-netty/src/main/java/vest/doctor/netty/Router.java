package vest.doctor.netty;

import vest.doctor.Prioritized;
import vest.doctor.ProviderRegistry;

public interface Router extends Prioritized {

    void init(ProviderRegistry providerRegistry);

    boolean accept(RequestContext requestContext) throws Exception;

    Websocket getWebsocket(String uri);
}