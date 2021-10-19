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
import vest.doctor.http.server.HttpServerConfiguration;
import vest.doctor.http.server.PipelineCustomizer;
import vest.doctor.http.server.impl.HttpServerChannelInitializer;

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

        HttpServerConfiguration httpConfig = init(providerRegistry);

        DoctorJerseyContainer container = new DoctorJerseyContainer(config);
//        JerseyHttpServer jerseyHttpServer = new JerseyHttpServer(httpConfig, new DoctorChannelInitializer(httpConfig, container, providerRegistry));

        List<PipelineCustomizer> pipelineCustomizers = providerRegistry.getProviders(PipelineCustomizer.class)
                .map(DoctorProvider::get)
                .collect(Collectors.toList());
        JerseyHttpServer jerseyHttpServer = new JerseyHttpServer(httpConfig, new HttpServerChannelInitializer(new JerseyChannelAdapter(httpConfig, container, providerRegistry), httpConfig, pipelineCustomizers));


        Optional<EventBus> eventBusOpt = providerRegistry.getInstanceOpt(EventBus.class);
        eventBusOpt.ifPresent(eventBus -> eventBus.publish(new ServiceStarted("netty-jersey-http", jerseyHttpServer)));

        providerRegistry.shutdownContainer().register(() -> {
            jerseyHttpServer.close();
            providerRegistry.getInstance(EventProducer.class)
                    .publish(new ServiceStopped("netty-jersey-http", jerseyHttpServer));
        });
        Runtime.getRuntime().addShutdownHook(new Thread(() -> container.getApplicationHandler().onShutdown(container), "netty-server-shutdown"));
    }

    public HttpServerConfiguration init(ProviderRegistry providerRegistry) {
        HttpServerConfiguration httpConfig = new HttpServerConfiguration();
        ConfigurationFacade httpConf = providerRegistry.configuration().subsection("doctor.jersey.http.");

        httpConfig.setTcpManagementThreads(httpConf.get("tcp.threads", 1, Integer::valueOf));
        httpConfig.setTcpThreadFormat(httpConf.get("tcp.threadFormat", "netty-jersey-tcp-%d"));
        httpConfig.setWorkerThreads(httpConf.get("worker.threads", 16, Integer::valueOf));
        httpConfig.setWorkerThreadFormat(httpConf.get("worker.threadFormat", "netty-jersey-worker-%d"));
        httpConfig.setSocketBacklog(httpConf.get("tcp.socketBacklog", 1024, Integer::valueOf));

        List<InetSocketAddress> bind = httpConf.getList("bind", List.of("localhost:9998"), Function.identity())
                .stream()
                .map(s -> s.split(":"))
                .map(hp -> new InetSocketAddress(hp[0].trim(), Integer.parseInt(hp[1].trim())))
                .collect(Collectors.toList());
        httpConfig.setBindAddresses(bind);

        try {
            if (httpConf.get("ssl.selfSigned", false, Boolean::valueOf)) {
                SelfSignedCertificate ssc = new SelfSignedCertificate();
                SslContext sslContext = SslContextBuilder.forServer(ssc.certificate(), ssc.privateKey()).build();
                httpConfig.setSslContext(sslContext);
            }

            String keyCertChainFile = httpConf.get("ssl.keyCertChainFile");
            if (keyCertChainFile != null && !keyCertChainFile.isEmpty()) {
                String keyFile = Objects.requireNonNull(httpConf.get("ssl.keyFile"), "missing ssl key file configuration");
                String keyPassword = httpConf.get("ssl.keyPassword");
                SslContext sslContext = SslContextBuilder.forServer(new File(keyCertChainFile), new File(keyFile), keyPassword).build();
                httpConfig.setSslContext(sslContext);
            }
        } catch (Throwable t) {
            throw new RuntimeException("error configuring ssl", t);
        }

        httpConfig.setMaxInitialLineLength(httpConf.get("maxInitialLineLength", 8192, Integer::valueOf));
        httpConfig.setMaxHeaderSize(httpConf.get("maxHeaderSize", 8192, Integer::valueOf));
        httpConfig.setMaxChunkSize(httpConf.get("maxChunkSize", 8192, Integer::valueOf));
        httpConfig.setValidateHeaders(httpConf.get("validateHeaders", false, Boolean::valueOf));
        httpConfig.setInitialBufferSize(httpConf.get("initialBufferSize", 8192, Integer::valueOf));
        httpConfig.setMaxContentLength(httpConf.get("maxContentLength", 8388608, Integer::valueOf));
        return httpConfig;
    }
}
