package vest.doctor.http.server.impl;

import io.netty.channel.ChannelDuplexHandler;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPromise;
import io.netty.handler.codec.http.HttpObject;
import vest.doctor.http.server.HttpListener;

import java.net.SocketAddress;
import java.util.List;

@ChannelHandler.Sharable
public class HttpListenerManager extends ChannelDuplexHandler {

    private final List<HttpListener> listeners;

    public HttpListenerManager(List<HttpListener> listeners) {
        this.listeners = listeners;
    }

    @Override
    public void bind(ChannelHandlerContext ctx, SocketAddress localAddress, ChannelPromise promise) throws Exception {
        super.bind(ctx, localAddress, promise);
    }

    @Override
    public void connect(ChannelHandlerContext ctx, SocketAddress remoteAddress, SocketAddress localAddress, ChannelPromise promise) throws Exception {
        super.connect(ctx, remoteAddress, localAddress, promise);
        for (HttpListener listener : listeners) {
            listener.connect(ctx, remoteAddress, localAddress);
        }
    }

    @Override
    public void disconnect(ChannelHandlerContext ctx, ChannelPromise promise) throws Exception {
        for (HttpListener listener : listeners) {
            listener.disconnect(ctx);
        }
        super.disconnect(ctx, promise);
    }

    @Override
    public void close(ChannelHandlerContext ctx, ChannelPromise promise) throws Exception {
        for (HttpListener listener : listeners) {
            listener.close(ctx);
        }
        super.close(ctx, promise);
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        if (msg instanceof HttpObject) {
            for (HttpListener listener : listeners) {
                listener.read(ctx, (HttpObject) msg);
            }
        }
        super.channelRead(ctx, msg);
    }

    @Override
    public void write(ChannelHandlerContext ctx, Object msg, ChannelPromise promise) throws Exception {
        if (msg instanceof HttpObject) {
            for (HttpListener listener : listeners) {
                listener.write(ctx, (HttpObject) msg);
            }
        }
        super.write(ctx, msg, promise);
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        for (HttpListener listener : listeners) {
            listener.exception(ctx, cause);
        }
        super.exceptionCaught(ctx, cause);
    }
}
