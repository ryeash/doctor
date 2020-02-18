package vest.doctor.netty;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.ByteBuffer;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.charset.StandardCharsets;
import java.nio.file.StandardOpenOption;

public class DefaultReaderWriter implements BodyReader, BodyWriter {

    @Override
    public boolean handles(RequestContext ctx, TypeInfo typeInfo) {
        Class<?> rawType = typeInfo.getRawType();
        return ByteBuf.class.isAssignableFrom(rawType)
                || InputStream.class.isAssignableFrom(rawType)
                || byte[].class.isAssignableFrom(rawType)
                || CharSequence.class.isAssignableFrom(rawType)
                || ByteBuffer.class.isAssignableFrom(rawType);
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> T read(RequestContext ctx, TypeInfo typeInfo) {
        Class<?> rawType = typeInfo.getRawType();
        if (ByteBuf.class.isAssignableFrom(rawType)) {
            return (T) ctx.requestBodyBuffer();
        } else if (InputStream.class.isAssignableFrom(rawType)) {
            return (T) ctx.requestBodyStream();
        } else if (byte[].class.isAssignableFrom(rawType)) {
            byte[] bytes = new byte[ctx.requestBodyBuffer().readableBytes()];
            ctx.requestBodyBuffer().readBytes(bytes);
            return (T) bytes;
        } else if (CharSequence.class.isAssignableFrom(rawType)) {
            return (T) ctx.requestBodyBuffer().toString(ctx.getRequestCharset(StandardCharsets.UTF_8));
        } else if (ByteBuffer.class.isAssignableFrom(rawType)) {
            return (T) ctx.requestBodyBuffer().nioBuffer();
        } else {
            throw new UnsupportedOperationException();
        }
    }

    @Override
    public boolean handles(RequestContext ctx, Object response) {
        return response instanceof byte[]
                || response instanceof InputStream
                || response instanceof CharSequence
                || response instanceof ByteBuf
                || response instanceof ByteBuffer
                || response instanceof StreamFile
                || response instanceof File;
    }

    @Override
    public void write(RequestContext ctx, Object response) {
        if (response instanceof byte[]) {
            ctx.responseBody((byte[]) response);
        } else if (response instanceof InputStream) {
            ctx.responseBody((InputStream) response);
        } else if (response instanceof CharSequence) {
            ctx.responseBody(String.valueOf(response), ctx.getResponseCharset(StandardCharsets.UTF_8));
        } else if (response instanceof ByteBuf) {
            ctx.responseBody((ByteBuf) response);
        } else if (response instanceof ByteBuffer) {
            ctx.responseBody(Unpooled.wrappedBuffer((ByteBuffer) response));
        } else if (response instanceof StreamFile) {
            ((StreamFile) response).write(ctx);
        } else if (response instanceof File) {
            try {
                File file = (File) response;
                FileChannel fc = FileChannel.open(file.toPath(), StandardOpenOption.READ);
                MappedByteBuffer bb = fc.map(FileChannel.MapMode.READ_ONLY, 0, file.length());
                ByteBuf body = Unpooled.wrappedBuffer(bb);
                fc.close();
                ctx.responseBody(body);
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
        } else {
            throw new UnsupportedOperationException();
        }
    }

    @Override
    public int priority() {
        return 0;
    }
}
