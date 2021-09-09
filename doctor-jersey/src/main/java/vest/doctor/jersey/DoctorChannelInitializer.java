package vest.doctor.jersey;

import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.socket.SocketChannel;
import io.netty.handler.codec.http.HttpContentCompressor;
import io.netty.handler.codec.http.HttpContentDecompressor;
import io.netty.handler.codec.http.HttpServerCodec;
import io.netty.handler.stream.ChunkedWriteHandler;
import org.glassfish.jersey.server.ResourceConfig;
import vest.doctor.ProviderRegistry;

/**
 * Netty {@link ChannelInitializer} that binds together the netty http handling with the jersey handling.
 */
final class DoctorChannelInitializer extends ChannelInitializer<SocketChannel> {
    static final String SSL_CONTEXT = "sslContext";
    static final String JERSEY_ADAPTER_NAME = "jerseyChannelAdapter";
    private final HttpServerConfiguration config;
    private final JerseyChannelAdapter jerseyChannelAdapter;

    public DoctorChannelInitializer(HttpServerConfiguration config, DoctorJerseyContainer container, ResourceConfig resourceConfig, ProviderRegistry providerRegistry) {
        this.config = config;
        this.jerseyChannelAdapter = new JerseyChannelAdapter(config, container, resourceConfig, providerRegistry);
    }

    @Override
    public void initChannel(SocketChannel ch) {
        ChannelPipeline p = ch.pipeline();
        if (config.getSslContext() != null) {
            p.addLast(SSL_CONTEXT, config.getSslContext().newHandler(ch.alloc()));
        }
        p.addLast("httpServerCodec", new HttpServerCodec(
                config.getMaxInitialLineLength(),
                config.getMaxHeaderSize(),
                config.getMaxChunkSize(),
                config.isValidateHeaders(),
                config.getInitialBufferSize()));
        p.addLast("httpContentDecompressor", new HttpContentDecompressor());
        p.addLast("httpContentCompressor", new HttpContentCompressor(6, 15, 8, 812));
        p.addLast("chunkedWriteHandler", new ChunkedWriteHandler());
        p.addLast(JERSEY_ADAPTER_NAME, jerseyChannelAdapter);
    }
}
