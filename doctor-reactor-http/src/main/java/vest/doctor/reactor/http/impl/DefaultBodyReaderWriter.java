package vest.doctor.reactor.http.impl;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufInputStream;
import io.netty.buffer.CompositeByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.handler.codec.http.DefaultLastHttpContent;
import io.netty.handler.codec.http.HttpContent;
import io.netty.util.ReferenceCounted;
import vest.doctor.TypeInfo;
import vest.doctor.http.server.MultiPartData;
import vest.doctor.http.server.RequestContext;
import vest.doctor.http.server.Response;
import vest.doctor.http.server.ResponseBody;
import vest.doctor.reactive.Rx;
import vest.doctor.reactor.http.BodyReader;
import vest.doctor.reactor.http.BodyWriter;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.concurrent.Flow;

public class DefaultBodyReaderWriter implements BodyReader, BodyWriter {

    @Override
    @SuppressWarnings("unchecked")
    public <T> Flow.Publisher<T> read(RequestContext requestContext, TypeInfo typeInfo) {
        if (typeInfo != null && typeInfo.matches(Flow.Publisher.class, Object.class)) {
            return (Flow.Publisher<T>) readFlowInternal(requestContext, typeInfo);
        } else {
            return (Flow.Publisher<T>) readInternal(requestContext, typeInfo);
        }
    }

    private Flow.Publisher<?> readFlowInternal(RequestContext requestContext, TypeInfo typeInfo) {
        TypeInfo flowableType = typeInfo.getParameterTypes().get(0);
        if (flowableType == null) {
            return unwrap(requestContext)
                    .map(ReferenceCounted::release)
                    .map(b -> null);
        } else if (flowableType.matches(ByteBuf.class)) {
            return unwrap(requestContext);
        } else if (flowableType.matches(byte[].class)) {
            return requestContext.request().body().asByteChunks();
        } else if (flowableType.matches(ByteBuffer.class)) {
            return unwrap(requestContext);
        } else if (flowableType.matches(String.class)) {
            return unwrap(requestContext)
                    .map(buf -> {
                        try {
                            return buf.toString(StandardCharsets.UTF_8);
                        } finally {
                            buf.release();
                        }
                    });
        } else if (flowableType.matches(InputStream.class)) {
            return unwrap(requestContext).map(buf -> new ByteBufInputStream(buf, true));
        } else if (flowableType.matches(MultiPartData.Part.class)) {
            return requestContext.request().multiPartBody().parts();
        } else {
            return null;
        }
    }

    private Flow.Publisher<?> readInternal(RequestContext requestContext, TypeInfo typeInfo) {
        if (typeInfo == null) {
            return requestContext.request().body().ignored();
        } else if (typeInfo.matches(ByteBuf.class)) {
            return agg(requestContext);
        } else if (typeInfo.matches(byte[].class)) {
            return agg(requestContext)
                    .map(buf -> {
                        try {
                            byte[] bytes = new byte[buf.readableBytes()];
                            buf.readBytes(bytes);
                            return bytes;
                        } finally {
                            buf.release();
                        }
                    });
        } else if (typeInfo.matches(ByteBuffer.class)) {
            return agg(requestContext).map(ByteBuf::nioBuffer);
        } else if (typeInfo.matches(String.class)) {
            return requestContext.request().body().asString();
        } else if (typeInfo.matches(InputStream.class)) {
            return agg(requestContext).map(buf -> new ByteBufInputStream(buf, true));
        } else {
            return null;
        }
    }

    private static final List<Class<?>> SUPPORTED_TYPES = List.of(
            RequestContext.class,
            Flow.Publisher.class,
            Response.class,
            ByteBuf.class,
            byte[].class,
            ByteBuffer.class,
            String.class,
            InputStream.class
    );

    @Override
    public Flow.Publisher<Response> write(RequestContext requestContext, TypeInfo responseTypeInfo, Object responseData) {
        // things that short circuit the return
        if (responseData instanceof RequestContext ctx) {
            return Rx.one(ctx.response());
        } else if (responseData instanceof Flow.Publisher<?> pub) {
            if (responseTypeInfo.matches(Flow.Publisher.class, Object.class)) {
                if (SUPPORTED_TYPES.contains(responseTypeInfo.getParameterTypes().get(0).getRawType())) {
                    return Rx.from(pub).map(o -> mapSupported(requestContext, o));
                } else {
                    return null;
                }
            } else {
                throw new UnsupportedOperationException("please correctly mark the return types for endpoint methods: " + requestContext);
            }
        } else if (responseData instanceof Response httpResponse) {
            return Rx.one(httpResponse);
        }

        // things that just get converted to response bodies
        if (responseData == null) {
            requestContext.response().body(ResponseBody.empty());
        } else if (responseData instanceof ByteBuf buf) {
            requestContext.response().body(ResponseBody.of(buf));
        } else if (responseData instanceof byte[] bytes) {
            requestContext.response().body(ResponseBody.of(bytes));
        } else if (responseData instanceof ByteBuffer buf) {
            requestContext.response().body(ResponseBody.of(Unpooled.wrappedBuffer(buf)));
        } else if (responseData instanceof String str) {
            requestContext.response().body(ResponseBody.of(str));
        } else if (responseData instanceof InputStream is) {
            requestContext.response().body(ResponseBody.of(Rx.one(is).map(this::toBuf).map(DefaultLastHttpContent::new)));
        } else {
            return null;
        }
        return Rx.one(requestContext.response());
    }

    private Response mapSupported(RequestContext requestContext, Object responseData) {
        if (responseData instanceof RequestContext ctx) {
            return ctx.response();
        } else if (responseData instanceof Response httpResponse) {
            return httpResponse;
        } else if (responseData == null) {
            return requestContext.response().body(ResponseBody.empty());
        } else if (responseData instanceof ByteBuf buf) {
            return requestContext.response().body(ResponseBody.of(buf));
        } else if (responseData instanceof byte[] bytes) {
            return requestContext.response().body(ResponseBody.of(bytes));
        } else if (responseData instanceof ByteBuffer buf) {
            return requestContext.response().body(ResponseBody.of(Unpooled.wrappedBuffer(buf)));
        } else if (responseData instanceof String str) {
            return requestContext.response().body(ResponseBody.of(str));
        } else if (responseData instanceof InputStream is) {
            return requestContext.response().body(ResponseBody.of(Rx.one(is).map(this::toBuf).map(DefaultLastHttpContent::new)));
        } else {
            throw new UnsupportedOperationException("not actually a supported type: " + responseData);
        }
    }

    @Override
    public int priority() {
        return 1_000_000;
    }

    private static Rx<ByteBuf> agg(RequestContext ctx) {
        return Rx.from(ctx.request().body().asBuffer());
    }

    private static Rx<ByteBuf> unwrap(RequestContext ctx) {
        return Rx.from(ctx.request().body().flow()).map(HttpContent::content);
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