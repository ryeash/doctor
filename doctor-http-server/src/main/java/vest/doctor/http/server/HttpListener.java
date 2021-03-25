package vest.doctor.http.server;

import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.HttpObject;
import vest.doctor.Prioritized;

import java.net.SocketAddress;

public interface HttpListener extends Prioritized {

    void connect(ChannelHandlerContext ctx, SocketAddress remoteAddress, SocketAddress localAddress);

    void disconnect(ChannelHandlerContext ctx);

    void close(ChannelHandlerContext ctx);

    void read(ChannelHandlerContext ctx, HttpObject msg);

    void write(ChannelHandlerContext ctx, HttpObject msg);

    void exception(ChannelHandlerContext ctx, Throwable t);
}
