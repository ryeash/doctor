package vest.doctor.netty;

import vest.doctor.AdHocProvider;
import vest.doctor.AppLoader;
import vest.doctor.Prioritized;
import vest.doctor.ProviderRegistry;
import vest.doctor.event.EventProducer;
import vest.doctor.event.ServiceStarted;
import vest.doctor.event.ServiceStopped;

import java.util.LinkedList;
import java.util.List;
import java.util.ServiceLoader;

public class NettyLoader implements AppLoader {

    private ProviderRegistry providerRegistry;
    private HttpServer server;

    @Override
    public void postProcess(ProviderRegistry providerRegistry) {
        this.providerRegistry = providerRegistry;
        NettyConfiguration nettyConfiguration = new NettyConfiguration(providerRegistry.configuration());
        if (nettyConfiguration.getListenAddresses().isEmpty()) {
            // don't start the server if no addresses are listed
            return;
        }
        List<Router> routerList = new LinkedList<>();
        for (Router router : ServiceLoader.load(Router.class, NettyLoader.class.getClassLoader())) {
            routerList.add(router);
        }
        routerList.sort(Prioritized.COMPARATOR);
        Routers routers = new Routers(routerList);
        routers.init(providerRegistry);
        this.server = new HttpServer(providerRegistry, routers);
        providerRegistry.register(new AdHocProvider<>(Router.class, routers, null));
        providerRegistry.register(new AdHocProvider<>(HttpServer.class, this.server, null));
        providerRegistry.getInstance(EventProducer.class).publish(new ServiceStarted("netty-http", server));
    }

    @Override
    public void close() {
        if (server != null) {
            server.close();
        }
        if (providerRegistry != null) {
            providerRegistry.getInstance(EventProducer.class).publish(new ServiceStopped("netty-http", server));
        }
    }
}
