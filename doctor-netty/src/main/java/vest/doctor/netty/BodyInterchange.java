package vest.doctor.netty;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import vest.doctor.Prioritized;
import vest.doctor.ProviderRegistry;

import javax.inject.Provider;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.ByteBuffer;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.charset.StandardCharsets;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

public final class BodyInterchange {
    private final List<BodyReader> readers;
    private final List<BodyWriter> writers;

    public BodyInterchange(ProviderRegistry providerRegistry) {
        JacksonInterchange jacksonInterchange = providerRegistry.getProviderOpt(ObjectMapper.class)
                .map(Provider::get)
                .map(JacksonInterchange::new)
                .orElseGet(() -> new JacksonInterchange(JacksonInterchange.defaultConfig()));

        this.readers = new ArrayList<>();
        readers.add(new DefaultReader());
        readers.add(jacksonInterchange);
        providerRegistry.getProviders(BodyReader.class)
                .map(Provider::get)
                .forEach(readers::add);
        readers.sort(Prioritized.COMPARATOR);

        this.writers = new ArrayList<>();
        writers.add(new DefaultWriter());
        writers.add(jacksonInterchange);
        providerRegistry.getProviders(BodyWriter.class)
                .map(Provider::get)
                .forEach(writers::add);
        writers.sort(Prioritized.COMPARATOR);
    }

    public <T> T read(RequestContext ctx, TypeInfo typeInfo) {
        for (BodyReader reader : readers) {
            if (reader.handles(ctx, typeInfo)) {
                return reader.read(ctx, typeInfo);
            }
        }
        throw new UnsupportedOperationException("unsupported request body type: " + typeInfo);
    }

    public void write(RequestContext ctx, Object response) {
        if (response == null) {
            ctx.complete();
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
        public int priority() {
            return 0;
        }
    }

    private static final class DefaultWriter implements BodyWriter {
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
}
