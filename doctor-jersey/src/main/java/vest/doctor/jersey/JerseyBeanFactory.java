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
                                            @Named("server") Optional<SslContext> sslContext) {
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

        config = providerRegistry.getInstances(ResourceConfigCustomizer.class)
                .reduce(config, (c, customizer) -> customizer.customize(c), (a,b) -> b);

        DoctorJerseyContainer container = new DoctorJerseyContainer(config);
        NettyHttpServer httpServer = new NettyHttpServer(
                providerRegistry,
                httpConfig,
                new JerseyChannelAdapter(httpConfig, container, providerRegistry, sslContext.isPresent()),
                sslContext.orElse(null));
        eventBus.publish(new ServiceStarted("netty-jersey-http", httpServer));
        return new JerseyServerHolder(container, httpServer);
    }
}
