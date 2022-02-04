package vest.doctor.reactor.http.impl;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.CompositeByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.handler.codec.http.multipart.HttpData;
import io.netty.util.ReferenceCounted;
import org.reactivestreams.Publisher;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.netty.ByteBufFlux;
import reactor.netty.ByteBufMono;
import vest.doctor.TypeInfo;
import vest.doctor.reactor.http.BodyReader;
import vest.doctor.reactor.http.BodyWriter;
import vest.doctor.reactor.http.HttpResponse;
import vest.doctor.reactor.http.RequestContext;
import vest.doctor.reactor.http.ResponseBody;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.ByteBuffer;
import java.util.List;

public class DefaultBodyReaderWriter implements BodyReader, BodyWriter {

    @Override
    @SuppressWarnings("unchecked")
    public <T> Publisher<T> read(RequestContext requestContext, TypeInfo typeInfo) {
        if (typeInfo != null && typeInfo.matches(Publisher.class, Object.class)) {
            return (Publisher<T>) readFlowInternal(requestContext, typeInfo);
        } else {
            return (Publisher<T>) readInternal(requestContext, typeInfo);
        }
    }

    private Publisher<?> readFlowInternal(RequestContext requestContext, TypeInfo typeInfo) {
        TypeInfo flowableType = typeInfo.getParameterTypes().get(0);
        if (flowableType == null) {
            return unwrap(requestContext)
                    .map(ReferenceCounted::release)
                    .map(b -> null);
        } else if (flowableType.matches(ByteBuf.class)) {
            return unwrap(requestContext);
        } else if (flowableType.matches(byte[].class)) {
            return unwrap(requestContext).asByteArray();
        } else if (flowableType.matches(ByteBuffer.class)) {
            return unwrap(requestContext).asByteBuffer();
        } else if (flowableType.matches(String.class)) {
            return unwrap(requestContext).asString();
        } else if (flowableType.matches(InputStream.class)) {
            return unwrap(requestContext).asInputStream();
        } else if (flowableType.matches(HttpData.class)) {
            return requestContext.request().unwrap().receiveForm();
        } else {
            return null;
        }
    }

    private Publisher<?> readInternal(RequestContext requestContext, TypeInfo typeInfo) {
        if (typeInfo == null) {
            return unwrap(requestContext)
                    .map(ReferenceCounted::release)
                    .map(b -> null);
        } else if (typeInfo.matches(ByteBuf.class)) {
            return agg(requestContext);
        } else if (typeInfo.matches(byte[].class)) {
            return agg(requestContext).asByteArray();
        } else if (typeInfo.matches(ByteBuffer.class)) {
            return agg(requestContext).asByteBuffer();
        } else if (typeInfo.matches(String.class)) {
            return agg(requestContext).asString();
        } else if (typeInfo.matches(InputStream.class)) {
            return agg(requestContext).asInputStream();
        } else {
            return null;
        }
    }

    private static final List<Class<?>> SUPPORTED_TYPES = List.of(
            RequestContext.class,
            Publisher.class,
            HttpResponse.class,
            ByteBuf.class,
            byte[].class,
            ByteBuffer.class,
            String.class,
            InputStream.class
    );

    @Override
    public Publisher<HttpResponse> write(RequestContext requestContext, TypeInfo responseTypeInfo, Object responseData) {
        // things that short circuit the return
        if (responseData instanceof RequestContext ctx) {
            return ctx;
        } else if (responseData instanceof Publisher<?> pub) {
            if (responseTypeInfo.matches(Publisher.class, Object.class)) {
                if (SUPPORTED_TYPES.contains(responseTypeInfo.getParameterTypes().get(0).getRawType())) {
                    return Flux.from(pub).map(o -> mapSupported(requestContext, responseTypeInfo, o));
                }
            } else {
                throw new UnsupportedOperationException("please correctly mark the return types for endpoint methods: " + requestContext);
            }
        } else if (responseData instanceof HttpResponse httpResponse) {
            return Mono.just(httpResponse);
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
            requestContext.response().body(ResponseBody.ofBuffers(Mono.just(is).map(this::toBuf)));
        } else {
            return null;
        }
        return requestContext;
    }

    private HttpResponse mapSupported(RequestContext requestContext, TypeInfo responseTypeInfo, Object responseData) {
        if (responseData instanceof RequestContext ctx) {
            return ctx.response();
        } else if (responseData instanceof HttpResponse httpResponse) {
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
            return requestContext.response().body(ResponseBody.ofBuffers(Mono.just(is).map(this::toBuf)));
        } else {
            throw new UnsupportedOperationException("not actually a supported type: " + responseData);
        }
    }

    @Override
    public int priority() {
        return 1_000_000;
    }

    private static ByteBufMono agg(RequestContext ctx) {
        return ctx.request().unwrap().receive().aggregate();
    }

    private static ByteBufFlux unwrap(RequestContext ctx) {
        return ctx.request().unwrap().receive();
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