package vest.doctor.netty;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.buffer.PooledByteBufAllocator;
import io.netty.channel.AdaptiveRecvByteBufAllocator;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.WriteBufferWaterMark;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.http.HttpContentCompressor;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http.HttpServerCodec;
import io.netty.handler.stream.ChunkedWriteHandler;
import io.netty.util.concurrent.DefaultThreadFactory;

import java.net.InetSocketAddress;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;

@ChannelHandler.Sharable
public class HttpServer extends ChannelInitializer<SocketChannel> implements AutoCloseable {

    private final NettyConfiguration config;
    private final EventLoopGroup bossGroup;
    private final EventLoopGroup workerGroup;
    private final List<Channel> serverChannels;

    private HttpHandler httpHandler;

    public HttpServer(NettyConfiguration config) {
        super();
        this.config = config;
        try {
            bossGroup = new NioEventLoopGroup(config.getTcpManagementThreads(), new DefaultThreadFactory(config.getTcpThreadPrefix(), false));
            workerGroup = new NioEventLoopGroup(config.getWorkerThreads(), new DefaultThreadFactory(config.getWorkerThreadPrefix(), true));
            ServerBootstrap b = new ServerBootstrap()
                    .group(bossGroup, workerGroup)
                    .channel(NioServerSocketChannel.class)
                    .childOption(ChannelOption.WRITE_BUFFER_WATER_MARK, WriteBufferWaterMark.DEFAULT)
                    .option(ChannelOption.ALLOCATOR, PooledByteBufAllocator.DEFAULT)
                    .option(ChannelOption.RCVBUF_ALLOCATOR, new AdaptiveRecvByteBufAllocator())
                    .option(ChannelOption.SO_BACKLOG, config.getSocketBacklog())
                    .childHandler(this);

            this.serverChannels = new LinkedList<>();
            for (InetSocketAddress inetSocketAddress : config.getListenAddresses()) {
                System.out.println("netty binding to " + inetSocketAddress);
                serverChannels.add(b.bind(inetSocketAddress).channel());
            }
        } catch (Exception e) {
            throw new RuntimeException("error starting http server", e);
        }
    }

    public void setRequestHandler(Route requestHandler) {
        this.httpHandler = new HttpHandler(config, Objects.requireNonNull(requestHandler, "request handler may not be null"));
    }

    @Override
    public void close() {
        for (Channel serverChannel : serverChannels) {
            try {
                serverChannel.close().sync();
            } catch (InterruptedException e) {
//                log.trace("ignored", e);
            }
        }
        workerGroup.shutdownGracefully();
        bossGroup.shutdownGracefully();
    }

    @Override
    protected void initChannel(SocketChannel ch) {
        if (httpHandler == null) {
            throw new IllegalStateException("no handler has been found");
        }

        ChannelPipeline p = ch.pipeline();
        if (config.getSslContext() != null) {
            p.addLast(config.getSslContext().newHandler(ch.alloc()));
        }
        p.addLast(new HttpServerCodec(
                config.getMaxInitialLineLength(),
                config.getMaxHeaderSize(),
                config.getMaxChunkSize(),
                config.isValidateHeaders(),
                config.getInitialBufferSize()));
        // p.addLast(new HttpContentDecompressor());
        p.addLast(new HttpObjectAggregator(config.getMaxContentLength()));
        p.addLast(new ChunkedWriteHandler());
        p.addLast(new HttpContentCompressor());
//        p.addLast(websocketHandler);
        p.addLast(httpHandler);
    }
}