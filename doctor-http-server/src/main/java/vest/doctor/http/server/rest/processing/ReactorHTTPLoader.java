package vest.doctor.http.server.rest.processing;

import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.SslContextBuilder;
import io.netty.handler.ssl.util.SelfSignedCertificate;
import vest.doctor.AdHocProvider;
import vest.doctor.AnnotationData;
import vest.doctor.ApplicationLoader;
import vest.doctor.Prioritized;
import vest.doctor.ProviderRegistry;
import vest.doctor.conf.ConfigurationFacade;
import vest.doctor.event.EventBus;
import vest.doctor.event.ServiceStarted;
import vest.doctor.http.server.ExceptionHandler;
import vest.doctor.http.server.Filter;
import vest.doctor.http.server.Handler;
import vest.doctor.http.server.HttpServerBuilder;
import vest.doctor.http.server.Server;
import vest.doctor.http.server.Websocket;
import vest.doctor.http.server.impl.CompositeExceptionHandler;
import vest.doctor.http.server.impl.Router;
import vest.doctor.http.server.rest.BodyInterchange;
import vest.doctor.http.server.rest.BodyReader;
import vest.doctor.http.server.rest.BodyWriter;
import vest.doctor.http.server.rest.DefaultBodyReaderWriter;
import vest.doctor.http.server.rest.Endpoint;
import vest.doctor.http.server.rest.HttpMethod;
import vest.doctor.http.server.rest.RouteOrchestration;

import java.io.File;
import java.net.InetSocketAddress;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.ServiceLoader;
import java.util.stream.Collectors;

public class ReactorHTTPLoader implements ApplicationLoader {

    @Override
    public void stage4(ProviderRegistry providerRegistry) {

        ConfigurationFacade httpConf = providerRegistry.configuration().getSubConfiguration("doctor.reactor.http");

        HttpServerBuilder builder = new HttpServerBuilder();

        long binds = Optional.ofNullable(httpConf.get("bind"))
                .map(s -> s.split(","))
                .stream()
                .flatMap(Arrays::stream)
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .map(s -> s.split(":"))
                .map(hp -> new InetSocketAddress(hp[0].trim(), Integer.parseInt(hp[1].trim())))
                .peek(builder::addBindAddress)
                .count();
        if (binds == 0L) {
            return;
        }

        builder.setTcpManagementThreads(httpConf.get("tcp.threads", 1, Integer::valueOf));
        builder.setTcpThreadFormat(httpConf.get("tcp.threadFormat", "netty-tcp-%d"));

        try {
            if (httpConf.get("ssl.selfSigned", false, Boolean::valueOf)) {
                SelfSignedCertificate ssc = new SelfSignedCertificate();
                SslContext sslContext = SslContextBuilder.forServer(ssc.certificate(), ssc.privateKey()).build();
                builder.setSslContext(sslContext);
            }

            String keyCertChainFile = httpConf.get("ssl.keyCertChainFile");
            if (keyCertChainFile != null && !keyCertChainFile.isEmpty()) {
                String keyFile = Objects.requireNonNull(httpConf.get("ssl.keyFile"), "missing ssl key file configuration");
                String keyPassword = httpConf.get("ssl.keyPassword");
                SslContext sslContext = SslContextBuilder.forServer(new File(keyCertChainFile), new File(keyFile), keyPassword).build();
                builder.setSslContext(sslContext);
            }
        } catch (Throwable t) {
            throw new RuntimeException("error configuring ssl", t);
        }

        builder.setMaxInitialLineLength(httpConf.get("maxInitialLineLength", 8192, Integer::valueOf));
        builder.setMaxHeaderSize(httpConf.get("maxHeaderSize", 8192, Integer::valueOf));
        builder.setMaxChunkSize(httpConf.get("maxChunkSize", 8192, Integer::valueOf));
        builder.setValidateHeaders(httpConf.get("validateHeaders", false, Boolean::valueOf));
        builder.setInitialBufferSize(httpConf.get("initialBufferSize", 8192, Integer::valueOf));
        builder.setMaxContentLength(httpConf.get("maxContentLength", 8388608, Integer::valueOf));
        builder.setMinGzipSize(httpConf.get("minGzipSize", 812, Integer::valueOf));
        builder.setRouterPrefix(httpConf.get("routePrefix", ""));

        if (builder.getConfig().getBindAddresses() == null || builder.getConfig().getBindAddresses().isEmpty()) {
            return;
        }

        DefaultBodyReaderWriter defRW = new DefaultBodyReaderWriter();
        List<BodyReader> readers = providerRegistry.getInstances(BodyReader.class).collect(Collectors.toCollection(LinkedList::new));
        List<BodyWriter> writers = providerRegistry.getInstances(BodyWriter.class).collect(Collectors.toCollection(LinkedList::new));
        readers.add(defRW);
        readers.sort(Prioritized.COMPARATOR);
        writers.add(defRW);
        writers.sort(Prioritized.COMPARATOR);
        BodyInterchange bodyInterchange = new BodyInterchange(readers, writers);
        providerRegistry.register(AdHocProvider.createPrimary(bodyInterchange));


        CompositeExceptionHandler compositeExceptionHandler = new CompositeExceptionHandler();
        providerRegistry.getInstances(ExceptionHandler.class)
                .forEach(compositeExceptionHandler::addHandler);
        providerRegistry.register(AdHocProvider.createPrimary(compositeExceptionHandler));

        providerRegistry.getProviders(Filter.class)
                .forEach(provider -> {
                    List<String> paths;
                    try {
                        paths = provider.annotationMetadata().stringArrayValue(Endpoint.class, "value");
                    } catch (IllegalArgumentException e) {
                        paths = Collections.singletonList(Router.MATCH_ALL_PATH_SPEC);
                    }
                    Filter filter = provider.get();
                    for (String path : paths) {
                        builder.filter(path, filter);
                    }
                });

        providerRegistry.getProviders(Handler.class)
                .forEach(provider -> {
                    List<String> methods = new LinkedList<>();
                    for (AnnotationData annotationMetadatum : provider.annotationMetadata()) {
                        HttpMethod method = annotationMetadatum.type().getAnnotation(HttpMethod.class);
                        if (method != null) {
                            methods.add(method.value());
                        }
                    }
                    if (methods.isEmpty()) {
                        throw new IllegalArgumentException("missing http method for Handler: " + provider);
                    }
                    List<String> paths;
                    try {
                        paths = provider.annotationMetadata().stringArrayValue(Endpoint.class, "value");
                    } catch (IllegalArgumentException e) {
                        throw new IllegalArgumentException("missing @Endpoint for Handler: " + provider);
                    }
                    Handler handler = provider.get();
                    for (String method : methods) {
                        for (String path : paths) {
                            builder.route(method, path, handler);
                        }
                    }
                });


        List<RouteOrchestration> orchestrations = new LinkedList<>();
        for (RouteOrchestration routeOrchestration : ServiceLoader.load(RouteOrchestration.class)) {
            orchestrations.add(routeOrchestration);
        }
        orchestrations.sort(Prioritized.COMPARATOR);
        for (RouteOrchestration orchestration : orchestrations) {
            builder.routes(router -> orchestration.addRoutes(providerRegistry, router));
        }

        providerRegistry.getProviders(Websocket.class)
                .forEach(provider -> builder.ws(provider::get));

        Server server = builder.start();
        providerRegistry.register(new AdHocProvider<>(Server.class, server, null, server));
        providerRegistry.getInstance(EventBus.class).publish(new ServiceStarted("reactor-http", server));
    }

    @Override
    public int priority() {
        return 100000;
    }
}
