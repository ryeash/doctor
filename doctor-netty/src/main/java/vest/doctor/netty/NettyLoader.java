package vest.doctor.netty;

import vest.doctor.AppLoader;
import vest.doctor.BeanProvider;

import java.util.LinkedList;
import java.util.List;
import java.util.ServiceLoader;

public class NettyLoader implements AppLoader {

    private HttpServer server;

    @Override
    public void postProcess(BeanProvider beanProvider) {
        this.server = new HttpServer(new NettyConfiguration(beanProvider.configuration()));
        List<Router> routerList = new LinkedList<>();
        for (Router router : ServiceLoader.load(Router.class, NettyLoader.class.getClassLoader())) {
            routerList.add(router);
        }
        Routers routers = new Routers(routerList);
        routers.init(beanProvider);
        server.setRequestHandler(routers);
    }

    @Override
    public void close() {
        if (server != null) {
            server.close();
        }
    }
}
