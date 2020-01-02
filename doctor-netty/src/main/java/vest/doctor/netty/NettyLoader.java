package vest.doctor.netty;

import vest.doctor.AppLoader;
import vest.doctor.BeanProvider;

import java.util.ServiceLoader;

public class NettyLoader implements AppLoader {

    private HttpServer server;

    @Override
    public void postProcess(BeanProvider beanProvider) {
        this.server = new HttpServer(new NettyConfiguration(beanProvider.configuration()));
        for (Route route : ServiceLoader.load(Route.class, NettyLoader.class.getClassLoader())) {
            route.init(beanProvider);
            server.setRequestHandler(route);
            break;
        }
    }

    @Override
    public void close() {
        if (server != null) {
            server.close();
        }
    }
}
