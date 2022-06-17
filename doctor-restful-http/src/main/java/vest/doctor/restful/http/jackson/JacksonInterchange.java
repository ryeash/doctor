package vest.doctor.restful.http.jackson;

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
import io.netty.handler.codec.http.HttpContent;
import io.netty.handler.codec.http.HttpHeaderNames;
import io.netty.handler.codec.http.HttpHeaderValues;
import vest.doctor.Prioritized;
import vest.doctor.ProviderRegistry;
import vest.doctor.TypeInfo;
import vest.doctor.http.server.RequestContext;
import vest.doctor.http.server.Response;
import vest.doctor.http.server.ResponseBody;
import vest.doctor.reactive.Rx;
import vest.doctor.restful.http.BodyReader;
import vest.doctor.restful.http.BodyWriter;

import java.io.UncheckedIOException;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.Flow;

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
    public <T> Flow.Publisher<T> read(RequestContext requestContext, TypeInfo typeInfo) {
        String contentType = requestContext.request().header(HttpHeaderNames.CONTENT_TYPE);
        if (contentType.contains(HttpHeaderValues.APPLICATION_JSON)) {
            if (typeInfo.matches(Flow.Publisher.class, Object.class)) {
                return (Flow.Publisher<T>) internalRead(requestContext, typeInfo.getParameterTypes().get(0));
            } else {
                return (Flow.Publisher<T>) internalRead(requestContext, typeInfo);
            }
        } else {
            return null;
        }
    }

    private Flow.Publisher<?> internalRead(RequestContext ctx, TypeInfo typeInfo) {
        JavaType javaType = jacksonType(objectMapper, typeInfo);
        for (AsyncParserFactory parserFactory : parserFactories) {
            AsyncParser<?> parser = parserFactory.build(ctx, javaType);
            if (parser != null) {
                return Rx.from(ctx.request().body().flow())
                        .<HttpContent>onNext((content, subscription, subscriber) -> {
                            try {
                                subscriber.onNext(content);
                            } finally {
                                content.release();
                            }
                        })
                        .map(content -> content.content().nioBuffer())
                        .flatMapIterable(new AsyncTokenizer(objectMapper))
                        .mapAsync(parser);
            }
        }
        throw new UnsupportedOperationException("no async parser can handle this type: " + typeInfo);
    }

    @Override
    public Flow.Publisher<Response> write(RequestContext ctx, TypeInfo responseTypeInfo, Object response) {
        String accept = ctx.request().header(HttpHeaderNames.ACCEPT);
        if (accept.contains(HttpHeaderValues.APPLICATION_JSON)) {
            Response res = ctx.response();
            if (!res.headers().contains(HttpHeaderNames.CONTENT_TYPE)) {
                res.headers().set(HttpHeaderNames.CONTENT_TYPE, HttpHeaderValues.APPLICATION_JSON + ";charset=utf-8");
            }
            Rx<?> flux = responseTypeInfo.matches(Flow.Publisher.class, Object.class)
                    ? Rx.from((Flow.Publisher<?>) response)
                    : Rx.one(response);
            return flux.map(this::writeJsonBytes)
                    .map(ResponseBody::of)
                    .map(ctx.response()::body);
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
