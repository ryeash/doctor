package vest.doctor.http.server.impl;

import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.HttpChunkedInput;
import io.netty.handler.stream.ChunkedFile;
import vest.doctor.http.server.ResponseBody;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.io.UncheckedIOException;

public class SendFileResponseBody implements ResponseBody {

    private final File file;

    public SendFileResponseBody(File file) {
        this.file = file;
    }

    @Override
    public ChannelFuture writeTo(ChannelHandlerContext channel) {
        try {
            RandomAccessFile raf = new RandomAccessFile(file, "r");
            HttpChunkedInput httpChunkedInput = new HttpChunkedInput(new ChunkedFile(raf, 0, file.length(), 8192));
            return channel.writeAndFlush(httpChunkedInput);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
