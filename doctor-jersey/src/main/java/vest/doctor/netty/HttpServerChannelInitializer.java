package vest.doctor.netty;

import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.socket.SocketChannel;
import io.netty.handler.codec.compression.StandardCompressionOptions;
import io.netty.handler.codec.http.HttpContentCompressor;
import io.netty.handler.codec.http.HttpContentDecompressor;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http.HttpServerCodec;
import io.netty.handler.ssl.SslContext;
import io.netty.handler.stream.ChunkedWriteHandler;
import vest.doctor.ProviderRegistry;
import vest.doctor.jersey.JerseyHttpConfiguration;

import java.util.List;

public final class HttpServerChannelInitializer extends ChannelInitializer<SocketChannel> {

    public static final String SSL_CONTEXT = "sslContext";
    public static final String HTTP_SERVER_CODEC = "httpServerCodec";
    public static final String HTTP_CONTENT_DECOMPRESSOR = "httpContentDecompressor";
    public static final String HTTP_CONTENT_COMPRESSOR = "httpContentCompressor";
    public static final String CHUNKED_WRITE_HANDLER = "chunkedWriteHandler";
    public static final String SERVER_HANDLER = "serverHandler";
    public static final String HTTP_AGGREGATOR = "httpAggregator";


    private final ChannelHandler server;
    private final JerseyHttpConfiguration config;
    private final SslContext sslContext;

    private final List<PipelineCustomizer> pipelineCustomizers;
    private final int maxInitialLineLength;
    private final int maxHeaderSize;
    private final int maxChunkSize;
    private final boolean validateHeaders;
    private final int initialBufferSize;
    private final int minGzipSize;
    private final int maxContentLength;

    public HttpServerChannelInitializer(ProviderRegistry providerRegistry,
                                        ChannelHandler server,
                                        JerseyHttpConfiguration config,
                                        SslContext sslContext) {
        this.pipelineCustomizers = providerRegistry.getInstances(PipelineCustomizer.class).toList();
        this.server = server;
        this.config = config;
        this.sslContext = sslContext;
        this.maxInitialLineLength = config.maxInitialLineLength().orElse(8192);
        this.maxHeaderSize = config.maxHeaderSize().orElse(8192);
        this.maxChunkSize = config.maxChunkSize().orElse(8192);
        this.validateHeaders = config.validateHeaders().orElse(false);
        this.initialBufferSize = config.initialBufferSize().orElse(256);
        this.minGzipSize = config.minGzipSize().orElse(812);
        this.maxContentLength = config.maxContentLength().orElse(8 * 1024 * 1024);
    }

    @Override
    protected void initChannel(SocketChannel ch) {
        ChannelPipeline p = ch.pipeline();
        if (sslContext != null) {
            p.addLast(SSL_CONTEXT, sslContext.newHandler(ch.alloc()));
        }
        p.addLast(HTTP_SERVER_CODEC, new HttpServerCodec(
                maxInitialLineLength,
                maxHeaderSize,
                maxChunkSize,
                validateHeaders,
                initialBufferSize));
        p.addLast(HTTP_CONTENT_DECOMPRESSOR, new HttpContentDecompressor());
        p.addLast(HTTP_CONTENT_COMPRESSOR, new HttpContentCompressor(minGzipSize, StandardCompressionOptions.gzip(6, 15, 8)));
        p.addLast(CHUNKED_WRITE_HANDLER, new ChunkedWriteHandler());
        p.addLast(HTTP_AGGREGATOR, new HttpObjectAggregator(maxContentLength, true));
        p.addLast(SERVER_HANDLER, server);

        for (PipelineCustomizer pipelineCustomizer : pipelineCustomizers) {
            pipelineCustomizer.customize(p);
        }
    }
}
