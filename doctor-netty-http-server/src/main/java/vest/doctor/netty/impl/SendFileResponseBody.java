package vest.doctor.netty.impl;

import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.DefaultFileRegion;
import io.netty.channel.FileRegion;
import io.netty.handler.codec.http.HttpChunkedInput;
import io.netty.handler.codec.http.LastHttpContent;
import io.netty.handler.ssl.SslHandler;
import io.netty.handler.stream.ChunkedFile;
import vest.doctor.netty.ResponseBody;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.io.UncheckedIOException;
import java.util.Objects;

public class SendFileResponseBody implements ResponseBody {

    private final File file;

    public SendFileResponseBody(File file) {
        this.file = file;
    }

    @Override
    public ChannelFuture writeTo(ChannelHandlerContext channel) {
        if (channel.pipeline().get(SslHandler.class) != null) {
            try {
                RandomAccessFile raf = new RandomAccessFile(file, "r");
                HttpChunkedInput httpChunkedInput = new HttpChunkedInput(new ChunkedFile(raf, 0, file.length(), 8192));
                return channel.write(httpChunkedInput);
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
        } else {
            FileRegion region = new DefaultFileRegion(Objects.requireNonNull(file), 0, file.length());
            channel.write(region);
            return channel.write(LastHttpContent.EMPTY_LAST_CONTENT);
        }
    }
}
