package vest.doctor.http.server;

import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.HttpObject;
import vest.doctor.Prioritized;

import java.net.SocketAddress;

/**
 * A listener that will get notified for various events in the netty http server.
 */
public interface HttpListener extends Prioritized {

    /**
     * Notified when a new connection has been made.
     *
     * @param ctx           the channel context
     * @param remoteAddress the remote address
     * @param localAddress  the local address
     */
    default void connect(ChannelHandlerContext ctx, SocketAddress remoteAddress, SocketAddress localAddress) {
    }

    /**
     * Notified when a connection disconnect is complete.
     *
     * @param ctx the channel context
     */
    default void disconnect(ChannelHandlerContext ctx) {
    }

    /**
     * Notified when a connection has been closed.
     *
     * @param ctx the channel context
     */
    default void close(ChannelHandlerContext ctx) {
    }

    /**
     * Notified when data has been read from the channel. Implementations are discouraged
     * from modifying the message data, as it may break the http channel.
     *
     * @param ctx the channel context
     * @param msg the data that was read
     */
    default void read(ChannelHandlerContext ctx, HttpObject msg) {
    }

    /**
     * Notified when data has been written to the channel. Implementations are discouraged
     * from modifying the message data, as it may break the http channel.
     *
     * @param ctx the channel context
     * @param msg the data was written
     */
    default void write(ChannelHandlerContext ctx, HttpObject msg) {
    }

    /**
     * Notified when an exception has been thrown during processing.
     *
     * @param ctx the channel context
     * @param t   the error
     */
    default void exception(ChannelHandlerContext ctx, Throwable t) {
    }
}
