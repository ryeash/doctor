package vest.doctor.http.server;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.FileRegion;
import io.netty.handler.codec.http.DefaultHttpContent;
import io.netty.handler.codec.http.HttpContent;
import io.netty.handler.codec.http.LastHttpContent;
import vest.doctor.http.server.impl.DefaultResponseBody;
import vest.doctor.http.server.impl.FloResponseBody;
import vest.doctor.http.server.impl.SendFileResponseBody;
import vest.doctor.http.server.impl.StreamingResponseBody;

import java.io.File;
import java.io.InputStream;
import java.nio.channels.ReadableByteChannel;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Objects;
import java.util.concurrent.Flow;

/**
 * Defines an object that can be sent as an HTTP response body.
 */
@FunctionalInterface
public interface ResponseBody {

    /**
     * Write this body to the channel.
     *
     * @param channel the {@link ChannelHandlerContext}
     * @return the {@link ChannelFuture} representing the completion of the write
     */
    ChannelFuture writeTo(ChannelHandlerContext channel);

    /**
     * Create a string response body. The string will be encoded using UTF-8.
     *
     * @param str the string to send
     * @return a new {@link ResponseBody}
     */
    static ResponseBody of(String str) {
        return of(str, StandardCharsets.UTF_8);
    }

    /**
     * Create a string response body.
     *
     * @param str     the string to send
     * @param charset the {@link Charset} to use when encoding the string to bytes
     * @return a new {@link ResponseBody}
     */
    static ResponseBody of(String str, Charset charset) {
        if (str == null || str.isEmpty()) {
            return empty();
        } else {
            return new DefaultResponseBody(Unpooled.wrappedBuffer(str.getBytes(charset)));
        }
    }

    /**
     * Create a byte array response body.
     *
     * @param content the content to send
     * @return a new {@link ResponseBody}
     */
    static ResponseBody of(byte[] content) {
        if (content == null) {
            return empty();
        } else {
            return new DefaultResponseBody(Unpooled.wrappedBuffer(content));
        }
    }

    /**
     * Create a response body from the {@link HttpContent}.
     *
     * @param content the content to send
     * @return a new {@link ResponseBody}
     */
    static ResponseBody of(HttpContent content) {
        return new DefaultResponseBody(content.content());
    }

    /**
     * Create a {@link ByteBuf} response body.
     *
     * @param content the content to send
     * @return a new {@link ResponseBody}
     */
    static ResponseBody of(ByteBuf content) {
        return of(new DefaultHttpContent(content));
    }

    /**
     * Create an {@link InputStream} response body.
     *
     * @param inputStream the input stream to send
     * @return a new {@link ResponseBody}
     */
    static ResponseBody of(InputStream inputStream) {
        return new StreamingResponseBody(inputStream);
    }

    /**
     * Create a response body from the {@link ReadableByteChannel}.
     *
     * @param readableByteChannel the byte channel to send the data from
     * @return a new {@link ResponseBody}
     */
    static ResponseBody of(ReadableByteChannel readableByteChannel) {
        return new StreamingResponseBody(readableByteChannel);
    }

    /**
     * Create a response body from an asynchronous processing flow of {@link HttpContent}.
     *
     * @param flo the processing flow
     * @return a new {@link ResponseBody}
     */
    static ResponseBody of(Flow.Publisher<? extends HttpContent> flo) {
        return new FloResponseBody(flo);
    }

    /**
     * Uses {@link FileRegion} to perform (if possible) zero-copy file send. Due to the nature
     * of zero-copy, this will bypass the gzip handler.
     *
     * @param file the file to send
     * @return a new response body
     */
    static ResponseBody sendFile(File file) {
        Objects.requireNonNull(file);
        if (!file.exists() || file.isDirectory()) {
            throw new IllegalArgumentException("file not found: " + file.getPath());
        }
        if (file.isDirectory()) {
            throw new IllegalArgumentException("invalid file, can not send directories: " + file.getPath());
        }
        return new SendFileResponseBody(file);
    }

    /**
     * An empty body.
     */
    static ResponseBody empty() {
        return channel -> channel.writeAndFlush(LastHttpContent.EMPTY_LAST_CONTENT);
    }
}
