package vest.doctor.jersey;

import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.SslContextBuilder;
import io.netty.handler.ssl.util.SelfSignedCertificate;
import jakarta.ws.rs.Path;
import org.glassfish.jersey.server.ResourceConfig;
import vest.doctor.AdHocProvider;
import vest.doctor.ApplicationLoader;
import vest.doctor.DoctorProvider;
import vest.doctor.ProviderRegistry;
import vest.doctor.conf.ConfigurationFacade;
import vest.doctor.event.EventBus;
import vest.doctor.event.ServiceStarted;
import vest.doctor.jersey.ext.DoctorCustomValueParamProvider;
import vest.doctor.netty.HttpServerConfiguration;
import vest.doctor.netty.NettyHttpServer;
import vest.doctor.netty.PipelineCustomizer;
import vest.doctor.netty.ServerBootstrapCustomizer;

import java.io.File;
import java.net.InetSocketAddress;
import java.util.List;
import java.util.Objects;
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

        config.register(new DoctorCustomValueParamProvider(providerRegistry));
        config.register(new DoctorBinder(providerRegistry));

        providerRegistry.getProvidersWithAnnotation(Path.class)
                .map(DoctorProvider::type)
                .forEach(config::register);

        providerRegistry.getProviders(ResourceConfigCustomizer.class)
                .map(DoctorProvider::get)
                .forEach(c -> c.customize(config));

        DoctorJerseyContainer container = new DoctorJerseyContainer(config);
        NettyHttpServer httpServer = new NettyHttpServer(
                httpConfig,
                new JerseyChannelAdapter(httpConfig, container, providerRegistry),
                false);
        providerRegistry.register(new AdHocProvider<>(NettyHttpServer.class, httpServer, "jersey", httpServer));
        providerRegistry.register(new AdHocProvider<>(DoctorJerseyContainer.class, container, "doctorJerseyContainer", container));
        providerRegistry.getInstance(EventBus.class).publish(new ServiceStarted("netty-jersey-http", httpServer));
    }

    public HttpServerConfiguration init(ProviderRegistry providerRegistry) {
        HttpServerConfiguration httpConfig = new HttpServerConfiguration();
        ConfigurationFacade cf = providerRegistry.configuration().getSubConfiguration("doctor.jersey.http");

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
