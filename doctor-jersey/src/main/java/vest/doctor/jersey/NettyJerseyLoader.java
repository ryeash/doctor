package vest.doctor.jersey;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.buffer.PooledByteBufAllocator;
import io.netty.channel.AdaptiveRecvByteBufAllocator;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.WriteBufferWaterMark;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.SslContextBuilder;
import io.netty.handler.ssl.util.SelfSignedCertificate;
import jakarta.ws.rs.Path;
import org.glassfish.jersey.server.ResourceConfig;
import vest.doctor.ApplicationLoader;
import vest.doctor.ConfigurationFacade;
import vest.doctor.CustomThreadFactory;
import vest.doctor.DoctorProvider;
import vest.doctor.ProviderRegistry;

import java.io.File;
import java.net.InetSocketAddress;
import java.util.List;
import java.util.Map;
import java.util.Objects;
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

        config.register(new DoctorBinder(providerRegistry));
        config.register(ProvidedValueParamProvider.class);

        HttpServerConfiguration httpConfig = buildConf(providerRegistry);

        EventLoopGroup bossGroup = new NioEventLoopGroup(httpConfig.getTcpManagementThreads(), new CustomThreadFactory(true, httpConfig.getTcpThreadNameFormat(), LoggingUncaughtExceptionHandler.INSTANCE, JerseyChannelAdapter.class.getClassLoader()));
        EventLoopGroup workerGroup = new NioEventLoopGroup(httpConfig.getWorkerThreads(), new CustomThreadFactory(true, httpConfig.getWorkerThreadFormat(), LoggingUncaughtExceptionHandler.INSTANCE, JerseyChannelAdapter.class.getClassLoader()));
        DoctorJerseyContainer container = new DoctorJerseyContainer(config, workerGroup);

        ServerBootstrap b = new ServerBootstrap()
                .group(bossGroup)
                .channel(NioServerSocketChannel.class)
                .option(ChannelOption.SO_BACKLOG, httpConfig.getSocketBacklog())
                .option(ChannelOption.ALLOCATOR, PooledByteBufAllocator.DEFAULT)
                .option(ChannelOption.RCVBUF_ALLOCATOR, new AdaptiveRecvByteBufAllocator())
                .childOption(ChannelOption.WRITE_BUFFER_WATER_MARK, WriteBufferWaterMark.DEFAULT)
                .childHandler(new DoctorChannelInitializer(httpConfig, container, config, providerRegistry));

        List<Channel> channels = httpConfig.getBindAddresses()
                .stream()
                .map(b::bind)
                .map(ChannelFuture::syncUninterruptibly)
                .map(ChannelFuture::channel)
                .collect(Collectors.toList());

        channels.get(0)
                .closeFuture()
                .addListener(future -> {
                    container.getApplicationHandler().onShutdown(container);
                    bossGroup.shutdownGracefully();
                    workerGroup.shutdownGracefully();
                });
        ServerHolder holder = new ServerHolder(channels);
        Runtime.getRuntime().addShutdownHook(new Thread(holder::close, "netty-server-shutdown"));
    }

    private HttpServerConfiguration buildConf(ProviderRegistry providerRegistry) {
        ConfigurationFacade httpConf = providerRegistry.configuration().subsection("doctor.jersey.");

        HttpServerConfiguration conf = new HttpServerConfiguration();
        conf.setTcpManagementThreads(httpConf.get("tcp.threads", 1, Integer::valueOf));
        conf.setTcpThreadNameFormat(httpConf.get("tcp.threadFormat", "netty-jersey-tcp-%d"));
        conf.setWorkerThreads(httpConf.get("worker.threads", 16, Integer::valueOf));
        conf.setWorkerThreadFormat(httpConf.get("worker.threadFormat", "netty-jersey-worker-%d"));
        conf.setSocketBacklog(httpConf.get("tcp.socketBacklog", 1024, Integer::valueOf));

        List<InetSocketAddress> bind = httpConf.getList("bind", List.of("localhost:9998"), Function.identity())
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
        return conf;
    }

    private record ServerHolder(List<Channel> channels) implements AutoCloseable {
        @Override
        public void close() {
            for (Channel channel : channels) {
                channel.close();
            }
        }
    }
}
