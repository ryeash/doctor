package org.glassfish.jersey.netty.httpserver;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.binder.netty4.NettyAllocatorMetrics;
import io.micrometer.core.instrument.binder.netty4.NettyEventExecutorMetrics;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.buffer.UnpooledByteBufAllocator;
import io.netty.channel.Channel;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.compression.StandardCompressionOptions;
import io.netty.handler.codec.http.HttpContentCompressor;
import io.netty.handler.codec.http.HttpContentDecompressor;
import io.netty.handler.codec.http.HttpDecoderConfig;
import io.netty.handler.codec.http.HttpServerCodec;
import io.netty.handler.codec.http.HttpServerExpectContinueHandler;
import io.netty.handler.ssl.SslContext;
import io.netty.handler.stream.ChunkedWriteHandler;
import jakarta.ws.rs.ProcessingException;
import org.glassfish.jersey.server.ResourceConfig;
import vest.doctor.CustomThreadFactory;
import vest.doctor.jaxrs.JAXRSConfiguration;
import vest.doctor.jaxrs.ServerBootstrapCustomizer;

import java.net.URI;
import java.util.List;
import java.util.Optional;

public class NettyJersey extends ChannelInitializer<SocketChannel> implements AutoCloseable {

    private final JAXRSConfiguration config;
    private final SslContext sslContext;
    private final NettyHttpContainer container;
    private final EventLoopGroup bossGroup;
    private final EventLoopGroup workerGroup;
    private final String applicationPath;
    private final URI baseUri;
    private final Channel channel;
    private final HttpDecoderConfig httpDecoderConfig;

    public NettyJersey(JAXRSConfiguration config,
                       SslContext sslContext,
                       ResourceConfig resourceConfig,
                       List<ServerBootstrapCustomizer> serverBootstrapCustomizers,
                       Optional<MeterRegistry> meterRegistry) {
        this.config = config;
        this.sslContext = sslContext;
        this.container = new NettyHttpContainer(resourceConfig);
        this.bossGroup = new NioEventLoopGroup(config.tcpManagementThreads().orElse(1),
                new CustomThreadFactory(false, config.tcpThreadFormat().orElse("tcp-%d"), null, null));
        this.workerGroup = new NioEventLoopGroup(config.workerThreads().orElse(0),
                new CustomThreadFactory(true, config.workerThreadFormat().orElse("worker-%d"), null, null));

        URI uri = URI.create(config.bindAddress().orElseThrow());

        System.setProperty("jersey.max.http.request.entitySizeMb", config.maxContentLength()
                .map(String::valueOf)
                .orElse("8"));

        this.applicationPath = container.getApplicationHandler().getConfiguration().getApplicationPath();
        String uriPath = applicationPath == null ? uri.toString() : uri + "/" + applicationPath + "/";
        int doubleSlash = uriPath.indexOf("/") + 2;
        String path = uriPath.substring(0, doubleSlash) + uriPath.substring(doubleSlash).replaceAll("/{2,}", "/");
        this.baseUri = URI.create(path);

        UnpooledByteBufAllocator bufAllocator = new UnpooledByteBufAllocator(true);
        meterRegistry.ifPresent(mr -> {
            new NettyEventExecutorMetrics(bossGroup).bindTo(mr);
            new NettyEventExecutorMetrics(workerGroup).bindTo(mr);
            new NettyAllocatorMetrics(bufAllocator).bindTo(mr);
        });

        ServerBootstrap serverBootstrap = new ServerBootstrap()
                .group(bossGroup, workerGroup)
                .option(ChannelOption.SO_BACKLOG, config.socketBacklog().orElse(1024))
                .option(ChannelOption.SO_KEEPALIVE, true)
                .option(ChannelOption.TCP_NODELAY, true)
                .option(ChannelOption.ALLOCATOR, bufAllocator)
                .channel(NioServerSocketChannel.class)
                .childHandler(this);

        serverBootstrap = serverBootstrapCustomizers.stream()
                .reduce(serverBootstrap, (c, customizer) -> customizer.customize(c), (a, b) -> b);

        this.httpDecoderConfig = new HttpDecoderConfig();
        httpDecoderConfig.setChunkedSupported(true);
        httpDecoderConfig.setAllowPartialChunks(true);
        httpDecoderConfig.setAllowDuplicateContentLengths(false);
        httpDecoderConfig.setValidateHeaders(config.validateHeaders().orElse(true));
        config.maxInitialLineLength().ifPresent(httpDecoderConfig::setMaxInitialLineLength);
        config.maxHeaderSize().ifPresent(httpDecoderConfig::setMaxHeaderSize);
        config.maxChunkSize().ifPresent(httpDecoderConfig::setMaxChunkSize);
        config.initialBufferSize().ifPresent(httpDecoderConfig::setInitialBufferSize);

        try {
            this.channel = serverBootstrap.bind(uri.getPort()).sync().channel();
        } catch (InterruptedException e) {
            throw new ProcessingException(e);
        }
    }

    @Override
    protected void initChannel(SocketChannel ch) {
        ChannelPipeline p = ch.pipeline();
        if (sslContext != null) {
            p.addLast(sslContext.newHandler(ch.alloc()));
        }
        p.addLast(new HttpServerCodec(httpDecoderConfig));
        p.addLast(new HttpContentDecompressor());
        p.addLast(new HttpContentCompressor(config.minGzipSize().orElse(814), StandardCompressionOptions.gzip()));
        p.addLast(new HttpServerExpectContinueHandler());
        p.addLast(new ChunkedWriteHandler());
        p.addLast(new JerseyServerHandler(baseUri, applicationPath, container, container.getConfiguration()));
    }

    @Override
    public void close() throws Exception {
        container.getApplicationHandler().onShutdown(container);
        bossGroup.shutdownGracefully();
        workerGroup.shutdownGracefully();
        channel.close();
    }
}
