package vest.doctor.jersey;

import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.SslContextBuilder;
import io.netty.handler.ssl.util.SelfSignedCertificate;
import jakarta.inject.Named;
import jakarta.inject.Singleton;
import jakarta.ws.rs.Path;
import org.glassfish.jersey.server.ResourceConfig;
import vest.doctor.Activation;
import vest.doctor.ApplicationLoader;
import vest.doctor.Configuration;
import vest.doctor.DoctorProvider;
import vest.doctor.Eager;
import vest.doctor.Factory;
import vest.doctor.ProviderRegistry;
import vest.doctor.TypeInfo;
import vest.doctor.event.EventBus;
import vest.doctor.event.ServiceStarted;
import vest.doctor.jersey.ext.DoctorCustomValueParamProvider;
import vest.doctor.netty.NettyHttpServer;

import javax.net.ssl.SSLException;
import java.security.cert.CertificateException;
import java.util.List;
import java.util.Optional;

@Configuration
public final class JerseyBeanFactory implements ApplicationLoader {

    @Singleton
    @Factory
    @Activation(SelfSignedSSLActivation.class)
    @Named("server")
    public SslContext selfSignedSslFactory() throws CertificateException, SSLException {
        SelfSignedCertificate ssc = new SelfSignedCertificate();
        return SslContextBuilder.forServer(ssc.certificate(), ssc.privateKey()).build();
    }

    @Eager
    @Singleton
    @Factory
    public JerseyServerHolder jerseyFactory(ProviderRegistry providerRegistry,
                                            EventBus eventBus,
                                            JerseyHttpConfiguration httpConfig,
                                            @Named("server") Optional<SslContext> sslContext,
                                            List<ResourceConfigCustomizer> resourceConfigCustomizers) {
        if (httpConfig.bindAddresses().isEmpty()) {
            return new JerseyServerHolder(null, null);
        }

        ResourceConfig config = new ResourceConfig();
        config.register(new DoctorCustomValueParamProvider(providerRegistry));
        config.register(new DoctorBinder(providerRegistry));

        providerRegistry.getProvidersWithAnnotation(Path.class)
                .map(DoctorProvider::typeInfo)
                .map(TypeInfo::getRawType)
                .forEach(config::register);

        for (ResourceConfigCustomizer resourceConfigCustomizer : resourceConfigCustomizers) {
            config = resourceConfigCustomizer.customize(config);
        }

        DoctorJerseyContainer container = new DoctorJerseyContainer(config);
        NettyHttpServer httpServer = new NettyHttpServer(
                providerRegistry,
                httpConfig,
                new JerseyChannelAdapter(httpConfig, container, providerRegistry, sslContext.isPresent()),
                sslContext.orElse(null));
        eventBus.publish(new ServiceStarted("netty-jersey-http", httpServer));
        return new JerseyServerHolder(container, httpServer);
    }

    public record JerseyServerHolder(DoctorJerseyContainer container,
                                     NettyHttpServer httpServer) implements AutoCloseable {

        @Override
        public void close() throws Exception {
            if (container != null) {
                container.close();
            }
            if (httpServer != null) {
                httpServer.close();
            }
        }
    }
}
