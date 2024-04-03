package vest.doctor.jaxrs;

import jakarta.inject.Singleton;
import vest.doctor.Configuration;
import vest.doctor.ExecutorBuilder;
import vest.doctor.Factory;

import java.util.concurrent.ExecutorService;

@JAXRSFeature
@Configuration
public class AppConfig {
    @Factory
    @Singleton
    public ExecutorService executorService() {
        return ExecutorBuilder.start().standard();
    }
}
