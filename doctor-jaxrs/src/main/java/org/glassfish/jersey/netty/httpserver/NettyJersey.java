package org.glassfish.jersey.netty.httpserver;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.compression.StandardCompressionOptions;
import io.netty.handler.codec.http.HttpContentCompressor;
import io.netty.handler.codec.http.HttpContentDecompressor;
import io.netty.handler.codec.http.HttpMessage;
import io.netty.handler.codec.http.HttpServerCodec;
import io.netty.handler.codec.http.HttpServerExpectContinueHandler;
import io.netty.handler.codec.http.HttpServerUpgradeHandler;
import io.netty.handler.codec.http2.Http2CodecUtil;
import io.netty.handler.codec.http2.Http2MultiplexCodecBuilder;
import io.netty.handler.codec.http2.Http2ServerUpgradeCodec;
import io.netty.handler.ssl.SslContext;
import io.netty.handler.stream.ChunkedWriteHandler;
import io.netty.util.AsciiString;
import jakarta.ws.rs.ProcessingException;
import org.glassfish.jersey.server.ResourceConfig;
import vest.doctor.CustomThreadFactory;
import vest.doctor.jaxrs.JAXRSConfiguration;

import java.net.URI;

public class NettyJersey {

    public static Channel jerseyNettyStartup(JAXRSConfiguration config, SslContext sslContext, ResourceConfig resourceConfig) {
        NettyHttpContainer container = new NettyHttpContainer(resourceConfig);
        EventLoopGroup bossGroup = new NioEventLoopGroup(config.tcpManagementThreads().orElse(1),
                new CustomThreadFactory(false, config.tcpThreadFormat().orElse("tcp-%d"), null, null));
        EventLoopGroup workerGroup = new NioEventLoopGroup(config.workerThreads().orElse(0),
                new CustomThreadFactory(true, config.workerThreadFormat().orElse("worker-%d"), null, null));

        URI uri = URI.create(config.bindAddress().orElseThrow());

        ServerBootstrap b = new ServerBootstrap();
        b.option(ChannelOption.SO_BACKLOG, config.socketBacklog().orElse(1024));
        b.group(bossGroup, workerGroup)
                .channel(NioServerSocketChannel.class)
                .childHandler(new JerseyServerInitializer(
                        uri,
                        sslContext,
                        container,
                        container.getConfiguration(),
                        false,
                        config));

        System.setProperty("jersey.max.http.request.entitySizeMb", config.maxContentLength()
                .map(String::valueOf)
                .orElse("8"));
        try {
            Channel ch = b.bind(uri.getPort()).sync().channel();
            ch.closeFuture().addListener(future -> {
                container.getApplicationHandler().onShutdown(container);
                bossGroup.shutdownGracefully();
                workerGroup.shutdownGracefully();
            });
            return ch;
        } catch (InterruptedException e) {
            throw new ProcessingException(e);
        }
    }

    static final class JerseyServerInitializer extends ChannelInitializer<SocketChannel> {

        private final URI baseUri;
        private final String applicationPath;
        private final SslContext sslCtx;
        private final NettyHttpContainer container;
        private final boolean http2;
        private final ResourceConfig resourceConfig;
        private final JAXRSConfiguration jaxrsConfiguration;

        public JerseyServerInitializer(URI baseUri, SslContext sslCtx, NettyHttpContainer container, ResourceConfig resourceConfig, boolean http2, JAXRSConfiguration jaxrsConfiguration) {
            applicationPath = container.getApplicationHandler().getConfiguration().getApplicationPath();
            String uriPath = applicationPath == null
                    ? baseUri.toString()
                    : baseUri.toString() + "/" + applicationPath + "/";
            int doubleSlash = uriPath.indexOf("/") + 2;
            String path = uriPath.substring(0, doubleSlash) + uriPath.substring(doubleSlash).replaceAll("/{2,}", "/");

            this.baseUri = URI.create(path);
            this.sslCtx = sslCtx;
            this.container = container;
            this.resourceConfig = resourceConfig;
            this.http2 = http2;
            this.jaxrsConfiguration = jaxrsConfiguration;
        }

        @Override
        public void initChannel(SocketChannel ch) {
            if (http2) {
                if (sslCtx != null) {
                    configureSsl(ch);
                } else {
                    configureClearText(ch);
                }
            } else {
                ChannelPipeline p = ch.pipeline();
                if (sslCtx != null) {
                    p.addLast(sslCtx.newHandler(ch.alloc()));
                }
                p.addLast(new HttpServerCodec(
                        jaxrsConfiguration.maxInitialLineLength().orElse(8192),
                        jaxrsConfiguration.maxHeaderSize().orElse(8192),
                        jaxrsConfiguration.maxChunkSize().orElse(8192),
                        jaxrsConfiguration.validateHeaders().orElse(false),
                        jaxrsConfiguration.initialBufferSize().orElse(256)
                ));
                p.addLast(new HttpContentDecompressor());
                p.addLast(new HttpContentCompressor(jaxrsConfiguration.minGzipSize().orElse(814), StandardCompressionOptions.gzip()));
                p.addLast(new HttpServerExpectContinueHandler());
                p.addLast(new ChunkedWriteHandler());
                p.addLast(new JerseyServerHandler(baseUri, applicationPath, container, resourceConfig));
            }
        }

        /**
         * Configure the pipeline for TLS NPN negotiation to HTTP/2.
         */
        private void configureSsl(SocketChannel ch) {
            ch.pipeline().addLast(sslCtx.newHandler(ch.alloc()), new HttpVersionChooser(baseUri, container, resourceConfig));
        }

        /**
         * Configure the pipeline for a cleartext upgrade from HTTP to HTTP/2.
         */
        private void configureClearText(SocketChannel ch) {
            ChannelPipeline p = ch.pipeline();
            HttpServerCodec sourceCodec = new HttpServerCodec();
            p.addLast(sourceCodec);
            p.addLast("respondExpectContinue", new HttpServerExpectContinueHandler());
            p.addLast(new HttpServerUpgradeHandler(sourceCodec, protocol -> {
                if (AsciiString.contentEquals(Http2CodecUtil.HTTP_UPGRADE_PROTOCOL_NAME, protocol)) {
                    return new Http2ServerUpgradeCodec(Http2MultiplexCodecBuilder.forServer(new JerseyHttp2ServerHandler(baseUri, container, resourceConfig)).build());
                } else {
                    return null;
                }
            }));
            p.addLast(new SimpleChannelInboundHandler<HttpMessage>() {
                @Override
                protected void channelRead0(ChannelHandlerContext ctx, HttpMessage msg) {
                    ChannelPipeline pipeline = ctx.pipeline();
                    ChannelHandlerContext thisCtx = pipeline.context(this);
                    pipeline.addAfter(thisCtx.name(), null, new JerseyServerHandler(baseUri, container, resourceConfig));
                    pipeline.replace(this, null, new ChunkedWriteHandler());
                    ctx.fireChannelRead(msg);
                }
            });
        }
    }
}
