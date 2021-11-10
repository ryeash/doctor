package vest.doctor.http.server.rest;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufInputStream;
import io.netty.buffer.Unpooled;
import io.netty.handler.codec.http.HttpContent;
import io.netty.handler.codec.http.HttpHeaderNames;
import io.netty.handler.codec.http.HttpHeaderValues;
import vest.doctor.TypeInfo;
import vest.doctor.flow.Flo;
import vest.doctor.http.server.MultiPartData;
import vest.doctor.http.server.Request;
import vest.doctor.http.server.RequestBody;
import vest.doctor.http.server.Response;
import vest.doctor.http.server.ResponseBody;

import java.io.File;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.concurrent.CompletableFuture;

public class DefaultReaderWriter implements BodyReader, BodyWriter {

    private final List<Class<?>> SUPPORTED_TYPES = List.of(
            ByteBuf.class,
            RequestBody.class,
            InputStream.class,
            byte[].class,
            CharSequence.class,
            ByteBuffer.class,
            MultiPartData.class);

    @Override
    public boolean canRead(Request request, TypeInfo typeInfo) {
        if (typeInfo == null) {
            return true;
        }
        Class<?> rawType = typeInfo.getRawType();
        for (Class<?> supportedType : SUPPORTED_TYPES) {
            if (supportedType.isAssignableFrom(rawType)) {
                return true;
            }
        }
        return CompletableFuture.class.isAssignableFrom(rawType)
                && canRead(request, typeInfo.getParameterTypes().get(0));
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> Flo<?, T> read(Request request, TypeInfo typeInfo) {
        return convertStandard(request, typeInfo)
                .map(o -> (T) o);
    }

    private Flo<?, ?> convertStandard(Request request, TypeInfo typeInfo) {
        if (typeInfo == null) {
            return request.body().ignored();
        } else if (typeInfo.matches(ByteBuf.class)) {
            return request.body().asBuffer();
        } else if (typeInfo.matches(RequestBody.class)) {
            return Flo.of(request.body());
        } else if (typeInfo.matches(InputStream.class)) {
            return request.body()
                    .asBuffer()
                    .map(buf -> new ByteBufInputStream(buf, true));
        } else if (typeInfo.matches(byte[].class)) {
            return request.body()
                    .asBuffer()
                    .map(buf -> {
                        byte[] bytes = new byte[buf.readableBytes()];
                        buf.readBytes(bytes);
                        return bytes;
                    });
        } else if (typeInfo.matches(CharSequence.class)) {
            return request.body().asString();
        } else if (typeInfo.matches(ByteBuffer.class)) {
            return request.body()
                    .asBuffer()
                    .map(ByteBuf::nioBuffer);
        } else if (typeInfo.matches(MultiPartData.class)) {
            return Flo.of(request.multiPartBody());
        } else if (CompletableFuture.class.isAssignableFrom(typeInfo.getRawType())) {
            return convertStandard(request, typeInfo.getParameterTypes().get(0))
                    .map(CompletableFuture::completedFuture);
        } else {
            return Flo.error(HttpContent.class, new UnsupportedOperationException("parameter type is not supported: " + typeInfo));
        }
    }

    @Override
    public boolean canWrite(Response response, Object data) {
        return data instanceof byte[]
                || data instanceof InputStream
                || data instanceof CharSequence
                || data instanceof ByteBuf
                || data instanceof ByteBuffer
                || data instanceof File;
    }

    @Override
    public ResponseBody write(Response response, Object data) {
        if (data instanceof byte[] bytes) {
            setContentTypeIfAbsent(response, HttpHeaderValues.APPLICATION_OCTET_STREAM);
            return ResponseBody.of(bytes);
        } else if (data instanceof InputStream is) {
            setContentTypeIfAbsent(response, HttpHeaderValues.APPLICATION_OCTET_STREAM);
            return ResponseBody.of(is);
        } else if (data instanceof CharSequence chars) {
            setContentTypeIfAbsent(response, HttpHeaderValues.TEXT_PLAIN);
            return ResponseBody.of(chars.toString(), response.request().requestCharset(StandardCharsets.UTF_8));
        } else if (data instanceof ByteBuf buf) {
            setContentTypeIfAbsent(response, HttpHeaderValues.APPLICATION_OCTET_STREAM);
            return ResponseBody.of(buf);
        } else if (data instanceof ByteBuffer buf) {
            setContentTypeIfAbsent(response, HttpHeaderValues.APPLICATION_OCTET_STREAM);
            return ResponseBody.of(Unpooled.wrappedBuffer(buf));
        } else if (data instanceof File file) {
            return ResponseBody.sendFile(file);
        } else {
            throw new UnsupportedOperationException();
        }
    }

    @Override
    public int priority() {
        return 1_000_000;
    }

    private void setContentTypeIfAbsent(Response response, CharSequence contentType) {
        if (!response.headers().contains(HttpHeaderNames.CONTENT_TYPE)) {
            response.headers().set(HttpHeaderNames.CONTENT_TYPE, contentType);
        }
    }
}
