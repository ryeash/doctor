package vest.doctor.jaxrs;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.jaxrs.json.JacksonJaxbJsonProvider;
import vest.doctor.ProviderRegistry;

import javax.inject.Provider;
import javax.ws.rs.Path;
import javax.ws.rs.container.ContainerRequestFilter;
import javax.ws.rs.container.ContainerResponseFilter;
import javax.ws.rs.core.Application;
import javax.ws.rs.ext.ContextResolver;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.InterceptorContext;
import javax.ws.rs.ext.MessageBodyReader;
import javax.ws.rs.ext.MessageBodyWriter;
import javax.ws.rs.ext.ParamConverter;
import javax.ws.rs.ext.ParamConverterProvider;
import javax.ws.rs.ext.ReaderInterceptor;
import javax.ws.rs.ext.WriterInterceptor;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class RESTEasyAppConfig extends Application {

    private static final List<Class<?>> JAX_RS_TYPES = Arrays.asList(
            ContainerRequestFilter.class,
            ContainerResponseFilter.class,
            ExceptionMapper.class,
            MessageBodyWriter.class,
            MessageBodyReader.class,
            ReaderInterceptor.class,
            WriterInterceptor.class,
            InterceptorContext.class,
            Provider.class,
            ParamConverter.class,
            ParamConverterProvider.class,
            ContextResolver.class);

    @Override
    public Set<Class<?>> getClasses() {
        return Collections.emptySet();
    }

    @Override
    public Set<Object> getSingletons() {
        Set<Object> singletons = new HashSet<>();
        ProviderRegistry providerRegistry = JAXRSServer.providerRegistry;

        ObjectMapper mapper = providerRegistry.getProviderOpt(ObjectMapper.class)
                .map(Provider::get)
                .orElseGet(() -> new ObjectMapper().setSerializationInclusion(JsonInclude.Include.NON_EMPTY)
                        .configure(JsonGenerator.Feature.IGNORE_UNKNOWN, true)
                        .configure(DeserializationFeature.FAIL_ON_NULL_FOR_PRIMITIVES, false)
                        .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
                        .configure(DeserializationFeature.ACCEPT_SINGLE_VALUE_AS_ARRAY, true)
                        .disable(SerializationFeature.FAIL_ON_EMPTY_BEANS)
                        .setDefaultMergeable(true)
                        .registerModules(ObjectMapper.findModules()));

        singletons.add(new JacksonJaxbJsonProvider(mapper, JacksonJaxbJsonProvider.DEFAULT_ANNOTATIONS));
        singletons.add(new GZipDecoder());

        JAX_RS_TYPES.stream()
                .flatMap(providerRegistry::getProviders)
                .map(Provider::get)
                .distinct()
                .forEach(singletons::add);

        providerRegistry.getProvidersWithAnnotation(Path.class)
                .map(Provider::get)
                .forEach(singletons::add);
        return singletons;
    }

    @Override
    public Map<String, Object> getProperties() {
        Map<String, Object> props = new HashMap<>();
        for (String propertyName : JAXRSServer.providerRegistry.configuration().propertyNames()) {
            if (propertyName.startsWith("resteasy")) {
                props.put(propertyName, JAXRSServer.providerRegistry.configuration().get(propertyName));
            }
        }
        return props;
    }
}
