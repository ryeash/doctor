package vest.doctor.http.server;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufInputStream;
import io.netty.buffer.CompositeByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.handler.codec.http.DefaultLastHttpContent;
import vest.doctor.Prioritized;
import vest.doctor.TypeInfo;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.ByteBuffer;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

public class DefaultBodyReaderWriter implements BodyReader, BodyWriter {

    private static final Object NULL_BODY = new Object();

    @Override
    @SuppressWarnings("unchecked")
    public <T> T read(RequestContext requestContext, TypeInfo typeInfo) {
        return (T) readInternal(requestContext, typeInfo);
    }

    private Object readInternal(RequestContext requestContext, TypeInfo typeInfo) {
        // TODO: release properly
        ByteBuf buf = requestContext.request().body();
        if (typeInfo == null) {
            buf.release();
            return NULL_BODY;
        } else if (typeInfo.matches(ByteBuf.class)) {
            return requestContext.request().body();
        } else if (typeInfo.matches(byte[].class)) {
            return BodyUtils.toBytes(requestContext);
        } else if (typeInfo.matches(ByteBuffer.class)) {
            return buf.nioBuffer();
        } else if (typeInfo.matches(String.class)) {
            return BodyUtils.toString(requestContext);
        } else if (typeInfo.matches(InputStream.class)) {
            return new ByteBufInputStream(buf, true);
        } else {
            return null;
        }
    }

    @Override
    public CompletableFuture<RequestContext> write(RequestContext requestContext, TypeInfo responseTypeInfo, Object responseData) {
        // things that short circuit the return
        if (responseData instanceof RequestContext) {
            return CompletableFuture.completedFuture(requestContext);
        } else if (responseData instanceof Response httpResponse) {
            requestContext.response().status(httpResponse.status());
            requestContext.response().body(httpResponse.body());
            for (Map.Entry<String, String> header : httpResponse.headers()) {
                requestContext.response().header(header.getKey(), header.getValue());
            }
            return CompletableFuture.completedFuture(requestContext);
        }

        // things that just get converted to response bodies
        switch (responseData) {
            case null -> requestContext.response().body(ResponseBody.empty());
            case ByteBuf buf -> requestContext.response().body(ResponseBody.of(buf));
            case byte[] bytes -> requestContext.response().body(ResponseBody.of(bytes));
            case ByteBuffer buf -> requestContext.response().body(ResponseBody.of(Unpooled.wrappedBuffer(buf)));
            case String str -> requestContext.response().body(ResponseBody.of(str));
            case InputStream is ->
                    requestContext.response().body(ResponseBody.of(new DefaultLastHttpContent(toBuf(is))));
            default -> {
            }
        }
        return CompletableFuture.completedFuture(requestContext);
    }

    @Override
    public int priority() {
        return Prioritized.LOWEST_PRIORITY;
    }

    public ByteBuf toBuf(InputStream is) {
        try {
            CompositeByteBuf composite = Unpooled.compositeBuffer(128);
            byte[] buf = new byte[1024];
            int read;
            while ((read = is.read(buf)) >= 0) {
                composite.writeBytes(buf, 0, read);
            }
            return composite;
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}