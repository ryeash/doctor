package vest.doctor.netty;

import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.SslContextBuilder;
import io.netty.handler.ssl.util.SelfSignedCertificate;
import vest.doctor.AdHocProvider;
import vest.doctor.AppLoader;
import vest.doctor.ConfigurationFacade;
import vest.doctor.ProviderRegistry;
import vest.doctor.event.EventProducer;
import vest.doctor.event.ServiceStarted;
import vest.doctor.event.ServiceStopped;
import vest.doctor.netty.impl.CompositeExceptionHandler;
import vest.doctor.netty.impl.Router;

import javax.inject.Provider;
import java.io.File;
import java.net.InetSocketAddress;
import java.util.List;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

public class NettyLoader implements AppLoader {

    private ProviderRegistry providerRegistry;
    private HttpServer server;

    @Override
    public void postProcess(ProviderRegistry providerRegistry) {
        this.providerRegistry = providerRegistry;

        NettyConfiguration conf = buildConf(providerRegistry);
        if (conf.getBindAddresses().isEmpty()) {
            return;
        }
        BodyInterchange bodyInterchange = new BodyInterchange(providerRegistry);
        providerRegistry.register(new AdHocProvider<>(BodyInterchange.class, bodyInterchange, null));

        Router router = new Router(conf.getCaseInsensitiveMatching());
        providerRegistry.getProviders(Filter.class)
                .map(Provider::get)
                .forEach(router::addFilter);

        providerRegistry.getProviders(Endpoint.class)
                .map(Provider::get)
                .forEach(endpoint -> router.addRoute(endpoint.method(), endpoint.path(), endpoint));

        CompositeExceptionHandler compositeExceptionHandler = new CompositeExceptionHandler();
        providerRegistry.getProviders(ExceptionHandler.class)
                .map(Provider::get)
                .forEach(compositeExceptionHandler::addHandler);

        this.server = new HttpServer(conf, router, compositeExceptionHandler);

        providerRegistry.getProviders(Websocket.class)
                .forEach(ws -> {
                    Path path = ws.type().getAnnotation(Path.class);
                    if (path == null) {
                        throw new IllegalArgumentException("websockets must have a @Path annotation: " + ws.type());
                    }
                    for (String p : path.value()) {
                        server.addWebsocket(p, ws.get());
                    }
                });

        providerRegistry.register(new AdHocProvider<>(Router.class, router, null));
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

    private NettyConfiguration buildConf(ProviderRegistry providerRegistry) {
        ConfigurationFacade cf = providerRegistry.configuration();

        NettyConfiguration conf = new NettyConfiguration();
        conf.setTcpManagementThreads(cf.get("doctor.netty.tcp.threads", 1, Integer::valueOf));
        conf.setTcpThreadPrefix(cf.get("doctor.netty.tcp.threadPrefix", "netty-tcp"));
        conf.setWorkerThreads(cf.get("doctor.netty.worker.threads", 16, Integer::valueOf));
        conf.setWorkerThreadPrefix(cf.get("doctor.netty.worker.threadPrefix", "netty-worker"));
        conf.setSocketBacklog(cf.get("doctor.netty.tcp.socketBacklog", 1024, Integer::valueOf));

        List<InetSocketAddress> bind = cf.getList("doctor.netty.bind", Function.identity())
                .stream()
                .map(s -> s.split(":"))
                .map(hp -> new InetSocketAddress(hp[0].trim(), Integer.parseInt(hp[1].trim())))
                .collect(Collectors.toList());
        conf.setBindAddresses(bind);

        try {
            if (cf.get("doctor.netty.ssl.selfSigned", false, Boolean::valueOf)) {
                SelfSignedCertificate ssc = new SelfSignedCertificate();
                SslContext sslContext = SslContextBuilder.forServer(ssc.certificate(), ssc.privateKey()).build();
                conf.setSslContext(sslContext);
            }

            String keyCertChainFile = cf.get("doctor.netty.ssl.keyCertChainFile");
            if (keyCertChainFile != null && !keyCertChainFile.isEmpty()) {
                String keyFile = Objects.requireNonNull(cf.get("doctor.netty.ssl.keyFile"), "missing ssl key file configuration");
                String keyPassword = cf.get("doctor.netty.ssl.keyPassword");
                SslContext sslContext = SslContextBuilder.forServer(new File(keyCertChainFile), new File(keyFile), keyPassword).build();
                conf.setSslContext(sslContext);
            }
        } catch (Throwable t) {
            throw new RuntimeException("error configuring ssl", t);
        }

        conf.setMaxInitialLineLength(cf.get("doctor.netty.http.maxInitialLineLength", 8192, Integer::valueOf));
        conf.setMaxHeaderSize(cf.get("doctor.netty.http.maxHeaderSize", 8192, Integer::valueOf));
        conf.setMaxChunkSize(cf.get("doctor.netty.http.maxChunkSize", 8192, Integer::valueOf));
        conf.setValidateHeaders(cf.get("doctor.netty.http.validateHeaders", false, Boolean::valueOf));
        conf.setInitialBufferSize(cf.get("doctor.netty.http.initialBufferSize", 8192, Integer::valueOf));
        conf.setMaxContentLength(cf.get("doctor.netty.http.maxContentLength", 8388608, Integer::valueOf));

        conf.setCaseInsensitiveMatching(cf.get("doctor.netty.http.caseInsensitiveMatching", false, Boolean::valueOf));
        // TODO
        boolean debugRouting = cf.get("doctor.netty.http.debugRequestRouting", false, Boolean::valueOf);
        return conf;
    }
}
