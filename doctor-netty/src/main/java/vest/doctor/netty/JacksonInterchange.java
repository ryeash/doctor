package vest.doctor.netty;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import io.netty.handler.codec.http.HttpHeaderNames;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.List;
import java.util.concurrent.CompletableFuture;

public class JacksonInterchange implements BodyReader, BodyWriter {

    private final ObjectMapper objectMapper;

    public JacksonInterchange(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public boolean handles(RequestContext ctx, TypeInfo typeInfo) {
        return true;
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> T read(RequestContext ctx, TypeInfo typeInfo) {
        try {
            if (typeInfo.getRawType() == CompletableFuture.class) {
                return (T) asyncRead(ctx, typeInfo);
            } else if (!typeInfo.hasParameterizedTypes()) {
                return (T) objectMapper.readValue(ctx.requestBodyStream(), typeInfo.getRawType());
            } else {
                return objectMapper.readValue(ctx.requestBodyStream(), jacksonType(objectMapper, typeInfo));
            }
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private CompletableFuture<?> asyncRead(RequestContext ctx, TypeInfo typeInfo) {
        if (!typeInfo.hasParameterizedTypes() || typeInfo.getParameterTypes().size() != 1) {
            throw new IllegalArgumentException("asynchronous bodies must have exactly one parameterized type: " + typeInfo);
        }
        TypeInfo paramType = typeInfo.getParameterTypes().get(0);

        AsyncMapper<?> asyncMapper;

        if (paramType.hasParameterizedTypes()) {
            asyncMapper = new AsyncMapper<>(objectMapper, jacksonType(objectMapper, paramType));
        } else {
            asyncMapper = new AsyncMapper<>(objectMapper, paramType.getRawType());
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

    public static JavaType jacksonType(ObjectMapper mapper, TypeInfo typeInfo) {
        Class<?> rawType = typeInfo.getRawType();
        List<TypeInfo> parameterTypes = typeInfo.getParameterTypes();
        if (parameterTypes == null || parameterTypes.isEmpty()) {
            return mapper.getTypeFactory().constructType(rawType);
        } else {
            JavaType[] javaTypes = new JavaType[parameterTypes.size()];
            for (int i = 0; i < parameterTypes.size(); i++) {
                javaTypes[i] = jacksonType(mapper, parameterTypes.get(i));
            }
            return mapper.getTypeFactory().constructParametricType(rawType, javaTypes);
        }
    }
}
