package vest.doctor.netty;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.buffer.PooledByteBufAllocator;
import io.netty.channel.AdaptiveRecvByteBufAllocator;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandler.Sharable;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.WriteBufferWaterMark;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.http.HttpContentCompressor;
import io.netty.handler.codec.http.HttpContentDecompressor;
import io.netty.handler.codec.http.HttpServerCodec;
import io.netty.handler.ssl.SslContext;
import io.netty.handler.stream.ChunkedWriteHandler;
import io.netty.util.concurrent.DefaultThreadFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetSocketAddress;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;

@Sharable
public class HttpServer implements AutoCloseable {
    private static final Logger log = LoggerFactory.getLogger(HttpServer.class);

    private final NettyConfiguration config;
    private final EventLoopGroup bossGroup;
    private final List<Channel> serverChannels;
    private final HttpHandler httpHandler;
    private final SslContext sslContext;

    public HttpServer(NettyConfiguration config, Router router) {
        super();
        this.config = config;
        this.sslContext = config.getSslContext();
        try {
            bossGroup = new NioEventLoopGroup(config.getTcpManagementThreads(), new DefaultThreadFactory(config.getTcpThreadPrefix(), false));
            ServerBootstrap b = new ServerBootstrap()
                    .group(bossGroup)
                    .channel(NioServerSocketChannel.class)
                    .childOption(ChannelOption.WRITE_BUFFER_WATER_MARK, WriteBufferWaterMark.DEFAULT)
                    .option(ChannelOption.ALLOCATOR, PooledByteBufAllocator.DEFAULT)
                    .option(ChannelOption.RCVBUF_ALLOCATOR, new AdaptiveRecvByteBufAllocator())
                    .option(ChannelOption.SO_BACKLOG, config.getSocketBacklog())
                    .childHandler(new NettyChannelInit());

            this.serverChannels = new LinkedList<>();
            for (InetSocketAddress inetSocketAddress : config.getListenAddresses()) {
                log.info("netty http server binding to {}", inetSocketAddress);
                serverChannels.add(b.bind(inetSocketAddress).channel());
            }

            this.httpHandler = new HttpHandler(config, Objects.requireNonNull(router, "request handler may not be null"));
        } catch (Exception e) {
            throw new RuntimeException("error starting http server", e);
        }
    }

    @Override
    public void close() {
        for (Channel serverChannel : serverChannels) {
            try {
                serverChannel.close().sync();
            } catch (InterruptedException e) {
                log.trace("ignored", e);
            }
        }
        bossGroup.shutdownGracefully();
    }

    private final class NettyChannelInit extends ChannelInitializer<SocketChannel> {
        @Override
        protected void initChannel(SocketChannel ch) {
            ChannelPipeline p = ch.pipeline();
            if (sslContext != null) {
                p.addLast(sslContext.newHandler(ch.alloc()));
            }
            p.addLast(new HttpServerCodec(
                    config.getMaxInitialLineLength(),
                    config.getMaxHeaderSize(),
                    config.getMaxChunkSize(),
                    config.isValidateHeaders(),
                    config.getInitialBufferSize()));
            p.addLast(new HttpContentCompressor(6, 15, 8, 812));
            p.addLast(new HttpContentDecompressor());
//        p.addLast(new HttpObjectAggregator(config.getMaxContentLength()));
            p.addLast(new ChunkedWriteHandler());
            p.addLast(new HttpContentCompressor());
            p.addLast(httpHandler);
        }
    }
}