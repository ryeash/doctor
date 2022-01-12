package vest.doctor.http.jackson;

import com.fasterxml.jackson.databind.ObjectMapper;
import vest.doctor.AdHocProvider;
import vest.doctor.ApplicationLoader;
import vest.doctor.ProviderRegistry;
import vest.doctor.http.server.rest.BodyReader;
import vest.doctor.http.server.rest.BodyWriter;

import java.util.List;

public class JacksonLoader implements ApplicationLoader {

    @Override
    public void stage3(ProviderRegistry providerRegistry) {
        JacksonInterchange jacksonInterchange;
        if (providerRegistry.hasProvider(ObjectMapper.class)) {
            ObjectMapper objectMapper = providerRegistry.getInstance(ObjectMapper.class);
            jacksonInterchange = new JacksonInterchange(objectMapper);
        } else {
            ObjectMapper objectMapper = JacksonInterchange.defaultConfig(providerRegistry);
            providerRegistry.register(new AdHocProvider<>(ObjectMapper.class, objectMapper, null));
            jacksonInterchange = new JacksonInterchange(objectMapper);
        }
        providerRegistry.register(new AdHocProvider<>(JacksonInterchange.class, jacksonInterchange, null, List.of(JacksonInterchange.class, BodyReader.class, BodyWriter.class)));
    }
}
