package vest.doctor.http.jackson;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import io.netty.handler.codec.http.HttpHeaderNames;
import vest.doctor.TypeInfo;
import vest.doctor.http.server.Request;
import vest.doctor.http.server.Response;
import vest.doctor.http.server.ResponseBody;
import vest.doctor.http.server.rest.BodyReader;
import vest.doctor.http.server.rest.BodyWriter;
import vest.doctor.workflow.Workflow;

import java.io.UncheckedIOException;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;

public class JacksonInterchange implements BodyReader, BodyWriter {

    private final ObjectMapper objectMapper;

    public JacksonInterchange(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public boolean canRead(Request request, TypeInfo typeInfo) {
        return true;
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> Workflow<?, T> read(Request request, TypeInfo typeInfo) {
        TypeInfo paramType = typeInfo.getParameterTypes().get(0);
        AsyncMapper<?> asyncMapper;
        if (paramType.hasParameterizedTypes()) {
            asyncMapper = new AsyncMapper<>(objectMapper, jacksonType(objectMapper, paramType));
        } else {
            asyncMapper = new AsyncMapper<>(objectMapper, paramType.getRawType());
        }
        return request.body()
                .flow()
                .map(asyncMapper::feed)
                .keep(Objects::nonNull)
                .map(o -> (T) o);
    }

    @Override
    public boolean canWrite(Response ctx, Object response) {
        return true;
    }

    @Override
    public CompletableFuture<ResponseBody> write(Response ctx, Object response) {
        try {
            byte[] bytes = objectMapper.writeValueAsBytes(response);
            if (!ctx.headers().contains(HttpHeaderNames.CONTENT_TYPE)) {
                ctx.headers().set(HttpHeaderNames.CONTENT_TYPE, "application/json;charset=utf-8");
            }
            return CompletableFuture.completedFuture(ResponseBody.of(bytes));
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
