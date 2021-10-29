package vest.doctor.jersey;

import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.SslContextBuilder;
import io.netty.handler.ssl.util.SelfSignedCertificate;
import jakarta.ws.rs.Path;
import org.glassfish.jersey.server.ResourceConfig;
import vest.doctor.ApplicationLoader;
import vest.doctor.ConfigurationFacade;
import vest.doctor.DoctorProvider;
import vest.doctor.ProviderRegistry;
import vest.doctor.event.EventBus;
import vest.doctor.event.EventProducer;
import vest.doctor.event.ServiceStarted;
import vest.doctor.event.ServiceStopped;
import vest.doctor.netty.common.HttpServerConfiguration;
import vest.doctor.netty.common.NettyHttpServer;
import vest.doctor.netty.common.PipelineCustomizer;
import vest.doctor.netty.common.ServerBootstrapCustomizer;

import java.io.File;
import java.net.InetSocketAddress;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

public final class NettyJerseyLoader implements ApplicationLoader {

    @Override
    public void stage5(ProviderRegistry providerRegistry) {
        HttpServerConfiguration httpConfig = init(providerRegistry);
        if (httpConfig.getBindAddresses() == null || httpConfig.getBindAddresses().isEmpty()) {
            return;
        }

        ResourceConfig config = new ResourceConfig();
        for (Map.Entry<Object, Object> entry : providerRegistry.configuration().toProperties().entrySet()) {
            config.property((String) entry.getKey(), entry.getValue());
        }

        providerRegistry.getProvidersWithAnnotation(Path.class)
                .map(DoctorProvider::type)
                .forEach(config::register);

        providerRegistry.getProviders(ResourceConfigCustomizer.class)
                .map(DoctorProvider::get)
                .forEach(c -> c.customize(config));

        config.register(DoctorCustomValueParamProvider.class);
        config.register(ContextParamsProvider.class);
        config.register(new DoctorBinder(providerRegistry));


        DoctorJerseyContainer container = new DoctorJerseyContainer(config);
        NettyHttpServer httpServer = new NettyHttpServer(
                httpConfig,
                new JerseyChannelAdapter(httpConfig, container, providerRegistry),
                false);

        Optional<EventBus> eventBusOpt = providerRegistry.getInstanceOpt(EventBus.class);
        eventBusOpt.ifPresent(eventBus -> eventBus.publish(new ServiceStarted("netty-jersey-http", httpServer)));

        providerRegistry.shutdownContainer().register(() -> {
            httpServer.close();
            providerRegistry.getInstance(EventProducer.class)
                    .publish(new ServiceStopped("netty-jersey-http", httpServer));
        });
        Runtime.getRuntime().addShutdownHook(new Thread(() -> container.getApplicationHandler().onShutdown(container), "netty-server-shutdown"));
    }

    public HttpServerConfiguration init(ProviderRegistry providerRegistry) {
        HttpServerConfiguration httpConfig = new HttpServerConfiguration();
        ConfigurationFacade cf = providerRegistry.configuration().subsection("doctor.jersey.http.");

        httpConfig.setTcpManagementThreads(cf.get("tcp.threads", 1, Integer::valueOf));
        httpConfig.setTcpThreadFormat(cf.get("tcp.threadFormat", "netty-jersey-tcp-%d"));
        httpConfig.setSocketBacklog(cf.get("tcp.socketBacklog", 1024, Integer::valueOf));
        httpConfig.setWorkerThreads(cf.get("worker.threads", 16, Integer::valueOf));
        httpConfig.setWorkerThreadFormat(cf.get("worker.threadFormat", "netty-jersey-worker-%d"));

        List<InetSocketAddress> bind = cf.getList("bind", List.of("localhost:9998"), Function.identity())
                .stream()
                .map(s -> s.split(":"))
                .map(hp -> new InetSocketAddress(hp[0].trim(), Integer.parseInt(hp[1].trim())))
                .collect(Collectors.toList());
        httpConfig.setBindAddresses(bind);

        try {
            if (cf.get("ssl.selfSigned", false, Boolean::valueOf)) {
                SelfSignedCertificate ssc = new SelfSignedCertificate();
                SslContext sslContext = SslContextBuilder.forServer(ssc.certificate(), ssc.privateKey()).build();
                httpConfig.setSslContext(sslContext);
            }

            String keyCertChainFile = cf.get("ssl.keyCertChainFile");
            if (keyCertChainFile != null && !keyCertChainFile.isEmpty()) {
                String keyFile = Objects.requireNonNull(cf.get("ssl.keyFile"), "missing ssl key file configuration");
                String keyPassword = cf.get("ssl.keyPassword");
                SslContext sslContext = SslContextBuilder.forServer(new File(keyCertChainFile), new File(keyFile), keyPassword).build();
                httpConfig.setSslContext(sslContext);
            }
        } catch (Throwable t) {
            throw new RuntimeException("error configuring ssl", t);
        }

        httpConfig.setMaxInitialLineLength(cf.get("maxInitialLineLength", 8192, Integer::valueOf));
        httpConfig.setMaxHeaderSize(cf.get("maxHeaderSize", 8192, Integer::valueOf));
        httpConfig.setMaxChunkSize(cf.get("maxChunkSize", 8192, Integer::valueOf));
        httpConfig.setValidateHeaders(cf.get("validateHeaders", false, Boolean::valueOf));
        httpConfig.setInitialBufferSize(cf.get("initialBufferSize", 8192, Integer::valueOf));
        httpConfig.setMaxContentLength(cf.get("maxContentLength", 8388608, Integer::valueOf));
        httpConfig.setMinGzipSize(cf.get("minGzipSize", 812, Integer::valueOf));

        List<PipelineCustomizer> pipelineCustomizers = providerRegistry.getProviders(PipelineCustomizer.class)
                .map(DoctorProvider::get)
                .collect(Collectors.toList());
        pipelineCustomizers.add(new HttpAggregatorCustomizer(httpConfig.getMaxContentLength()));
        httpConfig.setPipelineCustomizers(pipelineCustomizers);

        List<ServerBootstrapCustomizer> serverBootstrapCustomizers = providerRegistry.getProviders(ServerBootstrapCustomizer.class)
                .map(DoctorProvider::get)
                .collect(Collectors.toList());
        httpConfig.setServerBootstrapCustomizers(serverBootstrapCustomizers);
        return httpConfig;
    }
}
