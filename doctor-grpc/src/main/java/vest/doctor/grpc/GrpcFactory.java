package vest.doctor.grpc;

import io.grpc.BinaryLog;
import io.grpc.BindableService;
import io.grpc.HandlerRegistry;
import io.grpc.Server;
import io.grpc.ServerBuilder;
import io.grpc.ServerInterceptor;
import io.grpc.ServerServiceDefinition;
import io.grpc.ServerStreamTracer;
import io.grpc.ServerTransportFilter;
import io.grpc.health.v1.HealthCheckResponse;
import io.grpc.netty.shaded.io.netty.handler.ssl.util.SelfSignedCertificate;
import io.grpc.protobuf.services.HealthStatusManager;
import jakarta.inject.Singleton;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import vest.doctor.Configuration;
import vest.doctor.Eager;
import vest.doctor.Factory;
import vest.doctor.InjectionException;
import vest.doctor.Prioritized;
import vest.doctor.ProviderRegistry;
import vest.doctor.scheduled.Interval;

import java.io.File;
import java.io.IOException;
import java.security.cert.CertificateException;
import java.util.function.Consumer;

@Configuration
public final class GrpcFactory {

    private static final Logger log = LoggerFactory.getLogger(GrpcFactory.class);

    @Factory
    @Singleton
    public HealthStatusManager healthStatusManagerFactory() {
        return new HealthStatusManager();
    }

    @Eager
    @Factory
    @Singleton
    public GrpcServerHandle grpcServerFactory(ProviderRegistry providerRegistry,
                                              HealthStatusManager healthStatusManager,
                                              GrpcConfig grpcConfig) throws IOException {
        if (grpcConfig.port().isEmpty()) {
            log.warn("grpc.port not set, not starting grpc server");
            return new GrpcServerHandle(null);
        }
        int port = grpcConfig.port().get();
        Interval handshakeTimeout = grpcConfig.handshakeTimeout().orElse(new Interval("5s"));
        Interval keepAlive = grpcConfig.keepAlive().orElse(new Interval("2h"));
        Interval keepAliveTimeout = grpcConfig.keepAliveTimeout().orElse(new Interval("20s"));
        Interval maxConnectionIdle = grpcConfig.maxConnectionIdle().orElse(new Interval("2h"));
        Interval maxConnectionAge = grpcConfig.maxConnectionAge().orElse(new Interval("7d"));
        Interval maxConnectionAgeGrace = grpcConfig.maxConnectionAgeGrace().orElse(new Interval("15m"));
        Interval permitKeepAliveTime = grpcConfig.permitKeepAliveTime().orElse(new Interval("5m"));
        Boolean permitKeepAliveWithoutCalls = grpcConfig.permitKeepAliveWithoutCalls().orElse(false);
        Integer maxInboundMessageSize = grpcConfig.maxInboundMessageSize().orElse(4 * 1024 * 1024);
        Integer maxInboundMetadataSize = grpcConfig.maxInboundMetadataSize().orElse(8 * 1024);

        ServerBuilder<?> serverBuilder = ServerBuilder.forPort(port)
                .handshakeTimeout(handshakeTimeout.getMagnitude(), handshakeTimeout.getUnit())
                .keepAliveTime(keepAlive.getMagnitude(), keepAlive.getUnit())
                .keepAliveTimeout(keepAliveTimeout.getMagnitude(), keepAliveTimeout.getUnit())
                .maxConnectionIdle(maxConnectionIdle.getMagnitude(), maxConnectionIdle.getUnit())
                .maxConnectionAge(maxConnectionAge.getMagnitude(), maxConnectionAge.getUnit())
                .maxConnectionAgeGrace(maxConnectionAgeGrace.getMagnitude(), maxConnectionAgeGrace.getUnit())
                .permitKeepAliveTime(permitKeepAliveTime.getMagnitude(), permitKeepAliveTime.getUnit())
                .permitKeepAliveWithoutCalls(permitKeepAliveWithoutCalls)
                .maxInboundMessageSize(maxInboundMessageSize)
                .maxInboundMetadataSize(maxInboundMetadataSize)
                .addService(healthStatusManager.getHealthService());

        providedList(BindableService.class, providerRegistry, serverBuilder::addService);
        providedList(ServerServiceDefinition.class, providerRegistry, serverBuilder::addService);
        providedList(ServerInterceptor.class, providerRegistry, serverBuilder::intercept);
        providedList(ServerTransportFilter.class, providerRegistry, serverBuilder::addTransportFilter);
        providedList(ServerStreamTracer.Factory.class, providerRegistry, serverBuilder::addStreamTracerFactory);

        providerRegistry.getInstanceOpt(HandlerRegistry.class).ifPresent(serverBuilder::fallbackHandlerRegistry);
        providerRegistry.getInstanceOpt(BinaryLog.class).ifPresent(serverBuilder::setBinaryLog);

        if (grpcConfig.sslCertificate().isPresent()) {
            serverBuilder.useTransportSecurity(
                    new File(grpcConfig.sslCertificate().get()),
                    new File(grpcConfig.sslPrivateKey().orElseThrow(() -> new IllegalStateException("missing private key config"))));
        } else if (grpcConfig.sslSelfSigned().orElse(false)) {
            try {
                SelfSignedCertificate ssc = new SelfSignedCertificate();
                serverBuilder.useTransportSecurity(ssc.certificate(), ssc.privateKey());
            } catch (CertificateException e) {
                throw new InjectionException("error initializing grpc self-signed certificate", e);
            }
        }
        log.info("starting grpc server on port {}", port);
        Server build = serverBuilder.build().start();
        healthStatusManager.setStatus("grpc-" + port, HealthCheckResponse.ServingStatus.SERVING);
        return new GrpcServerHandle(build);
    }

    private static <T> void providedList(Class<T> type, ProviderRegistry providerRegistry, Consumer<? super T> consumer) {
        providerRegistry.getInstances(type).sorted(Prioritized.COMPARATOR_ANY).forEach(consumer);
    }
}
