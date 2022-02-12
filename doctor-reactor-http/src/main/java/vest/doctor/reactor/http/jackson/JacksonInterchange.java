package vest.doctor.reactor.http.jackson;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.Module;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.module.SimpleModule;
import io.netty.handler.codec.http.HttpHeaderNames;
import io.netty.handler.codec.http.HttpHeaderValues;
import org.reactivestreams.Publisher;
import reactor.core.publisher.Flux;
import vest.doctor.Prioritized;
import vest.doctor.ProviderRegistry;
import vest.doctor.TypeInfo;
import vest.doctor.reactor.http.BodyReader;
import vest.doctor.reactor.http.BodyWriter;
import vest.doctor.reactor.http.HttpResponse;
import vest.doctor.reactor.http.RequestContext;
import vest.doctor.reactor.http.ResponseBody;

import java.io.UncheckedIOException;
import java.util.LinkedList;
import java.util.List;

public class JacksonInterchange implements BodyReader, BodyWriter {

    private final ObjectMapper objectMapper;
    private final List<AsyncParserFactory> parserFactories;

    public JacksonInterchange(ObjectMapper objectMapper) {
        this(objectMapper, new LinkedList<>(List.of(new GenericJsonBeanParserFactory())));
    }

    public JacksonInterchange(ObjectMapper objectMapper, List<AsyncParserFactory> parserFactories) {
        this.objectMapper = objectMapper;
        this.parserFactories = parserFactories;
        this.parserFactories.sort(Prioritized.COMPARATOR);
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> Publisher<T> read(RequestContext requestContext, TypeInfo typeInfo) {
        String contentType = requestContext.request().header(HttpHeaderNames.CONTENT_TYPE);
        if (contentType.contains(HttpHeaderValues.APPLICATION_JSON)) {
            if (typeInfo.matches(Publisher.class, Object.class)) {
                return (Publisher<T>) internalRead(requestContext, typeInfo.getParameterTypes().get(0));
            } else {
                return (Publisher<T>) internalRead(requestContext, typeInfo);
            }
        } else {
            return null;
        }
    }

    private Flux<?> internalRead(RequestContext ctx, TypeInfo typeInfo) {
        JavaType javaType = jacksonType(objectMapper, typeInfo);
        for (AsyncParserFactory parserFactory : parserFactories) {
            AsyncParser<?> parser = parserFactory.build(ctx, javaType);
            if (parser != null) {
                return ctx.request()
                        .body()
                        .asByteBuffer()
                        .flatMapIterable(new AsyncTokenizer(objectMapper))
                        .handle(parser);
            }
        }
        throw new UnsupportedOperationException("no async parser can handle this type: " + typeInfo);
    }

    @Override
    public Publisher<HttpResponse> write(RequestContext ctx, TypeInfo responseTypeInfo, Object response) {
        String accept = ctx.request().header(HttpHeaderNames.ACCEPT);
        if (accept.contains(HttpHeaderValues.APPLICATION_JSON)) {
            HttpResponse res = ctx.response();
            if (!res.headers().contains(HttpHeaderNames.CONTENT_TYPE)) {
                res.headers().set(HttpHeaderNames.CONTENT_TYPE, HttpHeaderValues.APPLICATION_JSON + ";charset=utf-8");
            }
            if (responseTypeInfo.matches(Publisher.class, Object.class)) {
                return Flux.from((Publisher<?>) response)
                        .map(this::writeJsonBytes)
                        .map(ResponseBody::of)
                        .map(ctx.response()::body);
            } else {
                return Flux.just(response)
                        .map(this::writeJsonBytes)
                        .map(ResponseBody::of)
                        .map(ctx.response()::body);
            }
        } else {
            return null;
        }
    }

    private byte[] writeJsonBytes(Object o) {
        try {
            return objectMapper.writeValueAsBytes(o);
        } catch (JsonProcessingException e) {
            throw new UncheckedIOException(e);
        }
    }

    @Override
    public int priority() {
        return Prioritized.LOWEST_PRIORITY;
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

    @SuppressWarnings("unchecked")
    public static ObjectMapper defaultConfig(ProviderRegistry providerRegistry) {
        SimpleModule injected = new SimpleModule();
        providerRegistry.getInstances(JsonSerializer.class)
                .forEach(injected::addSerializer);
        providerRegistry.getInstances(JsonDeserializer.class)
                .forEach(jsonDeserializer -> injected.addDeserializer(jsonDeserializer.handledType(), jsonDeserializer));
        List<Module> modules = providerRegistry.getInstances(Module.class).toList();
        return defaultConfig()
                .registerModule(injected)
                .registerModules(modules);
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
