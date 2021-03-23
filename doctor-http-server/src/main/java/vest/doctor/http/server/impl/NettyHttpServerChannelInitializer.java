package vest.doctor.http.server.impl;

import io.netty.channel.ChannelPipeline;
import io.netty.channel.socket.SocketChannel;
import io.netty.handler.codec.http.HttpContentCompressor;
import io.netty.handler.codec.http.HttpContentDecompressor;
import io.netty.handler.codec.http.HttpServerCodec;
import io.netty.handler.stream.ChunkedWriteHandler;
import vest.doctor.http.server.HttpServerConfiguration;

public final class NettyHttpServerChannelInitializer extends ServerSocketChannelInitializer {

    private final HttpServerConfiguration config;

    public NettyHttpServerChannelInitializer(HttpServerConfiguration config) {
        this.config = config;
    }

    @Override
    protected void initChannel(SocketChannel ch) {
        ChannelPipeline p = ch.pipeline();
        if (config.getSslContext() != null) {
            p.addLast(config.getSslContext().newHandler(ch.alloc()));
        }
        // the ordering of these handlers is very sensitive
        p.addLast(new HttpServerCodec(config.getMaxInitialLineLength(),
                config.getMaxHeaderSize(),
                config.getMaxChunkSize(),
                config.isValidateHeaders(),
                config.getInitialBufferSize()));
        p.addLast(new HttpContentDecompressor());
        p.addLast(new HttpContentCompressor(6, 15, 8, 812));
        p.addLast(new ChunkedWriteHandler());
        p.addLast(getServer());
    }
}
