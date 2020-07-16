package vest.doctor.netty;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.DefaultFileRegion;
import io.netty.channel.FileRegion;
import io.netty.handler.codec.http.DefaultHttpContent;
import io.netty.handler.codec.http.HttpContent;
import vest.doctor.netty.impl.DefaultResponseBody;
import vest.doctor.netty.impl.EmptyBody;
import vest.doctor.netty.impl.StreamingResponseBody;

import java.io.File;
import java.io.InputStream;
import java.nio.channels.ReadableByteChannel;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

public interface ResponseBody {

    ChannelFuture writeTo(ChannelHandlerContext channel);

    static ResponseBody of(String str) {
        return of(str, StandardCharsets.UTF_8);
    }

    static ResponseBody of(String str, Charset charset) {
        if (str == null || str.isEmpty()) {
            return EmptyBody.INSTANCE;
        } else {
            return new DefaultResponseBody(Unpooled.wrappedBuffer(str.getBytes(charset)));
        }
    }

    static ResponseBody of(byte[] content) {
        if (content == null) {
            return EmptyBody.INSTANCE;
        } else {
            return new DefaultResponseBody(Unpooled.wrappedBuffer(content));
        }
    }

    static ResponseBody of(HttpContent content) {
        return new DefaultResponseBody(content.content());
    }

    static ResponseBody of(ByteBuf content) {
        return of(new DefaultHttpContent(content));
    }

    static ResponseBody of(InputStream inputStream) {
        return new StreamingResponseBody(inputStream);
    }

    static ResponseBody of(ReadableByteChannel readableByteChannel) {
        return new StreamingResponseBody(readableByteChannel);
    }

    /**
     * Uses {@link FileRegion} to perform (if possible) zero-copy file send. Due to the nature
     * of zero-copy, this will bypass the gzip handler.
     *
     * @param file the file to send
     * @return a new response body
     */
    static ResponseBody sendfile(File file) {
        FileRegion fileRegion = new DefaultFileRegion(file, 0, file.length());
        return ch -> ch.write(fileRegion);
    }

    static ResponseBody empty() {
        return EmptyBody.INSTANCE;
    }
}
