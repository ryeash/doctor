package vest.doctor.http.server.rest;

import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.SslContextBuilder;
import io.netty.handler.ssl.util.SelfSignedCertificate;
import jakarta.inject.Provider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import vest.doctor.AdHocProvider;
import vest.doctor.ApplicationLoader;
import vest.doctor.ConfigurationFacade;
import vest.doctor.DoctorProvider;
import vest.doctor.ProviderRegistry;
import vest.doctor.event.EventBus;
import vest.doctor.event.ServiceStarted;
import vest.doctor.http.server.DoctorHttpServerConfiguration;
import vest.doctor.http.server.ExceptionHandler;
import vest.doctor.http.server.impl.CompositeExceptionHandler;
import vest.doctor.http.server.impl.DoctorHttpHandler;
import vest.doctor.http.server.impl.Router;
import vest.doctor.netty.common.NettyHttpServer;
import vest.doctor.netty.common.PipelineCustomizer;
import vest.doctor.netty.common.ServerBootstrapCustomizer;
import vest.doctor.netty.common.Websocket;

import java.io.File;
import java.net.InetSocketAddress;
import java.util.List;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

public class NettyLoader implements ApplicationLoader {

    private static final Logger log = LoggerFactory.getLogger(NettyLoader.class);

    @Override
    public void stage4(ProviderRegistry providerRegistry) {
        DoctorHttpServerConfiguration conf = buildConf(providerRegistry);
        if (conf.getBindAddresses() == null || conf.getBindAddresses().isEmpty()) {
            log.warn("not starting the netty http server: no bind addresses set");
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
        conf.setExceptionHandler(compositeExceptionHandler);

        DoctorHttpHandler doctorHttpHandler = new DoctorHttpHandler(conf, router, compositeExceptionHandler);
        providerRegistry.getProviders(Websocket.class)
                .forEach(ws -> doctorHttpHandler.addWebsocket(ws::get));
        NettyHttpServer server = new NettyHttpServer(
                conf,
                doctorHttpHandler,
                true);

        DoctorProvider<NettyHttpServer> serverProvider = new AdHocProvider<>(
                NettyHttpServer.class,
                server,
                "netty",
                server
        );
        providerRegistry.register(serverProvider);
        providerRegistry.getInstance(EventBus.class).publish(new ServiceStarted("netty-http", server));
    }

    private DoctorHttpServerConfiguration buildConf(ProviderRegistry providerRegistry) {
        ConfigurationFacade httpConf = providerRegistry.configuration().subsection("doctor.netty.http.");

        DoctorHttpServerConfiguration conf = new DoctorHttpServerConfiguration();

        List<InetSocketAddress> bind = httpConf.getList("bind", Function.identity())
                .stream()
                .map(s -> s.split(":"))
                .map(hp -> new InetSocketAddress(hp[0].trim(), Integer.parseInt(hp[1].trim())))
                .collect(Collectors.toList());
        conf.setBindAddresses(bind);

        conf.setTcpManagementThreads(httpConf.get("tcp.threads", 1, Integer::valueOf));
        conf.setTcpThreadFormat(httpConf.get("tcp.threadFormat", "netty-tcp-%d"));
        conf.setSocketBacklog(httpConf.get("tcp.socketBacklog", 1024, Integer::valueOf));
        conf.setWorkerThreads(httpConf.get("worker.threads", 16, Integer::valueOf));
        conf.setWorkerThreadFormat(httpConf.get("worker.threadFormat", "netty-worker-%d"));


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
        conf.setMinGzipSize(httpConf.get("minGzipSize", 812, Integer::valueOf));

        conf.setCaseInsensitiveMatching(httpConf.get("caseInsensitiveMatching", false, Boolean::valueOf));
        conf.setDebugRequestRouting(httpConf.get("debugRequestRouting", false, Boolean::valueOf));
        conf.setRouterPrefix(httpConf.get("routePrefix", ""));

        List<PipelineCustomizer> pipelineCustomizers = providerRegistry.getProviders(PipelineCustomizer.class)
                .map(Provider::get)
                .collect(Collectors.toList());
        conf.setPipelineCustomizers(pipelineCustomizers);

        List<ServerBootstrapCustomizer> serverBootstrapCustomizers = providerRegistry.getProviders(ServerBootstrapCustomizer.class)
                .map(DoctorProvider::get)
                .collect(Collectors.toList());
        conf.setServerBootstrapCustomizers(serverBootstrapCustomizers);
        return conf;
    }

    @Override
    public int priority() {
        return 10000;
    }
}
