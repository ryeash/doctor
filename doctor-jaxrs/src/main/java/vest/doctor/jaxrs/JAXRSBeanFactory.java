package vest.doctor.jaxrs;

import io.netty.handler.ssl.SslContext;
import jakarta.inject.Named;
import jakarta.inject.Singleton;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.container.ContainerRequestFilter;
import jakarta.ws.rs.container.ContainerResponseFilter;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.MessageBodyReader;
import jakarta.ws.rs.ext.MessageBodyWriter;
import org.glassfish.jersey.netty.httpserver.NettyJersey;
import org.glassfish.jersey.server.ResourceConfig;
import org.glassfish.jersey.server.ServerProperties;
import vest.doctor.Activation;
import vest.doctor.ApplicationLoader;
import vest.doctor.Configuration;
import vest.doctor.DoctorProvider;
import vest.doctor.Eager;
import vest.doctor.Factory;
import vest.doctor.ProviderRegistry;
import vest.doctor.event.EventBus;
import vest.doctor.event.ServiceStarted;

import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

@Configuration
public final class JAXRSBeanFactory implements ApplicationLoader {
    @Eager
    @Singleton
    @Factory
    @Activation(StartNettyJerseyService.class)
    public NettyJersey jerseyFactory(ProviderRegistry providerRegistry,
                                     EventBus eventBus,
                                     JAXRSConfiguration httpConfig,
                                     List<ResourceConfigCustomizer> resourceConfigCustomizers,
                                     List<ServerBootstrapCustomizer> serverBootstrapCustomizers,
                                     @Named("server") Optional<SslContext> sslContext) {
        ResourceConfig config = new ResourceConfig();
        config.setApplicationName("netty-jaxrs");
        config.property(ServerProperties.TRACING, "ALL");
        config.property(ServerProperties.TRACING_THRESHOLD, "VERBOSE");

        Stream.of(ContainerRequestFilter.class, ContainerResponseFilter.class, MessageBodyWriter.class, MessageBodyReader.class, ExceptionMapper.class)
                .flatMap(providerRegistry::getProviders)
                .distinct()
                .map(DoctorProvider::get)
                .forEach(config::register);

        providerRegistry.getProvidersWithAnnotation(Path.class)
                .map(DoctorProvider::get)
                .forEach(config::register);

        config = resourceConfigCustomizers.stream()
                .reduce(config, (c, customizer) -> customizer.customize(c), (a, b) -> b);

        NettyJersey nettyJersey = new NettyJersey(httpConfig, sslContext.orElse(null), config, serverBootstrapCustomizers);
        eventBus.publish(new ServiceStarted("netty-jersey-http", nettyJersey));
        return nettyJersey;
    }
}
