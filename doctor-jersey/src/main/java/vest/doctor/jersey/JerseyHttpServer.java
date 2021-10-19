package vest.doctor.jersey;

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
import vest.doctor.CustomThreadFactory;
import vest.doctor.http.server.HttpServerConfiguration;
import vest.doctor.runtime.LoggingUncaughtExceptionHandler;

import java.util.List;
import java.util.stream.Collectors;

public class JerseyHttpServer implements AutoCloseable {

    private final HttpServerConfiguration httpConfig;
    private final EventLoopGroup bossGroup;
    private final List<Channel> serverChannels;

    public JerseyHttpServer(HttpServerConfiguration httpConfig, ChannelHandler handler) {
        this.httpConfig = httpConfig;
        this.bossGroup = new NioEventLoopGroup(httpConfig.getTcpManagementThreads(), new CustomThreadFactory(false, httpConfig.getTcpThreadFormat(), LoggingUncaughtExceptionHandler.INSTANCE, getClass().getClassLoader()));

        ServerBootstrap b = new ServerBootstrap()
                .group(bossGroup)
                .channel(NioServerSocketChannel.class)
                .option(ChannelOption.SO_BACKLOG, httpConfig.getSocketBacklog())
                .option(ChannelOption.ALLOCATOR, PooledByteBufAllocator.DEFAULT)
                .option(ChannelOption.RCVBUF_ALLOCATOR, new AdaptiveRecvByteBufAllocator())
                .childOption(ChannelOption.WRITE_BUFFER_WATER_MARK, WriteBufferWaterMark.DEFAULT)
                .childHandler(handler);

        this.serverChannels = httpConfig.getBindAddresses()
                .stream()
                .map(b::bind)
                .map(ChannelFuture::syncUninterruptibly)
                .map(ChannelFuture::channel)
                .collect(Collectors.toList());
    }

    @Override
    public void close() {
        for (Channel serverChannel : serverChannels) {
            doQuietly(serverChannel::close);
        }
        doQuietly(bossGroup::shutdownGracefully);
    }

    private static void doQuietly(Runnable runnable) {
        try {
            runnable.run();
        } catch (Throwable t) {
            // ignored
        }
    }
}
