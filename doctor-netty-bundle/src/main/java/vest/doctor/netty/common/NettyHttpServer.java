package vest.doctor.netty.common;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.buffer.PooledByteBufAllocator;
import io.netty.channel.AdaptiveRecvByteBufAllocator;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.WriteBufferWaterMark;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import vest.doctor.CustomThreadFactory;
import vest.doctor.runtime.LoggingUncaughtExceptionHandler;

import java.util.List;
import java.util.stream.Collectors;

public class NettyHttpServer implements AutoCloseable {

    private static final Logger log = LoggerFactory.getLogger(NettyHttpServer.class);
    private final EventLoopGroup bossGroup;
    private final EventLoopGroup workerGroup;
    private final List<Channel> serverChannels;

    public NettyHttpServer(HttpServerConfiguration httpConfig, ChannelHandler handler, boolean useChildGroup) {
        this.bossGroup = new NioEventLoopGroup(httpConfig.getTcpManagementThreads(), new CustomThreadFactory(false, httpConfig.getTcpThreadFormat(), LoggingUncaughtExceptionHandler.INSTANCE, getClass().getClassLoader()));

        ServerBootstrap b = new ServerBootstrap();
        if (useChildGroup) {
            this.workerGroup = new NioEventLoopGroup(httpConfig.getWorkerThreads(), new CustomThreadFactory(true, httpConfig.getWorkerThreadFormat(), LoggingUncaughtExceptionHandler.INSTANCE, getClass().getClassLoader()));
            b.group(bossGroup, workerGroup);
        } else {
            this.workerGroup = null;
            b.group(bossGroup);
        }
        b.channelFactory(NioServerSocketChannel::new)
                .option(ChannelOption.SO_BACKLOG, httpConfig.getSocketBacklog())
                .option(ChannelOption.ALLOCATOR, PooledByteBufAllocator.DEFAULT)
                .option(ChannelOption.RCVBUF_ALLOCATOR, new AdaptiveRecvByteBufAllocator())
                .childOption(ChannelOption.WRITE_BUFFER_WATER_MARK, WriteBufferWaterMark.DEFAULT)
                .childHandler(new HttpServerChannelInitializer(handler, httpConfig));

        this.serverChannels = httpConfig.getBindAddresses()
                .stream()
                .map(b::bind)
                .map(ChannelFuture::syncUninterruptibly)
                .map(ChannelFuture::channel)
                .collect(Collectors.toList());
        log.info("netty http server binding to {}", serverChannels);
    }

    @Override
    public void close() {
        for (Channel serverChannel : serverChannels) {
            doQuietly(serverChannel::close);
        }
        doQuietly(bossGroup::shutdownGracefully);
        if (workerGroup != null) {
            doQuietly(workerGroup::shutdownGracefully);
        }
    }

    private static void doQuietly(Runnable runnable) {
        try {
            runnable.run();
        } catch (Throwable t) {
            // ignored
        }
    }
}
