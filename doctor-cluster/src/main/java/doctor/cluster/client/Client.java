package doctor.cluster.client;

import doctor.cluster.MessageDecoder;
import doctor.cluster.MessageEncoder;
import doctor.cluster.model.MessageContainer;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.pool.AbstractChannelPoolMap;
import io.netty.channel.pool.ChannelPool;
import io.netty.channel.pool.ChannelPoolHandler;
import io.netty.channel.pool.ChannelPoolMap;
import io.netty.channel.pool.FixedChannelPool;
import io.netty.channel.socket.nio.NioSocketChannel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetSocketAddress;
import java.util.concurrent.CompletableFuture;
import java.util.function.BiConsumer;

public class Client extends ChannelInboundHandlerAdapter {

    private static final Logger log = LoggerFactory.getLogger(Client.class);
    private final EventLoopGroup group;
    private final ChannelPoolMap<InetSocketAddress, ChannelPool> poolMap;
    private final BiConsumer<ChannelHandlerContext, MessageContainer> handler;

    public Client(BiConsumer<ChannelHandlerContext, MessageContainer> handler) {
        this.handler = handler;
        this.group = new NioEventLoopGroup(1);
        this.poolMap = new AbstractChannelPoolMap<InetSocketAddress, ChannelPool>() {
            @Override
            protected ChannelPool newPool(InetSocketAddress key) {
                return createPool(key);
            }
        };
    }

    public CompletableFuture<MessageContainer> send(InetSocketAddress address, MessageContainer message) {
        CompletableFuture<MessageContainer> future = new CompletableFuture<>();
        poolMap.get(address)
                .acquire()
                .addListener((f) -> {
                    Channel ch = (Channel) f.getNow();
                    ch.writeAndFlush(message);
                });
        return future;
    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) {
        // Send the first message if this handler is a client-side handler.
//        ctx.writeAndFlush(firstMessage);
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) {
        MessageContainer message = (MessageContainer) msg;
        handler.accept(ctx, message);
    }

    @Override
    public void channelReadComplete(ChannelHandlerContext ctx) {
        ctx.flush();
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        cause.printStackTrace();
        ctx.close();
    }

    private ChannelPool createPool(InetSocketAddress address) {
//        try {
        Bootstrap b = new Bootstrap();
        b.group(group).channel(NioSocketChannel.class);
        // TODO: do we do this here?
//            b.connect(address.getHostName(), address.getPort());
        ManagedChannelPoolHandler handler = new ManagedChannelPoolHandler(this, address);
        return new FixedChannelPool(b, handler, 32, 1024);
//        } finally {
//            group.shutdownGracefully();
//        }
    }

    private static final class ManagedChannelPoolHandler implements ChannelPoolHandler {

        private final Client connectionManager;
        private final InetSocketAddress address;

        public ManagedChannelPoolHandler(Client client, InetSocketAddress address) {
            this.connectionManager = client;
            this.address = address;
        }

        @Override
        public void channelReleased(Channel ch) {
        }

        @Override
        public void channelAcquired(Channel ch) {
        }

        @Override
        public void channelCreated(Channel ch) {
            ChannelPipeline p = ch.pipeline();
//            if (sslCtx != null) {
//                p.addLast(sslCtx.newHandler(ch.alloc(), HOST, PORT));
//            }
            p.addLast(
                    MessageEncoder.INSTANCE,
                    MessageDecoder.INSTANCE,
                    connectionManager);
            ch.connect(address);
        }
    }
}
