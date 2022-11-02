package vest.doctor.jersey;

import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.SslContextBuilder;
import io.netty.handler.ssl.util.SelfSignedCertificate;
import jakarta.inject.Singleton;
import jakarta.ws.rs.Path;
import org.glassfish.jersey.server.ResourceConfig;
import vest.doctor.ApplicationLoader;
import vest.doctor.DoctorProvider;
import vest.doctor.Eager;
import vest.doctor.Factory;
import vest.doctor.ProviderRegistry;
import vest.doctor.event.EventBus;
import vest.doctor.event.ServiceStarted;
import vest.doctor.jersey.ext.DoctorCustomValueParamProvider;
import vest.doctor.netty.NettyHttpServer;

import javax.net.ssl.SSLException;
import java.io.File;
import java.security.cert.CertificateException;

@Singleton
public final class JerseyBeanFactory implements ApplicationLoader {

    @Eager
    @Singleton
    @Factory
    public JerseyServerHolder jerseyFactory(ProviderRegistry providerRegistry,
                                            EventBus eventBus,
                                            JerseyHttpConfiguration httpConfig) {
        if (httpConfig.bindAddresses().isEmpty()) {
            return new JerseyServerHolder(null, null);
        }

        ResourceConfig config = new ResourceConfig();

        config.register(new DoctorCustomValueParamProvider(providerRegistry));
        config.register(new DoctorBinder(providerRegistry));

        providerRegistry.getProvidersWithAnnotation(Path.class)
                .map(DoctorProvider::type)
                .forEach(config::register);

        SslContext sslContext = null;
        try {
            if (httpConfig.sslSelfSigned().orElse(false)) {
                SelfSignedCertificate ssc = new SelfSignedCertificate();
                sslContext = SslContextBuilder.forServer(ssc.certificate(), ssc.privateKey()).build();
            } else if (httpConfig.sslCertificate().isPresent()) {
                String cert = httpConfig.sslCertificate().orElseThrow();
                String key = httpConfig.sslPrivateKey().orElseThrow();
                sslContext = SslContextBuilder.forServer(new File(cert), new File(key)).build();
            }
        } catch (CertificateException | SSLException e) {
            throw new RuntimeException("error initializing ssl context", e);
        }

        config = providerRegistry.getProviders(ResourceConfigCustomizer.class)
                .map(DoctorProvider::get)
                .reduce(config, (rc, c) -> c.customize(rc), (a, b) -> b);

        DoctorJerseyContainer container = new DoctorJerseyContainer(config);
        NettyHttpServer httpServer = new NettyHttpServer(
                providerRegistry,
                httpConfig,
                new JerseyChannelAdapter(httpConfig, container, providerRegistry, sslContext != null),
                sslContext);
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
