package vest.doctor.http.server.processing;

import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.SslContextBuilder;
import io.netty.handler.ssl.util.SelfSignedCertificate;
import jakarta.inject.Named;
import jakarta.inject.Singleton;
import vest.doctor.Activation;
import vest.doctor.AnnotationData;
import vest.doctor.Configuration;
import vest.doctor.DoctorProvider;
import vest.doctor.Eager;
import vest.doctor.Factory;
import vest.doctor.Prioritized;
import vest.doctor.ProviderRegistry;
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

import javax.net.ssl.SSLException;
import java.net.InetSocketAddress;
import java.security.cert.CertificateException;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;
import java.util.ServiceLoader;
import java.util.concurrent.Flow;

@Configuration
public class HttpBeanFactory {

    @Singleton
    @Factory
    @Activation(SelfSignedSSLActivation.class)
    @Named("server")
    public SslContext selfSignedSslFactory() throws CertificateException, SSLException {
        SelfSignedCertificate ssc = new SelfSignedCertificate();
        return SslContextBuilder.forServer(ssc.certificate(), ssc.privateKey()).build();
    }

    @Singleton
    @Factory
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
                                      HttpProperties httpConf,
                                      List<DoctorProvider<Filter>> filters,
                                      List<DoctorProvider<Handler>> handlers,
                                      List<DoctorProvider<Websocket>> websockets,
                                      List<ExceptionHandler> exceptionHandlers,
                                      List<PipelineCustomizer> pipelineCustomizers,
                                      List<ServerBootstrapCustomizer> serverBootstrapCustomizers,
                                      @Named("server") Optional<SslContext> sslContext,
                                      BodyInterchange bodyInterchange,
                                      EventBus eventBus) {
        HttpServerBuilder builder = new HttpServerBuilder();

        long binds = httpConf.bindAddresses()
                .stream()
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .map(s -> s.split(":"))
                .map(hp -> new InetSocketAddress(hp[0].trim(), Integer.parseInt(hp[1].trim())))
                .peek(builder::addBindAddress)
                .count();
        if (binds == 0L) {
            return new ServerHolder(null);
        }

        builder.setTcpManagementThreads(httpConf.tcpManagementThreads().orElse(1));
        builder.setTcpThreadFormat(httpConf.tcpThreadFormat().orElse("netty-tcp-%d"));
        builder.setSocketBacklog(httpConf.socketBacklog().orElse(1024));
        builder.setWorkerThreads(httpConf.workerThreads().orElse(Math.max(Runtime.getRuntime().availableProcessors() * 2, 8)));
        builder.setWorkerThreadFormat(httpConf.workerThreadFormat().orElse("netty-worker-%d"));

        sslContext.ifPresent(builder::setSslContext);

        builder.setMaxInitialLineLength(httpConf.maxInitialLineLength().orElse(8192));
        builder.setMaxHeaderSize(httpConf.maxHeaderSize().orElse(8192));
        builder.setMaxChunkSize(httpConf.maxChunkSize().orElse(8192));
        builder.setValidateHeaders(httpConf.validateHeaders().orElse(false));
        builder.setInitialBufferSize(httpConf.initialBufferSize().orElse(8192));
        builder.setMaxContentLength(httpConf.maxContentLength().orElse(4194304));
        builder.setMinGzipSize(httpConf.minGzipSize().orElse(812));
        builder.setReactiveBodyMaxBuffer(httpConf.reactiveBodyMaxBuffer().orElse(Flow.defaultBufferSize()));
        builder.setRouterPrefix(httpConf.routePrefix().orElse(""));
        builder.setCaseInsensitiveMatching(httpConf.caseInsensitiveMatching().orElse(true));

        builder.setPipelineCustomizers(pipelineCustomizers);
        builder.setServerBootstrapCustomizers(serverBootstrapCustomizers);
        builder.setExceptionHandler(new CompositeExceptionHandler(exceptionHandlers));

        for (DoctorProvider<Filter> filter : filters) {
            List<String> paths = filter.typeInfo().annotationMetadata().findOne(Endpoint.class)
                    .map(endpoint -> endpoint.stringArrayValue("value"))
                    .orElse(Collections.singletonList(Router.MATCH_ALL_PATH_SPEC));
            Filter f = filter.get();
            for (String path : paths) {
                builder.filter(path, f);
            }
        }

        for (DoctorProvider<Handler> handler : handlers) {
            List<String> methods = new LinkedList<>();
            for (AnnotationData annotationMetadata : handler.typeInfo().annotationMetadata()) {
                HttpMethod method = annotationMetadata.annotationType().getAnnotation(HttpMethod.class);
                if (method != null) {
                    methods.add(method.value());
                }
            }
            if (methods.isEmpty()) {
                throw new IllegalArgumentException("missing http method for Handler: " + handler);
            }
            List<String> paths;
            try {
                paths = handler.typeInfo().annotationMetadata().stringArrayValue(Endpoint.class, "value");
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

        for (DoctorProvider<Websocket> provider : websockets) {
            builder.ws(provider::get);
        }

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
