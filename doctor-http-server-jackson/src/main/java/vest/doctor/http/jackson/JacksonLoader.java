package vest.doctor.http.jackson;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.inject.Provider;
import vest.doctor.AdHocProvider;
import vest.doctor.AppLoader;
import vest.doctor.ProviderRegistry;
import vest.doctor.http.server.rest.BodyReader;
import vest.doctor.http.server.rest.BodyWriter;

import java.util.List;

public class JacksonLoader implements AppLoader {

    @Override
    public void load(ProviderRegistry providerRegistry) {
        JacksonInterchange jacksonInterchange = providerRegistry.getProviderOpt(ObjectMapper.class)
                .map(Provider::get)
                .map(JacksonInterchange::new)
                .orElseGet(() -> new JacksonInterchange(JacksonInterchange.defaultConfig()));

        providerRegistry.register(new AdHocProvider<>(JacksonInterchange.class, jacksonInterchange, null, List.of(JacksonInterchange.class, BodyReader.class, BodyWriter.class)));
    }
}
