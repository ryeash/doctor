package vest.doctor.netty;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufInputStream;
import io.netty.buffer.Unpooled;
import io.netty.handler.codec.http.HttpHeaderNames;
import io.netty.handler.codec.http.HttpHeaderValues;
import vest.doctor.TypeInfo;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.ByteBuffer;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.charset.StandardCharsets;
import java.nio.file.StandardOpenOption;
import java.util.concurrent.CompletableFuture;

public class DefaultReaderWriter implements BodyReader, BodyWriter {

    @Override
    public boolean handles(Request request, TypeInfo typeInfo) {
        Class<?> rawType = typeInfo.getRawType();
        return ByteBuf.class.isAssignableFrom(rawType)
                || InputStream.class.isAssignableFrom(rawType)
                || byte[].class.isAssignableFrom(rawType)
                || CharSequence.class.isAssignableFrom(rawType)
                || ByteBuffer.class.isAssignableFrom(rawType);
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> CompletableFuture<T> read(Request request, TypeInfo typeInfo) {
        Class<?> rawType = typeInfo.getRawType();
        if (ByteBuf.class.isAssignableFrom(rawType)) {
            return (CompletableFuture<T>) request.body().completionFuture();
        } else if (InputStream.class.isAssignableFrom(rawType)) {
            return (CompletableFuture<T>) request.body()
                    .completionFuture()
                    .thenApply(ByteBufInputStream::new);
        } else if (byte[].class.isAssignableFrom(rawType)) {
            return (CompletableFuture<T>) request.body()
                    .completionFuture()
                    .thenApply(buf -> {
                        byte[] bytes = new byte[buf.readableBytes()];
                        buf.readBytes(bytes);
                        return bytes;
                    });
        } else if (CharSequence.class.isAssignableFrom(rawType)) {
            return (CompletableFuture<T>) request.body()
                    .asString();
        } else if (ByteBuffer.class.isAssignableFrom(rawType)) {
            return (CompletableFuture<T>) request.body()
                    .completionFuture()
                    .thenApply(ByteBuf::nioBuffer);
        } else {
            throw new UnsupportedOperationException();
        }
    }

    @Override
    public boolean handles(Response response, Object data) {
        return data instanceof byte[]
                || data instanceof InputStream
                || data instanceof CharSequence
                || data instanceof ByteBuf
                || data instanceof ByteBuffer
                || data instanceof File;
    }

    @Override
    public CompletableFuture<ResponseBody> write(Response response, Object data) {
        if (data instanceof byte[]) {
            setContentTypeIfAbsent(response, HttpHeaderValues.APPLICATION_OCTET_STREAM);
            return CompletableFuture.completedFuture(ResponseBody.of((byte[]) data));
        } else if (data instanceof InputStream) {
            setContentTypeIfAbsent(response, HttpHeaderValues.APPLICATION_OCTET_STREAM);
            return CompletableFuture.completedFuture(ResponseBody.of((InputStream) data));
        } else if (data instanceof CharSequence) {
            setContentTypeIfAbsent(response, HttpHeaderValues.TEXT_PLAIN);
            return CompletableFuture.completedFuture(ResponseBody.of(data.toString(), response.request().requestCharset(StandardCharsets.UTF_8)));
        } else if (data instanceof ByteBuf) {
            setContentTypeIfAbsent(response, HttpHeaderValues.APPLICATION_OCTET_STREAM);
            return CompletableFuture.completedFuture(ResponseBody.of((ByteBuf) data));
        } else if (data instanceof ByteBuffer) {
            setContentTypeIfAbsent(response, HttpHeaderValues.APPLICATION_OCTET_STREAM);
            return CompletableFuture.completedFuture(ResponseBody.of(Unpooled.wrappedBuffer((ByteBuffer) data)));
        } else if (data instanceof File) {
            try {
                File file = (File) data;
                setContentTypeIfAbsent(response, Utils.getContentType(file));
                FileChannel fc = FileChannel.open(file.toPath(), StandardOpenOption.READ);
                MappedByteBuffer bb = fc.map(FileChannel.MapMode.READ_ONLY, 0, file.length());
                ByteBuf body = Unpooled.wrappedBuffer(bb);
                fc.close();
                return CompletableFuture.completedFuture(ResponseBody.of(body));
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
        } else {
            throw new UnsupportedOperationException();
        }
    }

    @Override
    public int priority() {
        return Integer.MAX_VALUE;
    }

    private void setContentTypeIfAbsent(Response response, CharSequence contentType) {
        if (!response.headers().contains(HttpHeaderNames.CONTENT_TYPE)) {
            response.headers().set(HttpHeaderNames.CONTENT_TYPE, contentType);
        }
    }
}
