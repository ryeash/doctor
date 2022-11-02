package vest.doctor.http.server.processing;

import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.SslContextBuilder;
import io.netty.handler.ssl.util.SelfSignedCertificate;
import jakarta.inject.Singleton;
import vest.doctor.AnnotationData;
import vest.doctor.DoctorProvider;
import vest.doctor.Eager;
import vest.doctor.Factory;
import vest.doctor.Prioritized;
import vest.doctor.ProviderRegistry;
import vest.doctor.conf.ConfigurationFacade;
import vest.doctor.event.EventBus;
import vest.doctor.event.ServiceStarted;
import vest.doctor.http.server.BodyInterchange;
import vest.doctor.http.server.BodyReader;
import vest.doctor.http.server.BodyWriter;
import vest.doctor.http.server.DefaultBodyReaderWriter;
import vest.doctor.http.server.Endpoint;
import vest.doctor.http.server.ExceptionHandler;
import vest.doctor.http.server.Filter;
import vest.doctor.http.server.Handler;
import vest.doctor.http.server.HttpMethod;
import vest.doctor.http.server.HttpServerBuilder;
import vest.doctor.http.server.PipelineCustomizer;
import vest.doctor.http.server.Server;
import vest.doctor.http.server.ServerBootstrapCustomizer;
import vest.doctor.http.server.Websocket;
import vest.doctor.http.server.impl.CompositeExceptionHandler;
import vest.doctor.http.server.impl.Router;

import java.io.File;
import java.net.InetSocketAddress;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.ServiceLoader;
import java.util.concurrent.Flow;

@Singleton
public class HttpConfig {

    @Factory
    @Singleton
    public BodyInterchange bodyInterchangeFactory(List<BodyReader> readers,
                                                  List<BodyWriter> writers) {
        DefaultBodyReaderWriter defRW = new DefaultBodyReaderWriter();
        readers.add(defRW);
        writers.add(defRW);
        readers.sort(Prioritized.COMPARATOR);
        writers.sort(Prioritized.COMPARATOR);
        return new BodyInterchange(readers, writers);
    }

    @Eager
    @Singleton
    @Factory
    public ServerHolder serverFactory(ProviderRegistry providerRegistry,
                                      List<DoctorProvider<Filter>> filters,
                                      List<DoctorProvider<Handler>> handlers,
                                      List<DoctorProvider<Websocket>> websockets,
                                      List<ExceptionHandler> exceptionHandlers,
                                      List<PipelineCustomizer> pipelineCustomizers,
                                      List<ServerBootstrapCustomizer> serverBootstrapCustomizers,
                                      BodyInterchange bodyInterchange,
                                      EventBus eventBus) {

        ConfigurationFacade httpConf = providerRegistry.configuration().prefix("doctor.reactor.http.");

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
            return new ServerHolder(null);
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
        builder.setReactiveBodyMaxBuffer(httpConf.get("reactiveBodyMaxBuffer", Flow.defaultBufferSize(), Integer::valueOf));
        builder.setRouterPrefix(httpConf.get("routePrefix", ""));
        builder.setCaseInsensitiveMatching(httpConf.get("caseInsensitiveMatching", true, Boolean::valueOf));

        builder.setPipelineCustomizers(pipelineCustomizers);
        builder.setServerBootstrapCustomizers(serverBootstrapCustomizers);
        builder.setExceptionHandler(new CompositeExceptionHandler(exceptionHandlers));

        for (DoctorProvider<Filter> filter : filters) {
            List<String> paths = filter.annotationMetadata().findOne(Endpoint.class)
                    .map(endpoint -> endpoint.stringArrayValue("value"))
                    .orElse(Collections.singletonList(Router.MATCH_ALL_PATH_SPEC));
            Filter f = filter.get();
            for (String path : paths) {
                builder.filter(path, f);
            }
        }

        for (DoctorProvider<Handler> handler : handlers) {
            List<String> methods = new LinkedList<>();
            for (AnnotationData annotationMetadata : handler.annotationMetadata()) {
                HttpMethod method = annotationMetadata.type().getAnnotation(HttpMethod.class);
                if (method != null) {
                    methods.add(method.value());
                }
            }
            if (methods.isEmpty()) {
                throw new IllegalArgumentException("missing http method for Handler: " + handler);
            }
            List<String> paths;
            try {
                paths = handler.annotationMetadata().stringArrayValue(Endpoint.class, "value");
            } catch (IllegalArgumentException e) {
                throw new IllegalArgumentException("missing @Endpoint for Handler: " + handler);
            }
            Handler h = handler.get();
            for (String method : methods) {
                for (String path : paths) {
                    builder.route(method, path, h);
                }
            }
        }

        for (GeneratedHandler generatedHandler : ServiceLoader.load(GeneratedHandler.class)) {
            generatedHandler.init(providerRegistry, builder.router(), bodyInterchange);
        }

        websockets.forEach(provider -> builder.ws(provider::get));

        Server server = builder.start();
        eventBus.publish(new ServiceStarted("reactor-http", server));
        return new ServerHolder(server);
    }

    public record ServerHolder(Server server) implements AutoCloseable {
        @Override
        public void close() throws Exception {
            if (server != null) {
                server.close();
            }
        }
    }
}
