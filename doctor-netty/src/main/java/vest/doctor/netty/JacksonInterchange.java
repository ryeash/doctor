package vest.doctor.netty;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.type.TypeBindings;
import io.netty.handler.codec.http.HttpHeaderNames;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class JacksonInterchange implements BodyReader, BodyWriter {

    private final ObjectMapper objectMapper;

    public JacksonInterchange(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public boolean handles(RequestContext ctx, Class<?> rawType, Class<?>... parameterTypes) {
        return true;
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> T read(RequestContext ctx, Class<T> rawType, Class<?>... parameterTypes) {
        try {
            if (rawType == CompletableFuture.class) {
                return (T) asyncRead(ctx, parameterTypes);
            } else if (parameterTypes == null || parameterTypes.length == 0) {
                return objectMapper.readValue(ctx.requestBodyStream(), rawType);
            } else {
                List<JavaType> typeParams = Stream.of(parameterTypes)
                        .map(objectMapper::constructType)
                        .collect(Collectors.toList());
                TypeBindings typeBindings = TypeBindings.create(rawType, typeParams);
                JavaType javaType = objectMapper.getTypeFactory().constructType(rawType, typeBindings);
                return objectMapper.readValue(ctx.requestBodyStream(), javaType);
            }
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private CompletableFuture<?> asyncRead(RequestContext ctx, Class<?>... parameterTypes) {
        Class<?> rawType = parameterTypes[0];

        AsyncMapper<?> asyncMapper;

        if (parameterTypes.length > 1) {
            List<JavaType> typeParams = Stream.of(parameterTypes)
                    .skip(1)
                    .map(objectMapper::constructType)
                    .collect(Collectors.toList());
            TypeBindings typeBindings = TypeBindings.create(rawType, typeParams);
            JavaType javaType = objectMapper.getTypeFactory().constructType(rawType, typeBindings);
            asyncMapper = new AsyncMapper<>(objectMapper, javaType);
        } else {
            asyncMapper = new AsyncMapper<>(objectMapper, rawType);
        }
        ctx.requestBody().readData((buf, finished) -> {
            byte[] b = new byte[1024];
            while (buf.readableBytes() > 0) {
                int toRead = Math.min(buf.readableBytes(), b.length);
                buf.readBytes(b, 0, toRead);
                asyncMapper.feed(b, 0, toRead);
            }
        });
        return asyncMapper.future();
    }

    @Override
    public boolean handles(RequestContext ctx, Object response) {
        return true;
    }

    @Override
    public void write(RequestContext ctx, Object response) {
        try {
            ctx.responseBody(objectMapper.writeValueAsBytes(response));
            if (!ctx.responseHeaders().contains(HttpHeaderNames.CONTENT_TYPE)) {
                ctx.responseHeaders().set(HttpHeaderNames.CONTENT_TYPE, "application/json;charset=utf-8");
            }
        } catch (JsonProcessingException e) {
            throw new UncheckedIOException(e);
        }
    }

    @Override
    public int priority() {
        return Integer.MAX_VALUE;
    }

    public static ObjectMapper defaultConfig() {
        return new ObjectMapper()
                .setSerializationInclusion(JsonInclude.Include.NON_EMPTY)
                .configure(JsonGenerator.Feature.IGNORE_UNKNOWN, true)
                .configure(DeserializationFeature.FAIL_ON_NULL_FOR_PRIMITIVES, false)
                .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
                .configure(DeserializationFeature.ACCEPT_SINGLE_VALUE_AS_ARRAY, true)
                .disable(SerializationFeature.FAIL_ON_EMPTY_BEANS)
                .setDefaultMergeable(true)
                .registerModules(ObjectMapper.findModules());
    }
}
