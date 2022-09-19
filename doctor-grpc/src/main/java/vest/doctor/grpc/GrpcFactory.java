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
import vest.doctor.Eager;
import vest.doctor.Factory;
import vest.doctor.InjectionException;
import vest.doctor.Prioritized;
import vest.doctor.ProviderRegistry;
import vest.doctor.conf.ConfigurationFacade;
import vest.doctor.scheduled.Interval;

import java.io.File;
import java.io.IOException;
import java.security.cert.CertificateException;
import java.util.function.Consumer;

@Singleton
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
                                              ConfigurationFacade configurationFacade,
                                              HealthStatusManager healthStatusManager) throws IOException {
        ConfigurationFacade config = configurationFacade.prefix("grpc.");
        if (config.get("port") == null) {
            return new GrpcServerHandle(null);
        }
        int port = config.get("port", Integer::parseInt);
        Interval handshakeTimeout = config.get("handshakeTimeout", new Interval("5s"), Interval::new);
        Interval keepAlive = config.get("keepAlive", new Interval("2h"), Interval::new);
        Interval keepAliveTimeout = config.get("keepAliveTimeout", new Interval("20s"), Interval::new);
        Interval maxConnectionIdle = config.get("maxConnectionIdle", new Interval("2h"), Interval::new);
        Interval maxConnectionAge = config.get("maxConnectionAge", new Interval("7d"), Interval::new);
        Interval maxConnectionAgeGrace = config.get("maxConnectionAgeGrace", new Interval("15m"), Interval::new);
        Interval permitKeepAliveTime = config.get("permitKeepAliveTime", new Interval("5m"), Interval::new);
        Boolean permitKeepAliveWithoutCalls = config.get("permitKeepAliveWithoutCalls", false, Boolean::valueOf);
        Integer maxInboundMessageSize = config.get("maxInboundMessageSize", 4 * 1024 * 1024, Integer::valueOf);
        Integer maxInboundMetadataSize = config.get("maxInboundMetadataSize", 8 * 1024, Integer::valueOf);

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

        String certificate = config.get("ssl.certificate");
        if (certificate != null && !certificate.isEmpty()) {
            String privateKey = config.get("ssl.privateKey");
            serverBuilder.useTransportSecurity(new File(certificate), new File(privateKey));
        } else if (config.get("ssl.selfSigned", false, Boolean::valueOf)) {
            try {
                SelfSignedCertificate ssc = new SelfSignedCertificate();
                serverBuilder.useTransportSecurity(ssc.certificate(), ssc.privateKey());
            } catch (CertificateException e) {
                throw new InjectionException("error initializing grpc self-signed certificate", e);
            }
        }
        log.info("starting grpc server on port {}", port);
        Server build = serverBuilder.build().start();
        healthStatusManager.setStatus("", HealthCheckResponse.ServingStatus.SERVING);
        return new GrpcServerHandle(build);
    }

    private static <T> void providedList(Class<T> type, ProviderRegistry providerRegistry, Consumer<? super T> consumer) {
        providerRegistry.getInstances(type).sorted(Prioritized.COMPARATOR_ANY).forEach(consumer);
    }
}
