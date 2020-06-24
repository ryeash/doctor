package vest.doctor.netty.impl;

import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.HttpChunkedInput;
import io.netty.handler.stream.ChunkedNioStream;
import io.netty.handler.stream.ChunkedStream;
import vest.doctor.netty.ResponseBody;

import java.io.InputStream;
import java.nio.channels.ReadableByteChannel;
import java.util.Objects;

public class StreamingResponseBody implements ResponseBody {
    private final HttpChunkedInput chunked;

    public StreamingResponseBody(InputStream is) {
        this.chunked = new HttpChunkedInput(new ChunkedStream(Objects.requireNonNull(is)));
    }

    public StreamingResponseBody(ReadableByteChannel readableByteChannel) {
        this.chunked = new HttpChunkedInput(new ChunkedNioStream(Objects.requireNonNull(readableByteChannel)));
    }

    @Override
    public void writeTo(ChannelHandlerContext channel) {
        channel.write(chunked);
    }
}
