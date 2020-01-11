package vest.doctor.netty;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import vest.doctor.BeanProvider;
import vest.doctor.Prioritized;

import javax.inject.Provider;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CompletableFuture;

public final class BodyInterchange {
    private final List<BodyReader> readers;
    private final List<BodyWriter> writers;

    public BodyInterchange(BeanProvider beanProvider) {
        JacksonInterchange jacksonInterchange = beanProvider.getProviderOpt(ObjectMapper.class)
                .map(Provider::get)
                .map(JacksonInterchange::new)
                .orElseGet(() -> new JacksonInterchange(JacksonInterchange.defaultConfig()));

        this.readers = new ArrayList<>();
        readers.add(new DefaultReader());
        readers.add(jacksonInterchange);
        beanProvider.getProviders(BodyReader.class)
                .map(Provider::get)
                .forEach(readers::add);
        readers.sort(Prioritized.COMPARATOR);

        this.writers = new ArrayList<>();
        writers.add(new DefaultWriter());
        writers.add(jacksonInterchange);
        beanProvider.getProviders(BodyWriter.class)
                .map(Provider::get)
                .forEach(writers::add);
        writers.sort(Prioritized.COMPARATOR);
    }

    public <T> T read(RequestContext ctx, Class<T> type, Class<?>... genericTypes) {
        for (BodyReader reader : readers) {
            if (reader.handles(ctx, type, genericTypes)) {
                return reader.read(ctx, type, genericTypes);
            }
        }
        throw new UnsupportedOperationException("unsupported request body type: " + type + " paramaterizedTypes: " + Arrays.toString(genericTypes));
    }

    public void write(RequestContext ctx, Object response) {
        if (response == null) {
            ctx.responseBody(Unpooled.EMPTY_BUFFER);
        } else if (response instanceof CompletableFuture) {
            write(ctx, (CompletableFuture<?>) response);
        } else if (response instanceof R) {
            write(ctx, (R) response);
        } else {
            for (BodyWriter writer : writers) {
                if (writer.handles(ctx, response)) {
                    writer.write(ctx, response);
                    ctx.complete();
                    return;
                }
            }
            throw new UnsupportedOperationException("unsupported response type: " + response.getClass());
        }
    }

    public void write(RequestContext ctx, CompletableFuture<?> response) {
        if (response == null) {
            ctx.responseBody(Unpooled.EMPTY_BUFFER);
            return;
        }
        response.whenComplete((value, error) -> {
            if (error != null) {
                ctx.complete(error);
            } else {
                write(ctx, value);
            }
        });
    }

    public void write(RequestContext ctx, R response) {
        if (response == null) {
            ctx.responseBody(Unpooled.EMPTY_BUFFER);
            return;
        }
        ctx.responseStatus(response.status());
        response.headers().forEach(ctx.responseHeaders()::set);
        write(ctx, response.body());
    }

    private static final class DefaultReader implements BodyReader {

        @Override
        public boolean handles(RequestContext ctx, Class<?> rawType, Class<?>... genericTypes) {
            return ByteBuf.class.isAssignableFrom(rawType)
                    || InputStream.class.isAssignableFrom(rawType)
                    || byte[].class.isAssignableFrom(rawType)
                    || CharSequence.class.isAssignableFrom(rawType)
                    || ByteBuffer.class.isAssignableFrom(rawType);
        }

        @Override
        @SuppressWarnings("unchecked")
        public <T> T read(RequestContext ctx, Class<T> rawType, Class<?>... genericTypes) {
            if (ByteBuf.class.isAssignableFrom(rawType)) {
                return (T) ctx.requestBody();
            } else if (InputStream.class.isAssignableFrom(rawType)) {
                return (T) ctx.requestBodyStream();
            } else if (byte[].class.isAssignableFrom(rawType)) {
                byte[] bytes = new byte[ctx.requestBody().readableBytes()];
                ctx.requestBody().readBytes(bytes);
                return (T) bytes;
            } else if (CharSequence.class.isAssignableFrom(rawType)) {
                return (T) ctx.requestBody().toString(ctx.getRequestCharset(StandardCharsets.UTF_8));
            } else if (ByteBuffer.class.isAssignableFrom(rawType)) {
                return (T) ctx.requestBody().nioBuffer();
            } else {
                throw new UnsupportedOperationException();
            }
        }

        @Override
        public int priority() {
            return 0;
        }
    }

    private static final class DefaultWriter implements BodyWriter {
        @Override
        public boolean handles(RequestContext ctx, Object response) {
            return response == null
                    || response instanceof byte[]
                    || response instanceof InputStream
                    || response instanceof CharSequence
                    || response instanceof ByteBuf
                    || response instanceof ByteBuffer;
        }

        @Override
        public void write(RequestContext ctx, Object response) {
            if (response == null) {
                ctx.responseBody("");
            } else if (response instanceof byte[]) {
                ctx.responseBody((byte[]) response);
            } else if (response instanceof InputStream) {
                ctx.responseBody((InputStream) response);
            } else if (response instanceof CharSequence) {
                ctx.responseBody(String.valueOf(response), ctx.getResponseCharset(StandardCharsets.UTF_8));
            } else if (response instanceof ByteBuf) {
                ctx.responseBody((ByteBuf) response);
            } else if (response instanceof ByteBuffer) {
                ctx.responseBody(Unpooled.wrappedBuffer((ByteBuffer) response));
            } else {
                throw new UnsupportedOperationException();
            }
        }

        @Override
        public int priority() {
            return 0;
        }
    }
}
