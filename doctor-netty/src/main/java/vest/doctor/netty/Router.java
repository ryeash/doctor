package vest.doctor.netty;

import vest.doctor.BeanProvider;

public interface Router {

    void init(BeanProvider beanProvider);

    void accept(RequestContext requestContext) throws Exception;

    Websocket getWebsocket(String uri);
}