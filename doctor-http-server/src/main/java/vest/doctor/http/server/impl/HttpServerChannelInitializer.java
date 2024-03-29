package vest.doctor.http.server.impl;

import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.socket.SocketChannel;
import io.netty.handler.codec.compression.StandardCompressionOptions;
import io.netty.handler.codec.http.HttpContentCompressor;
import io.netty.handler.codec.http.HttpContentDecompressor;
import io.netty.handler.codec.http.HttpServerCodec;
import io.netty.handler.stream.ChunkedWriteHandler;
import vest.doctor.http.server.HttpServerConfiguration;
import vest.doctor.http.server.PipelineCustomizer;

public final class HttpServerChannelInitializer extends ChannelInitializer<SocketChannel> {

    public static final String SSL_CONTEXT = "sslContext";
    public static final String HTTP_SERVER_CODEC = "httpServerCodec";
    public static final String HTTP_CONTENT_DECOMPRESSOR = "httpContentDecompressor";
    public static final String HTTP_CONTENT_COMPRESSOR = "httpContentCompressor";
    public static final String CHUNKED_WRITE_HANDLER = "chunkedWriteHandler";
    public static final String SERVER_HANDLER = "serverHandler";
    public static final String WEBSOCKET_HANDLER = "websocketHandler";

    private final ChannelHandler server;
    private final HttpServerConfiguration config;

    public HttpServerChannelInitializer(ChannelHandler server, HttpServerConfiguration config) {
        this.server = server;
        this.config = config;
    }

    @Override
    protected void initChannel(SocketChannel ch) {
        ChannelPipeline p = ch.pipeline();
        if (config.getSslContext() != null) {
            p.addLast(SSL_CONTEXT, config.getSslContext().newHandler(ch.alloc()));
        }
        p.addLast(HTTP_SERVER_CODEC, new HttpServerCodec(
                config.getMaxInitialLineLength(),
                config.getMaxHeaderSize(),
                config.getMaxChunkSize(),
                config.isValidateHeaders(),
                config.getInitialBufferSize()));
        p.addLast(HTTP_CONTENT_DECOMPRESSOR, new HttpContentDecompressor());
        p.addLast(HTTP_CONTENT_COMPRESSOR, new HttpContentCompressor(config.getMinGzipSize(), StandardCompressionOptions.gzip(6, 15, 8)));
        p.addLast(CHUNKED_WRITE_HANDLER, new ChunkedWriteHandler());
        p.addLast(SERVER_HANDLER, server);
        if (config.getPipelineCustomizers() != null) {
            for (PipelineCustomizer pipelineCustomizer : config.getPipelineCustomizers()) {
                pipelineCustomizer.customize(p);
            }
        }
    }
}
