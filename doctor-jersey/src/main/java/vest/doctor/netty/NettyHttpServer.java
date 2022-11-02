package vest.doctor.netty;

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
import io.netty.handler.ssl.SslContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import vest.doctor.CustomThreadFactory;
import vest.doctor.ProviderRegistry;
import vest.doctor.jersey.JerseyHttpConfiguration;
import vest.doctor.runtime.LoggingUncaughtExceptionHandler;

import java.net.InetSocketAddress;
import java.util.List;
import java.util.stream.Collectors;

public class NettyHttpServer implements AutoCloseable {

    private static final Logger log = LoggerFactory.getLogger(NettyHttpServer.class);
    private final EventLoopGroup bossGroup;
    private final List<Channel> serverChannels;

    public NettyHttpServer(ProviderRegistry providerRegistry,
                           JerseyHttpConfiguration httpConfig,
                           ChannelHandler handler,
                           SslContext sslContext) {
        this.bossGroup = new NioEventLoopGroup(httpConfig.tcpManagementThreads().orElse(1),
                new CustomThreadFactory(false, httpConfig.tcpThreadFormat().orElse("netty-tcp-%d"), LoggingUncaughtExceptionHandler.INSTANCE, getClass().getClassLoader()));

        ServerBootstrap bootstrap = new ServerBootstrap();
        bootstrap.group(bossGroup);
        bootstrap.channelFactory(NioServerSocketChannel::new)
                .option(ChannelOption.SO_BACKLOG, httpConfig.socketBacklog().orElse(1024))
                .option(ChannelOption.ALLOCATOR, PooledByteBufAllocator.DEFAULT)
                .option(ChannelOption.RCVBUF_ALLOCATOR, new AdaptiveRecvByteBufAllocator())
                .childOption(ChannelOption.WRITE_BUFFER_WATER_MARK, WriteBufferWaterMark.DEFAULT)
                .childHandler(new HttpServerChannelInitializer(providerRegistry, handler, httpConfig, sslContext));

        providerRegistry.getInstances(ServerBootstrapCustomizer.class)
                .forEach(c -> c.customize(bootstrap));

        System.out.println(httpConfig.bindAddresses());
        this.serverChannels = httpConfig.bindAddresses()
                .stream()
                .map(s -> s.split(":"))
                .map(hp -> new InetSocketAddress(hp[0].trim(), Integer.parseInt(hp[1].trim())))
                .map(bootstrap::bind)
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
    }

    private static void doQuietly(Runnable runnable) {
        try {
            runnable.run();
        } catch (Throwable t) {
            // ignored
        }
    }
}
