package vest.doctor.reactor.http.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.SslContextBuilder;
import io.netty.handler.ssl.util.SelfSignedCertificate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.scheduler.Scheduler;
import reactor.core.scheduler.Schedulers;
import reactor.netty.DisposableServer;
import reactor.netty.http.server.HttpServer;
import vest.doctor.AdHocProvider;
import vest.doctor.ApplicationLoader;
import vest.doctor.ConfigurationFacade;
import vest.doctor.CustomThreadFactory;
import vest.doctor.DoctorProvider;
import vest.doctor.Prioritized;
import vest.doctor.ProviderRegistry;
import vest.doctor.event.EventBus;
import vest.doctor.event.ServiceStarted;
import vest.doctor.reactor.http.BodyReader;
import vest.doctor.reactor.http.BodyWriter;
import vest.doctor.reactor.http.Filter;
import vest.doctor.reactor.http.Handler;
import vest.doctor.reactor.http.HttpServerConfiguration;
import vest.doctor.reactor.http.HttpServerCustomizer;
import vest.doctor.reactor.http.RunOn;
import vest.doctor.reactor.http.Websocket;
import vest.doctor.reactor.http.jackson.AsyncParserFactory;
import vest.doctor.reactor.http.jackson.GenericJsonBeanParserFactory;
import vest.doctor.reactor.http.jackson.JacksonInterchange;

import java.io.File;
import java.net.InetSocketAddress;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

public class ReactorHTTPLoader implements ApplicationLoader {

    private static final Logger log = LoggerFactory.getLogger(ReactorHTTPLoader.class);

    @Override
    public void stage4(ProviderRegistry providerRegistry) {
        HttpServerConfiguration configuration = buildConf(providerRegistry);
        if (configuration.getBindAddress() == null) {
            return;
        }

        DefaultBodyReaderWriter defRW = new DefaultBodyReaderWriter();

        ObjectMapper objectMapper = providerRegistry.getInstanceOpt(ObjectMapper.class).orElseGet(() -> JacksonInterchange.defaultConfig(providerRegistry));
        List<AsyncParserFactory> parserFactories = providerRegistry.getInstances(AsyncParserFactory.class).collect(Collectors.toCollection(LinkedList::new));
        parserFactories.add(new GenericJsonBeanParserFactory());
        JacksonInterchange jacksonInterchange = new JacksonInterchange(objectMapper, parserFactories);

        List<BodyReader> readers = providerRegistry.getInstances(BodyReader.class).collect(Collectors.toCollection(LinkedList::new));
        List<BodyWriter> writers = providerRegistry.getInstances(BodyWriter.class).collect(Collectors.toCollection(LinkedList::new));
        readers.add(defRW);
        readers.add(jacksonInterchange);
        readers.sort(Prioritized.COMPARATOR);
        writers.add(defRW);
        writers.add(jacksonInterchange);
        writers.sort(Prioritized.COMPARATOR);
        BodyInterchange bodyInterchange = new BodyInterchange(readers, writers);
        providerRegistry.register(AdHocProvider.createDefault(bodyInterchange));

        Scheduler scheduler = Schedulers.newBoundedElastic(configuration.getWorkerThreads(), configuration.getWorkerThreads() * 2,
                new CustomThreadFactory(true,
                        configuration.getWorkerThreadFormat(),
                        LoggingUncaughtExceptionHandler.INSTANCE,
                        getClass().getClassLoader()), 60);
        providerRegistry.register(new AdHocProvider<>(Scheduler.class, scheduler, RunOn.DEFAULT_SCHEDULER, List.of(Scheduler.class)));

        NioEventLoopGroup bossGroup = new NioEventLoopGroup(configuration.getTcpManagementThreads(),
                new CustomThreadFactory(false,
                        configuration.getTcpThreadFormat(),
                        LoggingUncaughtExceptionHandler.INSTANCE,
                        getClass().getClassLoader()));

        HttpServer httpServer = HttpServer.create();
        providerRegistry.getInstances(HttpServerCustomizer.class).forEach(c -> c.customize(httpServer));

        DisposableServer disposableServer = httpServer.runOn(bossGroup)
                .host(configuration.getBindAddress().getHostName())
                .port(configuration.getBindAddress().getPort())
                .route(routes -> {
                    providerRegistry.getInstances(Handler.class)
                            .forEach(handler -> {
                                for (String method : handler.method()) {
                                    for (String path : handler.path()) {
                                        String finalPath = RouterWriter.squeeze(configuration.getRouterPrefix() + "/" + path, '/');
                                        log.info("Route: {} {} {}", method, finalPath, handler);
                                        switch (method.trim().toUpperCase()) {
                                            case "GET" -> {
                                                routes.get(finalPath, handler);
                                                // cross-reference all GETs as HEADs
                                                routes.head(finalPath, handler);
                                            }
                                            case "PUT" -> routes.put(finalPath, handler);
                                            case "POST" -> routes.post(finalPath, handler);
                                            case "DELETE" -> routes.delete(finalPath, handler);
                                            case "OPTIONS" -> routes.options(finalPath, handler);
                                            default -> throw new IllegalArgumentException("unknown http method " + method + " on " + handler);
                                        }
                                    }
                                }
                            });

                    providerRegistry.getInstances(Filter.class)
                            .sorted(Prioritized.COMPARATOR)
                            .forEach(filter -> log.info("Filter: {}", filter));

                    providerRegistry.getInstances(Websocket.class)
                            .forEach(handler -> {
                                for (String path : handler.path()) {
                                    String finalPath = RouterWriter.squeeze(configuration.getRouterPrefix() + "/" + path, '/');
                                    log.info("Websocket: {} {}", finalPath, handler);
                                    routes.ws(finalPath, handler);
                                }
                            });
                })
                .bind()
                .block();
        Objects.requireNonNull(disposableServer);

        DoctorProvider<DisposableServer> serverProvider = new AdHocProvider<>(
                DisposableServer.class,
                disposableServer,
                "netty",
                disposableServer::onDispose
        );
        providerRegistry.register(serverProvider);
        providerRegistry.getInstance(EventBus.class).publish(new ServiceStarted("netty-http", disposableServer));

    }

    private HttpServerConfiguration buildConf(ProviderRegistry providerRegistry) {
        ConfigurationFacade httpConf = providerRegistry.configuration().subsection("doctor.reactor.http.");

        HttpServerConfiguration conf = new HttpServerConfiguration();

        InetSocketAddress bind = Optional.ofNullable(httpConf.get("bind"))
                .map(s -> s.split(":"))
                .map(hp -> new InetSocketAddress(hp[0].trim(), Integer.parseInt(hp[1].trim())))
                .orElse(null);
        conf.setBindAddress(bind);

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
        return conf;
    }

    @Override
    public int priority() {
        return 10000;
    }
}
