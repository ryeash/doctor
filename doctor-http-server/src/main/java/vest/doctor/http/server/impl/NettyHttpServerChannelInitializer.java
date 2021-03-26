package vest.doctor.http.server.impl;

import io.netty.channel.ChannelPipeline;
import io.netty.channel.socket.SocketChannel;
import io.netty.handler.codec.http.HttpContentCompressor;
import io.netty.handler.codec.http.HttpContentDecompressor;
import io.netty.handler.codec.http.HttpServerCodec;
import io.netty.handler.stream.ChunkedWriteHandler;
import vest.doctor.http.server.HttpListener;
import vest.doctor.http.server.HttpServerConfiguration;

import java.util.List;

public final class NettyHttpServerChannelInitializer extends ServerSocketChannelInitializer {

    private final HttpServerConfiguration config;
    private final HttpListenerManager httpListenerManager;

    public NettyHttpServerChannelInitializer(HttpServerConfiguration config, List<HttpListener> listeners) {
        this.config = config;
        this.httpListenerManager = new HttpListenerManager(listeners);
    }

    @Override
    protected void initChannel(SocketChannel ch) {
        ChannelPipeline p = ch.pipeline();
        if (config.getSslContext() != null) {
            p.addLast(config.getSslContext().newHandler(ch.alloc()));
        }
        p.addLast("httpServerCodec", new HttpServerCodec(config.getMaxInitialLineLength(),
                config.getMaxHeaderSize(),
                config.getMaxChunkSize(),
                config.isValidateHeaders(),
                config.getInitialBufferSize()));
        p.addLast("httpListenerManager", httpListenerManager);
        p.addLast("httpContentDecompressor", new HttpContentDecompressor());
        p.addLast("httpContentCompressor", new HttpContentCompressor(6, 15, 8, 812));
        p.addLast("chunkedWriteHandler", new ChunkedWriteHandler());
        p.addLast(getServer());
    }
}
