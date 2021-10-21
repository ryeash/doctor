package vest.doctor.http.server.rest;

import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.SslContextBuilder;
import io.netty.handler.ssl.util.SelfSignedCertificate;
import jakarta.inject.Provider;
import vest.doctor.AdHocProvider;
import vest.doctor.ApplicationLoader;
import vest.doctor.ConfigurationFacade;
import vest.doctor.ProviderRegistry;
import vest.doctor.event.EventBus;
import vest.doctor.event.EventProducer;
import vest.doctor.event.ServiceStarted;
import vest.doctor.event.ServiceStopped;
import vest.doctor.http.server.DoctorHttpServerConfiguration;
import vest.doctor.http.server.ExceptionHandler;
import vest.doctor.http.server.HttpServer;
import vest.doctor.http.server.impl.CompositeExceptionHandler;
import vest.doctor.http.server.impl.Router;
import vest.doctor.netty.common.Websocket;

import java.io.File;
import java.net.InetSocketAddress;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

public class NettyLoader implements ApplicationLoader {

    @Override
    public void stage4(ProviderRegistry providerRegistry) {
        DoctorHttpServerConfiguration conf = buildConf(providerRegistry);
        if (conf.getBindAddresses().isEmpty()) {
            return;
        }
        BodyInterchange bodyInterchange = new BodyInterchange(providerRegistry);
        providerRegistry.register(new AdHocProvider<>(BodyInterchange.class, bodyInterchange, null));

        Router router = new Router(conf);
        providerRegistry.register(new AdHocProvider<>(Router.class, router, null));

        CompositeExceptionHandler compositeExceptionHandler = new CompositeExceptionHandler();
        providerRegistry.getProviders(ExceptionHandler.class)
                .map(Provider::get)
                .forEach(compositeExceptionHandler::addHandler);

        HttpServer server = new HttpServer(conf, router, compositeExceptionHandler);

        providerRegistry.getProviders(Websocket.class)
                .forEach(ws -> {
                    Websocket websocket = ws.get();
                    List<String> paths = Optional.ofNullable(websocket.paths())
                            .stream()
                            .flatMap(List::stream)
                            .map(String::trim)
                            .filter(s -> !s.isEmpty())
                            .collect(Collectors.toList());
                    if (paths.isEmpty()) {
                        throw new IllegalArgumentException("empty websocket path " + ws.type());
                    }
                    for (String p : paths) {
                        if (p == null || p.isEmpty()) {
                            throw new IllegalArgumentException("empty websocket path " + ws.type());
                        }
                        server.addWebsocket(p, ws::get);
                    }
                });

        providerRegistry.register(new AdHocProvider<>(HttpServer.class, server, null));

        Optional<EventBus> eventBusOpt = providerRegistry.getInstanceOpt(EventBus.class);
        eventBusOpt.ifPresent(eventBus -> eventBus.publish(new ServiceStarted("netty-http", server)));

        providerRegistry.shutdownContainer().register(() -> {
            server.close();
            providerRegistry.getInstance(EventProducer.class)
                    .publish(new ServiceStopped("netty-http", server));
        });
    }

    private DoctorHttpServerConfiguration buildConf(ProviderRegistry providerRegistry) {
        ConfigurationFacade httpConf = providerRegistry.configuration().subsection("doctor.netty.http.");

        DoctorHttpServerConfiguration conf = new DoctorHttpServerConfiguration();
        conf.setTcpManagementThreads(httpConf.get("tcp.threads", 1, Integer::valueOf));
        conf.setTcpThreadFormat(httpConf.get("tcp.threadPrefix", "netty-tcp"));
        conf.setWorkerThreads(httpConf.get("worker.threads", 16, Integer::valueOf));
        conf.setWorkerThreadFormat(httpConf.get("worker.threadPrefix", "netty-worker"));
        conf.setSocketBacklog(httpConf.get("tcp.socketBacklog", 1024, Integer::valueOf));

        List<InetSocketAddress> bind = httpConf.getList("bind", Function.identity())
                .stream()
                .map(s -> s.split(":"))
                .map(hp -> new InetSocketAddress(hp[0].trim(), Integer.parseInt(hp[1].trim())))
                .collect(Collectors.toList());
        conf.setBindAddresses(bind);

        try {
            if (httpConf.get("ssl.selfSigned", false, Boolean::valueOf)) {
                SelfSignedCertificate ssc = new SelfSignedCertificate();
                SslContext sslContext = SslContextBuilder.forServer(ssc.certificate(), ssc.privateKey()).build();
                conf.setSslContext(sslContext);
            }

            String keyCertChainFile = httpConf.get("ssl.keyCertChainFile");
            if (keyCertChainFile != null && !keyCertChainFile.isEmpty()) {
                String keyFile = Objects.requireNonNull(httpConf.get("ssl.keyFile"), "missing ssl key file configuration");
                String keyPassword = httpConf.get("ssl.keyPassword");
                SslContext sslContext = SslContextBuilder.forServer(new File(keyCertChainFile), new File(keyFile), keyPassword).build();
                conf.setSslContext(sslContext);
            }
        } catch (Throwable t) {
            throw new RuntimeException("error configuring ssl", t);
        }

        conf.setMaxInitialLineLength(httpConf.get("maxInitialLineLength", 8192, Integer::valueOf));
        conf.setMaxHeaderSize(httpConf.get("maxHeaderSize", 8192, Integer::valueOf));
        conf.setMaxChunkSize(httpConf.get("maxChunkSize", 8192, Integer::valueOf));
        conf.setValidateHeaders(httpConf.get("validateHeaders", false, Boolean::valueOf));
        conf.setInitialBufferSize(httpConf.get("initialBufferSize", 8192, Integer::valueOf));
        conf.setMaxContentLength(httpConf.get("maxContentLength", 8388608, Integer::valueOf));

        conf.setCaseInsensitiveMatching(httpConf.get("caseInsensitiveMatching", false, Boolean::valueOf));
        conf.setDebugRequestRouting(httpConf.get("debugRequestRouting", false, Boolean::valueOf));
        conf.setRouterPrefix(httpConf.get("routePrefix", ""));
        return conf;
    }

    @Override
    public int priority() {
        return 10000;
    }
}
