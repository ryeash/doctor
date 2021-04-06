package demo.app;

import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.HttpObject;
import jakarta.inject.Singleton;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import vest.doctor.http.server.HttpListener;

import java.net.SocketAddress;

@Singleton
public class LoggingHttpListener implements HttpListener {
    private static final Logger log = LoggerFactory.getLogger(LoggingHttpListener.class);

    @Override
    public void connect(ChannelHandlerContext ctx, SocketAddress remoteAddress, SocketAddress localAddress) {
        log.debug("connect: {} {} {}", ctx, remoteAddress, localAddress);
    }

    @Override
    public void disconnect(ChannelHandlerContext ctx) {
        log.info("disconnect: {}", ctx);
    }

    @Override
    public void close(ChannelHandlerContext ctx) {
        log.info("close: {}", ctx);
    }

    @Override
    public void read(ChannelHandlerContext ctx, HttpObject msg) {
        log.debug("read: {} {}", ctx, msg);
    }

    @Override
    public void write(ChannelHandlerContext ctx, HttpObject msg) {
        log.debug("write: {} {}", ctx, msg);
    }

    @Override
    public void exception(ChannelHandlerContext ctx, Throwable t) {
        log.info("exception: {}", ctx, t);
    }
}
