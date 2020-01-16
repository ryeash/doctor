package vest.doctor.netty;

import vest.doctor.BeanProvider;
import vest.doctor.Prioritized;

public interface Router extends Prioritized {

    void init(BeanProvider beanProvider);

    boolean accept(RequestContext requestContext) throws Exception;

    Websocket getWebsocket(String uri);
}