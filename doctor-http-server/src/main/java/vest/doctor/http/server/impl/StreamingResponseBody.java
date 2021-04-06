package vest.doctor.http.server.impl;

import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.HttpChunkedInput;
import io.netty.handler.codec.http.LastHttpContent;
import io.netty.handler.stream.ChunkedNioStream;
import io.netty.handler.stream.ChunkedStream;
import io.netty.util.concurrent.Future;
import vest.doctor.http.server.ResponseBody;

import java.io.InputStream;
import java.nio.channels.ReadableByteChannel;
import java.util.Objects;

public class StreamingResponseBody implements ResponseBody {

    private final AutoCloseable closeable;
    private final HttpChunkedInput chunked;

    public StreamingResponseBody(InputStream is) {
        this.closeable = is;
        this.chunked = new HttpChunkedInput(new ChunkedStream(Objects.requireNonNull(is)));
    }

    public StreamingResponseBody(ReadableByteChannel readableByteChannel) {
        this.closeable = readableByteChannel;
        this.chunked = new HttpChunkedInput(new ChunkedNioStream(Objects.requireNonNull(readableByteChannel)));
    }

    @Override
    public ChannelFuture writeTo(ChannelHandlerContext channel) {
        channel.write(chunked);
        return channel.write(LastHttpContent.EMPTY_LAST_CONTENT)
                .addListener(this::close);
    }

    private void close(Future<?> future) {
        try {
            closeable.close();
        } catch (Exception e) {
            // ignored
        }
    }
}
