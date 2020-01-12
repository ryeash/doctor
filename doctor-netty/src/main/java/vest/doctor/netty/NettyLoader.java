package vest.doctor.netty;

import vest.doctor.AppLoader;
import vest.doctor.BeanProvider;

import java.util.ServiceLoader;

public class NettyLoader implements AppLoader {

    private HttpServer server;

    @Override
    public void postProcess(BeanProvider beanProvider) {
        this.server = new HttpServer(new NettyConfiguration(beanProvider.configuration()));
        for (Router router : ServiceLoader.load(Router.class, NettyLoader.class.getClassLoader())) {
            router.init(beanProvider);
            server.setRequestHandler(router);
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
