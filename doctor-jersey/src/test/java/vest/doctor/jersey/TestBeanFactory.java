package vest.doctor.jersey;

import jakarta.inject.Singleton;
import vest.doctor.Configuration;
import vest.doctor.ExecutorBuilder;
import vest.doctor.Factory;

import java.util.concurrent.ExecutorService;

@Configuration
public class TestBeanFactory {

    @Factory
    @Singleton
    public ExecutorService defaultExecutorService(){
        return ExecutorBuilder.start().standard();
    }
}
