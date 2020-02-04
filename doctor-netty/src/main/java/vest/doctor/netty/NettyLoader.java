package vest.doctor.netty;

import vest.doctor.AppLoader;
import vest.doctor.Prioritized;
import vest.doctor.ProviderRegistry;

import java.util.LinkedList;
import java.util.List;
import java.util.ServiceLoader;

public class NettyLoader implements AppLoader {

    private HttpServer server;

    @Override
    public void postProcess(ProviderRegistry providerRegistry) {
        NettyConfiguration nettyConfiguration = new NettyConfiguration(providerRegistry.configuration());
        if (nettyConfiguration.getListenAddresses().isEmpty()) {
            // don't start the server if no addresses are listed
            return;
        }
        this.server = new HttpServer(nettyConfiguration);
        List<Router> routerList = new LinkedList<>();
        for (Router router : ServiceLoader.load(Router.class, NettyLoader.class.getClassLoader())) {
            routerList.add(router);
        }
        routerList.sort(Prioritized.COMPARATOR);
        Routers routers = new Routers(routerList);
        routers.init(providerRegistry);
        server.setRequestHandler(routers);
    }

    @Override
    public void close() {
        if (server != null) {
            server.close();
        }
    }
}
